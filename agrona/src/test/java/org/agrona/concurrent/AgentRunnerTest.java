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
package org.agrona.concurrent;

import org.agrona.ErrorHandler;
import org.agrona.LangUtil;
import org.agrona.collections.MutableBoolean;
import org.agrona.concurrent.status.AtomicCounter;
import org.junit.jupiter.api.Test;

import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.answersWithDelay;
import static org.mockito.Mockito.*;

class AgentRunnerTest
{
    private final AtomicCounter mockAtomicCounter = mock(AtomicCounter.class);

    private final ErrorHandler mockErrorHandler = mock(ErrorHandler.class);
    private final Agent mockAgent = mock(Agent.class);
    private final IdleStrategy idleStrategy = new NoOpIdleStrategy();
    private final AgentRunner runner = new AgentRunner(idleStrategy, mockErrorHandler, mockAtomicCounter, mockAgent);

    @Test
    void shouldReturnAgent()
    {
        assertThat(runner.agent(), is(mockAgent));
    }

    @Test
    void shouldNotDoWorkOnClosedRunner() throws Exception
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
    void shouldHandleAgentTerminationExceptionThrownByAgent() throws Exception
    {
        final RuntimeException expectedException = new AgentTerminationException();
        when(mockAgent.doWork()).thenThrow(expectedException);

        runner.run();

        verify(mockAgent).doWork();
        verify(mockErrorHandler).onError(expectedException);
        verify(mockAtomicCounter, never()).increment();
        verify(mockAgent, times(1)).onClose();
        assertTrue(runner.isClosed());
    }

    @Test
    void shouldReportExceptionThrownByAgent() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final RuntimeException expectedException = new RuntimeException();
        when(mockAgent.doWork()).thenThrow(expectedException);

        doAnswer(
            (invocation) ->
            {
                latch.countDown();
                return null;
            })
            .when(mockErrorHandler).onError(expectedException);

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
    void shouldNotReportClosedByInterruptException() throws Exception
    {
        when(mockAgent.doWork()).thenThrow(new ClosedByInterruptException());

        assertExceptionNotReported();
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
    }

    @Test
    void shouldRaiseInterruptFlagByClose() throws Exception
    {
        when(mockAgent.doWork()).then(answersWithDelay(500, RETURNS_DEFAULTS));

        new Thread(runner).start();

        while (runner.thread() == null)
        {
            Thread.yield();
        }

        Thread.currentThread().interrupt();

        final MutableBoolean actionCalled = new MutableBoolean();
        final Consumer<Thread> failedCloseAction = (ignore) -> actionCalled.set(true);
        runner.close(AgentRunner.RETRY_CLOSE_TIMEOUT_MS, failedCloseAction);

        assertTrue(Thread.interrupted());
        assertTrue(actionCalled.get());
    }

    @Test
    void shouldInvokeActionOnRetryCloseTimeout() throws Exception
    {
        when(mockAgent.doWork()).then(answersWithDelay(500, RETURNS_DEFAULTS));

        final Thread agentRunnerThread = new Thread(runner);
        agentRunnerThread.start();

        Thread.sleep(100);

        final AtomicInteger closeTimeoutCalls = new AtomicInteger();
        runner.close(
            1,
            (t) ->
            {
                closeTimeoutCalls.incrementAndGet();
                assertSame(t, agentRunnerThread);
            });

        assertThat(closeTimeoutCalls.get(), greaterThan(0));
    }

    private void assertExceptionNotReported() throws Exception
    {
        new Thread(runner).start();

        Thread.sleep(100);

        runner.close();

        verify(mockAgent, times(1)).onStart();
        verify(mockAgent, atLeastOnce()).doWork();
        verify(mockErrorHandler, never()).onError(any());
        verify(mockAtomicCounter, never()).increment();
    }
}
