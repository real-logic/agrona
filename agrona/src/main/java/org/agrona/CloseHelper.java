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
package org.agrona;

import java.util.Collection;

/**
 * Utility functions to help with using {@link java.lang.AutoCloseable} resources. If a null exception is passed
 * then it is ignored.
 */
public final class CloseHelper
{
    private CloseHelper()
    {
    }

    /**
     * Quietly close a {@link java.lang.AutoCloseable} dealing with nulls and exceptions.
     *
     * @param closeable to be closed.
     */
    public static void quietClose(final AutoCloseable closeable)
    {
        try
        {
            if (null != closeable)
            {
                closeable.close();
            }
        }
        catch (final Throwable ignore)
        {
        }
    }

    /**
     * Close all closeables in closeables. All exceptions and nulls will be ignored.
     *
     * @param closeables to be closed.
     */
    public static void quietCloseAll(final Collection<? extends AutoCloseable> closeables)
    {
        if (closeables == null || closeables.isEmpty())
        {
            return;
        }

        for (final AutoCloseable closeable : closeables)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Throwable ignore)
                {
                }
            }
        }
    }

    /**
     * Close all closeables in closeables. All exceptions and nulls will be ignored.
     *
     * @param closeables to be closed.
     */
    public static void quietCloseAll(final AutoCloseable... closeables)
    {
        if (closeables == null || closeables.length == 0)
        {
            return;
        }

        for (final AutoCloseable closeable : closeables)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Throwable ignore)
                {
                }
            }
        }
    }

    /**
     * Close a {@link java.lang.AutoCloseable} dealing with nulls and exceptions.
     * This version re-throws exceptions as runtime exceptions.
     *
     * @param closeable to be closed.
     */
    public static void close(final AutoCloseable closeable)
    {
        try
        {
            if (null != closeable)
            {
                closeable.close();
            }
        }
        catch (final Throwable ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    /**
     * Close all provided closeables. If any of them throw then throw that exception.
     * If multiple closeables throw an exception when being closed, then throw an exception that contains
     * all of them as suppressed exceptions.
     *
     * @param closeables to be closed.
     */
    public static void closeAll(final Collection<? extends AutoCloseable> closeables)
    {
        if (closeables == null || closeables.isEmpty())
        {
            return;
        }

        Throwable error = null;
        for (final AutoCloseable closeable : closeables)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Throwable ex)
                {
                    if (error == null)
                    {
                        error = ex;
                    }
                    else
                    {
                        error.addSuppressed(ex);
                    }
                }
            }
        }

        if (error != null)
        {
            LangUtil.rethrowUnchecked(error);
        }
    }

    /**
     * Close all provided closeables. If any of them throw then throw that exception.
     * If multiple closeables throw an exception when being closed, then throw an exception that contains
     * all of them as suppressed exceptions.
     *
     * @param closeables to be closed.
     */
    public static void closeAll(final AutoCloseable... closeables)
    {
        if (closeables == null || closeables.length == 0)
        {
            return;
        }

        Throwable error = null;
        for (final AutoCloseable closeable : closeables)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Throwable ex)
                {
                    if (error == null)
                    {
                        error = ex;
                    }
                    else
                    {
                        error.addSuppressed(ex);
                    }
                }
            }
        }

        if (error != null)
        {
            LangUtil.rethrowUnchecked(error);
        }
    }

    /**
     * Close a {@link java.lang.AutoCloseable} delegating exceptions to the {@link ErrorHandler}.
     *
     * @param errorHandler to delegate exceptions to.
     * @param closeable    to be closed.
     */
    public static void close(final ErrorHandler errorHandler, final AutoCloseable closeable)
    {
        try
        {
            if (null != closeable)
            {
                closeable.close();
            }
        }
        catch (final Throwable ex)
        {
            errorHandler.onError(ex);
        }
    }

    /**
     * Close all closeables and delegate exceptions to the {@link ErrorHandler}.
     *
     * @param errorHandler to delegate exceptions to.
     * @param closeables   to be closed.
     */
    public static void closeAll(final ErrorHandler errorHandler, final Collection<? extends AutoCloseable> closeables)
    {
        if (closeables == null || closeables.isEmpty())
        {
            return;
        }

        for (final AutoCloseable closeable : closeables)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Throwable ex)
                {
                    errorHandler.onError(ex);
                }
            }
        }
    }

    /**
     * Close all closeables and delegate exceptions to the {@link ErrorHandler}.
     *
     * @param errorHandler to delegate exceptions to.
     * @param closeables   to be closed.
     */
    public static void closeAll(final ErrorHandler errorHandler, final AutoCloseable... closeables)
    {
        if (closeables == null || closeables.length == 0)
        {
            return;
        }

        for (final AutoCloseable closeable : closeables)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Throwable ex)
                {
                    errorHandler.onError(ex);
                }
            }
        }
    }
}
