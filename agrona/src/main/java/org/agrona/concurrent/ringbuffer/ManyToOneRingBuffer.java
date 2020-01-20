/*
 * Copyright 2014-2020 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.concurrent.ringbuffer;

import org.agrona.DirectBuffer;
import org.agrona.UnsafeAccess;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;

import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.*;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.*;

/**
 * A ring-buffer that supports the exchange of messages from many producers to a single consumer.
 */
public final class ManyToOneRingBuffer implements RingBuffer
{

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
    public ManyToOneRingBuffer(final AtomicBuffer buffer)
    {
        this.buffer = buffer;
        checkCapacity(buffer.capacity());
        capacity = buffer.capacity() - TRAILER_LENGTH;

        buffer.verifyAlignment();

        maxMsgLength = capacity >> 3;
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
    public boolean write(final int msgTypeId, final DirectBuffer srcBuffer, final int offset, final int length)
    {
        checkTypeId(msgTypeId);
        checkMsgLength(length);

        final AtomicBuffer buffer = this.buffer;
        final int recordLength = length + HEADER_LENGTH;
        final int recordIndex = claimCapacity(buffer, recordLength);

        if (INSUFFICIENT_CAPACITY == recordIndex)
        {
            return false;
        }

        buffer.putIntOrdered(lengthOffset(recordIndex), -recordLength);
        UnsafeAccess.UNSAFE.storeFence();

        buffer.putInt(typeOffset(recordIndex), msgTypeId);
        buffer.putBytes(encodedMsgOffset(recordIndex), srcBuffer, offset, length);
        buffer.putIntOrdered(lengthOffset(recordIndex), recordLength);

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
        final int headPositionIndex = this.headPositionIndex;
        final long head = buffer.getLong(headPositionIndex);

        final int capacity = this.capacity;
        final int headIndex = (int)head & (capacity - 1);
        final int maxBlockLength = capacity - headIndex;
        int bytesRead = 0;

        try
        {
            while ((bytesRead < maxBlockLength) && (messagesRead < messageCountLimit))
            {
                final int recordIndex = headIndex + bytesRead;
                final int recordLength = buffer.getIntVolatile(lengthOffset(recordIndex));
                if (recordLength <= 0)
                {
                    break;
                }

                bytesRead += align(recordLength, ALIGNMENT);

                final int messageTypeId = buffer.getInt(typeOffset(recordIndex));
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
        final AtomicBuffer buffer = this.buffer;
        final int headPositionIndex = this.headPositionIndex;
        final int tailPositionIndex = this.tailPositionIndex;
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
        final long headPosition = buffer.getLongVolatile(headPositionIndex);
        final long tailPosition = buffer.getLongVolatile(tailPositionIndex);

        if (headPosition == tailPosition)
        {
            return false;
        }

        final int mask = capacity - 1;
        final int consumerIndex = (int)(headPosition & mask);
        final int producerIndex = (int)(tailPosition & mask);

        boolean unblocked = false;
        int length = buffer.getIntVolatile(consumerIndex);
        if (length < 0)
        {
            buffer.putInt(typeOffset(consumerIndex), PADDING_MSG_TYPE_ID);
            buffer.putIntOrdered(lengthOffset(consumerIndex), -length);
            unblocked = true;
        }
        else if (0 == length)
        {
            // go from (consumerIndex to producerIndex) or (consumerIndex to capacity)
            final int limit = producerIndex > consumerIndex ? producerIndex : capacity;
            int i = consumerIndex + ALIGNMENT;

            do
            {
                // read the top int of every long (looking for length aligned to 8=ALIGNMENT)
                length = buffer.getIntVolatile(i);
                if (0 != length)
                {
                    if (scanBackToConfirmStillZeroed(buffer, i, consumerIndex))
                    {
                        buffer.putInt(typeOffset(consumerIndex), PADDING_MSG_TYPE_ID);
                        buffer.putIntOrdered(lengthOffset(consumerIndex), i - consumerIndex);
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

    /**
     * {@inheritDoc}
     */
    public int tryClaim(final int msgTypeId, final int length)
    {
        checkTypeId(msgTypeId);
        checkMsgLength(length);

        final AtomicBuffer buffer = this.buffer;
        final int recordLength = length + HEADER_LENGTH;
        final int recordIndex = claimCapacity(buffer, recordLength);

        if (INSUFFICIENT_CAPACITY == recordIndex)
        {
            return recordIndex;
        }

        buffer.putIntOrdered(lengthOffset(recordIndex), -recordLength);
        UnsafeAccess.UNSAFE.storeFence();
        buffer.putInt(typeOffset(recordIndex), msgTypeId);

        return encodedMsgOffset(recordIndex);
    }

    /**
     * {@inheritDoc}
     */
    public void commit(final int index)
    {
        final int recordIndex = computeRecordIndex(index);
        final AtomicBuffer buffer = this.buffer;
        final int recordLength = verifyClaimedSpaceNotReleased(buffer, recordIndex);

        buffer.putIntOrdered(lengthOffset(recordIndex), -recordLength);
    }

    /**
     * {@inheritDoc}
     */
    public void abort(final int index)
    {
        final int recordIndex = computeRecordIndex(index);
        final AtomicBuffer buffer = this.buffer;
        final int recordLength = verifyClaimedSpaceNotReleased(buffer, recordIndex);

        buffer.putInt(typeOffset(recordIndex), PADDING_MSG_TYPE_ID);
        buffer.putIntOrdered(lengthOffset(recordIndex), -recordLength);
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
        if (length < 0)
        {
            throw new IllegalArgumentException("invalid message length=" + length);
        }
        else if (length > maxMsgLength)
        {
            throw new IllegalArgumentException(
                "encoded message exceeds maxMsgLength of " + maxMsgLength + ", length=" + length);
        }
    }

    private int claimCapacity(final AtomicBuffer buffer, final int recordLength)
    {
        final int requiredCapacity = align(recordLength, ALIGNMENT);
        final int capacity = this.capacity;
        final int tailPositionIndex = this.tailPositionIndex;
        final int headCachePositionIndex = this.headCachePositionIndex;
        final int mask = capacity - 1;

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
            buffer.putIntOrdered(lengthOffset(tailIndex), -padding);
            UnsafeAccess.UNSAFE.storeFence();

            buffer.putInt(typeOffset(tailIndex), PADDING_MSG_TYPE_ID);
            buffer.putIntOrdered(lengthOffset(tailIndex), padding);
            tailIndex = 0;
        }

        return tailIndex;
    }

    private int computeRecordIndex(final int index)
    {
        final int recordIndex = index - HEADER_LENGTH;
        if (recordIndex < 0 || recordIndex > (capacity - HEADER_LENGTH))
        {
            throw new IllegalArgumentException("invalid message index " + index);
        }

        return recordIndex;
    }

    private int verifyClaimedSpaceNotReleased(final AtomicBuffer buffer, final int recordIndex)
    {
        final int recordLength = buffer.getInt(lengthOffset(recordIndex));
        if (recordLength < 0)
        {
            return recordLength;
        }

        throw new IllegalStateException("claimed space previously " +
            (PADDING_MSG_TYPE_ID == buffer.getInt(typeOffset(recordIndex)) ? "aborted" : "committed"));
    }
}
