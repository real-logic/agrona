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

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.broadcast.fixed.RecordDescriptor.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class BroadcastTransmitterTest
{
    private static final int MSG_TYPE_ID = 7;
    private static final int MSG_CAPACITY = 1024;
    private static final int MAX_MESSAGE_SIZE = 8;
    private static final int CAPACITY = BroadcastBufferDescriptor.calculateCapacity(MAX_MESSAGE_SIZE, MSG_CAPACITY);
    private static final int TOTAL_BUFFER_LENGTH = CAPACITY + BroadcastBufferDescriptor.TRAILER_LENGTH;
    private static final int LATEST_COUNTER_INDEX = CAPACITY + BroadcastBufferDescriptor.LATEST_COUNTER_OFFSET;

    private final UnsafeBuffer buffer = mock(UnsafeBuffer.class);
    private BroadcastTransmitter broadcastTransmitter;

    @Before
    public void setUp()
    {
        when(buffer.capacity()).thenReturn(TOTAL_BUFFER_LENGTH);

        broadcastTransmitter = new BroadcastTransmitter(buffer, MAX_MESSAGE_SIZE);
    }

    @Test
    public void shouldCalculateCapacityForBuffer()
    {
        assertThat(broadcastTransmitter.capacity(), is(CAPACITY));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionForCapacityThatIsNotPowerOfTwo()
    {
        final int totalBufferLength = TOTAL_BUFFER_LENGTH + 1;

        when(buffer.capacity()).thenReturn(totalBufferLength);

        new BroadcastTransmitter(buffer, MAX_MESSAGE_SIZE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenMaxMessageLengthExceeded()
    {
        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        broadcastTransmitter.transmit(MSG_TYPE_ID, srcBuffer, 0, broadcastTransmitter.maxMsgLength() + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenMessageTypeIdInvalid()
    {
        final int invalidMsgId = -1;
        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        broadcastTransmitter.transmit(invalidMsgId, srcBuffer, 0, 32);
    }

    @Test
    public void shouldTransmitIntoEmptyBuffer()
    {
        final long tail = 0L;
        assert tail < MSG_CAPACITY;
        final long nextTail = 1L;
        final int recordOffset = (int)tail;
        final int sequenceIndicatorOffset = sequenceIndicatorOffset(recordOffset);
        final int length = 8;
        assert length <= MAX_MESSAGE_SIZE;
        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        broadcastTransmitter.transmit(MSG_TYPE_ID, srcBuffer, srcIndex, length);
        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLong(LATEST_COUNTER_INDEX);
        inOrder.verify(buffer).putLong(sequenceIndicatorOffset, tail);
        inOrder.verify(buffer).putInt(lengthOffset(recordOffset), length);
        inOrder.verify(buffer).putInt(typeOffset(recordOffset), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(msgOffset(recordOffset), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putLongOrdered(sequenceIndicatorOffset, nextTail);
        inOrder.verify(buffer).putLongOrdered(LATEST_COUNTER_INDEX, nextTail);
    }

    @Test
    public void shouldTransmitIntoUsedBuffer()
    {
        final int length = 8;
        assert length <= MAX_MESSAGE_SIZE;
        final long tail = 3;
        assert tail < MSG_CAPACITY;
        final long nextTail = 4;
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);
        final int recordOffset = (int)tail * recordLengthAligned;
        final int sequenceIndicatorOffset = sequenceIndicatorOffset(recordOffset);

        when(buffer.getLong(LATEST_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        broadcastTransmitter.transmit(MSG_TYPE_ID, srcBuffer, srcIndex, length);
        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLong(LATEST_COUNTER_INDEX);
        inOrder.verify(buffer).putLong(sequenceIndicatorOffset, tail);
        inOrder.verify(buffer).putInt(lengthOffset(recordOffset), length);
        inOrder.verify(buffer).putInt(typeOffset(recordOffset), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(msgOffset(recordOffset), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putLongOrdered(sequenceIndicatorOffset, nextTail);
        inOrder.verify(buffer).putLongOrdered(LATEST_COUNTER_INDEX, nextTail);
    }


    @Test
    public void shouldTransmitIntoEndOfBuffer()
    {
        final int length = 8;
        assert length <= MAX_MESSAGE_SIZE;
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);
        final long tail = MSG_CAPACITY - 1;
        assert tail < MSG_CAPACITY;
        final long nextTail = tail + 1;
        final int recordOffset = (int)tail * recordLengthAligned;
        final int sequenceIndicatorOffset = sequenceIndicatorOffset(recordOffset);


        when(buffer.getLong(LATEST_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        broadcastTransmitter.transmit(MSG_TYPE_ID, srcBuffer, srcIndex, length);
        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLong(LATEST_COUNTER_INDEX);
        inOrder.verify(buffer).putLong(sequenceIndicatorOffset, tail);
        inOrder.verify(buffer).putInt(lengthOffset(recordOffset), length);
        inOrder.verify(buffer).putInt(typeOffset(recordOffset), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(msgOffset(recordOffset), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putLongOrdered(sequenceIndicatorOffset, nextTail);
        inOrder.verify(buffer).putLongOrdered(LATEST_COUNTER_INDEX, nextTail);
    }
}
