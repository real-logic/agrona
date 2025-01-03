/*
 * Copyright 2014-2025 Real Logic Limited.
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
package org.agrona.concurrent.errors;

import org.agrona.BitUtil;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.CachedEpochClock;
import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.PrintWriter;

import static java.nio.ByteBuffer.allocateDirect;
import static org.agrona.concurrent.errors.DistinctErrorLog.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DistinctErrorLogTest
{
    private static final UnsafeBuffer DIRECT_BUFFER = new UnsafeBuffer(allocateDirect(32 * 1024));
    private final AtomicBuffer buffer = spy(DIRECT_BUFFER);
    private final EpochClock clock = mock(EpochClock.class);
    private final DistinctErrorLog log = new DistinctErrorLog(buffer, clock);

    @BeforeEach
    void beforeEach()
    {
        DIRECT_BUFFER.setMemory(0, DIRECT_BUFFER.capacity(), (byte)0);
    }

    @Test
    void shouldRecordFirstObservation()
    {
        final long timestamp = 7;
        final int offset = 0;
        final RuntimeException error = new RuntimeException("Test Error");

        when(clock.time()).thenReturn(timestamp);

        assertTrue(log.record(error));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putBytes(eq(offset + ENCODED_ERROR_OFFSET), any(byte[].class));
        inOrder.verify(buffer).putLong(offset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestamp);
        inOrder.verify(buffer).putIntOrdered(eq(offset + LENGTH_OFFSET), anyInt());
        inOrder.verify(buffer).getAndAddInt(offset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestamp);
    }

    @Test
    void shouldRecordFirstObservationOnly()
    {
        final long timestampOne = 7;
        final long timestampTwo = 8;
        final int offset = 0;
        final RuntimeException error = new RuntimeException("Test Error");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        assertTrue(log.record(error));
        assertTrue(log.record(error));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putBytes(eq(offset + ENCODED_ERROR_OFFSET), any(byte[].class));
        inOrder.verify(buffer).putLong(offset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);
        inOrder.verify(buffer).putIntOrdered(eq(offset + LENGTH_OFFSET), anyInt());
        inOrder.verify(buffer).getAndAddInt(offset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);
        inOrder.verify(buffer).getAndAddInt(offset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampTwo);
    }

    @Test
    void shouldRecordDifferentMessage()
    {
        final long timestampOne = 7;
        final long timestampTwo = 8;
        final int offset = 0;
        final Exception errorOne = new Exception("Error 1"), errorTwo = new Exception("Error 2");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        assertTrue(log.record(errorOne));
        assertTrue(log.record(errorTwo));

        final ArgumentCaptor<Integer> lengthArg = ArgumentCaptor.forClass(Integer.class);
        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putBytes(eq(offset + ENCODED_ERROR_OFFSET), any(byte[].class));
        inOrder.verify(buffer).putLong(offset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);
        inOrder.verify(buffer).putIntOrdered(eq(offset + LENGTH_OFFSET), lengthArg.capture());
        inOrder.verify(buffer).getAndAddInt(offset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);

        final int recordTwoOffset = BitUtil.align(lengthArg.getValue(), RECORD_ALIGNMENT);

        inOrder.verify(buffer).putBytes(eq(recordTwoOffset + ENCODED_ERROR_OFFSET), any(byte[].class));
        inOrder.verify(buffer).putLong(recordTwoOffset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestampTwo);
        inOrder.verify(buffer).putIntOrdered(eq(recordTwoOffset + LENGTH_OFFSET), anyInt());
        inOrder.verify(buffer).getAndAddInt(recordTwoOffset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(recordTwoOffset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampTwo);
    }

    @Test
    void shouldSummariseObservations()
    {
        final long timestampOne = 7;
        final long timestampTwo = 10;
        final int offset = 0;
        final RuntimeException error = new RuntimeException("Test Error");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        assertTrue(log.record(error));
        assertTrue(log.record(error));

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putBytes(eq(offset + ENCODED_ERROR_OFFSET), any(byte[].class));
        inOrder.verify(buffer).putLong(offset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);
        inOrder.verify(buffer).putIntOrdered(eq(offset + LENGTH_OFFSET), anyInt());
        inOrder.verify(buffer).getAndAddInt(offset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);

        inOrder.verify(buffer).getAndAddInt(offset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampTwo);
    }

    @Test
    void shouldRecordTwoDistinctObservations()
    {
        final long timestampOne = 7;
        final long timestampTwo = 10;
        final int offset = 0;
        final RuntimeException errorOne = new RuntimeException("Test Error One");
        final IllegalStateException errorTwo = new IllegalStateException("Test Error Two");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        assertTrue(log.record(errorOne));
        assertTrue(log.record(errorTwo));

        final ArgumentCaptor<Integer> lengthArg = ArgumentCaptor.forClass(Integer.class);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putBytes(eq(offset + ENCODED_ERROR_OFFSET), any(byte[].class));
        inOrder.verify(buffer).putLong(offset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);
        inOrder.verify(buffer).putIntOrdered(eq(offset + LENGTH_OFFSET), lengthArg.capture());
        inOrder.verify(buffer).getAndAddInt(offset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);

        final int recordTwoOffset = BitUtil.align(lengthArg.getValue(), RECORD_ALIGNMENT);

        inOrder.verify(buffer).putBytes(eq(recordTwoOffset + ENCODED_ERROR_OFFSET), any(byte[].class));
        inOrder.verify(buffer).putLong(recordTwoOffset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestampTwo);
        inOrder.verify(buffer).putIntOrdered(eq(recordTwoOffset + LENGTH_OFFSET), anyInt());
        inOrder.verify(buffer).getAndAddInt(recordTwoOffset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(recordTwoOffset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampTwo);
    }

    @Test
    void shouldRecordTwoDistinctObservationsOnCause()
    {
        final long timestampOne = 7;
        final long timestampTwo = 10;
        final int offset = 0;

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        for (int i = 0; i < 2; i++)
        {
            assertTrue(log.record(i == 1 ?
                new RuntimeException("One") :
                new RuntimeException("One", new Exception("Cause"))));
        }

        final ArgumentCaptor<Integer> lengthArg = ArgumentCaptor.forClass(Integer.class);

        final InOrder inOrder = inOrder(buffer);
        inOrder.verify(buffer).putBytes(eq(offset + ENCODED_ERROR_OFFSET), any(byte[].class));
        inOrder.verify(buffer).putLong(offset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);
        inOrder.verify(buffer).putIntOrdered(eq(offset + LENGTH_OFFSET), lengthArg.capture());
        inOrder.verify(buffer).getAndAddInt(offset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampOne);

        final int recordTwoOffset = BitUtil.align(lengthArg.getValue(), RECORD_ALIGNMENT);

        inOrder.verify(buffer).putBytes(eq(recordTwoOffset + ENCODED_ERROR_OFFSET), any(byte[].class));
        inOrder.verify(buffer).putLong(recordTwoOffset + FIRST_OBSERVATION_TIMESTAMP_OFFSET, timestampTwo);
        inOrder.verify(buffer).putIntOrdered(eq(recordTwoOffset + LENGTH_OFFSET), anyInt());
        inOrder.verify(buffer).getAndAddInt(recordTwoOffset + OBSERVATION_COUNT_OFFSET, 1);
        inOrder.verify(buffer).putLongOrdered(recordTwoOffset + LAST_OBSERVATION_TIMESTAMP_OFFSET, timestampTwo);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 24 })
    void shouldExitEarlyWhenInsufficientSpaceToRecordAnyInformation(final int capacity)
    {
        final long timestamp = 7;
        final RuntimeException error = spy(new RuntimeException("Test Error"));

        when(clock.time()).thenReturn(timestamp);
        when(buffer.capacity()).thenReturn(capacity);

        assertFalse(log.record(error));

        verifyNoMoreInteractions(error);
    }

    @Test
    void shouldFailToRecordWhenInsufficientSpace()
    {
        final long timestamp = 7;
        final RuntimeException error = spy(new RuntimeException("Test Error"));

        when(clock.time()).thenReturn(timestamp);
        when(buffer.capacity()).thenReturn(42);

        assertFalse(log.record(error));

        verify(error).printStackTrace(any(PrintWriter.class));
    }

    @Test
    void shouldTestRecordingWithoutStackTrace()
    {
        final CachedEpochClock clock = new CachedEpochClock();
        final DistinctErrorLog log = new DistinctErrorLog(DIRECT_BUFFER, clock);

        clock.advance(10);
        log.record(new TestEvent("event one"));

        clock.advance(10);
        log.record(new TestEvent("event one"));

        clock.advance(10);
        log.record(new TestEvent("event two"));

        assertTrue(ErrorLogReader.hasErrors(DIRECT_BUFFER));

        final StringBuilder sb = new StringBuilder();
        final int errorCount = ErrorLogReader.read(
            DIRECT_BUFFER,
            (observationCount, firstObservationTimestamp, lastObservationTimestamp, encodedException) ->
            {
                sb
                    .append(observationCount)
                    .append(',')
                    .append(firstObservationTimestamp)
                    .append(',')
                    .append(lastObservationTimestamp)
                    .append(',')
                    .append(encodedException);
            });

        assertEquals(2, errorCount);

        final String expectedOutput =
            "2,10,20,org.agrona.concurrent.errors.DistinctErrorLogTest$TestEvent: event one" + System.lineSeparator() +
            "1,30,30,org.agrona.concurrent.errors.DistinctErrorLogTest$TestEvent: event two" + System.lineSeparator();
        assertEquals(expectedOutput, sb.toString());
    }

    static class TestEvent extends RuntimeException
    {
        private static final long serialVersionUID = 5487718852587392272L;

        TestEvent(final String message)
        {
            super(message);
        }

        public synchronized Throwable fillInStackTrace()
        {
            return this;
        }
    }
}
