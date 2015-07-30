package uk.co.real_logic.agrona.nio;

import uk.co.real_logic.agrona.LangUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.Selector;

/**
 * Implements the common functionality for a transport poller.
 */
public class TransportPoller implements AutoCloseable
{
    private static final String SELECTOR_IMPL = "sun.nio.ch.SelectorImpl";

    protected static final Field SELECTED_KEYS_FIELD;
    protected static final Field PUBLIC_SELECTED_KEYS_FIELD;
    protected static final int ITERATION_THRESHOLD = 5;

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

    protected final Selector selector;
    protected final NioSelectedKeySet selectedKeySet;

    public TransportPoller()
    {
        try
        {
            selector = Selector.open(); // yes, SelectorProvider, blah, blah
            selectedKeySet = new NioSelectedKeySet();

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
     * Explicit call to selectNow but without processing of selected keys.
     */
    public void selectNowWithoutProcessing()
    {
        try
        {
            selector.selectNow();
            selectedKeySet.reset();
        }
        catch (final IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }
}
