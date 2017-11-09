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
package org.agrona.concurrent;

import org.agrona.ErrorHandler;
import org.agrona.LangUtil;
import org.agrona.concurrent.status.AtomicCounter;
import org.junit.Test;

import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class AgentRunnerTest
{
    private final AtomicCounter mockAtomicCounter = mock(AtomicCounter.class);

    private final ErrorHandler mockErrorHandler = mock(ErrorHandler.class);
    private final Agent mockAgent = mock(Agent.class);
    private final IdleStrategy idleStrategy = new NoOpIdleStrategy();
    private final AgentRunner runner = new AgentRunner(idleStrategy, mockErrorHandler, mockAtomicCounter, mockAgent);

    @Test
    public void shouldReturnAgent()
    {
        assertThat(runner.agent(), is(mockAgent));
    }

    @Test
    public void shouldNotDoWorkOnClosedRunner() throws Exception
    {
        runner.close();
        runner.run();

        verify(mockAgent, never()).onStart();
        verify(mockAgent, never()).doWork();
        verify(mockErrorHandler, never()).onError(any());
        verify(mockAtomicCounter, never()).increment();
        verify(mockAgent, times(1)).onClose();
        assertTrue(runner.isClosed());
    }

    @Test
    public void shouldHandleAgentTerminationExceptionThrownByAgent() throws Exception
    {
        final RuntimeException expectedException = new AgentTerminationException();
        when(mockAgent.doWork()).thenThrow(expectedException);

        runner.run();

        verify(mockAgent).doWork();
        verify(mockErrorHandler).onError(expectedException);
        verify(mockAtomicCounter).increment();
        verify(mockAgent, atLeastOnce()).onClose();
        assertTrue(runner.isClosed());
    }

    @Test
    public void shouldReportExceptionThrownByAgent() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final RuntimeException expectedException = new RuntimeException();
        when(mockAgent.doWork()).thenThrow(expectedException);

        doAnswer(
            (invocation) ->
            {
                latch.countDown();
                return null;
            }).when(mockErrorHandler).onError(expectedException);

        new Thread(runner).start();

        if (!latch.await(3, TimeUnit.SECONDS))
        {
            fail("Should have called error handler");
        }

        verify(mockAgent, times(1)).onStart();
        verify(mockAgent, atLeastOnce()).doWork();
        verify(mockErrorHandler, atLeastOnce()).onError(expectedException);
        verify(mockAtomicCounter, atLeastOnce()).increment();

        runner.close();
    }

    @Test
    public void shouldNotReportClosedByInterruptException() throws Exception
    {
        when(mockAgent.doWork()).thenThrow(new ClosedByInterruptException());

        assertExceptionNotReported();
    }

    @Test
    public void shouldNotReportRethrownClosedByInterruptException() throws Exception
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
    }

    private void assertExceptionNotReported() throws InterruptedException
    {
        new Thread(runner).start();

        Thread.sleep(100);

        runner.close();

        verify(mockAgent, times(1)).onStart();
        verify(mockErrorHandler, never()).onError(any());
        verify(mockAtomicCounter, never()).increment();
    }
}
