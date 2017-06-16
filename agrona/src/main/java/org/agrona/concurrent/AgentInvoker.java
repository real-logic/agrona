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

/**
 * {@link Agent} container which does not start a thread. It instead allows the duty cycle {@link Agent#doWork()} to be
 * invoked directly. {@link #start()} should be called to allow the {@link Agent#onStart()} to fire before any calls
 * to {@link #invoke()} of the agent duty cycle.
 * <p>
 * Exceptions which occur during the {@link Agent#doWork()} invocation will be caught and passed to the provided
 * {@link ErrorHandler}.
 * <p>
 * <b>Note:</b> This class is not threadsafe.
 */
public class AgentInvoker implements AutoCloseable
{
    private boolean closed = false;
    private boolean started = false;

    private final AtomicCounter errorCounter;
    private final ErrorHandler errorHandler;
    private final Agent agent;

    /**
     * Create an agent and initialise it.
     *
     * @param errorHandler to be called if an {@link Throwable} is encountered
     * @param errorCounter to be incremented each time an exception is encountered. This may be null.
     * @param agent        to be run in this thread.
     */
    public AgentInvoker(
        final ErrorHandler errorHandler,
        final AtomicCounter errorCounter,
        final Agent agent)
    {
        Objects.requireNonNull(errorHandler, "errorHandler");
        Objects.requireNonNull(agent, "agent");

        this.errorHandler = errorHandler;
        this.errorCounter = errorCounter;
        this.agent = agent;
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
     * Invoke the {@link Agent#doWork()} method and return the work count.
     * <p>
     * If an error occurs then the {@link AtomicCounter#increment()} will be called on the errorCounter if not null
     * and the {@link Throwable} will be passed to the {@link ErrorHandler#onError(Throwable)} method.
     * <p>
     * Once closed this method will return without invoking the {@link Agent}.
     *
     * @return the work count for the {@link Agent#doWork()} method.
     */
    public int invoke()
    {
        int workCount = 0;
        if (!closed)
        {
            try
            {
                workCount = agent.doWork();
            }
            catch (final InterruptedException | ClosedByInterruptException ignore)
            {
                Thread.interrupted();
            }
            catch (final Throwable throwable)
            {
                handleError(throwable);
            }
        }

        return workCount;
    }

    /**
     * Mark the invoker as started and call the {@link Agent#onStart()} method.
     * <p>
     * Startup logic will only be performed once.
     */
    public void start()
    {
        try
        {
            if (!started)
            {
                started = true;
                agent.onStart();
            }
        }
        catch (final Throwable throwable)
        {
            handleError(throwable);
        }
    }

    /**
     * Mark the invoker as closed and call the {@link Agent#onClose()} logic for clean up.
     * <p>
     * The clean up logic will only be performed once.
     */
    public final void close()
    {
        if (!closed)
        {
            closed = true;
            agent.onClose();
        }
    }

    private void handleError(final Throwable throwable)
    {
        if (null != errorCounter)
        {
            errorCounter.increment();
        }

        errorHandler.onError(throwable);
    }
}
