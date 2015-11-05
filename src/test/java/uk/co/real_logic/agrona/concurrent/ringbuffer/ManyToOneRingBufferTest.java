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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.co.real_logic.agrona.BitUtil.align;
import static uk.co.real_logic.agrona.concurrent.ringbuffer.ManyToOneRingBuffer.PADDING_MSG_TYPE_ID;
import static uk.co.real_logic.agrona.concurrent.ringbuffer.RecordDescriptor.ALIGNMENT;
import static uk.co.real_logic.agrona.concurrent.ringbuffer.RecordDescriptor.HEADER_LENGTH;
import static uk.co.real_logic.agrona.concurrent.ringbuffer.RecordDescriptor.encodedMsgOffset;
import static uk.co.real_logic.agrona.concurrent.ringbuffer.RecordDescriptor.lengthOffset;
import static uk.co.real_logic.agrona.concurrent.ringbuffer.RecordDescriptor.makeHeader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import uk.co.real_logic.agrona.concurrent.MemoryAccess;
import uk.co.real_logic.agrona.concurrent.MessageHandler;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;


@RunWith(PowerMockRunner.class)
@PrepareForTest(MemoryAccess.class)
public class ManyToOneRingBufferTest
{
    private static final int MSG_TYPE_ID = 7;
    private static final int CAPACITY = 4096;
    private static final int TOTAL_BUFFER_LENGTH = CAPACITY + RingBufferDescriptor.TRAILER_LENGTH;
    private static final int TAIL_COUNTER_INDEX = CAPACITY + RingBufferDescriptor.TAIL_POSITION_OFFSET;
    private static final int HEAD_COUNTER_INDEX = CAPACITY + RingBufferDescriptor.HEAD_POSITION_OFFSET;
    private static final int HEAD_COUNTER_CACHE_INDEX = CAPACITY + RingBufferDescriptor.HEAD_CACHE_POSITION_OFFSET;

    private UnsafeBuffer buffer;
    private ManyToOneRingBuffer ringBuffer;
    private MemoryAccess memory;

