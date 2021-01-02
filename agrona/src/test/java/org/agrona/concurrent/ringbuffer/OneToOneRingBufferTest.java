/*
 * Copyright 2014-2021 Real Logic Limited.
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

import org.agrona.ExpandableArrayBuffer;
import org.agrona.collections.MutableInteger;
import org.agrona.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import java.util.function.IntConsumer;

import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer.PADDING_MSG_TYPE_ID;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.*;
import static org.agrona.concurrent.ringbuffer.RingBuffer.INSUFFICIENT_CAPACITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @BeforeEach
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
        inOrder.verify(buffer).putLongOrdered(TAIL_COUNTER_INDEX, tail + alignedRecordLength);
        inOrder.verify(buffer).putLong((int)tail + alignedRecordLength, 0L);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), -recordLength);
        inOrder.verify(buffer).putBytes(encodedMsgOffset((int)tail), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putInt(typeOffset((int)tail), MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), recordLength);
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
        inOrder.verify(buffer).putLongOrdered(TAIL_COUNTER_INDEX, tail + alignedRecordLength + HEADER_LENGTH);

        inOrder.verify(buffer).putLong(0, 0L);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), -HEADER_LENGTH);
        inOrder.verify(buffer).putInt(typeOffset((int)tail), PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), HEADER_LENGTH);

        inOrder.verify(buffer).putLong(alignedRecordLength, 0L);

        inOrder.verify(buffer).putIntOrdered(lengthOffset(0), -recordLength);
        inOrder.verify(buffer).putBytes(encodedMsgOffset(0), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putInt(typeOffset(0), MSG_TYPE_ID);
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
        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putLongOrdered(TAIL_COUNTER_INDEX, tail + alignedRecordLength + HEADER_LENGTH);

        inOrder.verify(buffer).putLong(0, 0L);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), -HEADER_LENGTH);
        inOrder.verify(buffer).putInt(typeOffset((int)tail), PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset((int)tail), HEADER_LENGTH);

        inOrder.verify(buffer).putLong(alignedRecordLength, 0L);

        inOrder.verify(buffer).putIntOrdered(lengthOffset(0), -recordLength);
        inOrder.verify(buffer).putBytes(encodedMsgOffset(0), srcBuffer, srcIndex, length);
        inOrder.verify(buffer).putInt(typeOffset(0), MSG_TYPE_ID);
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

        final MutableInteger times = new MutableInteger();
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times.increment();
        final int messagesRead = ringBuffer.read(handler);

        assertThat(messagesRead, is(0));
        assertThat(times.get(), is(0));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer, times(1)).getIntVolatile(lengthOffset(headIndex));
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
        when(buffer.getInt(typeOffset(headIndex))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(headIndex + alignedRecordLength))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex + alignedRecordLength))).thenReturn(recordLength);

        final MutableInteger times = new MutableInteger();
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times.increment();
        final int messagesRead = ringBuffer.read(handler);

        assertThat(messagesRead, is(2));
        assertThat(times.get(), is(2));

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
        when(buffer.getInt(typeOffset(headIndex))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex))).thenReturn(recordLength);

        final MutableInteger times = new MutableInteger();
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times.increment();
        final int limit = 1;
        final int messagesRead = ringBuffer.read(handler, limit);

        assertThat(messagesRead, is(1));
        assertThat(times.get(), is(1));

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
        when(buffer.getInt(typeOffset(headIndex))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(headIndex + alignedRecordLength))).thenReturn(MSG_TYPE_ID);
        when(buffer.getIntVolatile(lengthOffset(headIndex + alignedRecordLength))).thenReturn(recordLength);

        final MutableInteger times = new MutableInteger();
        final MessageHandler handler =
            (msgTypeId, buffer, index, length) ->
            {
                if (times.incrementAndGet() == 2)
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
            assertThat(times.get(), is(2));

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

    @Test
    public void shouldThrowExceptionForCapacityThatIsNotPowerOfTwo()
    {
        final int capacity = 777;
        final int totalBufferLength = capacity + RingBufferDescriptor.TRAILER_LENGTH;
        assertThrows(IllegalStateException.class,
            () -> new OneToOneRingBuffer(new UnsafeBuffer(new byte[totalBufferLength])));
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

    @Test
    public void tryClaimThrowsIllegalArgumentExceptionIfMessageTypeIdIsInvalid()
    {
        assertThrows(IllegalArgumentException.class, () -> ringBuffer.tryClaim(0, 10));
    }

    @Test
    public void tryClaimThrowsIllegalArgumentExceptionIfLengthIsBiggerThanMaxMessageLength()
    {
        assertThrows(IllegalArgumentException.class, () -> ringBuffer.tryClaim(MSG_TYPE_ID, 2000));
    }

    @Test
    public void tryClaimThrowsIllegalArgumentExceptionIfLengthIsNegative()
    {
        assertThrows(IllegalArgumentException.class, () -> ringBuffer.tryClaim(MSG_TYPE_ID, -5));
    }

    @Test
    public void tryClaimReturnsOffsetAtWhichMessageBodyCanBeWritten()
    {
        final int msgTypeId = MSG_TYPE_ID;
        final int length = 333;
        final int recordLength = HEADER_LENGTH + length;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);

        final int index = ringBuffer.tryClaim(msgTypeId, length);

        assertEquals(HEADER_LENGTH, index);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putLongOrdered(TAIL_COUNTER_INDEX, alignedRecordLength);
        inOrder.verify(buffer).putLong(alignedRecordLength, 0L);
        inOrder.verify(buffer).putIntOrdered(lengthOffset(0), -recordLength);
        inOrder.verify(buffer).putInt(typeOffset(0), msgTypeId);
    }

    @Test
    public void tryClaimReturnsInsufficientCapacityIfThereIsNotEnoughSpaceInTheBuffer()
    {
        final int length = 10;
        when(buffer.getLong(HEAD_COUNTER_CACHE_INDEX)).thenReturn(10L);
        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn((long)CAPACITY);

        final int index = ringBuffer.tryClaim(MSG_TYPE_ID, length);

        assertEquals(INSUFFICIENT_CAPACITY, index);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLong(HEAD_COUNTER_CACHE_INDEX);
        inOrder.verify(buffer).getLong(TAIL_COUNTER_INDEX);
        inOrder.verify(buffer).getLongVolatile(HEAD_COUNTER_INDEX);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void tryClaimReturnsInsufficientCapacityIfThereIsNotEnoughSpaceInTheBufferAfterWrap()
    {
        final int length = 100;
        when(buffer.getLong(HEAD_COUNTER_CACHE_INDEX)).thenReturn(22L);
        when(buffer.getLong(TAIL_COUNTER_INDEX)).thenReturn(CAPACITY * 2L - 10);
        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(CAPACITY + 111L, 3L);

        final int index = ringBuffer.tryClaim(MSG_TYPE_ID, length);

        assertEquals(INSUFFICIENT_CAPACITY, index);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLong(HEAD_COUNTER_CACHE_INDEX);
        inOrder.verify(buffer).getLong(TAIL_COUNTER_INDEX);
        inOrder.verify(buffer).getLongVolatile(HEAD_COUNTER_INDEX);
        inOrder.verify(buffer).putLong(HEAD_COUNTER_CACHE_INDEX, CAPACITY + 111L);
        inOrder.verify(buffer).getLongVolatile(HEAD_COUNTER_INDEX);
        inOrder.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 7, CAPACITY + 1 })
    public void commitThrowsIllegalArgumentExceptionIfIndexIsInvalid(final int index)
    {
        assertThrows(IllegalArgumentException.class, () -> ringBuffer.commit(index));
    }

    @Test
    public void commitThrowsIllegalStateExceptionIfSpaceWasAlreadyCommitted()
    {
        testAlreadyCommitted(ringBuffer::commit);
    }

    @Test
    public void commitThrowsIllegalStateExceptionIfSpaceWasAlreadyAborted()
    {
        testAlreadyAborted(ringBuffer::commit);
    }

    @Test
    public void commitPublishesMessageByInvertingTheLengthValue()
    {
        final int index = 32;
        final int recordIndex = index - HEADER_LENGTH;
        when(buffer.getInt(lengthOffset(recordIndex))).thenReturn(-19);

        ringBuffer.commit(index);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getInt(lengthOffset(recordIndex));
        inOrder.verify(buffer).putIntOrdered(lengthOffset(recordIndex), 19);
        inOrder.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 7, CAPACITY + 1 })
    public void abortThrowsIllegalArgumentExceptionIfIndexIsInvalid(final int index)
    {
        assertThrows(IllegalArgumentException.class, () -> ringBuffer.abort(index));
    }

    @Test
    public void abortThrowsIllegalStateExceptionIfSpaceWasAlreadyCommitted()
    {
        testAlreadyCommitted(ringBuffer::abort);
    }

    @Test
    public void abortThrowsIllegalStateExceptionIfSpaceWasAlreadyAborted()
    {
        testAlreadyAborted(ringBuffer::abort);
    }

    @Test
    public void abortMarksUnusedSpaceAsPadding()
    {
        final int index = 108;
        final int recordIndex = index - HEADER_LENGTH;
        when(buffer.getInt(lengthOffset(recordIndex))).thenReturn(-11111);

        ringBuffer.abort(index);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getInt(lengthOffset(recordIndex));
        inOrder.verify(buffer).putInt(typeOffset(recordIndex), PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered(lengthOffset(recordIndex), 11111);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldContinueOnControlledRead()
    {
        final String msg = "Hello World";
        final ExpandableArrayBuffer srcBuffer = new ExpandableArrayBuffer();
        final int srcLength = srcBuffer.putStringAscii(0, msg);
        final RingBuffer ringBuffer = new OneToOneRingBuffer(new UnsafeBuffer(new byte[1024]));

        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, srcLength));

        final ControlledMessageHandler controlledMessageHandler =
            (msgTypeId, buffer, index, length) ->
            {
                assertEquals(MSG_TYPE_ID, msgTypeId);
                assertEquals(HEADER_LENGTH, index);
                assertEquals(srcLength, length);
                assertEquals(msg, buffer.getStringAscii(index));

                return ControlledMessageHandler.Action.CONTINUE;
            };

        final int messagesRead = ringBuffer.controlledRead(controlledMessageHandler, 1);
        assertEquals(1, messagesRead);
        assertEquals(ringBuffer.producerPosition(), ringBuffer.consumerPosition());
    }

    @Test
    public void shouldAbortOnControlledRead()
    {
        final String msg = "Hello World";
        final ExpandableArrayBuffer srcBuffer = new ExpandableArrayBuffer();
        final int srcLength = srcBuffer.putStringAscii(0, msg);
        final RingBuffer ringBuffer = new OneToOneRingBuffer(new UnsafeBuffer(new byte[1024]));

        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, srcLength));

        final ControlledMessageHandler controlledMessageHandler =
            (msgTypeId, buffer, index, length) ->
            {
                assertEquals(MSG_TYPE_ID, msgTypeId);
                assertEquals(HEADER_LENGTH, index);
                assertEquals(srcLength, length);
                assertEquals(msg, buffer.getStringAscii(index));

                return ControlledMessageHandler.Action.ABORT;
            };

        final int messagesRead = ringBuffer.controlledRead(controlledMessageHandler, 1);
        assertEquals(0, messagesRead);
        assertEquals(0, ringBuffer.consumerPosition());
    }

    @Test
    public void shouldAbortOnControlledReadOfSecondMessage()
    {
        final String msg = "Hello World";
        final ExpandableArrayBuffer srcBuffer = new ExpandableArrayBuffer();
        final int srcLength = srcBuffer.putStringAscii(0, msg);
        final RingBuffer ringBuffer = new OneToOneRingBuffer(new UnsafeBuffer(new byte[1024]));
        final MutableInteger counter = new MutableInteger();

        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, srcLength));
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, srcLength));

        final ControlledMessageHandler controlledMessageHandler =
            (msgTypeId, buffer, index, length) ->
            {
                return counter.getAndIncrement() == 0 ?
                    ControlledMessageHandler.Action.CONTINUE : ControlledMessageHandler.Action.ABORT;
            };

        final int messagesRead = ringBuffer.controlledRead(controlledMessageHandler);
        assertEquals(2, counter.get());
        assertEquals(1, messagesRead);
        assertNotEquals(ringBuffer.producerPosition(), ringBuffer.consumerPosition());
    }

    @Test
    public void shouldCommitOnEachMessage()
    {
        final String msg = "Hello World";
        final ExpandableArrayBuffer srcBuffer = new ExpandableArrayBuffer();
        final int srcLength = srcBuffer.putStringAscii(0, msg);
        final RingBuffer ringBuffer = new OneToOneRingBuffer(new UnsafeBuffer(new byte[1024]));
        final MutableInteger counter = new MutableInteger();

        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, srcLength));
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, srcLength));

        final ControlledMessageHandler controlledMessageHandler =
            (msgTypeId, buffer, index, length) ->
            {
                if (0 == counter.getAndIncrement())
                {
                    assertEquals(0L, ringBuffer.consumerPosition());
                }
                else
                {
                    assertEquals(ringBuffer.producerPosition() / 2, ringBuffer.consumerPosition());
                }

                return ControlledMessageHandler.Action.COMMIT;
            };

        final int messagesRead = ringBuffer.controlledRead(controlledMessageHandler);
        assertEquals(2, counter.get());
        assertEquals(2, messagesRead);
        assertEquals(ringBuffer.producerPosition(), ringBuffer.consumerPosition());
    }

    private void testAlreadyCommitted(final IntConsumer action)
    {
        final int index = HEADER_LENGTH;
        final int recordIndex = index - HEADER_LENGTH;
        when(buffer.getInt(lengthOffset(recordIndex))).thenReturn(0);

        final IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> action.accept(index));
        assertEquals("claimed space previously committed", exception.getMessage());
    }

    private void testAlreadyAborted(final IntConsumer action)
    {
        final int index = 128;
        final int recordIndex = index - HEADER_LENGTH;
        when(buffer.getInt(lengthOffset(recordIndex))).thenReturn(10);
        when(buffer.getInt(typeOffset(recordIndex))).thenReturn(PADDING_MSG_TYPE_ID);

        final IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> action.accept(index));
        assertEquals("claimed space previously aborted", exception.getMessage());
    }
}
