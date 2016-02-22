/*
 * Copyright 2016 Real Logic Ltd.
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
package uk.co.real_logic.agrona.concurrent.errors;

import org.junit.Test;
import org.mockito.InOrder;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.EpochClock;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ErrorLogReaderTest
{
    private final AtomicBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(64 * 1024));
    private final EpochClock clock = mock(EpochClock.class);
    private final DistinctErrorLog log = new DistinctErrorLog(buffer, clock);

    @Test
    public void shouldReadNoExceptionsFromEmptyLog()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        assertThat(ErrorLogReader.read(buffer, consumer), is(0));

        verifyZeroInteractions(consumer);
    }

    @Test
    public void shouldReadFirstObservation()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        final long timestamp = 7;
        final RuntimeException error = new RuntimeException("Test Exception");

        when(clock.time()).thenReturn(timestamp);

        log.record(error);

        assertThat(ErrorLogReader.read(buffer, consumer), is(1));

        verify(consumer).accept(eq(1), eq(timestamp), eq(timestamp), any(String.class));
    }

    @Test
    public void shouldReadSummarisedObservation()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        final long timestampOne = 7;
        final long timestampTwo = 10;
        final RuntimeException error = new RuntimeException("Test Exception");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        log.record(error);
        log.record(error);

        assertThat(ErrorLogReader.read(buffer, consumer), is(1));

        verify(consumer).accept(eq(2), eq(timestampOne), eq(timestampTwo), any(String.class));
    }

    @Test
    public void shouldReadTwoDistinctObservations()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        final long timestampOne = 7;
        final long timestampTwo = 10;
        final RuntimeException errorOne = new RuntimeException("Test Exception One");
        final IllegalStateException errorTwo = new IllegalStateException("Test Exception Two");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        log.record(errorOne);
        log.record(errorTwo);

        assertThat(ErrorLogReader.read(buffer, consumer), is(2));

        final InOrder inOrder = inOrder(consumer);
        inOrder.verify(consumer).accept(eq(1), eq(timestampOne), eq(timestampOne), any(String.class));
        inOrder.verify(consumer).accept(eq(1), eq(timestampTwo), eq(timestampTwo), any(String.class));
    }

    @Test
    public void shouldReadOneObservationSinceTimestamp()
    {
        final ErrorConsumer consumer = mock(ErrorConsumer.class);

        final long timestampOne = 7;
        final long timestampTwo = 10;
        final RuntimeException errorOne = new RuntimeException("Test Exception One");
        final IllegalStateException errorTwo = new IllegalStateException("Test Exception Two");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        log.record(errorOne);
        log.record(errorTwo);

        assertThat(ErrorLogReader.read(buffer, consumer, timestampTwo), is(1));

        verify(consumer).accept(eq(1), eq(timestampTwo), eq(timestampTwo), any(String.class));
        verifyNoMoreInteractions(consumer);
    }
}