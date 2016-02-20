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
package uk.co.real_logic.agrona.concurrent.exceptions;

import org.junit.Test;
import org.mockito.InOrder;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.EpochClock;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ExceptionLogReaderTest
{
    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(64 * 1024));
    private final AtomicBuffer buffer = spy(unsafeBuffer);
    private final EpochClock clock = mock(EpochClock.class);
    private final DistinctExceptionLog log = new DistinctExceptionLog(buffer, clock);

    @Test
    public void shouldReadNoExceptionsFromEmptyLog()
    {
        final ExceptionHandler handler = mock(ExceptionHandler.class);

        assertThat(ExceptionLogReader.read(buffer, handler), is(0));

        verifyZeroInteractions(handler);
    }

    @Test
    public void shouldReadFirstObservation()
    {
        final ExceptionHandler handler = mock(ExceptionHandler.class);

        final long timestamp = 7;
        final RuntimeException ex = new RuntimeException("Test Exception");

        when(clock.time()).thenReturn(timestamp);

        log.record(ex);

        assertThat(ExceptionLogReader.read(buffer, handler), is(1));

        verify(handler).onException(eq(1), eq(timestamp), eq(timestamp), any(String.class));
    }

    @Test
    public void shouldReadSummarisedObservation()
    {
        final ExceptionHandler handler = mock(ExceptionHandler.class);

        final long timestampOne = 7;
        final long timestampTwo = 10;
        final RuntimeException ex = new RuntimeException("Test Exception");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        log.record(ex);
        log.record(ex);

        assertThat(ExceptionLogReader.read(buffer, handler), is(1));

        verify(handler).onException(eq(2), eq(timestampOne), eq(timestampTwo), any(String.class));
    }

    @Test
    public void shouldReadTwoDistinctObservations()
    {
        final ExceptionHandler handler = mock(ExceptionHandler.class);

        final long timestampOne = 7;
        final long timestampTwo = 10;
        final RuntimeException exOne = new RuntimeException("Test Exception One");
        final IllegalStateException exTwo = new IllegalStateException("Test Exception Two");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        log.record(exOne);
        log.record(exTwo);

        assertThat(ExceptionLogReader.read(buffer, handler), is(2));

        final InOrder inOrder = inOrder(handler);
        inOrder.verify(handler).onException(eq(1), eq(timestampOne), eq(timestampOne), any(String.class));
        inOrder.verify(handler).onException(eq(1), eq(timestampTwo), eq(timestampTwo), any(String.class));
    }

    @Test
    public void shouldReadOneObservationSinceTimestamp()
    {
        final ExceptionHandler handler = mock(ExceptionHandler.class);

        final long timestampOne = 7;
        final long timestampTwo = 10;
        final RuntimeException exOne = new RuntimeException("Test Exception One");
        final IllegalStateException exTwo = new IllegalStateException("Test Exception Two");

        when(clock.time()).thenReturn(timestampOne).thenReturn(timestampTwo);

        log.record(exOne);
        log.record(exTwo);

        assertThat(ExceptionLogReader.read(buffer, handler, timestampTwo), is(1));

        verify(handler).onException(eq(1), eq(timestampTwo), eq(timestampTwo), any(String.class));
        verifyNoMoreInteractions(handler);
    }
}