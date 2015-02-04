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
package uk.co.real_logic.agrona.concurrent.broadcast;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.co.real_logic.agrona.BitUtil.align;
import static uk.co.real_logic.agrona.concurrent.broadcast.RecordDescriptor.*;

public class BroadcastReceiverTest
{
    private static final int MSG_TYPE_ID = 7;
    private static final int CAPACITY = 1024;
    private static final int TOTAL_BUFFER_LENGTH = CAPACITY + BroadcastBufferDescriptor.TRAILER_LENGTH;
    private static final int TAIL_INTENT_COUNTER_OFFSET = CAPACITY + BroadcastBufferDescriptor.TAIL_INTENT_COUNTER_OFFSET;
    private static final int TAIL_COUNTER_INDEX = CAPACITY + BroadcastBufferDescriptor.TAIL_COUNTER_OFFSET;
    private static final int LATEST_COUNTER_INDEX = CAPACITY + BroadcastBufferDescriptor.LATEST_COUNTER_OFFSET;

    private final UnsafeBuffer buffer = mock(UnsafeBuffer.class);
    private BroadcastReceiver broadcastReceiver;

    @Before
    public void setUp()
    {
        when(buffer.capacity()).thenReturn(TOTAL_BUFFER_LENGTH);

        broadcastReceiver = new BroadcastReceiver(buffer);
    }

