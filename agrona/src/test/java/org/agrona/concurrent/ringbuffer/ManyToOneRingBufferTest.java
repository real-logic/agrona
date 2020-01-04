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

import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static java.lang.Boolean.TRUE;
import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer.PADDING_MSG_TYPE_ID;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ManyToOneRingBufferTest
{
    private static final int MSG_TYPE_ID = 7;
    private static final int CAPACITY = 4096;
    private static final int TOTAL_BUFFER_LENGTH = CAPACITY + RingBufferDescriptor.TRAILER_LENGTH;
    private static final int TAIL_COUNTER_INDEX = CAPACITY + RingBufferDescriptor.TAIL_POSITION_OFFSET;
    private static final int HEAD_COUNTER_INDEX = CAPACITY + RingBufferDescriptor.HEAD_POSITION_OFFSET;
    private static final int HEAD_COUNTER_CACHE_INDEX = CAPACITY + RingBufferDescriptor.HEAD_CACHE_POSITION_OFFSET;

    private final UnsafeBuffer buffer = mock(UnsafeBuffer.class);
    private ManyToOneRingBuffer ringBuffer;

    @BeforeEach
    public void setUp()
    {
        when(buffer.capacity()).thenReturn(TOTAL_BUFFER_LENGTH);

        ringBuffer = new ManyToOneRingBuffer(buffer);
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
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.compareAndSetLong(TAIL_COUNTER_INDEX, tail, tail + alignedRecordLength))
            .thenReturn(TRUE);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), -recordLength);
        inOrder.verify(buffer).putInt(typeOffset((int)tail), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(encodedMsgOffset((int)tail), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), recordLength);
    }

    @Test
    public void shouldRejectWriteWhenInsufficientSpace()
    {
        final int length = 200;
        final long head = 0L;
        final long tail = head + (CAPACITY - align(length - ALIGNMENT, ALIGNMENT));

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertFalse(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        verify(buffer, never()).putInt(anyInt(), anyInt());
        verify(buffer, never()).compareAndSetLong(anyInt(), anyLong(), anyLong());
        verify(buffer, never()).putBytes(anyInt(), eq(srcBuffer), anyInt(), anyInt());
        verify(buffer, never()).putIntOrdered(anyInt(), anyInt());
    }

    @Test
    public void shouldRejectWriteWhenBufferFull()
    {
        final int length = 8;
        final long head = 0L;
        final long tail = head + CAPACITY;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertFalse(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        verify(buffer, never()).putInt(anyInt(), anyInt());
        verify(buffer, never()).compareAndSetLong(anyInt(), anyLong(), anyLong());
        verify(buffer, never()).putIntOrdered(anyInt(), anyInt());
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
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.compareAndSetLong(TAIL_COUNTER_INDEX, tail, tail + alignedRecordLength + ALIGNMENT))
            .thenReturn(TRUE);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), -HEADER_LENGTH);
        inOrder.verify(buffer).putInt(typeOffset((int)tail), PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), HEADER_LENGTH);

        inOrder.verify(buffer).putIntOrdered(lengthOffset(0), -recordLength);
        inOrder.verify(buffer).putInt(typeOffset(0), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(encodedMsgOffset(0), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putIntOrdered(lengthOffset(0), recordLength);
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
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.compareAndSetLong(TAIL_COUNTER_INDEX, tail, tail + alignedRecordLength + ALIGNMENT))
            .thenReturn(TRUE);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), -HEADER_LENGTH);
        inOrder.verify(buffer).putInt(typeOffset((int)tail), PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), HEADER_LENGTH);

        inOrder.verify(buffer).putIntOrdered(lengthOffset(0), -recordLength);
        inOrder.verify(buffer).putInt(typeOffset(0), MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(encodedMsgOffset(0), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putIntOrdered(lengthOffset(0), recordLength);
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
        inOrder.verify(buffer, times(1)).getIntVolatile(lengthOffset(headIndex));
        inOrder.verify(buffer, times(0)).setMemory(headIndex, 0, (byte)0);
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
        when(buffer.getInt(typeOffset(headIndex))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(headIndex + alignedRecordLength))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex + alignedRecordLength))).thenReturn(recordLength);

        final int[] times = new int[1];
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times[0]++;
        final int messagesRead = ringBuffer.read(handler);

        assertThat(messagesRead, is(2));
        assertThat(times[0], is(2));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer, times(1)).setMemory(headIndex, alignedRecordLength * 2, (byte)0);
        inOrder.verify(buffer, times(1)).putLongOrdered(HEAD_COUNTER_INDEX, tail);
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
        when(buffer.getInt(typeOffset(headIndex))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex))).thenReturn(recordLength);

        final int[] times = new int[1];
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times[0]++;
        final int limit = 1;
        final int messagesRead = ringBuffer.read(handler, limit);

        assertThat(messagesRead, is(1));
        assertThat(times[0], is(1));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer, times(1)).setMemory(headIndex, alignedRecordLength, (byte)0);
        inOrder.verify(buffer, times(1)).putLongOrdered(HEAD_COUNTER_INDEX, head + alignedRecordLength);
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
        when(buffer.getInt(typeOffset(headIndex))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(headIndex + alignedRecordLength))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex + alignedRecordLength))).thenReturn(recordLength);

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
            inOrder.verify(buffer, times(1)).setMemory(headIndex, alignedRecordLength * 2, (byte)0);
            inOrder.verify(buffer, times(1)).putLongOrdered(HEAD_COUNTER_INDEX, tail);

            return;
        }

        fail("Should have thrown exception");
    }

    @Test
    public void shouldNotUnblockWhenEmpty()
    {
        final long position = ALIGNMENT * 4;
        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(position);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(position);

        assertFalse(ringBuffer.unblock());
    }

    @Test
    public void shouldUnblockMessageWithHeader()
    {
        final int messageLength = ALIGNMENT * 4;
        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn((long)messageLength);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn((long)messageLength * 2);
        when(buffer.getIntVolatile(messageLength)).thenReturn(-messageLength);

        assertTrue(ringBuffer.unblock());

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putInt(typeOffset(messageLength), PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset(messageLength), messageLength);
    }

    @Test
    public void shouldUnblockWhenFullWithHeader()
    {
        final int messageLength = ALIGNMENT * 4;
        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn((long)messageLength);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn((long)messageLength + CAPACITY);
        when(buffer.getIntVolatile(messageLength)).thenReturn(-messageLength);

        assertTrue(ringBuffer.unblock());

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putInt(typeOffset(messageLength), PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset(messageLength), messageLength);
    }

    @Test
    public void shouldUnblockWhenFullWithoutHeader()
    {
        final int messageLength = ALIGNMENT * 4;
        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn((long)messageLength);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn((long)messageLength + CAPACITY);
        when(buffer.getIntVolatile(messageLength * 2)).thenReturn(messageLength);

        assertTrue(ringBuffer.unblock());

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putInt(typeOffset(messageLength), PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset(messageLength), messageLength);
    }

    @Test
    public void shouldUnblockGapWithZeros()
    {
        final int messageLength = ALIGNMENT * 4;
        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn((long)messageLength);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn((long)messageLength * 3);
        when(buffer.getIntVolatile(messageLength * 2)).thenReturn(messageLength);

        assertTrue(ringBuffer.unblock());

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putInt(typeOffset(messageLength), PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset(messageLength), messageLength);
    }

    @Test
    public void shouldNotUnblockGapWithMessageRaceOnSecondMessageIncreasingTailThenInterrupting()
    {
        final int messageLength = ALIGNMENT * 4;
        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn((long)messageLength);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn((long)messageLength * 3);
        when(buffer.getIntVolatile(messageLength * 2)).thenReturn(0).thenReturn(messageLength);

        assertFalse(ringBuffer.unblock());
        verify(buffer, never()).putInt(typeOffset(messageLength), PADDING_MSG_TYPE_ID);
    }

    @Test
    public void shouldNotUnblockGapWithMessageRaceWhenScanForwardTakesAnInterrupt()
    {
        final int messageLength = ALIGNMENT * 4;
        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn((long)messageLength);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn((long)messageLength * 3);
        when(buffer.getIntVolatile(messageLength * 2)).thenReturn(0).thenReturn(messageLength);
        when(buffer.getIntVolatile(messageLength * 2 + ALIGNMENT)).thenReturn(7);

        assertFalse(ringBuffer.unblock());
        verify(buffer, never()).putInt(typeOffset(messageLength), PADDING_MSG_TYPE_ID);
    }

    @Test
    public void shouldCalculateCapacityForBuffer()
    {
        assertThat(ringBuffer.capacity(), is(CAPACITY));
    }

    @Test
    public void shouldThrowExceptionForCapacityThatIsNotPowerOfTwo()
    {
        final int capacity = 777;
        final int totalBufferLength = capacity + RingBufferDescriptor.TRAILER_LENGTH;
        assertThrows(IllegalStateException.class, () ->
            new ManyToOneRingBuffer(new UnsafeBuffer(new byte[totalBufferLength])));
    }

    @Test
    public void shouldThrowExceptionWhenMaxMessageSizeExceeded()
    {
        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        assertThrows(IllegalArgumentException.class,
            () -> ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, ringBuffer.maxMsgLength() + 1));
    }

    @Test
    public void shouldInsertPaddingAndWriteToBuffer()
    {
        final int padding = 200;
        final int messageLength = 400;
        final int recordLength = messageLength + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);

        final long tail = 2 * CAPACITY - padding;
        final long head = tail;

        // free space is (200 + 300) more than message length (400)
        // but contiguous space (300) is less than message length (400)
        final long headCache = CAPACITY + 300;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getLongVolatile(HEAD_COUNTER_CACHE_INDEX)).thenReturn(headCache);
        when(buffer.compareAndSetLong(TAIL_COUNTER_INDEX, tail, tail + alignedRecordLength + padding))
            .thenReturn(true);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[messageLength]);

        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, messageLength));
    }
}
