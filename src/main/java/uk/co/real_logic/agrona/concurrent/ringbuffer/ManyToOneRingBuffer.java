/*
 * Copyright 2014 Real Logic Ltd.
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
package uk.co.real_logic.agrona.concurrent.ringbuffer;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.UnsafeAccess;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.MessageHandler;

import static uk.co.real_logic.agrona.BitUtil.align;
import static uk.co.real_logic.agrona.concurrent.ringbuffer.RecordDescriptor.*;
import static uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor.checkCapacity;

/**
 * A ring-buffer that supports the exchange of messages from many producers to a single consumer.
 */
public class ManyToOneRingBuffer implements RingBuffer
{
    /**
     * Record type is padding to prevent fragmentation in the buffer.
     */
    public static final int PADDING_MSG_TYPE_ID = -1;

    /**
     * Buffer has insufficient capacity to record a message.
     */
    public static final int INSUFFICIENT_CAPACITY = -2;

    private final AtomicBuffer buffer;
    private final int capacity;
    private final int mask;
    private final int maxMsgLength;
    private final int tailCounterIndex;
    private final int headCounterIndex;
    private final int correlationIdCounterIndex;
    private final int consumerHeartbeatIndex;

    /**
     * Construct a new {@link RingBuffer} based on an underlying {@link AtomicBuffer}.
     * The underlying buffer must a power of 2 in size plus sufficient space
     * for the {@link RingBufferDescriptor#TRAILER_LENGTH}.
     *
     * @param buffer via which events will be exchanged.
     * @throws IllegalStateException if the buffer capacity is not a power of 2
     *                               plus {@link RingBufferDescriptor#TRAILER_LENGTH} in capacity.
     */
    public ManyToOneRingBuffer(final AtomicBuffer buffer)
    {
        this.buffer = buffer;
        capacity = buffer.capacity() - RingBufferDescriptor.TRAILER_LENGTH;

        checkCapacity(capacity);
        buffer.verifyAlignment();

        mask = capacity - 1;
        maxMsgLength = capacity / 8;
        tailCounterIndex = capacity + RingBufferDescriptor.TAIL_COUNTER_OFFSET;
        headCounterIndex = capacity + RingBufferDescriptor.HEAD_COUNTER_OFFSET;
        correlationIdCounterIndex = capacity + RingBufferDescriptor.CORRELATION_COUNTER_OFFSET;
        consumerHeartbeatIndex = capacity + RingBufferDescriptor.CONSUMER_HEARTBEAT_OFFSET;
    }

    /**
     * {@inheritDoc}
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * {@inheritDoc}
     */
    public boolean write(final int msgTypeId, final DirectBuffer srcBuffer, final int srcIndex, final int length)
    {
        checkTypeId(msgTypeId);
        checkMsgLength(length);

        boolean isSuccessful = false;

        final AtomicBuffer buffer = this.buffer;
        final int recordLength = length + HEADER_LENGTH;
        final int requiredCapacity = align(recordLength, ALIGNMENT);
        final int recordIndex = claimCapacity(buffer, requiredCapacity);

        if (INSUFFICIENT_CAPACITY != recordIndex)
        {
            buffer.putInt(lengthOffset(recordIndex), -recordLength);
            UnsafeAccess.UNSAFE.storeFence();

            buffer.putBytes(encodedMsgOffset(recordIndex), srcBuffer, srcIndex, length);

            buffer.putInt(typeOffset(recordIndex), msgTypeId);
            buffer.putIntOrdered(lengthOffset(recordIndex), recordLength);

            isSuccessful = true;
        }

        return isSuccessful;
    }

    /**
     * {@inheritDoc}
     */
    public int read(final MessageHandler handler)
    {
        return read(handler, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    public int read(final MessageHandler handler, final int messageCountLimit)
    {
        int messagesRead = 0;

        final AtomicBuffer buffer = this.buffer;
        final long tail = buffer.getLongVolatile(tailCounterIndex);
        final long head = buffer.getLongVolatile(headCounterIndex);
        final int available = (int)(tail - head);

        if (available > 0)
        {
            int bytesRead = 0;

            final int headIndex = (int)head & mask;
            final int contiguousBlockLength = Math.min(available, capacity - headIndex);

            try
            {
                while ((bytesRead < contiguousBlockLength) && (messagesRead < messageCountLimit))
                {
                    final int recordIndex = headIndex + bytesRead;
                    final int recordLength = buffer.getIntVolatile(lengthOffset(recordIndex));
                    if (recordLength <= 0)
                    {
                        break;
                    }

                    bytesRead += align(recordLength, ALIGNMENT);

                    final int typeId = buffer.getInt(typeOffset(recordIndex));
                    if (PADDING_MSG_TYPE_ID == typeId)
                    {
                        continue;
                    }

                    ++messagesRead;
                    handler.onMessage(typeId, buffer, encodedMsgOffset(recordIndex), recordLength - HEADER_LENGTH);
                }
            }
            finally
            {
                buffer.setMemory(headIndex, bytesRead, (byte)0);
                buffer.putLongOrdered(headCounterIndex, head + bytesRead);
            }
        }

        return messagesRead;
    }

    /**
     * {@inheritDoc}
     */
    public int maxMsgLength()
    {
        return maxMsgLength;
    }

    /**
     * {@inheritDoc}
     */
    public long nextCorrelationId()
    {
        return buffer.getAndAddLong(correlationIdCounterIndex, 1);
    }

    /**
     * {@inheritDoc}
     */
    public AtomicBuffer buffer()
    {
        return buffer;
    }

    /**
     * {@inheritDoc}
     */
    public void consumerHeartbeatTime(final long time)
    {
        buffer.putLongOrdered(consumerHeartbeatIndex, time);
    }

    /**
     * {@inheritDoc}
     */
    public long consumerHeartbeatTime()
    {
        return buffer.getLongVolatile(consumerHeartbeatIndex);
    }

    private void checkMsgLength(final int length)
    {
        if (length > maxMsgLength)
        {
            final String msg = String.format("encoded message exceeds maxMsgLength of %d, length=%d", maxMsgLength, length);

            throw new IllegalArgumentException(msg);
        }
    }

    private int claimCapacity(final AtomicBuffer buffer, final int requiredCapacity)
    {
        final int capacity = this.capacity;
        final long head = buffer.getLongVolatile(headCounterIndex);
        final int headIndex = (int)head & mask;

        long tail;
        int tailIndex;
        int padding;
        do
        {
            tail = buffer.getLongVolatile(tailCounterIndex);
            final int availableCapacity = capacity - (int)(tail - head);

            if (requiredCapacity > availableCapacity)
            {
                return INSUFFICIENT_CAPACITY;
            }

            padding = 0;
            tailIndex = (int)tail & mask;
            final int bufferEndLength = capacity - tailIndex;

            if (requiredCapacity > bufferEndLength)
            {
                if (requiredCapacity > headIndex)
                {
                    return INSUFFICIENT_CAPACITY;
                }

                padding = bufferEndLength;
            }
        }
        while (!buffer.compareAndSetLong(tailCounterIndex, tail, tail + requiredCapacity + padding));

        if (0 != padding)
        {
            buffer.putInt(lengthOffset(tailIndex), -padding);
            UnsafeAccess.UNSAFE.storeFence();

            buffer.putInt(typeOffset(tailIndex), PADDING_MSG_TYPE_ID);
            buffer.putIntOrdered(lengthOffset(tailIndex), padding);

            tailIndex = 0;
        }

        return tailIndex;
    }
}