    @Test
    public void shouldCalculateCapacityForBuffer()
    {
        assertThat(broadcastReceiver.capacity(), is(CAPACITY));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionForCapacityThatIsNotPowerOfTwo()
    {
        final int capacity = 777;
        final int totalBufferLength = capacity + BroadcastBufferDescriptor.TRAILER_LENGTH;

        when(buffer.capacity()).thenReturn(totalBufferLength);

        new BroadcastReceiver(buffer);
    }

    @Test
    public void shouldNotBeLappedBeforeReception()
    {
        assertThat(broadcastReceiver.lappedCount(), is(0L));
    }

    @Test
    public void shouldNotReceiveFromEmptyBuffer()
    {
        assertFalse(broadcastReceiver.receiveNext());
    }

    @Test
    public void shouldReceiveFirstMessageFromBuffer()
    {
        final int length = 8;
        final int recordLength = align(length + HEADER_LENGTH, RECORD_ALIGNMENT);
        final long tail = recordLength;
        final long latestRecord = tail - recordLength;
        final int recordOffset = (int)latestRecord;

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET)).thenReturn(tail);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getInt(msgLengthOffset(recordOffset))).thenReturn(length);
        when(buffer.getInt(msgTypeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertTrue(broadcastReceiver.receiveNext());
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffset)));
        assertThat(broadcastReceiver.length(), is(length));

        assertTrue(broadcastReceiver.validate());

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLongVolatile(TAIL_COUNTER_INDEX);
        inOrder.verify(buffer).getLongVolatile(TAIL_INTENT_COUNTER_OFFSET);
    }

    @Test
    public void shouldReceiveTwoMessagesFromBuffer()
    {
        final int length = 8;
        final int recordLength = align(length + HEADER_LENGTH, RECORD_ALIGNMENT);
        final long tail = recordLength * 2;
        final long latestRecord = tail - recordLength;
        final int recordOffsetOne = 0;
        final int recordOffsetTwo = (int)latestRecord;

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET)).thenReturn(tail);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        when(buffer.getInt(msgLengthOffset(recordOffsetOne))).thenReturn(length);
        when(buffer.getInt(msgTypeOffset(recordOffsetOne))).thenReturn(MSG_TYPE_ID);

        when(buffer.getInt(msgLengthOffset(recordOffsetTwo))).thenReturn(length);
        when(buffer.getInt(msgTypeOffset(recordOffsetTwo))).thenReturn(MSG_TYPE_ID);

        assertTrue(broadcastReceiver.receiveNext());
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffsetOne)));
        assertThat(broadcastReceiver.length(), is(length));

        assertTrue(broadcastReceiver.validate());

        assertTrue(broadcastReceiver.receiveNext());
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffsetTwo)));
        assertThat(broadcastReceiver.length(), is(length));

        assertTrue(broadcastReceiver.validate());

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLongVolatile(TAIL_COUNTER_INDEX);
        inOrder.verify(buffer).getLongVolatile(TAIL_INTENT_COUNTER_OFFSET);
        inOrder.verify(buffer).getLongVolatile(TAIL_INTENT_COUNTER_OFFSET);
        inOrder.verify(buffer).getLongVolatile(TAIL_COUNTER_INDEX);
        inOrder.verify(buffer).getLongVolatile(TAIL_INTENT_COUNTER_OFFSET);
        inOrder.verify(buffer).getLongVolatile(TAIL_INTENT_COUNTER_OFFSET);
    }

    @Test
    public void shouldLateJoinTransmission()
    {
        final int length = 8;
        final int recordLength = align(length + HEADER_LENGTH, RECORD_ALIGNMENT);
        final long tail = (CAPACITY * 3L) + HEADER_LENGTH + recordLength;
        final long latestRecord = tail - recordLength;
        final int recordOffset = (int)latestRecord & (CAPACITY - 1);

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET)).thenReturn(tail);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getLong(LATEST_COUNTER_INDEX)).thenReturn(latestRecord);

        when(buffer.getInt(msgLengthOffset(recordOffset))).thenReturn(length);
        when(buffer.getInt(msgTypeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertTrue(broadcastReceiver.receiveNext());
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffset)));
        assertThat(broadcastReceiver.length(), is(length));

        assertTrue(broadcastReceiver.validate());
        assertThat(broadcastReceiver.lappedCount(), is(greaterThan(0L)));
    }

    @Test
    public void shouldCopeWithPaddingRecordAndWrapOfBufferForNextRecord()
    {
        final int length = 120;
        final int recordLength = align(length + HEADER_LENGTH, RECORD_ALIGNMENT);
        final long catchupTail = (CAPACITY * 2L) - HEADER_LENGTH;
        final long postPaddingTail = catchupTail + HEADER_LENGTH + recordLength;
        final long latestRecord = catchupTail - recordLength;
        final int catchupOffset = (int)latestRecord & (CAPACITY - 1);

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET))
            .thenReturn(catchupTail)
            .thenReturn(postPaddingTail);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX))
            .thenReturn(catchupTail)
            .thenReturn(postPaddingTail);
        when(buffer.getLong(LATEST_COUNTER_INDEX)).thenReturn(latestRecord);
        when(buffer.getInt(msgLengthOffset(catchupOffset))).thenReturn(length);
        when(buffer.getInt(msgTypeOffset(catchupOffset))).thenReturn(MSG_TYPE_ID);

        final int paddingOffset = (int)catchupTail & (CAPACITY - 1);
        final int recordOffset = (int)(postPaddingTail - recordLength) & (CAPACITY - 1);
        when(buffer.getInt(msgTypeOffset(paddingOffset))).thenReturn(PADDING_MSG_TYPE_ID);

        when(buffer.getInt(msgLengthOffset(recordOffset))).thenReturn(length);
        when(buffer.getInt(msgTypeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertTrue(broadcastReceiver.receiveNext()); // To catch up to record before padding.

        assertTrue(broadcastReceiver.receiveNext()); // no skip over the padding and read next record.
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffset)));
        assertThat(broadcastReceiver.length(), is(length));

        assertTrue(broadcastReceiver.validate());
    }

    @Test
    public void shouldDealWithRecordBecomingInvalidDueToOverwrite()
    {
        final int length = 8;
        final int recordLength = align(length + HEADER_LENGTH, RECORD_ALIGNMENT);
        final long tail = recordLength;
        final long latestRecord = tail - recordLength;
        final int recordOffset = (int)latestRecord;

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET))
            .thenReturn(tail)
            .thenReturn(tail + (CAPACITY - (recordLength)));
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        when(buffer.getInt(msgLengthOffset(recordOffset))).thenReturn(length);
        when(buffer.getInt(msgTypeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertTrue(broadcastReceiver.receiveNext());
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffset)));
        assertThat(broadcastReceiver.length(), is(length));

        assertFalse(broadcastReceiver.validate()); // Need to receiveNext() to catch up with transmission again.

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLongVolatile(TAIL_COUNTER_INDEX);
    }
}