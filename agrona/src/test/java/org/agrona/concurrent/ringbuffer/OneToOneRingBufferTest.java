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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer.PADDING_MSG_TYPE_ID;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.*;

public class OneToOneRingBufferTest
{
    private static final int MSG_TYPE_ID = 7;
    private static final int CAPACITY = 4096;
    private static final int TOTAL_BUFFER_LENGTH = CAPACITY + RingBufferDescriptor.TRAILER_LENGTH;
    private static final int TAIL_COUNTER_INDEX = CAPACITY + RingBufferDescriptor.TAIL_POSITION_OFFSET;
    private static final int HEAD_COUNTER_INDEX = CAPACITY + RingBufferDescriptor.HEAD_POSITION_OFFSET;
    private static final int HEAD_COUNTER_CACHE_INDEX = CAPACITY + RingBufferDescriptor.HEAD_CACHE_POSITION_OFFSET;

    private final UnsafeBuffer buffer = mock(UnsafeBuffer.class);
    private OneToOneRingBuffer ringBuffer;

    @Before
    public void setUp()
    {
        when(buffer.capacity()).thenReturn(TOTAL_BUFFER_LENGTH);

        ringBuffer = new OneToOneRingBuffer(buffer);
    }

    @Test
    public void shouldWriteToEmptyBuffer()
    {
        final int length = 8;
        final int recordLength = length + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long tail = 0L;
        final long head = 0L;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putBytes(encodedMsgOffset((int)tail), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putLong((int)tail + alignedRecordLength, 0L);
        inOrder.verify(buffer).putLongOrdered((int)tail, makeHeader(recordLength, MSG_TYPE_ID));
        inOrder.verify(buffer).putLongOrdered(TAIL_COUNTER_INDEX, tail + alignedRecordLength);
    }

    @Test
    public void shouldRejectWriteWhenInsufficientSpace()
    {
        final int length = 200;
        final long head = 0L;
        final long tail = head + (CAPACITY - align(length - ALIGNMENT, ALIGNMENT));

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertFalse(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        verify(buffer, never()).putBytes(anyInt(), eq(srcBuffer), anyInt(), anyInt());
        verify(buffer, never()).putLong(anyInt(), anyInt());
        verify(buffer, never()).putLongOrdered(anyInt(), anyInt());
        verify(buffer, never()).putIntOrdered(anyInt(), anyInt());
    }

    @Test
    public void shouldRejectWriteWhenBufferFull()
    {
        final int length = 8;
        final long head = 0L;
        final long tail = head + CAPACITY;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertFalse(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        verify(buffer, never()).putLongOrdered(anyInt(), anyInt());
    }

    @Test
    public void shouldInsertPaddingRecordPlusMessageOnBufferWrap()
    {
        final int length = 200;
        final int recordLength = length + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long tail = CAPACITY - HEADER_LENGTH;
        final long head = tail - (ALIGNMENT * 4);

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putLong(0, 0L);
        inOrder.verify(buffer).putLongOrdered((int)tail, makeHeader(HEADER_LENGTH, PADDING_MSG_TYPE_ID));

        inOrder.verify(buffer).putBytes(encodedMsgOffset(0), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putLong(alignedRecordLength, 0L);
        inOrder.verify(buffer).putLongOrdered(0, makeHeader(recordLength, MSG_TYPE_ID));
    }

    @Test
    public void shouldInsertPaddingRecordPlusMessageOnBufferWrapWithHeadEqualToTail()
    {
        final int length = 200;
        final int recordLength = length + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long tail = CAPACITY - HEADER_LENGTH;
        final long head = tail;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putLong(0, 0L);
        inOrder.verify(buffer).putLongOrdered((int)tail, makeHeader(HEADER_LENGTH, PADDING_MSG_TYPE_ID));

        inOrder.verify(buffer).putBytes(encodedMsgOffset(0), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putLong(alignedRecordLength, 0L);
        inOrder.verify(buffer).putLongOrdered(0, makeHeader(recordLength, MSG_TYPE_ID));
    }

    @Test
    public void shouldReadNothingFromEmptyBuffer()
    {
        final long head = 0L;

        when(buffer.getLong(HEAD_COUNTER_INDEX)).thenReturn(head);

        final MessageHandler handler = (msgTypeId, buffer, index, length) -> fail("should not be called");
        final int messagesRead = ringBuffer.read(handler);

        assertThat(messagesRead, is(0));
    }

    @Test
    public void shouldNotReadSingleMessagePartWayThroughWriting()
    {
        final long head = 0L;
        final int headIndex = (int)head;

        when(buffer.getLong(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getIntVolatile(lengthOffset(headIndex))).thenReturn(0);

        final int[] times = new int[1];
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times[0]++;
        final int messagesRead = ringBuffer.read(handler);

        assertThat(messagesRead, is(0));
        assertThat(times[0], is(0));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer, times(1)).getLongVolatile(headIndex);
        inOrder.verify(buffer, times(0)).setMemory(anyInt(), anyInt(), anyByte());
        inOrder.verify(buffer, times(0)).putLongOrdered(HEAD_COUNTER_INDEX, headIndex);
    }

    @Test
    public void shouldReadTwoMessages()
    {
        final int msgLength = 16;
        final int recordLength = HEADER_LENGTH + msgLength;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long tail = alignedRecordLength * 2;
        final long head = 0L;
        final int headIndex = (int)head;

        when(buffer.getLong(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(headIndex)).thenReturn(makeHeader(recordLength, MSG_TYPE_ID));
        when(buffer.getLongVolatile(headIndex + alignedRecordLength))
            .thenReturn(makeHeader(recordLength, MSG_TYPE_ID));

        final int[] times = new int[1];
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times[0]++;
        final int messagesRead = ringBuffer.read(handler);

        assertThat(messagesRead, is(2));
        assertThat(times[0], is(2));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer, times(1)).putLongOrdered(HEAD_COUNTER_INDEX, tail);
        inOrder.verify(buffer, times(0)).setMemory(anyInt(), anyInt(), anyByte());
    }

    @Test
    public void shouldLimitReadOfMessages()
    {
        final int msgLength = 16;
        final int recordLength = HEADER_LENGTH + msgLength;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long head = 0L;
        final int headIndex = (int)head;

        when(buffer.getLong(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(headIndex)).thenReturn(makeHeader(recordLength, MSG_TYPE_ID));

        final int[] times = new int[1];
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times[0]++;
        final int limit = 1;
        final int messagesRead = ringBuffer.read(handler, limit);

        assertThat(messagesRead, is(1));
        assertThat(times[0], is(1));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer, times(1)).putLongOrdered(HEAD_COUNTER_INDEX, head + alignedRecordLength);
        inOrder.verify(buffer, times(0)).setMemory(anyInt(), anyInt(), anyByte());
    }

    @Test
    public void shouldCopeWithExceptionFromHandler()
    {
        final int msgLength = 16;
        final int recordLength = HEADER_LENGTH + msgLength;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long tail = alignedRecordLength * 2;
        final long head = 0L;
        final int headIndex = (int)head;

        when(buffer.getLong(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(headIndex)).thenReturn(makeHeader(recordLength, MSG_TYPE_ID));
        when(buffer.getLongVolatile(headIndex + alignedRecordLength))
            .thenReturn(makeHeader(recordLength, MSG_TYPE_ID));

        final int[] times = new int[1];
        final MessageHandler handler =
            (msgTypeId, buffer, index, length) ->
            {
                times[0]++;
                if (times[0] == 2)
                {
                    throw new RuntimeException();
                }
            };

        try
        {
            ringBuffer.read(handler);
        }
        catch (final RuntimeException ignore)
        {
            assertThat(times[0], is(2));

            final InOrder inOrder = inOrder(buffer);
            inOrder.verify(buffer, times(1)).putLongOrdered(HEAD_COUNTER_INDEX, tail);
            inOrder.verify(buffer, times(0)).setMemory(anyInt(), anyInt(), anyByte());

            return;
        }

        fail("Should have thrown exception");
    }

    @Test
    public void shouldNotUnblockBecauseNotOtherProducersToRaceWith()
    {
        assertFalse(ringBuffer.unblock());
    }

    @Test
    public void shouldCalculateCapacityForBuffer()
    {
        assertThat(ringBuffer.capacity(), is(CAPACITY));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionForCapacityThatIsNotPowerOfTwo()
    {
        final int capacity = 777;
        final int totalBufferLength = capacity + RingBufferDescriptor.TRAILER_LENGTH;
        new OneToOneRingBuffer(new UnsafeBuffer(new byte[totalBufferLength]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenMaxMessageSizeExceeded()
    {
        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, ringBuffer.maxMsgLength() + 1);
    }

    @Test
    public void shouldInsertPaddingAndWriteToBuffer()
    {
        final int padding = 200;
        final int messageLength = 400;

        final long tail = 2 * CAPACITY - padding;
        final long head = tail;

        // free space is (200 + 300) more than message length (400)
        // but contiguous space (300) is less than message length (400)
        final long headCache = CAPACITY + 300;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getLong(HEAD_COUNTER_CACHE_INDEX)).thenReturn(headCache);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[messageLength]);
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, messageLength));
    }
}
