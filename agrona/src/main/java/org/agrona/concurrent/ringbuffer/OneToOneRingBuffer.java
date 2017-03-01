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
package org.agrona.concurrent.ringbuffer;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;

import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.*;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.*;

/**
 * A ring-buffer that supports the exchange of messages from a single producer to a single consumer.
 *
 * This single producer ring-buffer can be used in combination a consumer using the {@link ManyToOneRingBuffer}
 * provided no other producers are accessing the same ring-buffer.
 */
public class OneToOneRingBuffer implements RingBuffer
{
    /**
     * Record type is padding to prevent fragmentation in the buffer.
     */
    public static final int PADDING_MSG_TYPE_ID = -1;

    private final int capacity;
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
    public OneToOneRingBuffer(final AtomicBuffer buffer)
    {
        this.buffer = buffer;
        checkCapacity(buffer.capacity());
        capacity = buffer.capacity() - TRAILER_LENGTH;

        buffer.verifyAlignment();

        maxMsgLength = capacity / 8;
        tailPositionIndex = capacity + TAIL_POSITION_OFFSET;
        headCachePositionIndex = capacity + HEAD_CACHE_POSITION_OFFSET;
        headPositionIndex = capacity + HEAD_POSITION_OFFSET;
        correlationIdCounterIndex = capacity + CORRELATION_COUNTER_OFFSET;
        consumerHeartbeatIndex = capacity + CONSUMER_HEARTBEAT_OFFSET;
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

        final AtomicBuffer buffer = this.buffer;
        final int recordLength = length + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final int requiredCapacity = alignedRecordLength + HEADER_LENGTH;
        final int capacity = this.capacity;
        final int tailPositionIndex = this.tailPositionIndex;
        final int headCachePositionIndex = this.headCachePositionIndex;
        final int mask = capacity - 1;

        long head = buffer.getLong(headCachePositionIndex);
        final long tail = buffer.getLong(tailPositionIndex);
        final int availableCapacity = capacity - (int)(tail - head);

        if (requiredCapacity > availableCapacity)
        {
            head = buffer.getLongVolatile(headPositionIndex);

            if (requiredCapacity > (capacity - (int)(tail - head)))
            {
                return false;
            }

            buffer.putLong(headCachePositionIndex, head);
        }

        int padding = 0;
        int recordIndex = (int)tail & mask;
        final int toBufferEndLength = capacity - recordIndex;

        if (requiredCapacity > toBufferEndLength)
        {
            int headIndex = (int)head & mask;

            if (requiredCapacity > headIndex)
            {
                head = buffer.getLongVolatile(headPositionIndex);
                headIndex = (int)head & mask;
                if (requiredCapacity > headIndex)
                {
                    return false;
                }

                buffer.putLong(headCachePositionIndex, head);
            }

            padding = toBufferEndLength;
        }

        if (0 != padding)
        {
            buffer.putLong(0, 0L);
            buffer.putLongOrdered(recordIndex, makeHeader(padding, PADDING_MSG_TYPE_ID));
            recordIndex = 0;
        }

        buffer.putBytes(encodedMsgOffset(recordIndex), srcBuffer, srcIndex, length);
        buffer.putLong(recordIndex + alignedRecordLength, 0L);
        buffer.putLongOrdered(recordIndex, makeHeader(recordLength, msgTypeId));
        buffer.putLongOrdered(tailPositionIndex, tail + alignedRecordLength + padding);

        return true;
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

        final int capacity = this.capacity;
        final int headIndex = (int)head & (capacity - 1);
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
        return false;
    }

    private void checkMsgLength(final int length)
    {
        if (length > maxMsgLength)
        {
            throw new IllegalArgumentException(String.format(
                "encoded message exceeds maxMsgLength of %d, length=%d", maxMsgLength, length));
        }
    }
}
