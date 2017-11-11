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
import org.agrona.concurrent.status.AtomicCounter;

import java.nio.channels.ClosedByInterruptException;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent runner containing an {@link Agent} which is run on a {@link Thread}.
 * <p>
 * <b>Note:</b> An instance should only be started once and then discarded, it should not be reused.
 */
public class AgentRunner implements Runnable, AutoCloseable
{
    /**
     * Indicates that the runner is being closed.
     */
    public static final Thread TOMBSTONE = new Thread();
    private static final int RETRY_CLOSE_TIMEOUT_MS = 3000;

    private volatile boolean isRunning = true;
    private volatile boolean isClosed = false;

    private final AtomicCounter errorCounter;
    private final ErrorHandler errorHandler;
    private final IdleStrategy idleStrategy;
    private final Agent agent;
    private final AtomicReference<Thread> thread = new AtomicReference<>();

    /**
     * Create an agent runner and initialise it.
     *
     * @param idleStrategy to use for Agent run loop
     * @param errorHandler to be called if an {@link Throwable} is encountered
     * @param errorCounter to be incremented each time an exception is encountered. This may be null.
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
     * @param runner        the agent runner to start.
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
     * The {@link Agent} which is contained
     *
     * @return {@link Agent} being contained.
     */
    public Agent agent()
    {
        return agent;
    }

    /**
     * Has the {@link Agent} been closed?
     *
     * @return has the {@link Agent} been closed?
     */
    public boolean isClosed()
    {
        return isClosed;
    }

    /**
     * Get the thread which is running that {@link Agent}.
     * <p>
     * If null then the runner has not been started. If {@link #TOMBSTONE} then the runner is being closed.
     *
     * @return the thread running the {@link Agent}.
     */
    public Thread thread()
    {
        return thread.get();
    }

    /**
     * Run an {@link Agent}.
     * <p>
     * This method does not return until the run loop is stopped via {@link #close()}.
     */
    public void run()
    {
        try
        {
            if (!thread.compareAndSet(null, Thread.currentThread()))
            {
                return;
            }

            final IdleStrategy idleStrategy = this.idleStrategy;
            final Agent agent = this.agent;
            try
            {
                agent.onStart();
            }
            catch (final Throwable throwable)
            {
                handleError(throwable);
                isRunning = false;
            }

            while (isRunning)
            {
                if (doDutyCycle(idleStrategy, agent))
                {
                    break;
                }
            }

            try
            {
                agent.onClose();
            }
            catch (final Throwable throwable)
            {
                handleError(throwable);
            }
        }
        finally
        {
            isClosed = true;
        }
    }

    /**
     * Stop the running Agent and cleanup. This will wait for the work loop to exit.
     */
    public final void close()
    {
        isRunning = false;

        final Thread thread = this.thread.getAndSet(TOMBSTONE);
        if (null == thread)
        {
            try
            {
                isClosed = true;
                agent.onClose();
            }
            catch (final Throwable throwable)
            {
                handleError(throwable);
            }
        }
        else if (TOMBSTONE != thread)
        {
            while (true)
            {
                try
                {
                    thread.join(RETRY_CLOSE_TIMEOUT_MS);

                    if (!thread.isAlive() || isClosed)
                    {
                        return;
                    }

                    System.err.println("Timeout waiting for agent '" + agent.roleName() + "' to close, Retrying...");

                    if (!thread.isInterrupted())
                    {
                        thread.interrupt();
                    }
                }
                catch (final InterruptedException ignore)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private boolean doDutyCycle(final IdleStrategy idleStrategy, final Agent agent)
    {
        try
        {
            idleStrategy.idle(agent.doWork());
        }
        catch (final InterruptedException | ClosedByInterruptException ignore)
        {
            return true;
        }
        catch (final AgentTerminationException ex)
        {
            handleError(ex);
            return true;
        }
        catch (final Throwable throwable)
        {
            handleError(throwable);
        }

        return false;
    }

    private void handleError(final Throwable throwable)
    {
        if (isRunning)
        {
            if (null != errorCounter)
            {
                errorCounter.increment();
            }

            errorHandler.onError(throwable);
        }
    }
}
