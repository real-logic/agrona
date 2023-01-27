/*
 * Copyright 2014-2023 Real Logic Limited.
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
package org.agrona.concurrent.status;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.IntObjConsumer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.agrona.concurrent.status.CountersReader.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CountersManagerTest
{
    private static final int NUMBER_OF_COUNTERS = 4;
    private static final long FREE_TO_REUSE_TIMEOUT = 1000;
    private static final int TYPE_ID = 101;
    private static final String LABEL = "label";

    private long currentTimestamp = 0;

    private final UnsafeBuffer metadataBuffer =
        new UnsafeBuffer(allocateDirect(NUMBER_OF_COUNTERS * METADATA_LENGTH));
    private final UnsafeBuffer valuesBuffer =
        new UnsafeBuffer(allocateDirect(NUMBER_OF_COUNTERS * COUNTER_LENGTH));
    private final CountersManager manager = new CountersManager(metadataBuffer, valuesBuffer, US_ASCII);
    private final CountersReader reader = new CountersManager(metadataBuffer, valuesBuffer, US_ASCII);
    private final CountersManager managerWithCooldown = new CountersManager(
        metadataBuffer, valuesBuffer, US_ASCII, () -> currentTimestamp, FREE_TO_REUSE_TIMEOUT);

    @SuppressWarnings("unchecked")
    private final IntObjConsumer<String> consumer = mock(IntObjConsumer.class);
    private final CountersReader.MetaData metaData = mock(CountersReader.MetaData.class);

    @Test
    void shouldSupportEmptyManager()
    {
        final UnsafeBuffer metadataBuffer = new UnsafeBuffer(allocateDirect(0));
        final UnsafeBuffer valuesBuffer = new UnsafeBuffer(allocateDirect(0));
        final CountersManager manager = new CountersManager(metadataBuffer, valuesBuffer, US_ASCII);

        assertEquals(-1, manager.maxCounterId());
        assertEquals(0, manager.capacity());
        assertEquals(0, manager.available());
    }

    @Test
    void shouldTruncateLongLabel()
    {
        final int labelLength = MAX_LABEL_LENGTH + 10;
        final StringBuilder sb = new StringBuilder(labelLength);

        for (int i = 0; i < labelLength; i++)
        {
            sb.append('x');
        }

        final String label = sb.toString();
        final int counterId = manager.allocate(label);

        reader.forEach(consumer);
        verify(consumer).accept(counterId, label.substring(0, MAX_LABEL_LENGTH));
    }

    @Test
    void shouldCopeWithExceptionKeyFunc()
    {
        final RuntimeException ex = new RuntimeException();

        try
        {
            manager.allocate(
                LABEL,
                DEFAULT_TYPE_ID,
                (buffer) ->
                {
                    throw ex;
                });
        }
        catch (final RuntimeException caught)
        {
            assertEquals(ex, caught);

            final AtomicCounter counter = manager.newCounter("new label");
            assertEquals(0, counter.id());

            return;
        }

        fail("Should have thrown exception");
    }

    @Test
    void shouldStoreLabels()
    {
        final int counterId = manager.allocate("abc");
        reader.forEach(consumer);
        verify(consumer).accept(counterId, "abc");
    }

    @Test
    void shouldSetRegistrationId()
    {
        final int counterId = manager.allocate("abc");
        assertEquals(DEFAULT_REGISTRATION_ID, reader.getCounterRegistrationId(counterId));

        final long registrationId = 777L;
        manager.setCounterRegistrationId(counterId, registrationId);
        assertEquals(registrationId, reader.getCounterRegistrationId(counterId));
    }

    @Test
    void shouldResetValueAndRegistrationIdIfReused()
    {
        final int counterIdOne = manager.allocate("abc");
        assertEquals(DEFAULT_REGISTRATION_ID, reader.getCounterRegistrationId(counterIdOne));

        final long registrationIdOne = 777L;
        manager.setCounterRegistrationId(counterIdOne, registrationIdOne);

        manager.free(counterIdOne);
        final int counterIdTwo = manager.allocate("def");
        assertEquals(counterIdOne, counterIdTwo);
        assertEquals(DEFAULT_REGISTRATION_ID, reader.getCounterRegistrationId(counterIdTwo));

        final long registrationIdTwo = 333L;
        manager.setCounterRegistrationId(counterIdTwo, registrationIdTwo);
        assertEquals(registrationIdTwo, reader.getCounterRegistrationId(counterIdTwo));
    }

    @Test
    void shouldSetOwnerId()
    {
        final int counterId = manager.allocate("abc");
        assertEquals(DEFAULT_OWNER_ID, reader.getCounterOwnerId(counterId));

        final long ownerId = 444L;
        manager.setCounterOwnerId(counterId, ownerId);
        assertEquals(ownerId, reader.getCounterOwnerId(counterId));
    }

    @Test
    void shouldResetValueAndOwnerIdIfReused()
    {
        final int counterIdOne = manager.allocate("abc");
        assertEquals(DEFAULT_OWNER_ID, reader.getCounterOwnerId(counterIdOne));

        final long ownerIdOne = 444L;
        manager.setCounterOwnerId(counterIdOne, ownerIdOne);

        manager.free(counterIdOne);
        final int counterIdTwo = manager.allocate("def");
        assertEquals(counterIdOne, counterIdTwo);
        assertEquals(DEFAULT_OWNER_ID, reader.getCounterOwnerId(counterIdTwo));

        final long ownerIdTwo = 222L;
        manager.setCounterOwnerId(counterIdTwo, ownerIdTwo);
        assertEquals(ownerIdTwo, reader.getCounterOwnerId(counterIdTwo));
    }

    @Test
    void shouldFindByRegistrationId()
    {
        final long registrationId = 777L;
        manager.allocate("null");
        final int counterId = manager.allocate("abc");
        manager.setCounterRegistrationId(counterId, registrationId);

        assertEquals(NULL_COUNTER_ID, manager.findByRegistrationId(1));
        assertEquals(counterId, manager.findByRegistrationId(registrationId));
    }

    @Test
    void shouldFindByTypeIdAndRegistrationId()
    {
        final long registrationId = 777L;
        final int typeId = 666;
        manager.allocate("null");
        final int counterId = manager.allocate("abc", typeId);
        manager.setCounterRegistrationId(counterId, registrationId);

        assertEquals(NULL_COUNTER_ID, manager.findByRegistrationId(1));
        assertEquals(NULL_COUNTER_ID, manager.findByTypeIdAndRegistrationId(DEFAULT_TYPE_ID, registrationId));
        assertEquals(NULL_COUNTER_ID, manager.findByTypeIdAndRegistrationId(typeId, 0));
        assertEquals(counterId, manager.findByTypeIdAndRegistrationId(typeId, registrationId));
    }

    @Test
    void shouldStoreMultipleLabels()
    {
        final int abc = manager.allocate("abc");
        final int def = manager.allocate("def");
        final int ghi = manager.allocate("ghi");

        reader.forEach(consumer);

        final InOrder inOrder = Mockito.inOrder(consumer);
        inOrder.verify(consumer).accept(abc, "abc");
        inOrder.verify(consumer).accept(def, "def");
        inOrder.verify(consumer).accept(ghi, "ghi");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldFreeAndReuseCounters()
    {
        assertEquals(NUMBER_OF_COUNTERS - 1, manager.maxCounterId());
        assertEquals(NUMBER_OF_COUNTERS, manager.capacity());
        assertEquals(NUMBER_OF_COUNTERS, manager.available());

        final int abc = manager.allocate("abc");
        final int def = manager.allocate("def");
        final int ghi = manager.allocate("ghi");

        assertEquals(NUMBER_OF_COUNTERS - 3, manager.available());

        manager.free(def);
        assertEquals(NUMBER_OF_COUNTERS - 2, manager.available());

        reader.forEach(consumer);

        final InOrder inOrder = Mockito.inOrder(consumer);
        inOrder.verify(consumer).accept(abc, "abc");
        inOrder.verify(consumer).accept(ghi, "ghi");
        inOrder.verifyNoMoreInteractions();

        assertEquals(def, manager.allocate("the next label"));
        assertEquals(NUMBER_OF_COUNTERS - 3, manager.available());
    }

    @Test
    void shouldFreeAndNotReuseCountersThatHaveCooldown()
    {
        managerWithCooldown.allocate("abc");
        final int def = managerWithCooldown.allocate("def");
        final int ghi = managerWithCooldown.allocate("ghi");

        managerWithCooldown.free(def);

        currentTimestamp += FREE_TO_REUSE_TIMEOUT - 1;
        assertEquals(ghi + 1, managerWithCooldown.allocate("the next label"));
    }

    @Test
    void shouldFreeAndReuseCountersAfterCooldown()
    {
        managerWithCooldown.allocate("abc");
        final int def = managerWithCooldown.allocate("def");
        managerWithCooldown.allocate("ghi");

        managerWithCooldown.free(def);

        currentTimestamp += FREE_TO_REUSE_TIMEOUT;
        assertEquals(def, managerWithCooldown.allocate("the next label"));
    }

    @Test
    void shouldNotOverAllocateCounters()
    {
        manager.allocate("abc");
        manager.allocate("def");
        manager.allocate("ghi");
        manager.allocate("jkl");

        assertThrows(IllegalStateException.class, () -> manager.allocate("mno"));
    }

    @Test
    void shouldMapAllocatedCounters()
    {
        manager.allocate("def");

        final int id = manager.allocate("abc");
        final ReadablePosition reader = new UnsafeBufferPosition(valuesBuffer, id);
        final Position writer = new UnsafeBufferPosition(valuesBuffer, id);
        final long expectedValue = 0xF_FFFF_FFFFL;

        writer.setOrdered(expectedValue);

        assertEquals(expectedValue, reader.getVolatile());
    }

    @Test
    void shouldStoreMetaData()
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
        assertEquals(keyOne, keyOneBuffer.getLong(0));

        final DirectBuffer keyTwoBuffer = argCaptorTwo.getValue();
        assertEquals(keyTwo, keyTwoBuffer.getLong(0));

        assertEquals(typeIdOne, manager.getCounterTypeId(counterIdOne));
        assertEquals(typeIdTwo, manager.getCounterTypeId(counterIdTwo));
    }

    @Test
    void shouldStoreRawData()
    {
        final int typeIdOne = 333;
        final long keyOne = 777L;
        final MutableDirectBuffer keyOneBuffer = new UnsafeBuffer(allocateDirect(8));
        keyOneBuffer.putLong(0, keyOne);
        final DirectBuffer labelOneBuffer = new UnsafeBuffer("Test Label One".getBytes(US_ASCII));

        final int typeIdTwo = 222;
        final long keyTwo = 444;
        final MutableDirectBuffer keyTwoBuffer = new UnsafeBuffer(allocateDirect(8));
        keyTwoBuffer.putLong(0, keyTwo);
        final DirectBuffer labelTwoBuffer = new UnsafeBuffer("Test Label Two".getBytes(US_ASCII));

        final int counterIdOne = manager.allocate(
            typeIdOne, keyOneBuffer, 0, keyOneBuffer.capacity(), labelOneBuffer, 0, labelOneBuffer.capacity());

        final int counterIdTwo = manager.allocate(
            typeIdTwo, keyTwoBuffer, 0, keyTwoBuffer.capacity(), labelTwoBuffer, 0, labelTwoBuffer.capacity());

        manager.forEach(metaData);

        final ArgumentCaptor<DirectBuffer> argCaptorOne = ArgumentCaptor.forClass(DirectBuffer.class);
        final ArgumentCaptor<DirectBuffer> argCaptorTwo = ArgumentCaptor.forClass(DirectBuffer.class);

        final InOrder inOrder = Mockito.inOrder(metaData);
        inOrder.verify(metaData).accept(eq(counterIdOne), eq(typeIdOne), argCaptorOne.capture(), eq("Test Label One"));
        inOrder.verify(metaData).accept(eq(counterIdTwo), eq(typeIdTwo), argCaptorTwo.capture(), eq("Test Label Two"));
        inOrder.verifyNoMoreInteractions();

        final DirectBuffer keyOneBufferCapture = argCaptorOne.getValue();
        assertEquals(keyOne, keyOneBufferCapture.getLong(0));

        final DirectBuffer keyTwoBufferCapture = argCaptorTwo.getValue();
        assertEquals(keyTwo, keyTwoBufferCapture.getLong(0));
    }

    @Test
    void shouldStoreAndLoadValue()
    {
        final int counterId = manager.allocate("Test Counter");

        final long value = 7L;
        manager.setCounterValue(counterId, value);

        assertEquals(value, manager.getCounterValue(counterId));
    }

    @Test
    void shouldGetAndUpdateCounterLabel()
    {
        final AtomicCounter counter = manager.newCounter("original label");

        assertEquals("original label", counter.label());
        counter.updateLabel(counter.label() + " with update");
        assertEquals("original label with update", counter.label());
    }

    @Test
    void shouldGetAndUpdateCounterKeyUsingCallback()
    {
        final String originalKey = "original key";
        final String updatedKey = "updated key";

        final AtomicCounter counter = manager.newCounter(
            LABEL, TYPE_ID, (keyBuffer) -> keyBuffer.putStringUtf8(0, originalKey));

        final StringKeyExtractor keyExtractor = new StringKeyExtractor(counter.id());

        manager.forEach(keyExtractor);
        assertEquals(originalKey, keyExtractor.key);

        manager.setCounterKey(counter.id(), (keyBuffer) -> keyBuffer.putStringUtf8(0, updatedKey));

        manager.forEach(keyExtractor);
        assertEquals(updatedKey, keyExtractor.key);
    }

    @Test
    void shouldGetAndUpdateCounterKey()
    {
        final String originalKey = "original key";
        final String updatedKey = "updated key";

        final AtomicCounter counter = manager.newCounter(
            LABEL, TYPE_ID, (keyBuffer) -> keyBuffer.putStringUtf8(0, originalKey));

        final StringKeyExtractor keyExtractor = new StringKeyExtractor(counter.id());

        manager.forEach(keyExtractor);

        assertEquals(originalKey, keyExtractor.key);

        final UnsafeBuffer tempBuffer = new UnsafeBuffer(new byte[128]);
        final int length = tempBuffer.putStringUtf8(0, updatedKey);

        manager.setCounterKey(counter.id(), tempBuffer, 0, length);

        manager.forEach(keyExtractor);
        assertEquals(updatedKey, keyExtractor.key);
    }

    @Test
    void shouldRejectOversizeKeys()
    {
        final String originalKey = "original key";

        final AtomicCounter counter = manager.newCounter(
            LABEL, TYPE_ID, (keyBuffer) -> keyBuffer.putStringUtf8(0, originalKey));

        final UnsafeBuffer tempBuffer = new UnsafeBuffer(new byte[256]);

        try
        {
            manager.setCounterKey(counter.id(), tempBuffer, 0, MAX_KEY_LENGTH + 1);
            fail("Should have thrown exception");
        }
        catch (final IllegalArgumentException e)
        {
            assertTrue(true);
        }
    }

    static final class StringKeyExtractor implements MetaData
    {
        private final int id;
        private String key;

        private StringKeyExtractor(final int id)
        {
            this.id = id;
        }

        public void accept(final int counterId, final int typeId, final DirectBuffer keyBuffer, final String label)
        {
            if (counterId == id && typeId == TYPE_ID)
            {
                assertEquals(LABEL, label);
                key = keyBuffer.getStringUtf8(0);
            }
        }
    }

    @Test
    void shouldAppendLabel()
    {
        final AtomicCounter counter = manager.newCounter("original label");

        assertEquals("original label", counter.label());
        counter.appendToLabel(" with update");
        assertEquals("original label with update", counter.label());
    }
}
