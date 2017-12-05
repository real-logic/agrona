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
import org.agrona.concurrent.broadcast.fixed.BroadcastReceiver.ReceiveReturnType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.agrona.concurrent.broadcast.fixed.RecordDescriptor.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BroadcastReceiverTest
{
    private static final int MSG_TYPE_ID = 7;
    private static final int MSG_CAPACITY = 1024;
    private static final int MAX_MESSAGE_SIZE = 8;
    private static final int CAPACITY = BroadcastBufferDescriptor.calculateCapacity(MAX_MESSAGE_SIZE, MSG_CAPACITY);
    private static final int TOTAL_BUFFER_LENGTH = CAPACITY + BroadcastBufferDescriptor.TRAILER_LENGTH;
    private static final int LATEST_COUNTER_INDEX = CAPACITY + BroadcastBufferDescriptor.LATEST_COUNTER_OFFSET;

    private final UnsafeBuffer buffer = mock(UnsafeBuffer.class);
    private BroadcastReceiver broadcastReceiver;

    @Before
    public void setUp()
    {
        when(buffer.capacity()).thenReturn(TOTAL_BUFFER_LENGTH);
        //mimic an already configured transmitter on it
        when(buffer.getIntVolatile(CAPACITY + BroadcastBufferDescriptor.RECORD_SIZE_OFFSET)).thenReturn(
            calculateRecordSize(MAX_MESSAGE_SIZE));
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
    public void shouldNotBeLossesBeforeReception()
    {
        assertThat(broadcastReceiver.lostTransmissions(), is(0L));
    }

    @Test
    public void shouldNotReceiveFromEmptyBuffer()
    {
        assertThat(broadcastReceiver.receiveNext(), is(ReceiveReturnType.NOT_AVAILABLE));
    }


    @Test
    public void shouldReceiveFirstMessageFromBuffer()
    {
        final int length = 8;
        assert length <= MAX_MESSAGE_SIZE;
        final long sequenceIndicator = 1;
        final int recordOffset = 0;
        final int sequenceIndicatorOffset = 0;

        when(buffer.getLongVolatile(sequenceIndicatorOffset)).thenReturn(sequenceIndicator);
        when(buffer.getInt(lengthOffset(recordOffset))).thenReturn(length);
        when(buffer.getInt(typeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertThat(broadcastReceiver.receiveNext(), is(ReceiveReturnType.ANY_AVAILABLE));
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffset)));
        assertThat(broadcastReceiver.length(), is(length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffset);
        inOrder.verify(buffer).getInt(lengthOffset(recordOffset));

        assertTrue(broadcastReceiver.validate());

        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffset);

    }

    @Test
    public void shouldReceiveTwoMessagesFromBuffer()
    {
        final int length = 8;
        assert length <= MAX_MESSAGE_SIZE;
        final int recordSize = calculateRecordSize(MAX_MESSAGE_SIZE);
        final long sequenceIndicatorOne = 1;
        final long sequenceIndicatorTwo = 2;
        final int recordOffsetOne = 0;
        final int recordOffsetTwo = recordSize;
        final int sequenceIndicatorOffsetOne = 0;
        final int sequenceIndicatorOffsetTwo = recordOffsetTwo;

        when(buffer.getLongVolatile(sequenceIndicatorOffsetOne)).thenReturn(sequenceIndicatorOne);
        when(buffer.getInt(lengthOffset(recordOffsetOne))).thenReturn(length);
        when(buffer.getInt(typeOffset(recordOffsetOne))).thenReturn(MSG_TYPE_ID);
        when(buffer.getLongVolatile(sequenceIndicatorOffsetTwo)).thenReturn(sequenceIndicatorTwo);
        when(buffer.getInt(lengthOffset(recordOffsetTwo))).thenReturn(length);
        when(buffer.getInt(typeOffset(recordOffsetTwo))).thenReturn(MSG_TYPE_ID);


        assertThat(broadcastReceiver.receiveNext(), is(ReceiveReturnType.ANY_AVAILABLE));
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffsetOne)));
        assertThat(broadcastReceiver.length(), is(length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffsetOne);
        inOrder.verify(buffer).getInt(lengthOffset(recordOffsetOne));

        assertTrue(broadcastReceiver.validate());

        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffsetOne);

        assertThat(broadcastReceiver.receiveNext(), is(ReceiveReturnType.ANY_AVAILABLE));
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffsetTwo)));
        assertThat(broadcastReceiver.length(), is(length));

        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffsetTwo);
        inOrder.verify(buffer).getInt(lengthOffset(recordOffsetTwo));

        assertTrue(broadcastReceiver.validate());

        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffsetTwo);
    }

    @Test
    public void shouldLateJoinTransmission()
    {
        final int length = 8;
        assert length <= MAX_MESSAGE_SIZE;
        final long tail = MSG_CAPACITY * 3L;
        final long sequenceIndicator = tail + 1;
        final int recordOffset = 0;
        final int sequenceIndicatorOffset = 0;

        when(buffer.getLongVolatile(sequenceIndicatorOffset)).thenReturn(sequenceIndicator);
        when(buffer.getInt(lengthOffset(recordOffset))).thenReturn(length);
        when(buffer.getInt(typeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertThat(broadcastReceiver.receiveNext(), is(ReceiveReturnType.LOSS));
        assertThat(broadcastReceiver.lostTransmissions(), is(MSG_CAPACITY * 3L));
        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffset);
        assertThat(broadcastReceiver.receiveNext(), is(ReceiveReturnType.ANY_AVAILABLE));
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffset)));
        assertThat(broadcastReceiver.length(), is(length));
        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffset);
        inOrder.verify(buffer).getInt(lengthOffset(recordOffset));
        assertTrue(broadcastReceiver.validate());
        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffset);
    }

    @Test
    public void shouldKeepUpWithTransmission()
    {
        final int length = 8;
        assert length <= MAX_MESSAGE_SIZE;
        final long tail = MSG_CAPACITY * 3L;
        final int recordOffset = 0;
        final int lostSequenceIndicatorOffset = calculateRecordSize(MAX_MESSAGE_SIZE);
        final long lostSequenceIndicator = ((tail + 1) - MSG_CAPACITY) + 1;

        when(buffer.getLongVolatile(lostSequenceIndicatorOffset)).thenReturn(lostSequenceIndicator);
        when(buffer.getLongVolatile(LATEST_COUNTER_INDEX)).thenReturn(tail + 1);
        when(buffer.getInt(lengthOffset(recordOffset))).thenReturn(length);
        when(buffer.getInt(typeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertThat(broadcastReceiver.keepUpWithTrasmitter(), is((MSG_CAPACITY * 3L) + 1L));
        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLongVolatile(LATEST_COUNTER_INDEX);
        assertThat(broadcastReceiver.lostTransmissions(), is((MSG_CAPACITY * 3L) + 1L));
        assertThat(broadcastReceiver.receiveNext(), is(ReceiveReturnType.NOT_AVAILABLE));
        inOrder.verify(buffer).getLongVolatile(lostSequenceIndicatorOffset);
    }

    @Test
    public void shouldDealWithRecordBecomingInvalidDueToInitiatedOverwrite()
    {
        final int length = 8;
        assert length <= MAX_MESSAGE_SIZE;
        final long sequenceIndicator = 1;
        final int recordOffset = 0;
        final int sequenceIndicatorOffset = 0;

        when(buffer.getLongVolatile(sequenceIndicatorOffset))
            .thenReturn(sequenceIndicator)
            .thenReturn(sequenceIndicator + MSG_CAPACITY - 1);
        when(buffer.getInt(lengthOffset(recordOffset))).thenReturn(length);
        when(buffer.getInt(typeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertThat(broadcastReceiver.receiveNext(), is(ReceiveReturnType.ANY_AVAILABLE));
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffset)));
        assertThat(broadcastReceiver.length(), is(length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffset);
        inOrder.verify(buffer).getInt(lengthOffset(recordOffset));

        assertFalse(broadcastReceiver.validate());
        assertThat(broadcastReceiver.lostTransmissions(), is((long)MSG_CAPACITY - 1));

        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffset);
    }

    @Test
    public void shouldDealWithRecordBecomingInvalidDueToCompleteOverwrite()
    {
        final int length = 8;
        assert length <= MAX_MESSAGE_SIZE;
        final long sequenceIndicator = 1;
        final int recordOffset = 0;
        final int sequenceIndicatorOffset = 0;

        when(buffer.getLongVolatile(sequenceIndicatorOffset))
            .thenReturn(sequenceIndicator)
            .thenReturn(sequenceIndicator + MSG_CAPACITY);
        when(buffer.getInt(lengthOffset(recordOffset))).thenReturn(length);
        when(buffer.getInt(typeOffset(recordOffset))).thenReturn(MSG_TYPE_ID);

        assertThat(broadcastReceiver.receiveNext(), is(ReceiveReturnType.ANY_AVAILABLE));
        assertThat(broadcastReceiver.typeId(), is(MSG_TYPE_ID));
        assertThat(broadcastReceiver.buffer(), is(buffer));
        assertThat(broadcastReceiver.offset(), is(msgOffset(recordOffset)));
        assertThat(broadcastReceiver.length(), is(length));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffset);
        inOrder.verify(buffer).getInt(lengthOffset(recordOffset));

        assertFalse(broadcastReceiver.validate());
        assertThat(broadcastReceiver.lostTransmissions(), is((long)MSG_CAPACITY));

        inOrder.verify(buffer).getLongVolatile(sequenceIndicatorOffset);
    }
}