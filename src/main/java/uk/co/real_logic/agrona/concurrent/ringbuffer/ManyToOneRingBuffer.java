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

    private final int capacity;
    private final int mask;
    private final int maxMsgLength;
    private final int tailPositionIndex;
    private final int headCachePositionIndex;
    private final int headPositionIndex;
    private final int correlationIdCounterIndex;
    private final int consumerHeartbeatIndex;
    private final AtomicBuffer buffer;

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
        tailPositionIndex = capacity + RingBufferDescriptor.TAIL_POSITION_OFFSET;
        headCachePositionIndex = capacity + RingBufferDescriptor.HEAD_CACHE_POSITION_OFFSET;
        headPositionIndex = capacity + RingBufferDescriptor.HEAD_POSITION_OFFSET;
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
            buffer.putLongOrdered(recordIndex, makeHeader(-recordLength, msgTypeId));
            buffer.putBytes(encodedMsgOffset(recordIndex), srcBuffer, srcIndex, length);
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
        final long head = buffer.getLong(headPositionIndex);

        int bytesRead = 0;

        final int headIndex = (int)head & mask;
        final int contiguousBlockLength = capacity - headIndex;

        try
        {
            while ((bytesRead < contiguousBlockLength) && (messagesRead < messageCountLimit))
            {
                final int recordIndex = headIndex + bytesRead;
                final long header = buffer.getLongVolatile(recordIndex);

                final int recordLength = recordLength(header);
                if (recordLength <= 0)
                {
                    break;
                }

                bytesRead += align(recordLength, ALIGNMENT);

                final int messageTypeId = messageTypeId(header);
                if (PADDING_MSG_TYPE_ID == messageTypeId)
                {
                    continue;
                }

                ++messagesRead;
                handler.onMessage(messageTypeId, buffer, recordIndex + HEADER_LENGTH, recordLength - HEADER_LENGTH);
            }
        }
        finally
        {
            if (bytesRead != 0)
            {
                buffer.setMemory(headIndex, bytesRead, (byte)0);
                buffer.putLongOrdered(headPositionIndex, head + bytesRead);
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

    /**
     * {@inheritDoc}
     */
    public long producerPosition()
    {
        return buffer.getLongVolatile(tailPositionIndex);
    }

    /**
     * {@inheritDoc}
     */
    public long consumerPosition()
    {
        return buffer.getLongVolatile(headPositionIndex);
    }

    /**
     * {@inheritDoc}
     */
    public int size()
    {
        long headBefore;
        long tail;
        long headAfter = buffer.getLongVolatile(headPositionIndex);

        do
        {
            headBefore = headAfter;
            tail = buffer.getLongVolatile(tailPositionIndex);
            headAfter = buffer.getLongVolatile(headPositionIndex);
        }
        while (headAfter != headBefore);

        return (int)(tail - headAfter);
    }

    /**
     * {@inheritDoc}
     */
    public boolean unblock()
    {
        final AtomicBuffer buffer = this.buffer;
        final int consumerIndex = (int)(buffer.getLongVolatile(headPositionIndex) & mask);
        final int producerIndex = (int)(buffer.getLongVolatile(tailPositionIndex) & mask);

        if (producerIndex == consumerIndex)
        {
            return false;
        }

        boolean unblocked = false;
        int length = buffer.getIntVolatile(consumerIndex);
        if (length < 0)
        {
            buffer.putLongOrdered(consumerIndex, makeHeader(-length, PADDING_MSG_TYPE_ID));
            unblocked = true;
        }
        else if (0 == length)
        {
            // go from (consumerIndex to producerIndex) or (consumerIndex to capacity)
            final int limit = producerIndex > consumerIndex ? producerIndex : buffer.capacity();
            int i = consumerIndex + ALIGNMENT;

            do
            {
                // read the top int of every long (looking for length aligned to 8=ALIGNMENT)
                length = buffer.getIntVolatile(i);
                if (0 != length)
                {
                    if (scanBackToConfirmStillZeroed(buffer, i, consumerIndex))
                    {
                        buffer.putLongOrdered(consumerIndex, makeHeader(i - consumerIndex, PADDING_MSG_TYPE_ID));
                        unblocked = true;
                    }

                    break;
                }

                i += ALIGNMENT;
            }
            while (i < limit);
        }

        return unblocked;
    }

    private static boolean scanBackToConfirmStillZeroed(final AtomicBuffer buffer, final int from, final int limit)
    {
        int i = from - ALIGNMENT;
        boolean allZeros = true;
        while (i >= limit)
        {
            if (0 != buffer.getIntVolatile(i))
            {
                allZeros = false;
                break;
            }

            i -= ALIGNMENT;
        }

        return allZeros;
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
        final int tailPositionIndex = this.tailPositionIndex;
        final int headCachePositionIndex = this.headCachePositionIndex;
        final int mask = this.mask;

        long head = buffer.getLongVolatile(headCachePositionIndex);

        long tail;
        int tailIndex;
        int padding;
        do
        {
            tail = buffer.getLongVolatile(tailPositionIndex);
            final int availableCapacity = capacity - (int)(tail - head);

            if (requiredCapacity > availableCapacity)
            {
                head = buffer.getLongVolatile(headPositionIndex);

                if (requiredCapacity > (capacity - (int)(tail - head)))
                {
                    return INSUFFICIENT_CAPACITY;
                }

                buffer.putLongOrdered(headCachePositionIndex, head);
            }

            padding = 0;
            tailIndex = (int)tail & mask;
            final int toBufferEndLength = capacity - tailIndex;

            if (requiredCapacity > toBufferEndLength)
            {
                int headIndex = (int)head & mask;

                if (requiredCapacity > headIndex)
                {
                    head = buffer.getLongVolatile(headPositionIndex);
                    headIndex = (int)head & mask;
                    if (requiredCapacity > headIndex)
                    {
                        return INSUFFICIENT_CAPACITY;
                    }

                    buffer.putLongOrdered(headCachePositionIndex, head);
                }

                padding = toBufferEndLength;
            }
        }
        while (!buffer.compareAndSetLong(tailPositionIndex, tail, tail + requiredCapacity + padding));

        if (0 != padding)
        {
            buffer.putLongOrdered(tailIndex, makeHeader(padding, PADDING_MSG_TYPE_ID));
            tailIndex = 0;
        }

        return tailIndex;
    }
}