    @Before
    public void setUp()
    {
        // use the memory spy to verify memory ordering/interaction
        // NOTE: why not mock the buffer? the buffer is not a necessary collaborator in this test. Mandating it as such will
        // block us from changing the internals of the ring buffer to use a different approach to accessing memory such as
        // splitting the buffer into header/body buffers, or accessing memory directly instead of through the UnsafeBuffer
        memory = Mockito.spy(MemoryAccess.class);
        PowerMockito.mockStatic(MemoryAccess.class);
        PowerMockito.when(MemoryAccess.memory()).thenReturn(memory);
        assertEquals(memory, MemoryAccess.memory());

        // we expect the buffer backing memory to be used by the ring buffer, can use it to control assert start/end state
        buffer = new UnsafeBuffer(new byte[TOTAL_BUFFER_LENGTH]);
        ringBuffer = new ManyToOneRingBuffer(buffer);
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
        new ManyToOneRingBuffer(new UnsafeBuffer(new byte[totalBufferLength]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenMaxMessageSizeExceeded()
    {
        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, ringBuffer.maxMsgLength() + 1);
    }

    @Test
    public void shouldWriteToEmptyBuffer()
    {
        final long startTail = 0L;
        final long head = 0L;
        final int length = 8;
        final int recordLength = length + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long endTail = startTail + alignedRecordLength;

        // verify the initial counter state
        assertEquals(startTail, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
        assertEquals(head, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
        Mockito.reset(memory);


        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
        final int srcIndex = 0;

        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(memory);

        verifyTailIncrementFastPath(inOrder, startTail, endTail);

        verifyMessageWrite(inOrder, startTail, recordLength, srcBuffer, srcIndex, length);

        verifyNoMoreInteractions(memory);

        // verify post operation counter state
        assertEquals(endTail, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
        assertEquals(head, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
    }

    @Test
    public void shouldRejectWriteWhenInsufficientSpace()
    {
        final int length = 200;
        final long head = 0L;
        final long tail = head + (CAPACITY - align(length - ALIGNMENT, ALIGNMENT));

        buffer.putLong(HEAD_COUNTER_INDEX, head);
        buffer.putLong(TAIL_COUNTER_INDEX, tail);
        Mockito.reset(memory);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertFalse(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(memory);

        verifyTailIncrementFail(inOrder);

        verifyNoMoreInteractions(memory);

        // verify post operation counter state
        assertEquals(tail, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
        assertEquals(head, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
    }

    @Test
    public void shouldRejectWriteWhenBufferFull()
    {
        final int length = 8;
        final long head = 0L;
        final long tail = head + CAPACITY;

        buffer.putLong(HEAD_COUNTER_INDEX, head);
        buffer.putLong(TAIL_COUNTER_INDEX, tail);
        Mockito.reset(memory);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertFalse(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(memory);

        verifyTailIncrementFail(inOrder);

        verifyNoMoreInteractions(memory);

        // verify post operation counter state
        assertEquals(tail, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
        assertEquals(head, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
    }


    @Test
    public void shouldInsertPaddingRecordPlusMessageOnBufferWrap()
    {
        final int length = 200;
        final int paddingLength = HEADER_LENGTH;
        final int recordLength = length + paddingLength;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long startTail = CAPACITY - paddingLength;
        final long head = startTail - (ALIGNMENT * 4);
        final long endTail = startTail + alignedRecordLength + ALIGNMENT;

        buffer.putLong(HEAD_COUNTER_INDEX, head);
        buffer.putLong(TAIL_COUNTER_INDEX, startTail);
        Mockito.reset(memory);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));

        final InOrder inOrder = inOrder(memory);

        verifyTailIncrementAndHeadCacheUpdate(inOrder, startTail, head, endTail);

        verifyPaddingRecordWrite(inOrder, startTail, paddingLength);

        verifyMessageWrite(inOrder, startTail + paddingLength, recordLength, srcBuffer, srcIndex, length);

        verifyNoMoreInteractions(memory);

        // verify post operation counter state
        assertEquals(endTail, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
        assertEquals(head, buffer.getLongVolatile(HEAD_COUNTER_INDEX));

    }


    @Test
    public void shouldInsertPaddingRecordPlusMessageOnBufferWrapWithHeadEqualToTail()
    {
        final int length = 200;
        final int paddingLength = HEADER_LENGTH;
        final int recordLength = length + paddingLength;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long startTail = CAPACITY - paddingLength;
        final long head = startTail;
        final long endTail = startTail + alignedRecordLength + paddingLength;

        buffer.putLong(HEAD_COUNTER_INDEX, head);
        buffer.putLong(TAIL_COUNTER_INDEX, startTail);
        Mockito.reset(memory);

        final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

        final int srcIndex = 0;
        assertTrue(ringBuffer.write(MSG_TYPE_ID, srcBuffer, srcIndex, length));


        final InOrder inOrder = inOrder(memory);

        verifyTailIncrementAndHeadCacheUpdate(inOrder, startTail, head, endTail);

        verifyPaddingRecordWrite(inOrder, startTail, paddingLength);

        verifyMessageWrite(inOrder, startTail + paddingLength, recordLength, srcBuffer, srcIndex, length);

        verifyNoMoreInteractions(memory);

        // verify post operation counter state
        assertEquals(endTail, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
        assertEquals(head, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
    }

    @Test
    public void shouldReadNothingFromEmptyBuffer()
    {
        final long head = 0L;
        assertEquals(head, buffer.getLong(HEAD_COUNTER_INDEX));
        Mockito.reset(memory);

        final MessageHandler handler = (msgTypeId, buffer, index, length) -> fail("should not be called");
        final int messagesRead = ringBuffer.read(handler);

        final InOrder inOrder = inOrder(memory);
        verifyReadFail(inOrder, head);

        verifyNoMoreInteractions(memory);

        assertThat(messagesRead, is(0));
        assertEquals(head, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
    }

    private void verifyReadFail(final InOrder inOrder, final long head)
    {
        inOrder.verify(memory).getLong(buffer.byteArray(), buffer.addressOffset() + HEAD_COUNTER_INDEX);
        inOrder.verify(memory).getLongVolatile(buffer.byteArray(), buffer.addressOffset() + lengthOffset((int)head));
    }

    @Test
    public void shouldNotReadSingleMessagePartWayThroughWriting()
    {
        final long startTail = 0L;
        final long head = 0L;
        final int length = 8;
        final int recordLength = length + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long endTail = startTail + alignedRecordLength;

        final int headIndex = (int)head;
        // simulate half written message
        buffer.putLongOrdered(TAIL_COUNTER_INDEX, endTail); // producer successfully moved tail
        buffer.putLongOrdered(0, makeHeader(-recordLength, MSG_TYPE_ID));
        buffer.putBytes(encodedMsgOffset((int) startTail), new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        // producer has not released the message by writing in the positive length
        Mockito.reset(memory);

        final int[] times = new int[1];
        final MessageHandler handler = (msgTypeId, buffer, index, l) -> times[0]++;
        final int messagesRead = ringBuffer.read(handler);

        assertThat(messagesRead, is(0));
        assertThat(times[0], is(0));

        final InOrder inOrder = inOrder(memory);
        verifyReadFail(inOrder, head);

        verifyNoMoreInteractions(memory);
    }

    @Test
    public void shouldReadTwoMessages()
    {
        final long startTail = 0L;
        final long head = 0L;
        final int msgLength = 16;
        final int recordLength = msgLength + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long endTail = startTail + 2 * alignedRecordLength;

        final int headIndex = (int)head;

        // simulate 2 written messages state
        buffer.putLongOrdered(TAIL_COUNTER_INDEX, endTail);
        buffer.putLongOrdered((int) startTail, makeHeader(recordLength, MSG_TYPE_ID));
        buffer.putLongOrdered((int) (startTail + alignedRecordLength), makeHeader(recordLength, MSG_TYPE_ID));
        Mockito.reset(memory);

        final int[] times = new int[1];
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times[0]++;
        final int messagesRead = ringBuffer.read(handler);

        assertThat(messagesRead, is(2));
        assertThat(times[0], is(2));
        final InOrder inOrder = inOrder(memory);
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesOffset = buffer.addressOffset();
        inOrder.verify(memory).getLong(rbBytesRef, rbBytesOffset + HEAD_COUNTER_INDEX);
        // read message 1 length
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + lengthOffset((int)head));
        // read message 2 length
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + lengthOffset((int)head + alignedRecordLength));
        // read message 3 length -> is zero
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + lengthOffset((int)endTail));

        inOrder.verify(memory).setMemory(rbBytesRef, rbBytesOffset + head, alignedRecordLength * 2, (byte)0);
        inOrder.verify(memory).putOrderedLong(rbBytesRef, rbBytesOffset + HEAD_COUNTER_INDEX, endTail);

        verifyNoMoreInteractions(memory);

        // verify post operation counter state
        assertEquals(endTail, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
        assertEquals(endTail, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
    }

    @Test
    public void shouldLimitReadOfMessages()
    {
        final long startTail = 0L;
        final long head = 0L;
        final int msgLength = 16;
        final int recordLength = msgLength + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long endTail = startTail + 2 * alignedRecordLength;

        final int headIndex = (int)head;

        // simulate 2 written messages state
        buffer.putLongOrdered(TAIL_COUNTER_INDEX, endTail);
        buffer.putLongOrdered((int) startTail, makeHeader(recordLength, MSG_TYPE_ID));
        buffer.putLongOrdered((int) (startTail + alignedRecordLength), makeHeader(recordLength, MSG_TYPE_ID));
        Mockito.reset(memory);

        final int[] times = new int[1];
        final MessageHandler handler = (msgTypeId, buffer, index, length) -> times[0]++;
        final int limit = 1;
        final int messagesRead = ringBuffer.read(handler, limit);

        assertThat(messagesRead, is(1));
        assertThat(times[0], is(1));

        final InOrder inOrder = inOrder(memory);
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesOffset = buffer.addressOffset();

        inOrder.verify(memory).getLong(rbBytesRef, rbBytesOffset + HEAD_COUNTER_INDEX);
        // read message 1 length
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + lengthOffset((int)head));

        inOrder.verify(memory).setMemory(rbBytesRef, rbBytesOffset + head, alignedRecordLength, (byte)0);
        inOrder.verify(memory).putOrderedLong(rbBytesRef, rbBytesOffset + HEAD_COUNTER_INDEX, head + alignedRecordLength);

        verifyNoMoreInteractions(memory);

        // verify post operation counter state
        assertEquals(endTail, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
        assertEquals(head + alignedRecordLength, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
    }

    @Test
    public void shouldCopeWithExceptionFromHandler()
    {
        final long startTail = 0L;
        final long head = 0L;
        final int msgLength = 16;
        final int recordLength = msgLength + HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        final long endTail = startTail + 2 * alignedRecordLength;

        final int headIndex = (int)head;

        // simulate 2 written messages state
        buffer.putLongOrdered(TAIL_COUNTER_INDEX, endTail);
        buffer.putLongOrdered((int) startTail, makeHeader(recordLength, MSG_TYPE_ID));
        buffer.putLongOrdered((int) (startTail + alignedRecordLength), makeHeader(recordLength, MSG_TYPE_ID));
        Mockito.reset(memory);

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

            final InOrder inOrder = inOrder(memory);
            final byte[] rbBytesRef = buffer.byteArray();
            final long rbBytesOffset = buffer.addressOffset();
            inOrder.verify(memory).getLong(rbBytesRef, rbBytesOffset + HEAD_COUNTER_INDEX);
            // read message 1 length
            inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + lengthOffset((int)head));
            // read message 2 length
            inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + lengthOffset((int)head + alignedRecordLength));
            // we hit an exception, so no attempt to read message 3 which is not there anyhow

            inOrder.verify(memory).setMemory(rbBytesRef, rbBytesOffset + head, alignedRecordLength * 2, (byte)0);
            inOrder.verify(memory).putOrderedLong(rbBytesRef, rbBytesOffset + HEAD_COUNTER_INDEX, endTail);

            verifyNoMoreInteractions(memory);

            // verify post operation counter state
            assertEquals(endTail, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
            assertEquals(endTail, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
            return;
        }

        fail("Should have thrown exception");
    }

    @Test
    public void shouldNotUnblockWhenEmpty()
    {
        final long position = ALIGNMENT * 4;
        buffer.putLong(HEAD_COUNTER_INDEX, position);
        buffer.putLong(TAIL_COUNTER_INDEX, position);
        reset(memory);

        assertFalse(ringBuffer.unblock());

        final InOrder inOrder = inOrder(memory);
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesOffset = buffer.addressOffset();
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + HEAD_COUNTER_INDEX);
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + TAIL_COUNTER_INDEX);

        verifyNoMoreInteractions(memory);

        // verify post operation counter state
        assertEquals(position, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
        assertEquals(position, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
    }

    @Test
    public void shouldUnblockMessageWithHeader()
    {
        final int messageLength = ALIGNMENT * 4;
        // setup stuck producer state
        buffer.putLong(HEAD_COUNTER_INDEX, messageLength);
        buffer.putLong(TAIL_COUNTER_INDEX, messageLength * 2);
        // producer is blocked on this message
        buffer.putLongOrdered(messageLength, makeHeader(-messageLength, MSG_TYPE_ID));
        reset(memory);

        assertTrue(ringBuffer.unblock());

        final InOrder inOrder = inOrder(memory);
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesOffset = buffer.addressOffset();
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + HEAD_COUNTER_INDEX);
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + TAIL_COUNTER_INDEX);
        // detect blocked message
        inOrder.verify(memory).getIntVolatile(rbBytesRef, rbBytesOffset + lengthOffset(messageLength));

        // convert blocked message to padding
        inOrder.verify(memory).putOrderedLong(rbBytesRef, rbBytesOffset + messageLength,
                                              makeHeader(messageLength, PADDING_MSG_TYPE_ID));

        verifyNoMoreInteractions(memory);

        // verify post operation counter state
        assertEquals(messageLength, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
        assertEquals(messageLength * 2, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
    }

    @Test
    public void shouldUnblockGapWithZeros()
    {
        final int messageLength = ALIGNMENT * 4;
        // setup stuck producer state
        buffer.putLong(HEAD_COUNTER_INDEX, messageLength);
        buffer.putLong(TAIL_COUNTER_INDEX, messageLength * 3);

        // some producer has setup a message after head
        buffer.putIntOrdered(messageLength * 2, messageLength);
        reset(memory);

        assertTrue(ringBuffer.unblock());

        final InOrder inOrder = inOrder(memory);
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesOffset = buffer.addressOffset();
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + HEAD_COUNTER_INDEX);
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesOffset + TAIL_COUNTER_INDEX);
        inOrder.verify(memory).putOrderedLong(rbBytesRef, rbBytesOffset + messageLength,
                makeHeader(messageLength, PADDING_MSG_TYPE_ID));

        // verify post operation counter state
        assertEquals(messageLength, buffer.getLongVolatile(HEAD_COUNTER_INDEX));
        assertEquals(messageLength * 3, buffer.getLongVolatile(TAIL_COUNTER_INDEX));
    }

    @Test
    public void shouldNotUnblockGapWithMessageRaceOnSecondMessageIncreasingTailThenInterrupting()
    {
        final int messageLength = ALIGNMENT * 4;
        buffer.putLong(HEAD_COUNTER_INDEX, messageLength);
        buffer.putLong(TAIL_COUNTER_INDEX, messageLength * 3);
        reset(memory);

        when(memory.getIntVolatile(buffer.byteArray(), buffer.addressOffset() + messageLength * 2)).
            thenReturn(0).
            thenReturn(messageLength);

        assertFalse(ringBuffer.unblock());

        verify(memory, never()).putOrderedLong(buffer.byteArray(), buffer.addressOffset() + messageLength,
                makeHeader(messageLength, PADDING_MSG_TYPE_ID));
    }

    @Test
    public void shouldNotUnblockGapWithMessageRaceWhenScanForwardTakesAnInterrupt()
    {
        final int messageLength = ALIGNMENT * 4;
        buffer.putLong(HEAD_COUNTER_INDEX, messageLength);
        buffer.putLong(TAIL_COUNTER_INDEX, messageLength * 3);
        reset(memory);

        when(memory.getIntVolatile(buffer.byteArray(), buffer.addressOffset() + messageLength * 2)).
            thenReturn(0).
            thenReturn(messageLength);

        when(memory.getIntVolatile(buffer.byteArray(), buffer.addressOffset() + messageLength * 2 + ALIGNMENT)).thenReturn(7);

        assertFalse(ringBuffer.unblock());

        verify(memory, never()).putOrderedLong(buffer.byteArray(), buffer.addressOffset() + messageLength,
                makeHeader(messageLength, PADDING_MSG_TYPE_ID));
    }


    /** verifiers **/
    private void verifyMessageWrite(final InOrder inOrder, final long tail, final int recordLength, final UnsafeBuffer srcBuffer,
            final int srcIndex, final int srcLength)
    {
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesBase = buffer.addressOffset();

        final long tailOffset = tail % CAPACITY;
        inOrder.verify(memory).putOrderedLong(rbBytesRef, rbBytesBase + tailOffset, makeHeader(-recordLength, MSG_TYPE_ID));
        inOrder.verify(memory).copyMemory(srcBuffer.byteArray(), srcBuffer.addressOffset() + srcIndex,
                rbBytesRef, rbBytesBase + encodedMsgOffset((int) tailOffset),
                srcLength);
        inOrder.verify(memory).putOrderedInt(rbBytesRef, rbBytesBase + lengthOffset((int) tailOffset), recordLength);
    }

    private void verifyPaddingRecordWrite(final InOrder inOrder, final long index, final int paddingLength)
    {
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesBase = buffer.addressOffset();
        inOrder.verify(memory).putOrderedLong(rbBytesRef, rbBytesBase + (index % CAPACITY),
                makeHeader(paddingLength, PADDING_MSG_TYPE_ID));
    }

    private void verifyTailIncrementFastPath(final InOrder inOrder, final long startTail, final long endTail)
    {
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesBase = buffer.addressOffset();
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesBase + HEAD_COUNTER_CACHE_INDEX);
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesBase + TAIL_COUNTER_INDEX);
        inOrder.verify(memory).compareAndSwapLong(rbBytesRef, rbBytesBase + TAIL_COUNTER_INDEX, startTail, endTail);
    }

    private void verifyTailIncrementFail(final InOrder inOrder)
    {
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesBase = buffer.addressOffset();

        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesBase + HEAD_COUNTER_CACHE_INDEX);
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesBase + TAIL_COUNTER_INDEX);
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesBase + HEAD_COUNTER_INDEX);
    }

    private void verifyTailIncrementAndHeadCacheUpdate(final InOrder inOrder, final long startTail, final long head,
            final long endTail)
    {
        final byte[] rbBytesRef = buffer.byteArray();
        final long rbBytesBase = buffer.addressOffset();
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesBase + HEAD_COUNTER_CACHE_INDEX);
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesBase + TAIL_COUNTER_INDEX);
        inOrder.verify(memory).getLongVolatile(rbBytesRef, rbBytesBase + HEAD_COUNTER_INDEX);
        inOrder.verify(memory).putOrderedLong(rbBytesRef, rbBytesBase + HEAD_COUNTER_CACHE_INDEX, head);
        // increment including the padding
        inOrder.verify(memory).compareAndSwapLong(rbBytesRef, rbBytesBase + TAIL_COUNTER_INDEX, startTail, endTail);
    }
}
