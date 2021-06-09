/*
 * Copyright 2014-2021 Real Logic Limited.
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
package org.agrona.nio;

import org.agrona.LangUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.Selector;

/**
 * Implements the common functionality for a transport poller.
 */
public class TransportPoller implements AutoCloseable
{
    /**
     * System property name for the threshold beyond which individual channel/socket polling will swap to to using
     * a {@link Selector}.
     */
    public static final String ITERATION_THRESHOLD_PROP_NAME = "org.agrona.transport.iteration.threshold";

    /**
     * Default threshold beyond which individual channel/socket polling will swap to to using a {@link Selector}.
     */
    public static final int ITERATION_THRESHOLD_DEFAULT = 5;

    /**
     * Threshold beyond which individual channel/socket polling will swap to to using a {@link Selector}.
     *
     * @see #ITERATION_THRESHOLD_PROP_NAME
     * @see #ITERATION_THRESHOLD_DEFAULT
     */
    public static final int ITERATION_THRESHOLD = Integer.getInteger(
        ITERATION_THRESHOLD_PROP_NAME, ITERATION_THRESHOLD_DEFAULT);

    /**
     * Reference to the {@code selectedKeys} field in the {@link Selector} class.
     */
    protected static final Field SELECTED_KEYS_FIELD;

    /**
     * Reference to the {@code publicSelectedKeys} field in the {@link Selector} class.
     */
    protected static final Field PUBLIC_SELECTED_KEYS_FIELD;

    private static final String SELECTOR_IMPL = "sun.nio.ch.SelectorImpl";

    static
    {
        Field selectKeysField = null;
        Field publicSelectKeysField = null;

        try
        {
            final Class<?> clazz = Class.forName(SELECTOR_IMPL, false, ClassLoader.getSystemClassLoader());

            if (clazz.isAssignableFrom(Selector.open().getClass()))
            {
                selectKeysField = clazz.getDeclaredField("selectedKeys");
                selectKeysField.setAccessible(true);

                publicSelectKeysField = clazz.getDeclaredField("publicSelectedKeys");
                publicSelectKeysField.setAccessible(true);
            }
        }
        catch (final Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
        finally
        {
            SELECTED_KEYS_FIELD = selectKeysField;
            PUBLIC_SELECTED_KEYS_FIELD = publicSelectKeysField;
        }
    }

    /**
     * KeySet used by the {@link Selector} which will be reused to avoid allocation.
     */
    protected final NioSelectedKeySet selectedKeySet = new NioSelectedKeySet();

    /**
     * Reference to the {@link Selector} for the transport.
     */
    protected final Selector selector;

    /**
     * Default constructor.
     */
    public TransportPoller()
    {
        try
        {
            selector = Selector.open(); // yes, SelectorProvider, blah, blah

            SELECTED_KEYS_FIELD.set(selector, selectedKeySet);
            PUBLIC_SELECTED_KEYS_FIELD.set(selector, selectedKeySet);
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Close NioSelector down. Returns immediately.
     */
    public void close()
    {
        selector.wakeup();
        try
        {
            selector.close();
        }
        catch (final IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    /**
     * Explicit call to {@link Selector#selectNow()} followed by clearing out the set without processing.
     */
    public void selectNowWithoutProcessing()
    {
        try
        {
            selector.selectNow();
            selectedKeySet.clear();
        }
        catch (final IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }
}
