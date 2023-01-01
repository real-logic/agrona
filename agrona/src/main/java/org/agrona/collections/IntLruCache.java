/*
 * Copyright 2014-2023 Real Logic Limited.
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
package org.agrona.collections;

import org.agrona.generation.DoNotSub;

import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * A fixed capacity cache of int keyed values that evicts the least-recently-used element when it runs out of space.
 * <p>
 * When an element is evicted it is closed by calling the closer function with the element as an argument.
 * <p>
 * When a new key arrives the factory function is called in order to create the new element associated with that key.
 *
 * @param <E> the type of element that this cache holds.
 */
public final class IntLruCache<E> implements AutoCloseable
{
    @DoNotSub private final int capacity;
    private final IntFunction<E> factory;
    private final Consumer<E> closer;
    private final int[] keys;
    private final Object[] values;

    @DoNotSub private int size;

    /**
     * Constructor.
     *
     * @param capacity this is the fixed capacity of the cache.
     * @param factory  a function for constructing new elements based upon keys.
     * @param closer   a function for cleaning up resources associated with elements.
     */
    public IntLruCache(
        @DoNotSub final int capacity,
        final IntFunction<E> factory,
        final Consumer<E> closer)
    {
        this.capacity = capacity;
        this.factory = factory;
        this.closer = closer;
        keys = new int[capacity];
        values = new Object[capacity];

        size = 0;
    }

    /**
     * Looks up an element in the cache, creating a new element if it doesn't exist and evicting the least recently
     * used element if there's no space left in the cache.
     *
     * @param key the key to look up the element by.
     * @return the element associated with this key.
     */
    @SuppressWarnings("unchecked")
    public E lookup(final int key)
    {
        @DoNotSub int size = this.size;
        final int[] keys = this.keys;
        final Object[] values = this.values;

        for (@DoNotSub int i = 0; i < size; i++)
        {
            if (key == keys[i])
            {
                final E value = (E)values[i];

                makeMostRecent(key, value, i);

                return value;
            }
        }

        final E value = factory.apply(key);

        if (value != null)
        {
            if (capacity == size)
            {
                closer.accept((E)values[size - 1]);
            }
            else
            {
                size++;
                this.size = size;
            }

            makeMostRecent(key, value, size - 1);
        }

        return value;
    }

    private void makeMostRecent(
        final int key,
        final Object value,
        @DoNotSub final int fromIndex)
    {
        final int[] keys = this.keys;
        final Object[] values = this.values;
        for (@DoNotSub int i = fromIndex; i > 0; i--)
        {
            keys[i] = keys[i - 1];
            values[i] = values[i - 1];
        }

        keys[0] = key;
        values[0] = value;
    }

    /**
     * Returns cache capacity.
     *
     * @return cache capacity.
     */
    @DoNotSub public int capacity()
    {
        return capacity;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void close()
    {
        final Consumer<E> closer = this.closer;
        final Object[] values = this.values;
        for (@DoNotSub int i = 0, size = this.size; i < size; i++)
        {
            closer.accept((E)values[i]);
        }
    }
}
