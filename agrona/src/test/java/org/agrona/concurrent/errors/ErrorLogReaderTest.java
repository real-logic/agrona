/*
 * Copyright 2014-2024 Real Logic Limited.
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

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;

import static org.agrona.concurrent.errors.DistinctErrorLog.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErrorLogReaderTest
{
    private final AtomicBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(64 * 1024));
    private final EpochClock clock = mock(EpochClock.class);
    private final DistinctErrorLog log = new DistinctErrorLog(buffer, clock);

    @Test
    void shouldReadNoExceptionsFromEmptyLog()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        assertThat(ErrorLogReader.read(buffer, consumer), is(0));

        verifyNoInteractions(consumer);
    }

    @Test
    void shouldReadFirstObservation()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        final long timestamp = 7;
        final RuntimeException error = new RuntimeException("Test Error");

        when(clock.time()).thenReturn(timestamp);

        log.record(error);

        assertThat(ErrorLogReader.read(buffer, consumer), is(1));

        verify(consumer).accept(eq(1), eq(timestamp), eq(timestamp), any(String.class));
    }

    @Test
    void shouldReadSummarisedObservation()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        final long timestampOne = 7;
        final long timestampTwo = 10;
        final RuntimeException error = new RuntimeException("Test Error");

        final StringWriter stringWriter = new StringWriter();
        error.printStackTrace(new PrintWriter(stringWriter));
        final String errorAsString = stringWriter.toString();

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        log.record(error);
        log.record(error);

        assertThat(ErrorLogReader.read(buffer, consumer), is(1));

        verify(consumer).accept(eq(2), eq(timestampOne), eq(timestampTwo), eq(errorAsString));
    }

    @Test
    void shouldReadTwoDistinctObservations()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        final long timestampOne = 7;
        final long timestampTwo = 10;
        final RuntimeException errorOne = new RuntimeException("Test Error One");
        final IllegalStateException errorTwo = new IllegalStateException("Test Error Two");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        log.record(errorOne);
        log.record(errorTwo);

        assertThat(ErrorLogReader.read(buffer, consumer), is(2));

        final InOrder inOrder = inOrder(consumer);
        inOrder.verify(consumer).accept(eq(1), eq(timestampOne), eq(timestampOne), any(String.class));
        inOrder.verify(consumer).accept(eq(1), eq(timestampTwo), eq(timestampTwo), any(String.class));
    }

    @Test
    void shouldReadOneObservationSinceTimestamp()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        final long timestampOne = 7;
        final long timestampTwo = 10;
        final RuntimeException errorOne = new RuntimeException("Test Error One");
        final IllegalStateException errorTwo = new IllegalStateException("Test Error Two");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        assertFalse(ErrorLogReader.hasErrors(buffer));

        log.record(errorOne);
        log.record(errorTwo);

        assertTrue(ErrorLogReader.hasErrors(buffer));
        assertThat(ErrorLogReader.read(buffer, consumer, timestampTwo), is(1));

        verify(consumer).accept(eq(1), eq(timestampTwo), eq(timestampTwo), any(String.class));
        verifyNoMoreInteractions(consumer);
    }

    @Test
    void readShouldNotReadIfRemainingSpaceIsLessThanOneErrorPrefix()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
        final long lastTimestamp = 543495734L;
        final long firstTimestamp = lastTimestamp - 1000;
        final int count = 123;
        final int totalLength = 45;
        buffer.putInt(LENGTH_OFFSET, totalLength);
        buffer.putLong(LAST_OBSERVATION_TIMESTAMP_OFFSET, lastTimestamp);
        buffer.putLong(FIRST_OBSERVATION_TIMESTAMP_OFFSET, firstTimestamp);
        buffer.putInt(OBSERVATION_COUNT_OFFSET, count);
        buffer.putStringWithoutLengthAscii(ENCODED_ERROR_OFFSET, "abcdefghijklmnopqrstuvwxyz");
        buffer.putInt(totalLength + LENGTH_OFFSET, 12);
        final ErrorConsumer errorConsumer = mock(ErrorConsumer.class);

        assertEquals(1, ErrorLogReader.read(buffer, errorConsumer, 0));

        verify(errorConsumer).accept(count, firstTimestamp, lastTimestamp, "abcdefghijklmnopqrstu");
    }

    @Test
    void shouldNotExceedEndOfBufferWhenReadinErrorMessage()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
        buffer.setMemory(0, buffer.capacity(), (byte)'?');
        final long lastTimestamp = 347923749327L;
        final long firstTimestamp = -8530458948593L;
        final int count = 999;
        buffer.putInt(LENGTH_OFFSET, Integer.MAX_VALUE);
        buffer.putLong(LAST_OBSERVATION_TIMESTAMP_OFFSET, lastTimestamp);
        buffer.putLong(FIRST_OBSERVATION_TIMESTAMP_OFFSET, firstTimestamp);
        buffer.putInt(OBSERVATION_COUNT_OFFSET, count);
        buffer.putStringWithoutLengthAscii(ENCODED_ERROR_OFFSET, "test");
        final String expectedErrorString =
            buffer.getStringWithoutLengthAscii(ENCODED_ERROR_OFFSET, buffer.capacity() - ENCODED_ERROR_OFFSET);
        final ErrorConsumer errorConsumer = mock(ErrorConsumer.class);

        assertEquals(1, ErrorLogReader.read(buffer, errorConsumer, 0));

        verify(errorConsumer).accept(count, firstTimestamp, lastTimestamp, expectedErrorString);
    }
}
