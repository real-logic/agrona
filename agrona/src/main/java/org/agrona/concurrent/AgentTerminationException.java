/*
 * Copyright 2014-2022 Real Logic Limited.
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

/**
 * Thrown to terminate the work/duty cycle of an {@link Agent}.
 *
 * @see Agent
 * @see AgentInvoker
 * @see AgentRunner
 */
public class AgentTerminationException extends RuntimeException
{
    private static final long serialVersionUID = 5962977383701965069L;

    /**
     * Default constructor.
     */
    public AgentTerminationException()
    {
    }

    /**
     * Create an exception with the given message.
     *
     * @param message to assign.
     */
    public AgentTerminationException(final String message)
    {
        super(message);
    }

    /**
     * Create an exception with the given message and a cause.
     *
     * @param message to assign.
     * @param cause   of the error.
     */
    public AgentTerminationException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Create an exception with the given cause.
     *
     * @param cause of the error.
     */
    public AgentTerminationException(final Throwable cause)
    {
        super(cause);
    }

    /**
     * Create an exception with the given message and a cause.
     *
     * @param message            to assign.
     * @param cause              of the error.
     * @param enableSuppression  true to enable suppression.
     * @param writableStackTrace true to enable writing a full stack trace.
     */
    public AgentTerminationException(
        final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
