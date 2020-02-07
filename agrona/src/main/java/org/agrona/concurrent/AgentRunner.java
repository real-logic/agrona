/*
 * Copyright 2014-2020 Real Logic Limited.
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
import org.agrona.concurrent.status.AtomicCounter;

import java.nio.channels.ClosedByInterruptException;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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

    /**
     * Default retry timeout for closing.
     */
    public static final int RETRY_CLOSE_TIMEOUT_MS = 5000;

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
     * @param runner the agent runner to start.
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
     * The {@link Agent} which is contained.
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
                errorHandler.onError(throwable);
                isRunning = false;
            }

            while (isRunning)
            {
                doDutyCycle(idleStrategy, agent);
            }

            try
            {
                agent.onClose();
            }
            catch (final Throwable throwable)
            {
                errorHandler.onError(throwable);
            }
        }
        finally
        {
            isClosed = true;
        }
    }

    /**
     * Stop the running Agent and cleanup.
     * <p>
     * This is equivalent to calling {@link AgentRunner#close(int, Consumer)}
     * using the default {@link AgentRunner#RETRY_CLOSE_TIMEOUT_MS} value and a
     * null action.
     */
    public final void close()
    {
        close(RETRY_CLOSE_TIMEOUT_MS, null);
    }

    /**
     * Stop the running Agent and cleanup.
     * <p>
     * This will wait for the work loop to exit. The close timeout parameter
     * controls how long we should wait before retrying to stop the agent by
     * interrupting the thread. If the calling thread has its interrupt flag
     * set then this method can return early before waiting for the running
     * agent to close.
     * <p>
     * An optional action can be invoked whenever we time out while waiting
     * which accepts the agent runner thread as the parameter (e.g. to obtain
     * and log a stack trace from the thread). If the action is null, a message
     * is written to stderr. Please note  that a retry close timeout of zero
     * waits indefinitely, in which case the fail action is only called on interrupt.
     *
     * @param retryCloseTimeoutMs how long to wait before retrying.
     * @param closeFailAction     function to invoke before retrying after close timeout.
     */
    public final void close(final int retryCloseTimeoutMs, final Consumer<Thread> closeFailAction)
    {
        isRunning = false;

        final Thread thread = this.thread.getAndSet(TOMBSTONE);
        if (null == thread)
        {
            try
            {
                agent.onClose();
            }
            catch (final Throwable throwable)
            {
                errorHandler.onError(throwable);
            }
            finally
            {
                isClosed = true;
            }
        }
        else if (TOMBSTONE != thread)
        {
            while (true)
            {
                try
                {
                    if (isClosed)
                    {
                        return;
                    }

                    thread.join(retryCloseTimeoutMs);

                    if (!thread.isAlive() || isClosed)
                    {
                        return;
                    }

                    failAction(closeFailAction, thread, " due to timeout, retrying...");

                    if (!thread.isInterrupted())
                    {
                        thread.interrupt();
                    }
                }
                catch (final InterruptedException ignore)
                {
                    Thread.currentThread().interrupt();

                    failAction(closeFailAction, thread, " due to thread interrupt");

                    if (!isClosed && !thread.isInterrupted())
                    {
                        thread.interrupt();
                        Thread.yield();
                    }

                    return;
                }
            }
        }
    }

    private void failAction(final Consumer<Thread> closeFailAction, final Thread thread, final String message)
    {
        if (null == closeFailAction)
        {
            System.err.println(agent.roleName() + " failed to close due to " + message);
        }
        else
        {
            closeFailAction.accept(thread);
        }
    }

    private void doDutyCycle(final IdleStrategy idleStrategy, final Agent agent)
    {
        try
        {
            idleStrategy.idle(agent.doWork());
        }
        catch (final InterruptedException | ClosedByInterruptException ignore)
        {
            isRunning = false;
            Thread.currentThread().interrupt();
        }
        catch (final AgentTerminationException ex)
        {
            isRunning = false;
            handleError(ex);
        }
        catch (final Throwable throwable)
        {
            handleError(throwable);
        }
    }

    private void handleError(final Throwable throwable)
    {
        if (null != errorCounter && isRunning && !errorCounter.isClosed())
        {
            errorCounter.increment();
        }

        errorHandler.onError(throwable);
    }
}
