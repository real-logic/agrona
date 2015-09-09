/*
 * Copyright 2014 Real Logic Ltd.
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
import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.LangUtil;

import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class AgentRunnerTest
{
    private final ErrorHandler errorHandler = mock(ErrorHandler.class);
    private final Agent mockAgent = mock(Agent.class);
    private final IdleStrategy idleStrategy = new NoOpIdleStrategy();
    private final AgentRunner runner = new AgentRunner(idleStrategy, errorHandler, null, mockAgent);

    @Test
    public void shouldReturnAgent()
    {
        assertThat(runner.agent(), is(mockAgent));
    }

    @Test
    public void shouldNotDoWorkOnClosedRunnerButCallOnClose() throws Exception
    {
        runner.close();
        runner.run();

        verify(mockAgent, never()).doWork();
        verify(mockAgent).onClose();
    }

    @Test
    public void shouldReportExceptionThrownByAgent() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final RuntimeException expectedException = new RuntimeException();
        when(mockAgent.doWork()).thenThrow(expectedException);

        final ErrorHandler errorHandler =
            (ex) ->
            {
                assertThat(ex, is(expectedException));
                latch.countDown();
            };

        final AgentRunner runner = new AgentRunner(idleStrategy, errorHandler, null, mockAgent);
        new Thread(runner).start();

        if (!latch.await(3, TimeUnit.SECONDS))
        {
            fail("Should have called error handler");
        }

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
        final ErrorHandler errorHandler = mock(ErrorHandler.class);

        final AgentRunner runner = new AgentRunner(idleStrategy, errorHandler, null, mockAgent);
        new Thread(runner).start();

        Thread.sleep(100);

        runner.close();

        verify(errorHandler, never()).onError(any());
    }
}
