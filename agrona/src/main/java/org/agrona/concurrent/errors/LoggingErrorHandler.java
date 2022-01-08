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
package org.agrona.concurrent.errors;

import org.agrona.ErrorHandler;

import java.io.PrintStream;
import java.util.Objects;

/**
 * A logging {@link ErrorHandler} that records to a {@link DistinctErrorLog} and if the log is full then overflows
 * to a {@link PrintStream}. If closed then error will be sent to {@link #errorOverflow()}.
 */
public class LoggingErrorHandler implements ErrorHandler, AutoCloseable
{
    private volatile boolean isClosed;
    private final DistinctErrorLog log;
    private final PrintStream errorOverflow;

    /**
     * Construct error handler wrapping a {@link DistinctErrorLog} with a default of {@link System#err} for the
     * {@link #errorOverflow()}.
     *
     * @param log to wrap.
     */
    public LoggingErrorHandler(final DistinctErrorLog log)
    {
        this(log, System.err);
    }

    /**
     * Construct error handler wrapping a {@link DistinctErrorLog} and {@link PrintStream} for error overflow.
     *
     * @param log           to wrap.
     * @param errorOverflow to be used if the log fills.
     */
    public LoggingErrorHandler(final DistinctErrorLog log, final PrintStream errorOverflow)
    {
        Objects.requireNonNull(log, "log");
        Objects.requireNonNull(log, "errorOverflow");

        this.log = log;
        this.errorOverflow = errorOverflow;
    }

    /**
     * Close error handler so that is does not attempt to write to underlying storage which may be unmapped.
     */
    public void close()
    {
        isClosed = true;
    }

    /**
     * Is this {@link LoggingErrorHandler} closed.
     *
     * @return true if {@link #close()} has been called otherwise false.
     */
    public boolean isClosed()
    {
        return isClosed;
    }

    /**
     * The wrapped log.
     *
     * @return the wrapped log.
     */
    public DistinctErrorLog distinctErrorLog()
    {
        return log;
    }

    /**
     * The wrapped {@link PrintStream} for error log overflow when the log is full.
     *
     * @return wrapped {@link PrintStream} for error log overflow when the log is full.
     */
    public PrintStream errorOverflow()
    {
        return errorOverflow;
    }

    /**
     * {@inheritDoc}
     */
    public void onError(final Throwable throwable)
    {
        if (isClosed)
        {
            errorOverflow.println("error log is closed");
            throwable.printStackTrace(errorOverflow);
        }
        else if (!log.record(throwable))
        {
            errorOverflow.println("error log is full, consider increasing length of error buffer");
            throwable.printStackTrace(errorOverflow);
        }
    }
}
