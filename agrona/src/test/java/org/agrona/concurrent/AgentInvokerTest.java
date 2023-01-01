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
package org.agrona.concurrent;

import org.agrona.ErrorHandler;
import org.agrona.LangUtil;
import org.agrona.concurrent.status.AtomicCounter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.channels.ClosedByInterruptException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AgentInvokerTest
{
    private final ErrorHandler mockErrorHandler = mock(ErrorHandler.class);
    private final AtomicCounter mockAtomicCounter = mock(AtomicCounter.class);
    private final Agent mockAgent = mock(Agent.class);
    private final AgentInvoker invoker = new AgentInvoker(mockErrorHandler, mockAtomicCounter, mockAgent);

    @Test
    void shouldFollowLifecycle() throws Exception
    {
        invoker.start();
        invoker.start();
        verify(mockAgent, times(1)).onStart();
        verifyNoMoreInteractions(mockAgent);

        invoker.invoke();
        invoker.invoke();
        verify(mockAgent, times(2)).doWork();
        verifyNoMoreInteractions(mockAgent);

        invoker.close();
        invoker.close();
        verify(mockAgent, times(1)).onClose();
        verifyNoMoreInteractions(mockAgent);
    }

    @Test
    void shouldReturnAgent()
    {
        assertThat(invoker.agent(), is(mockAgent));
    }

    @Test
    void shouldNotDoWorkOnClosedRunnerButCallOnClose() throws Exception
    {
        invoker.close();
        invoker.invoke();

        verify(mockAgent, never()).onStart();
        verify(mockAgent, never()).doWork();
        verify(mockErrorHandler, never()).onError(any());
        verify(mockAtomicCounter, never()).increment();
        verify(mockAgent).onClose();
    }

    @Test
    void shouldReportExceptionThrownByAgent() throws Exception
    {
        final RuntimeException expectedException = new RuntimeException();
        when(mockAgent.doWork()).thenThrow(expectedException);

        invoker.start();
        invoker.invoke();

        verify(mockAgent).doWork();
        verify(mockErrorHandler).onError(expectedException);
        verify(mockAtomicCounter).increment();
        verify(mockAgent, never()).onClose();
        reset(mockAgent);

        invoker.invoke();

        verify(mockAgent).doWork();
        reset(mockAgent);

        invoker.close();

        verify(mockAgent, never()).doWork();
        verify(mockAgent).onClose();
    }

    @Test
    void shouldReportExceptionThrownOnStart() throws Exception
    {
        final RuntimeException expectedException = new RuntimeException();
        Mockito.doThrow(expectedException).when(mockAgent).onStart();

        invoker.start();
        invoker.invoke();

        verify(mockAgent, never()).doWork();
        verify(mockErrorHandler).onError(expectedException);
        verify(mockAgent).onClose();

        assertTrue(invoker.isStarted());
        assertFalse(invoker.isRunning());
        assertTrue(invoker.isClosed());
    }

    @Test
    void shouldHandleAgentTerminationExceptionThrownByAgent() throws Exception
    {
        final RuntimeException expectedException = new AgentTerminationException();
        when(mockAgent.doWork()).thenThrow(expectedException);

        invoker.start();
        invoker.invoke();

        verify(mockAgent).doWork();
        verify(mockErrorHandler).onError(expectedException);
        verify(mockAtomicCounter, never()).increment();
        verify(mockAgent).onClose();
        assertTrue(invoker.isClosed());

        reset(mockAgent);
        invoker.invoke();

        verify(mockAgent, never()).doWork();
        assertTrue(invoker.isClosed());
    }

    @Test
    void shouldStopRunningOnRuntimeException() throws Exception
    {
        try
        {
            final RuntimeException expectedException = new RuntimeException();
            when(mockAgent.doWork()).thenThrow(expectedException);
            doAnswer(
                (invocation) ->
                {
                    Thread.currentThread().interrupt();
                    return null;
                })
                .when(mockErrorHandler).onError(expectedException);

            invoker.start();
            invoker.invoke();

            verify(mockAgent).doWork();
            verify(mockErrorHandler).onError(expectedException);
            verify(mockAtomicCounter).increment();
            verify(mockAgent).onClose();
            assertTrue(invoker.isClosed());
            assertFalse(invoker.isRunning());

            reset(mockAgent);
            invoker.invoke();

            verify(mockAgent, never()).doWork();
            assertTrue(invoker.isClosed());
        }
        finally
        {
            assertTrue(Thread.interrupted());
        }
    }

    @Test
    void shouldNotReportClosedByInterruptException() throws Exception
    {
        when(mockAgent.doWork()).thenThrow(new ClosedByInterruptException());

        assertExceptionNotReported();
        assertTrue(Thread.interrupted()); // by throwing ClosedByInterruptException
    }

    @Test
    void shouldNotReportRethrownClosedByInterruptException() throws Exception
    {
        when(mockAgent.doWork()).thenAnswer(
            (inv) ->
            {
                try
                {
                    throw new ClosedByInterruptException();
                }
                catch (final ClosedByInterruptException ex)
                {
                    LangUtil.rethrowUnchecked(ex);
                }

                return null;
            });

        assertExceptionNotReported();
        assertTrue(Thread.interrupted()); // by throwing ClosedByInterruptException
    }

    private void assertExceptionNotReported()
    {
        invoker.start();
        invoker.invoke();
        invoker.close();

        verify(mockErrorHandler, never()).onError(any());
        verify(mockAtomicCounter, never()).increment();
    }
}
