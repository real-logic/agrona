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
package org.agrona.concurrent;

import org.agrona.ErrorHandler;
import org.agrona.concurrent.status.AtomicCounter;

import java.nio.channels.ClosedByInterruptException;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base agent runner that is responsible for lifecycle of an {@link Agent} and ensuring exceptions are handled.
 * <p>
 * Note: An agent runner should only be once per instance.
 */
public class AgentRunner implements Runnable, AutoCloseable
{
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
        Objects.requireNonNull(idleStrategy, "idleStrategy");
        Objects.requireNonNull(errorHandler, "errorHandler");
        Objects.requireNonNull(agent, "agent");

        this.idleStrategy = idleStrategy;
        this.errorHandler = errorHandler;
        this.errorCounter = errorCounter;
        this.agent = agent;
    }

    /**
     * Start the given agent runner on a new thread.
     *
     * @param runner the agent runner to start
     * @return the new thread that has been started.
     */
    public static Thread startOnThread(final AgentRunner runner)
    {
        return startOnThread(runner, Thread::new);
    }

    /**
     * Start the given agent runner on a new thread.
     *
     * @param runner the agent runner to start.
     * @param threadFactory the factory to use to create the thread.
     * @return the new thread that has been started.
     */
    public static Thread startOnThread(final AgentRunner runner, final ThreadFactory threadFactory)
    {
        final Thread thread = threadFactory.newThread(runner);
        thread.setName(runner.agent().roleName());
        thread.start();

        return thread;
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
                idleStrategy.idle(agent.doWork());
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
     * <p>
     * The clean up logic will only be performed once even if close is called from multiple concurrent threads.
     */
    public final void close()
    {
        running = false;

        final Thread thread = this.thread.getAndSet(TOMBSTONE);
        if (TOMBSTONE != thread)
        {
            if (null != thread)
            {
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

                        thread.interrupt();
                    }
                    catch (final InterruptedException ignore)
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            try
            {
                agent.quiesce();
            }
            finally
            {
                agent.onClose();
            }
        }
    }
}
