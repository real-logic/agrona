/*
 * Copyright 2014-2024 Real Logic Limited.
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

import org.agrona.BitUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Utility functions for collection objects.
 */
public final class CollectionUtil
{
    private CollectionUtil()
    {
    }

    /**
     * A getOrDefault that doesn't create garbage if its suppler is non-capturing.
     *
     * @param map      to perform the lookup on.
     * @param key      on which the lookup is done.
     * @param supplier of the default value if one is not found.
     * @param <K>      type of the key
     * @param <V>      type of the value
     * @return the value if found or a new default which as been added to the map.
     */
    public static <K, V> V getOrDefault(final Map<K, V> map, final K key, final Function<K, V> supplier)
    {
        V value = map.get(key);
        if (value == null)
        {
            value = supplier.apply(key);
            map.put(key, value);
        }

        return value;
    }

    /**
     * Garbage free sum function.
     * <p>
     * <b>Note:</b> the list must implement {@link java.util.RandomAccess} to be efficient.
     *
     * @param values   the list of input values
     * @param function function that map each value to an int
     * @param <V>      the value to add up
     * @return the sum of all the int values returned for each member of the list.
     */
    public static <V> int sum(final List<V> values, final ToIntFunction<V> function)
    {
        int total = 0;

        final int size = values.size();
        for (int i = 0; i < size; i++)
        {
            final V value = values.get(i);
            total += function.applyAsInt(value);
        }

        return total;
    }

    /**
     * Validate that a load factor is in the range of 0.1 to 0.9.
     * <p>
     * Load factors in the range 0.5 - 0.7 are recommended for open-addressing with linear probing.
     *
     * @param loadFactor to be validated.
     */
    public static void validateLoadFactor(final float loadFactor)
    {
        if (loadFactor < 0.1f || loadFactor > 0.9f)
        {
            throw new IllegalArgumentException("load factor must be in the range of 0.1 to 0.9: " + loadFactor);
        }
    }

    /**
     * Validate that a number is a power of two.
     *
     * @param value to be validated.
     */
    public static void validatePositivePowerOfTwo(final int value)
    {
        if (!BitUtil.isPowerOfTwo(value))
        {
            throw new IllegalArgumentException("value must be a positive power of two: " + value);
        }
    }

    /**
     * Remove element from a list if it matches a predicate.
     * <p>
     * <b>Note:</b> the list must implement {@link java.util.RandomAccess} to be efficient.
     *
     * @param values    to be iterated over.
     * @param predicate to test the value against
     * @param <T>       type of the value.
     * @return the number of items remove.
     */
    public static <T> int removeIf(final List<T> values, final Predicate<T> predicate)
    {
        int size = values.size();
        int total = 0;

        for (int i = 0; i < size; )
        {
            final T value = values.get(i);
            if (predicate.test(value))
            {
                values.remove(i);
                total++;
                size--;
            }
            else
            {
                i++;
            }
        }

        return total;
    }
}
