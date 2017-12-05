/*
 * Copyright 2014-2017 Real Logic Ltd.
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
package org.agrona.concurrent.broadcast.fixed;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;

import java.util.concurrent.atomic.AtomicLong;

import static org.agrona.UnsafeAccess.UNSAFE;
import static org.agrona.concurrent.broadcast.fixed.BroadcastBufferDescriptor.*;
import static org.agrona.concurrent.broadcast.fixed.RecordDescriptor.*;

/**
 * Receive transmissions broadcast from a {@link BroadcastTransmitter} via an underlying buffer. Receivers can join
 * a transmission stream at any point by consuming the latest transmission at the point of joining and forward.
 * <p>
 * If a Receiver cannot keep up with the transmission stream then loss will be experienced. Loss is not an
 * error condition.
 * <p>
 * <b>Note:</b> Each Receiver is not threadsafe but there can be zero or many receivers to a transmission stream.
 */
public final class BroadcastReceiver
{

    public enum ReceiveReturnType
    {
        LOSS,
        ANY_AVAILABLE,
        NOT_AVAILABLE
    }

    private long cursor = 0;
    private long nextRecord = 0;
    private int recordOffset = 0;

    private final int capacity;
    private final int mask;
    private final AtomicBuffer buffer;
    private long lost = 0;
    private final AtomicLong lostTransmissions = new AtomicLong();
    private final int recordSize;
    private final int latestCounterIndex;

    /**
     * Construct a new broadcast receiver based on an underlying {@link AtomicBuffer}.
     * The underlying buffer must a power of 2 multiple of the recordSize
     * written on {@link BroadcastBufferDescriptor#RECORD_SIZE_OFFSET} of the buffer.
     *
     * @param buffer via which transmissions will be exchanged.
     * @throws IllegalStateException if the buffer capacity is not a power of 2 multiple of the recordSize
     *                               read on {@link BroadcastBufferDescriptor#RECORD_SIZE_OFFSET} of the buffer
     */
    public BroadcastReceiver(final AtomicBuffer buffer)
    {
        this.buffer = buffer;
        this.capacity = buffer.capacity() - TRAILER_LENGTH;
        this.recordSize = buffer.getIntVolatile(this.capacity + RECORD_SIZE_OFFSET);
        checkRecordSize(recordSize);
        checkCapacity(calculateAvailableMessageLength(recordSize), capacity);
        buffer.verifyAlignment();
        final int transmissions = capacity / recordSize;
        this.mask = transmissions - 1;
        this.latestCounterIndex = this.capacity + LATEST_COUNTER_OFFSET;
    }

    /**
     * @return The max size in bytes of a transmission (including any message header).
     */
    public int recordSize()
    {
        return recordSize;
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
     * The number of transmissions lost by this receiver.<p>
     * This method's accuracy is subject to concurrent modifications happening, but it is safe to be
     * called by any threads.
     *
     * @return the number of transmissions lost by this receiver
     */
    public long lostTransmissions()
    {
        return this.lostTransmissions.get();
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
     * The offset for the beginning of the next transmission in the transmission stream.
     *
     * @return offset for the beginning of the next transmission in the transmission stream.
     */
    public int offset()
    {
        return msgOffset(recordOffset);
    }

    /**
     * The length of the next transmission in the transmission stream.
     *
     * @return length of the next transmission in the transmission stream.
     */
    public int length()
    {
        return buffer.getInt(lengthOffset(recordOffset));
    }

    /**
     * The underlying buffer containing the broadcast transmission stream.
     *
     * @return the underlying buffer containing the broadcast transmission stream.
     */
    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    /**
     * Non-blocking receive of next transmission from the transmission stream.
     * <p>
     * If loss has occurred then {@link #lostTransmissions()} will be incremented.
     *
     * @return {@link ReceiveReturnType#ANY_AVAILABLE} if a transmission is available with {@link #offset()}, {@link #length()} and {@link #typeId()}
     * set for the next transmission to be consumed. If no transmission is available {@link ReceiveReturnType#NOT_AVAILABLE} or {@link ReceiveReturnType#LOSS} if loss is experienced.
     */
    public ReceiveReturnType receiveNext()
    {
        final AtomicBuffer buffer = this.buffer;
        final long cursor = this.nextRecord;
        final int recordOffset = calculatedRecordOffset(cursor, mask, recordSize);
        final int sequenceIndicatorOffset = sequenceIndicatorOffset(recordOffset);
        final long sequence = buffer.getLongVolatile(sequenceIndicatorOffset);
        final long expectedSequence = cursor + 1;
        if (sequence == expectedSequence)
        {
            this.recordOffset = recordOffset;
            this.cursor = cursor;
            this.nextRecord = expectedSequence;
            return ReceiveReturnType.ANY_AVAILABLE;
        }
        else
        {
            if (sequence > expectedSequence)
            {
                chase(cursor, sequence);
                return ReceiveReturnType.LOSS;
            }
            return ReceiveReturnType.NOT_AVAILABLE;
        }
    }

    /**
     * It allows this receiver to keep up with the transmission by moving right after the last transmitted transmission.
     * <p>
     * To continue receive any new transmission is necessary to use {@link #receiveNext()}:
     * this method will not initialize {@link #offset()} with any valid value.
     *
     * @return the number of transmissions lost during this operation
     */
    public long keepUpWithTrasmitter()
    {
        final long sequenceToChase = Math.max(0, buffer.getLongVolatile(latestCounterIndex));
        final long lostWhileChasing = sequenceToChase - this.nextRecord;
        if (lostWhileChasing == 0)
        {
            return 0;
        }
        this.nextRecord = sequenceToChase;
        this.lost += lostWhileChasing;
        this.lostTransmissions.lazySet(this.lost);
        return lostWhileChasing;
    }

    /**
     * It tries to chase the transmitter using just the already read sequence.
     */
    private void chase(final long cursor, final long sequence)
    {
        //if the sequence is claimed -> chasing the previous is ok
        //if the sequence is committed -> chasing the current is ok
        final long previousSequence = sequence - 1;
        //this is a partial view of the real lost giving just using the value of the sequence
        final long estimatedLost = previousSequence - cursor;
        this.lost += estimatedLost;
        this.lostTransmissions.lazySet(this.lost);
        this.nextRecord = previousSequence;
    }

    /**
     * Validate that the current received record is still valid and has not been overwritten.
     * <p>
     * If the receiver is not consuming transmissions fast enough to keep up with the transmitter then loss
     * can be experienced resulting in transmissions being overwritten thus making them no longer valid.
     *
     * @return true if still valid otherwise false.
     */
    public boolean validate()
    {
        UNSAFE.loadFence();
        final int sequenceIndicatorOffset = sequenceIndicatorOffset(recordOffset);
        final long sequence = buffer.getLongVolatile(sequenceIndicatorOffset);
        final long expectedCommittedSequence = cursor + 1;
        if (sequence == expectedCommittedSequence)
        {
            return true;
        }
        else
        {
            chase(cursor, sequence);
            return false;
        }
    }
}
