/*
 * Copyright 2014 - 2016 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.agrona.concurrent.broadcast;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;

import java.util.concurrent.atomic.AtomicLong;

import static uk.co.real_logic.agrona.BitUtil.align;
import static uk.co.real_logic.agrona.UnsafeAccess.UNSAFE;
import static uk.co.real_logic.agrona.concurrent.broadcast.BroadcastBufferDescriptor.*;
import static uk.co.real_logic.agrona.concurrent.broadcast.RecordDescriptor.*;

/**
 * Receive messages broadcast from a {@link BroadcastTransmitter} via an underlying buffer. Receivers can join
 * a transmission stream at any point by consuming the latest message at the point of joining and forward.
 * <p>
 * If a Receiver cannot keep up with the transmission stream then loss will be experienced. Loss is not an
 * error condition.
 * <p>
 * <b>Note:</b> Each Receiver is not threadsafe but there can be zero or many receivers to a transmission stream.
 */
public class BroadcastReceiver
{
    private long cursor = 0;
    private long nextRecord = 0;
    private int recordOffset = 0;

    private final int capacity;
    private final int tailIntentCounterIndex;
    private final int tailCounterIndex;

    private final int latestCounterIndex;
    private final AtomicBuffer buffer;
    private final AtomicLong lappedCount = new AtomicLong();

    /**
     * Construct a new broadcast receiver based on an underlying {@link AtomicBuffer}.
     * The underlying buffer must a power of 2 in size plus sufficient space
     * for the {@link BroadcastBufferDescriptor#TRAILER_LENGTH}.
     *
     * @param buffer via which messages will be exchanged.
     * @throws IllegalStateException if the buffer capacity is not a power of 2
     *                               plus {@link BroadcastBufferDescriptor#TRAILER_LENGTH} in capacity.
     */
    public BroadcastReceiver(final AtomicBuffer buffer)
    {
        this.buffer = buffer;
        this.capacity = buffer.capacity() - TRAILER_LENGTH;

        checkCapacity(capacity);
        buffer.verifyAlignment();

        this.tailIntentCounterIndex = capacity + TAIL_INTENT_COUNTER_OFFSET;
        this.tailCounterIndex = capacity + TAIL_COUNTER_OFFSET;
        this.latestCounterIndex = capacity + LATEST_COUNTER_OFFSET;
    }

    /**
     * Get the capacity of the underlying broadcast buffer.
     *
     * @return the capacity of the underlying broadcast buffer.
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * Get the number of times the transmitter has lapped this receiver around the buffer. On each lap
     * as least a buffer's worth of loss will be experienced.
     * <p>
     * <b>Note:</b> This method is threadsafe for calling from an external monitoring thread.
     *
     * @return the capacity of the underlying broadcast buffer.
     */
    public long lappedCount()
    {
        return lappedCount.get();
    }

    /**
     * Type of the message received.
     *
     * @return typeId of the message received.
     */
    public int typeId()
    {
        return buffer.getInt(typeOffset(recordOffset));
    }

    /**
     * The offset for the beginning of the next message in the transmission stream.
     *
     * @return offset for the beginning of the next message in the transmission stream.
     */
    public int offset()
    {
        return msgOffset(recordOffset);
    }

    /**
     * The length of the next message in the transmission stream.
     *
     * @return length of the next message in the transmission stream.
     */
    public int length()
    {
        return buffer.getInt(lengthOffset(recordOffset)) - HEADER_LENGTH;
    }

    /**
     * The underlying buffer containing the broadcast message stream.
     *
     * @return the underlying buffer containing the broadcast message stream.
     */
    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    /**
     * Non-blocking receive of next message from the transmission stream.
     * <p>
     * If loss has occurred then {@link #lappedCount()} will be incremented.
     *
     * @return true if transmission is available with {@link #offset()}, {@link #length()} and {@link #typeId()}
     * set for the next message to be consumed. If no transmission is available then false.
     */
    public boolean receiveNext()
    {
        boolean isAvailable = false;
        final AtomicBuffer buffer = this.buffer;
        final long tail = buffer.getLongVolatile(tailCounterIndex);
        long cursor = this.nextRecord;

        if (tail > cursor)
        {
            int recordOffset = (int)cursor & (capacity - 1);

            if (!validate(cursor))
            {
                lappedCount.lazySet(lappedCount.get() + 1);

                cursor = buffer.getLong(latestCounterIndex);
                recordOffset = (int)cursor & (capacity - 1);
            }

            this.cursor = cursor;
            nextRecord = cursor + align(buffer.getInt(lengthOffset(recordOffset)), RECORD_ALIGNMENT);

            if (PADDING_MSG_TYPE_ID == buffer.getInt(typeOffset(recordOffset)))
            {
                recordOffset = 0;
                this.cursor = nextRecord;
                nextRecord += align(buffer.getInt(lengthOffset(recordOffset)), RECORD_ALIGNMENT);
            }

            this.recordOffset = recordOffset;
            isAvailable = true;
        }

        return isAvailable;
    }

    /**
     * Validate that the current received record is still valid and has not been overwritten.
     * <p>
     * If the receiver is not consuming messages fast enough to keep up with the transmitter then loss
     * can be experienced resulting in messages being overwritten thus making them no longer valid.
     *
     * @return true if still valid otherwise false.
     */
    public boolean validate()
    {
        UNSAFE.loadFence(); // Needed to prevent older loads being moved ahead of the validate, see j.u.c.StampedLock.

        return validate(cursor);
    }

    private boolean validate(final long cursor)
    {
        return (cursor + capacity) > buffer.getLongVolatile(tailIntentCounterIndex);
    }
}
