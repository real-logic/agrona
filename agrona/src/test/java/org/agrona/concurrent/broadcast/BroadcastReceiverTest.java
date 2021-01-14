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
package org.agrona.concurrent.broadcast;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.broadcast.RecordDescriptor.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BroadcastReceiverTest
{
    private static final int MSG_TYPE_ID = 7;
    private static final int CAPACITY = 1024;
    private static final int TOTAL_BUFFER_LENGTH = CAPACITY + BroadcastBufferDescriptor.TRAILER_LENGTH;
    private static final int TAIL_INTENT_COUNTER_OFFSET =
        CAPACITY + BroadcastBufferDescriptor.TAIL_INTENT_COUNTER_OFFSET;
    private static final int TAIL_COUNTER_INDEX = CAPACITY + BroadcastBufferDescriptor.TAIL_COUNTER_OFFSET;
    private static final int LATEST_COUNTER_INDEX = CAPACITY + BroadcastBufferDescriptor.LATEST_COUNTER_OFFSET;

    private final UnsafeBuffer buffer = mock(UnsafeBuffer.class);
    private BroadcastReceiver broadcastReceiver;

    @BeforeEach
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

    @Test
    public void shouldThrowExceptionForCapacityThatIsNotPowerOfTwo()
    {
        final int capacity = 777;
        final int totalBufferLength = capacity + BroadcastBufferDescriptor.TRAILER_LENGTH;

        when(buffer.capacity()).thenReturn(totalBufferLength);

        assertThrows(IllegalStateException.class, () -> new BroadcastReceiver(buffer));
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
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);
        final long tail = recordLengthAligned;
        final long latestRecord = tail - recordLengthAligned;
        final int recordOffset = (int)latestRecord;

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET)).thenReturn(tail);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getInt(lengthOffset(recordOffset))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

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
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);
        final long tail = recordLengthAligned * 2L;
        final long latestRecord = tail - recordLengthAligned;
        final int recordOffsetOne = 0;
        final int recordOffsetTwo = (int)latestRecord;

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET)).thenReturn(tail);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        when(buffer.getInt(lengthOffset(recordOffsetOne))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(recordOffsetOne))).thenReturn(MSG_TYPE_ID);

        when(buffer.getInt(lengthOffset(recordOffsetTwo))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(recordOffsetTwo))).thenReturn(MSG_TYPE_ID);

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
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);
        final long tail = (CAPACITY * 3L) + HEADER_LENGTH + recordLengthAligned;
        final long latestRecord = tail - recordLengthAligned;
        final int recordOffset = (int)latestRecord & (CAPACITY - 1);

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET)).thenReturn(tail);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getLongVolatile(LATEST_COUNTER_INDEX)).thenReturn(latestRecord);

        when(buffer.getInt(lengthOffset(recordOffset))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

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
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);
        final long catchupTail = (CAPACITY * 2L) - HEADER_LENGTH;
        final long postPaddingTail = catchupTail + HEADER_LENGTH + recordLengthAligned;
        final long latestRecord = catchupTail - recordLengthAligned;
        final int catchupOffset = (int)latestRecord & (CAPACITY - 1);

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET))
            .thenReturn(catchupTail)
            .thenReturn(postPaddingTail);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX))
            .thenReturn(catchupTail)
            .thenReturn(postPaddingTail);
        when(buffer.getLongVolatile(LATEST_COUNTER_INDEX)).thenReturn(latestRecord);
        when(buffer.getInt(lengthOffset(catchupOffset))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(catchupOffset))).thenReturn(MSG_TYPE_ID);

        final int paddingOffset = (int)catchupTail & (CAPACITY - 1);
        final int recordOffset = (int)(postPaddingTail - recordLengthAligned) & (CAPACITY - 1);
        when(buffer.getInt(typeOffset(paddingOffset))).thenReturn(PADDING_MSG_TYPE_ID);

        when(buffer.getInt(lengthOffset(recordOffset))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

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
        final int recordLength = length + HEADER_LENGTH;
        final int recordLengthAligned = align(recordLength, RECORD_ALIGNMENT);
        final long tail = recordLengthAligned;
        final long latestRecord = tail - recordLengthAligned;
        final int recordOffset = (int)latestRecord;

        when(buffer.getLongVolatile(TAIL_INTENT_COUNTER_OFFSET))
            .thenReturn(tail)
            .thenReturn(tail + (CAPACITY - (recordLengthAligned)));
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        when(buffer.getInt(lengthOffset(recordOffset))).thenReturn(recordLength);
        when(buffer.getInt(typeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertTrue(broadcastReceiver.receiveNext());
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffset)));
        assertThat(broadcastReceiver.length(), is(length));

        assertFalse(broadcastReceiver.validate()); // Need to receiveNext() to catch up with transmission again.

        verify(buffer).getLongVolatile(TAIL_COUNTER_INDEX);
    }
}
