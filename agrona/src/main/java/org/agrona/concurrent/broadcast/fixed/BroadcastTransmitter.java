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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

import static org.agrona.UnsafeAccess.UNSAFE;
import static org.agrona.concurrent.broadcast.fixed.BroadcastBufferDescriptor.*;
import static org.agrona.concurrent.broadcast.fixed.RecordDescriptor.*;

/**
 * Transmit messages via an underlying broadcast buffer to zero or more {@link BroadcastReceiver}s.
 * <p>
 * <b>Note:</b> This class is not threadsafe. Only one transmitter is allowed per broadcast buffer.
 */
public final class BroadcastTransmitter
{

    public static final class Transmission extends UnsafeBuffer implements AutoCloseable
    {
        private static final byte[] EMPTY_BYTES = new byte[0];
        private final long transmissionBufferAddress;
        private final byte[] transmissionBufferBytes;
        private final long latestCounterAddress;
        private long nextTail;
        private int sequenceIndicatorOffset;

        private Transmission(final AtomicBuffer buffer, final int latestCounterIndex)
        {
            //it is the only initialization allowed
            super(EMPTY_BYTES);
            this.transmissionBufferAddress = buffer.addressOffset();
            this.transmissionBufferBytes = buffer.byteArray();
            this.latestCounterAddress = this.transmissionBufferAddress + latestCounterIndex;
        }

        private void commit()
        {
            if (addressOffset() != -1)
            {
                final byte[] bufferArray = this.transmissionBufferBytes;
                final long bufferAddress = this.transmissionBufferAddress;
                final int sequenceIndicatorOffset = this.sequenceIndicatorOffset;
                final long nextTail = this.nextTail;
                final long latestCounterAddress = this.latestCounterAddress;
                UNSAFE.putOrderedLong(bufferArray, bufferAddress + sequenceIndicatorOffset, nextTail);
                UNSAFE.putOrderedLong(bufferArray, latestCounterAddress, nextTail);
                super.wrap(-1, 0);
            }
        }

        @Override
        public void close()
        {
            commit();
        }

        private void unsafeWrap(final DirectBuffer buffer, final int offset, final int length)
        {
            super.wrap(buffer, offset, length);
        }

        @Override
        @Deprecated
        public void wrap(final byte[] buffer)
        {
            if (buffer != EMPTY_BYTES)
            {
                throw new UnsupportedOperationException();
            }
            else
            {
                super.wrap(EMPTY_BYTES);
            }
        }

