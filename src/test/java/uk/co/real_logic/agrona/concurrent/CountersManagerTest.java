/*
 * Copyright 2014 - 2016 Real Logic Ltd.
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
package uk.co.real_logic.agrona.concurrent;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.collections.IntObjConsumer;
import uk.co.real_logic.agrona.concurrent.status.Position;
import uk.co.real_logic.agrona.concurrent.status.ReadablePosition;
import uk.co.real_logic.agrona.concurrent.status.UnsafeBufferPosition;

import static java.nio.ByteBuffer.allocateDirect;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.co.real_logic.agrona.concurrent.CountersReader.COUNTER_LENGTH;
import static uk.co.real_logic.agrona.concurrent.CountersReader.METADATA_LENGTH;

public class CountersManagerTest
{
    private static final int NUMBER_OF_COUNTERS = 4;

    private UnsafeBuffer labelsBuffer = new UnsafeBuffer(allocateDirect(NUMBER_OF_COUNTERS * METADATA_LENGTH));
    private UnsafeBuffer counterBuffer = new UnsafeBuffer(allocateDirect(NUMBER_OF_COUNTERS * COUNTER_LENGTH));
    private CountersManager manager = new CountersManager(labelsBuffer, counterBuffer);
    private CountersReader otherManager = new CountersManager(labelsBuffer, counterBuffer);

    @SuppressWarnings("unchecked")
    private final IntObjConsumer<String> consumer = mock(IntObjConsumer.class);
    private final CountersReader.MetaData metaData = mock(CountersReader.MetaData.class);

    @Test
    public void managerShouldStoreLabels()
    {
        final int counterId = manager.allocate("abc");
        otherManager.forEach(consumer);
        verify(consumer).accept(counterId, "abc");
    }

    @Test
    public void managerShouldStoreMultipleLabels()
    {
        final int abc = manager.allocate("abc");
        final int def = manager.allocate("def");
        final int ghi = manager.allocate("ghi");

        otherManager.forEach(consumer);

        final InOrder inOrder = Mockito.inOrder(consumer);
        inOrder.verify(consumer).accept(abc, "abc");
        inOrder.verify(consumer).accept(def, "def");
        inOrder.verify(consumer).accept(ghi, "ghi");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldFreeAndReuseCounters()
    {
        final int abc = manager.allocate("abc");
        final int def = manager.allocate("def");
        final int ghi = manager.allocate("ghi");

        manager.free(def);

        otherManager.forEach(consumer);

        final InOrder inOrder = Mockito.inOrder(consumer);
        inOrder.verify(consumer).accept(abc, "abc");
        inOrder.verify(consumer).accept(ghi, "ghi");
        inOrder.verifyNoMoreInteractions();

        assertThat(manager.allocate("the next label"), is(def));
    }

    @Test(expected = IllegalArgumentException.class)
    public void managerShouldNotOverAllocateCounters()
    {
        manager.allocate("abc");
        manager.allocate("def");
        manager.allocate("ghi");
        manager.allocate("jkl");
        manager.allocate("mno");
    }

    @Test
    public void allocatedCountersCanBeMapped()
    {
        manager.allocate("def");

        final int id = manager.allocate("abc");
        final ReadablePosition reader = new UnsafeBufferPosition(counterBuffer, id);
        final Position writer = new UnsafeBufferPosition(counterBuffer, id);
        final long expectedValue = 0xF_FFFF_FFFFL;

        writer.setOrdered(expectedValue);

        assertThat(reader.getVolatile(), is(expectedValue));
    }

    @Test
    public void shouldStoreMetaData()
    {
        final int typeIdOne = 333;
        final long keyOne = 777L;

        final int typeIdTwo = 222;
        final long keyTwo = 444;

        final int counterIdOne = manager.allocate("Test Label One", typeIdOne, (buffer) -> buffer.putLong(0, keyOne));
        final int counterIdTwo = manager.allocate("Test Label Two", typeIdTwo, (buffer) -> buffer.putLong(0, keyTwo));

        manager.forEach(metaData);

        final ArgumentCaptor<DirectBuffer> argCaptorOne = ArgumentCaptor.forClass(DirectBuffer.class);
        final ArgumentCaptor<DirectBuffer> argCaptorTwo = ArgumentCaptor.forClass(DirectBuffer.class);

        final InOrder inOrder = Mockito.inOrder(metaData);
        inOrder.verify(metaData).accept(eq(counterIdOne), eq(typeIdOne), argCaptorOne.capture(), eq("Test Label One"));
        inOrder.verify(metaData).accept(eq(counterIdTwo), eq(typeIdTwo), argCaptorTwo.capture(), eq("Test Label Two"));
        inOrder.verifyNoMoreInteractions();

        final DirectBuffer keyOneBuffer = argCaptorOne.getValue();
        assertThat(keyOneBuffer.getLong(0), is(keyOne));

        final DirectBuffer keyTwoBuffer = argCaptorTwo.getValue();
        assertThat(keyTwoBuffer.getLong(0), is(keyTwo));
    }

    @Test
    public void shouldStoreAndLoadValue()
    {
        final int counterId = manager.allocate("Test Counter");

        final long value = 7L;
        manager.setCounterValue(counterId, value);

        assertThat(manager.getCounterValue(counterId), is(value));
    }
}
