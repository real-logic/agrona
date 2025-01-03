/*
 * Copyright 2014-2025 Real Logic Limited.
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
package org.agrona.concurrent.broadcast;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.broadcast.RecordDescriptor.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BroadcastTransmitterTest
{
    private static final int MSG_TYPE_ID = 7;
    private static final int CAPACITY = 1024;
    private static final int TOTAL_BUFFER_LENGTH = CAPACITY + BroadcastBufferDescriptor.TRAILER_LENGTH;
    private static final int TAIL_INTENT_COUNTER_OFFSET =
        CAPACITY + BroadcastBufferDescriptor.TAIL_INTENT_COUNTER_OFFSET;
    private static final int TAIL_COUNTER_INDEX = CAPACITY + BroadcastBufferDescriptor.TAIL_COUNTER_OFFSET;
    private static final int LATEST_COUNTER_INDEX = CAPACITY + BroadcastBufferDescriptor.LATEST_COUNTER_OFFSET;

    private final UnsafeBuffer buffer = mock(UnsafeBuffer.class);
    private BroadcastTransmitter broadcastTransmitter;

    @BeforeEach
    void setUp()
    {
        when(buffer.capacity()).thenReturn(TOTAL_BUFFER_LENGTH);

        broadcastTransmitter = new BroadcastTransmitter(buffer);
    }

    @Test
    void shouldCalculateCapacityForBuffer()
    {
        assertThat(broadcastTransmitter.capacity(), is(CAPACITY));
    }

    @Test
    void shouldThrowExceptionForCapacityThatIsNotPowerOfTwo()
    {
        final int capacity = 777;
        final int totalBufferLength = capacity + BroadcastBufferDescriptor.TRAILER_LENGTH;

        when(buffer.capacity()).thenReturn(totalBufferLength);

        assertThrows(IllegalStateException.class, () -> new BroadcastTransmitter(buffer));
    }

    @Test
    void shouldThrowExceptionWhenMaxMessageLengthExceeded()
    {
        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        assertThrows(IllegalArgumentException.class, () ->
            broadcastTransmitter.transmit(MSG_TYPE_ID, srcBuffer, 0, broadcastTransmitter.maxMsgLength() + 1));
    }

    @Test
    void shouldThrowExceptionWhenMessageTypeIdInvalid()
    {
        final int invalidMsgId = -1;
        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        assertThrows(IllegalArgumentException.class, () ->
            broadcastTransmitter.transmit(invalidMsgId, srcBuffer, 0, 32));
    }

    @Test
    void shouldTransmitIntoEmptyBuffer()
    {
        final long tail = 0L;
        final int recordOffset = (int)tail;
        final int length = 8;
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        broadcastTransmitter.transmit(MSG_TYPE_ID, srcBuffer, srcIndex, length);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLong(TAIL_COUNTER_INDEX);

        inOrder.verify(buffer).putLongOrdered(TAIL_INTENT_COUNTER_OFFSET, tail + recordLengthAligned);
        inOrder.verify(buffer).putInt(lengthOffset(recordOffset), recordLength);
        inOrder.verify(buffer).putInt(typeOffset(recordOffset), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(msgOffset(recordOffset), srcBuffer, srcIndex, length);

        inOrder.verify(buffer).putLongOrdered(LATEST_COUNTER_INDEX, tail);
        inOrder.verify(buffer).putLongOrdered(TAIL_COUNTER_INDEX, tail + recordLengthAligned);
    }

    @Test
    void shouldTransmitIntoUsedBuffer()
    {
        final long tail = RECORD_ALIGNMENT * 3;
        final int recordOffset = (int)tail;
        final int length = 8;
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);

        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        broadcastTransmitter.transmit(MSG_TYPE_ID, srcBuffer, srcIndex, length);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLong(TAIL_COUNTER_INDEX);

        inOrder.verify(buffer).putLongOrdered(TAIL_INTENT_COUNTER_OFFSET, tail + recordLengthAligned);
        inOrder.verify(buffer).putInt(lengthOffset(recordOffset), recordLength);
        inOrder.verify(buffer).putInt(typeOffset(recordOffset), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(msgOffset(recordOffset), srcBuffer, srcIndex, length);

        inOrder.verify(buffer).putLongOrdered(LATEST_COUNTER_INDEX, tail);
        inOrder.verify(buffer).putLongOrdered(TAIL_COUNTER_INDEX, tail + recordLengthAligned);
    }

    @Test
    void shouldTransmitIntoEndOfBuffer()
    {
        final int length = 8;
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);
        final long tail = CAPACITY - recordLengthAligned;
        final int recordOffset = (int)tail;


        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        broadcastTransmitter.transmit(MSG_TYPE_ID, srcBuffer, srcIndex, length);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLong(TAIL_COUNTER_INDEX);

        inOrder.verify(buffer).putLongOrdered(TAIL_INTENT_COUNTER_OFFSET, tail + recordLengthAligned);
        inOrder.verify(buffer).putInt(lengthOffset(recordOffset), recordLength);
        inOrder.verify(buffer).putInt(typeOffset(recordOffset), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(msgOffset(recordOffset), srcBuffer, srcIndex, length);

        inOrder.verify(buffer).putLongOrdered(LATEST_COUNTER_INDEX, tail);
        inOrder.verify(buffer).putLongOrdered(TAIL_COUNTER_INDEX, tail + recordLengthAligned);
    }

    @Test
    void shouldApplyPaddingWhenInsufficientSpaceAtEndOfBuffer()
    {
        long tail = CAPACITY - RECORD_ALIGNMENT;
        int recordOffset = (int)tail;
        final int length = RECORD_ALIGNMENT + 8;
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);
        final int toEndOfBuffer = CAPACITY - recordOffset;

        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        broadcastTransmitter.transmit(MSG_TYPE_ID, srcBuffer, srcIndex, length);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLong(TAIL_COUNTER_INDEX);

        inOrder.verify(buffer).putLongOrdered(TAIL_INTENT_COUNTER_OFFSET, tail + recordLengthAligned + toEndOfBuffer);

        inOrder.verify(buffer).putInt(lengthOffset(recordOffset), toEndOfBuffer);
        inOrder.verify(buffer).putInt(typeOffset(recordOffset), PADDING_MSG_TYPE_ID);

        tail += toEndOfBuffer;
        recordOffset = 0;
        inOrder.verify(buffer).putInt(lengthOffset(recordOffset), recordLength);
        inOrder.verify(buffer).putInt(typeOffset(recordOffset), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(msgOffset(recordOffset), srcBuffer, srcIndex, length);

        inOrder.verify(buffer).putLongOrdered(LATEST_COUNTER_INDEX, tail);
        inOrder.verify(buffer).putLongOrdered(TAIL_COUNTER_INDEX, tail + recordLengthAligned);
    }
}