        @Override
        @Deprecated
        public void wrap(final byte[] buffer, final int offset, final int length)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public void wrap(final ByteBuffer buffer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public void wrap(final ByteBuffer buffer, final int offset, final int length)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public void wrap(final DirectBuffer buffer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public void wrap(final DirectBuffer buffer, final int offset, final int length)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public void wrap(final long address, final int length)
        {
            throw new UnsupportedOperationException();
        }
    }

    private final Transmission transmission;
    private final AtomicBuffer buffer;
    private final int capacity;
    private final int mask;
    private final int recordSize;
    private final int maxMsgLength;
    private final int latestCounterIndex;

    /**
     * Construct a new fixed size message broadcast transmitter based on an underlying {@link AtomicBuffer}.
     * The underlying buffer must be a power of 2 multiple of {@link RecordDescriptor#calculateRecordSize(int)} of the given {@code requiredMsgLength} in size plus sufficient space
     * for the {@link BroadcastBufferDescriptor#TRAILER_LENGTH}.
     *
     * @param buffer            via which messages will be exchanged.
     * @param requiredMsgLength of each transmitted message
     * @throws IllegalStateException if the buffer capacity is not a power of 2
     *                               multiple of {@link RecordDescriptor#calculateRecordSize(int)} of the given {@code maxMessageSize}.
     */
    public BroadcastTransmitter(final AtomicBuffer buffer, final int requiredMsgLength)
    {
        this.buffer = buffer;
        this.capacity = buffer.capacity() - TRAILER_LENGTH;
        checkCapacity(requiredMsgLength, capacity);
        buffer.verifyAlignment();
        this.maxMsgLength = calculateMaxMessageLength(requiredMsgLength);
        this.recordSize = calculateRecordSize(requiredMsgLength);
        final int messages = capacity / recordSize;
        this.mask = messages - 1;
        this.latestCounterIndex = capacity + LATEST_COUNTER_OFFSET;
        final int recordSizeOffset = capacity + RECORD_SIZE_OFFSET;
        this.buffer.putIntOrdered(recordSizeOffset, this.recordSize);
        this.transmission = new Transmission(this.buffer, this.latestCounterIndex);
    }

    public MutableDirectBuffer buffer()
    {
        return this.buffer;
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
     * Get the maximum message length that can be transmitted for a buffer.
     *
     * @return the maximum message length that can be transmitted for a buffer.
     */
    public int maxMsgLength()
    {
        return maxMsgLength;
    }

    /**
     * Transmit a message to {@link BroadcastReceiver}s via the broadcast buffer.
     *
     * @param msgTypeId type of the message to be transmitted.
     * @param length    in bytes of the encoded message.
     * @return the claimed transmission
     * @throws IllegalArgumentException of the msgTypeId is not valid,
     *                                  or if the message length is greater than {@link #maxMsgLength()}.
     */
    public Transmission transmit(
        final int msgTypeId,
        final int length)
    {
        checkTypeId(msgTypeId);
        checkMessageLength(length);

        final AtomicBuffer buffer = this.buffer;
        final int latestCounterIndex = this.latestCounterIndex;
        final int mask = this.mask;
        final int recordSize = this.recordSize;

        final long currentTail = buffer.getLong(latestCounterIndex);
        final int recordOffset = calculatedRecordOffset(currentTail, mask, recordSize);
        final int sequenceIndicatorOffset = sequenceIndicatorOffset(recordOffset);
        buffer.putLong(sequenceIndicatorOffset, currentTail);
        UNSAFE.storeFence();
        buffer.putInt(lengthOffset(recordOffset), length);
        buffer.putInt(typeOffset(recordOffset), msgTypeId);
        final int msgOffset = msgOffset(recordOffset);
        final long nextTail = currentTail + 1;
        transmission.unsafeWrap(buffer, msgOffset, length);
        transmission.sequenceIndicatorOffset = sequenceIndicatorOffset;
        transmission.nextTail = nextTail;
        return transmission;
    }

    /**
     * Transmit a message to {@link BroadcastReceiver}s via the broadcast buffer.
     *
     * @param msgTypeId type of the message to be transmitted.
     * @param srcBuffer containing the encoded message to be transmitted.
     * @param srcIndex  srcIndex in the source buffer at which the encoded message begins.
     * @param length    in bytes of the encoded message.
     * @throws IllegalArgumentException of the msgTypeId is not valid,
     *                                  or if the message length is greater than {@link #maxMsgLength()}.
     */
    public void transmit(final int msgTypeId, final DirectBuffer srcBuffer, final int srcIndex, final int length)
    {
        checkTypeId(msgTypeId);
        checkMessageLength(length);

        final AtomicBuffer buffer = this.buffer;
        final int latestCounterIndex = this.latestCounterIndex;
        final int mask = this.mask;
        final int recordSize = this.recordSize;

        final long currentTail = buffer.getLong(latestCounterIndex);
        final int recordOffset = calculatedRecordOffset(currentTail, mask, recordSize);
        final int sequenceIndicatorOffset = sequenceIndicatorOffset(recordOffset);
        buffer.putLong(sequenceIndicatorOffset, currentTail);
        UNSAFE.storeFence();
        buffer.putInt(lengthOffset(recordOffset), length);
        buffer.putInt(typeOffset(recordOffset), msgTypeId);
        buffer.putBytes(msgOffset(recordOffset), srcBuffer, srcIndex, length);
        final long nextTail = currentTail + 1;
        buffer.putLongOrdered(sequenceIndicatorOffset, nextTail);
        buffer.putLongOrdered(latestCounterIndex, nextTail);
    }

    private void checkMessageLength(final int length)
    {
        if (length > maxMsgLength)
        {
            throw new IllegalArgumentException(
                "Encoded message exceeds maxMsgLength of " + maxMsgLength + ", length=" + length);
        }
    }
}
