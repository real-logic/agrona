/*
 * Copyright 2014 - 2015 Real Logic Ltd.
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

import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.Verify;

import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base agent runner that is responsible for lifecycle of an {@link Agent} and ensuring exceptions are handled.
 *
 * Note: An agent runner should only be once per instance.
 */
public class AgentRunner implements Runnable, AutoCloseable
{
    /**
     * Start the given agent runner on a new thread.
     *
     * @param runner the agent runner to start
     */
    public static void startOnThread(final AgentRunner runner)
    {
        final Thread thread = new Thread(runner);
        thread.setName(runner.agent().roleName());
        thread.start();
    }

    private static final Thread TOMBSTONE = new Thread();

    private volatile boolean running = true;

    private final AtomicCounter errorCounter;
    private final ErrorHandler errorHandler;
    private final IdleStrategy idleStrategy;
    private final Agent agent;
    private final AtomicReference<Thread> thread = new AtomicReference<>();

    /**
     * Create an agent passing in {@link IdleStrategy}
     *
     * @param idleStrategy to use for Agent run loop
     * @param errorHandler to be called if an {@link Throwable} is encountered
     * @param errorCounter for reporting how many exceptions have been seen.
     * @param agent        to be run in this thread.
     */
    public AgentRunner(
        final IdleStrategy idleStrategy,
        final ErrorHandler errorHandler,
        final AtomicCounter errorCounter,
        final Agent agent)
    {
        Verify.notNull(idleStrategy, "idleStrategy");
        Verify.notNull(errorHandler, "errorHandler");
        Verify.notNull(agent, "agent");

        this.idleStrategy = idleStrategy;
        this.errorHandler = errorHandler;
        this.errorCounter = errorCounter;
        this.agent = agent;
    }

    /**
     * The {@link Agent} who's lifecycle is being managed.
     *
     * @return {@link Agent} who's lifecycle is being managed.
     */
    public Agent agent()
    {
        return agent;
    }

    /**
     * Run an {@link Agent}.
     * <p>
     * This method does not return until the run loop is stopped via {@link #close()}.
     */
    public void run()
    {
        if (!thread.compareAndSet(null, Thread.currentThread()))
        {
            return;
        }

        final IdleStrategy idleStrategy = this.idleStrategy;
        final Agent agent = this.agent;
        while (running)
        {
            try
            {
                final int workCount = agent.doWork();
                idleStrategy.idle(workCount);
            }
            catch (final InterruptedException ignore)
            {
                Thread.interrupted();
            }
            catch (final ClosedByInterruptException ignore)
            {
                // Deliberately blank, if this exception is thrown then your interrupted status will be set.
            }
            catch (final Throwable ex)
            {
                if (null != errorCounter)
                {
                    errorCounter.increment();
                }

                errorHandler.onError(ex);
            }
        }
    }

    /**
     * Stop the running Agent and cleanup. This will wait for the work loop to exit and the {@link Agent} performing
     * it {@link Agent#onClose()} logic.
     *
     * The clean up logic will only be performed once even if close is called from multiple concurrent threads.
     */
    public final void close()
    {
        running = false;

        final Thread thread = this.thread.getAndSet(TOMBSTONE);
        if (null != thread && TOMBSTONE != thread && thread.isAlive())
        {
            thread.interrupt();

            while (true)
            {
                try
                {
                    thread.join(1000);
                    if (!thread.isAlive())
                    {
                        break;
                    }

                    System.err.println("timeout await for agent. Retrying...");
                }
                catch (final InterruptedException ignore)
                {
                    Thread.currentThread().interrupt();
                }
            }

            agent.onClose();
        }
    }
}
