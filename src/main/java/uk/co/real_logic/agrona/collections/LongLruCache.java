/*
 * Copyright 2015 Real Logic Ltd.
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
package uk.co.real_logic.agrona.collections;

import uk.co.real_logic.agrona.LangUtil;

import java.util.function.LongFunction;

public final class LongLruCache<T extends AutoCloseable>
{
    private static final int MISSING = -1;

    private final int capacity;
    private final LongFunction<T> factory;
    private final long[] keys;
    private final Object[] values;

    private int size;

    public LongLruCache(final int capacity, final LongFunction<T> factory)
    {
        this.capacity = capacity;
        this.factory = factory;
        keys = new long[capacity];
        values = new Object[capacity];

        size = 0;
    }

    @SuppressWarnings("unchecked")
    public T lookup(final long key)
    {
        int size = this.size;
        final long[] keys = this.keys;
        final Object[] values = this.values;

        for (int i = 0; i < size; i++)
        {
            if (keys[i] == key)
            {
                final T value = (T) values[i];

                makeMostRecent(key, value, i);

                return value;
            }
        }

        final T value = factory.apply(key);

        if (size == capacity)
        {
            try
            {
                ((AutoCloseable) values[size - 1]).close();
            }
            catch (Exception e)
            {
                LangUtil.rethrowUnchecked(e);
            }
        }
        else
        {
            size++;
            this.size = size;
        }

        makeMostRecent(key, value, size - 1);

        return value;
    }

    private void makeMostRecent(final long key, final Object value, final int fromIndex)
    {
        final long[] keys = this.keys;
        final Object[] values = this.values;

        for (int i = fromIndex; i > 0; i--)
        {
            keys[i] = keys[i - 1];
            values[i] = values[i - 1];
        }

        keys[0] = key;
        values[0] = value;
    }

    public int capacity()
    {
        return capacity;
    }
}
