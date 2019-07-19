/*
 * Copyright 2014-2019 Real Logic Ltd.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility functions to help with using {@link java.lang.AutoCloseable} resources.
 */
public class CloseHelper
{
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
        catch (final Exception ignore)
        {
        }
    }

    /**
     * Close all closeables in closeables. All exceptions and nulls will be ignored.
     *
     * @param closeables to be closed.
     */
    public static void quietCloseAll(final List<? extends AutoCloseable> closeables)
    {
        if (closeables == null)
        {
            return;
        }

        for (int i = 0, size = closeables.size(); i < size; i++)
        {
            final AutoCloseable closeable = closeables.get(i);
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Exception ignore)
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
        if (closeables == null)
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
                catch (final Exception ignore)
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
        catch (final Exception e)
        {
            LangUtil.rethrowUnchecked(e);
        }
    }

    /**
     * Close all closeables in closeables. If any of them throw then throw that exception.
     * If multiple closeables throw an exception when being closed, then throw an exception that contains
     * all of them as suppressed exceptions.
     *
     * @param closeables to be closed.
     */
    public static void closeAll(final List<? extends AutoCloseable> closeables)
    {
        if (closeables == null)
        {
            return;
        }

        List<Exception> exceptions = null;
        for (int i = 0, size = closeables.size(); i < size; i++)
        {
            final AutoCloseable closeable = closeables.get(i);
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Exception ex)
                {
                    if (exceptions == null)
                    {
                        exceptions = new ArrayList<>();
                    }
                    exceptions.add(ex);
                }
            }
        }

        if (exceptions != null)
        {
            final Exception exception = exceptions.remove(0);
            exceptions.forEach(exception::addSuppressed);
            LangUtil.rethrowUnchecked(exception);
        }
    }

    /**
     * Close all closeables in closeables. If any of them throw then throw that exception.
     * If multiple closeables throw an exception when being closed, then throw an exception that contains
     * all of them as suppressed exceptions.
     *
     * @param closeables to be closed.
     */
    public static void closeAll(final AutoCloseable... closeables)
    {
        if (closeables == null)
        {
            return;
        }

        List<Exception> exceptions = null;
        for (final AutoCloseable closeable : closeables)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (final Exception ex)
                {
                    if (exceptions == null)
                    {
                        exceptions = new ArrayList<>();
                    }
                    exceptions.add(ex);
                }
            }
        }

        if (exceptions != null)
        {
            final Exception exception = exceptions.remove(0);
            exceptions.forEach(exception::addSuppressed);
            LangUtil.rethrowUnchecked(exception);
        }
    }
}
