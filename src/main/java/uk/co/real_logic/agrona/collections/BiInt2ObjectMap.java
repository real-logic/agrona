/*
 * Copyright 2014 Real Logic Ltd.
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

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Map that takes two part int key and associates with an object.
 *
 * The underlying implementation use as {@link Long2ObjectHashMap} and combines both int keys into a long key.
 *
 * @param <V> type of the object stored in the map.
 */
public class BiInt2ObjectMap<V>
{
    /**
     * Handler for a map entry
     *
     * @param <V> type of the value
     */
    public interface EntryConsumer<V>
    {
        /**
         * A map entry
         *
         * @param keyPartA for the key
         * @param keyPartB for the key
         * @param value for the entry
         */
        void accept(int keyPartA, int keyPartB, V value);
    }

    /**
     * Creates new values based upon keys
     *
     * @param <V> type of the value
     */
    public interface EntryFunction<V>
    {
        /**
         * A map entry
         *
         * @param keyPartA for the key
         * @param keyPartB for the key
         * @return value for the entry
         */
        V apply(int keyPartA, int keyPartB);
    }

    private final Long2ObjectHashMap<V> map;

    /**
     * Construct an empty map
     */
    public BiInt2ObjectMap()
    {
        map = new Long2ObjectHashMap<>();
    }

    /**
     * See {@link Long2ObjectHashMap#Long2ObjectHashMap(int, double)}.
     *
     * @param initialCapacity for the underlying hash map
     * @param loadFactor for the underlying hash map
     */
    public BiInt2ObjectMap(final int initialCapacity, final double loadFactor)
    {
        map = new Long2ObjectHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Get the total capacity for the map to which the load factor with be a fraction of.
     *
     * @return the total capacity for the map.
     */
    public int capacity()
    {
        return map.capacity();
    }

    /**
     * Get the load factor beyond which the map will increase size.
     *
     * @return load factor for when the map should increase size.
     */
    public double loadFactor()
    {
        return map.loadFactor();
    }

    /**
     * Put a value into the map.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @param value to put into the map
     * @return the previous value if found otherwise null
     */
    public V put(final int keyPartA, final int keyPartB, final V value)
    {
        final long key = compoundKey(keyPartA, keyPartB);

        return map.put(key, value);
    }

    /**
     * Retrieve a value from the map.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @return value matching the key if found or null if not found.
     */
    public V get(final int keyPartA, final int keyPartB)
    {
        final long key = compoundKey(keyPartA, keyPartB);

        return map.get(key);
    }

    /**
     * Remove a value from the map and return the value.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @return the previous value if found otherwise null
     */
    public V remove(final int keyPartA, final int keyPartB)
    {
        final long key = compoundKey(keyPartA, keyPartB);

        return map.remove(key);
    }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}), attempts to compute its value using the given mapping
     * function and enters it into this map unless {@code null}.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @param mappingFunction creates values based upon keys if the key pair is missing
     * @return the newly created or stored value.
     */
    public V computeIfAbsent(
        final int keyPartA, final int keyPartB, final EntryFunction<? extends V> mappingFunction)
    {
        Objects.requireNonNull(mappingFunction);

        final long key = compoundKey(keyPartA, keyPartB);
        final V value = map.get(key);
        if (value == null)
        {
            final V newValue = mappingFunction.apply(keyPartA, keyPartB);
            if (newValue != null)
            {
                map.put(key, newValue);
                return newValue;
            }
        }

        return value;
    }

    /**
     * Iterate over the contents of the map
     *
     * @param consumer to apply to each value in the map
     */
    public void forEach(final Consumer<V> consumer)
    {
        map.forEach((k, v) -> consumer.accept(v));
    }

    /**
     * Iterate over the contents of the map
     *
     * @param consumer to apply to each value in the map
     */
    public void forEach(final EntryConsumer<V> consumer)
    {
        map.forEach(
            (compoundKey, value) ->
            {
                final int keyPartA = (int)(compoundKey >>> 32);
                final int keyPartB = (int)(compoundKey & 0xFFFFFFFFL);

                consumer.accept(keyPartA, keyPartB, value);
            });
    }

    /**
     * Return the number of unique entries in the map.
     *
     * @return number of unique entries in the map.
     */
    public int size()
    {
        return map.size();
    }

    /**
     * Is map empty or not.
     *
     * @return boolean indicating empty map or not
     */
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    private static long compoundKey(final int keyPartA, final int keyPartB)
    {
        return ((long)keyPartA << 32) | keyPartB;
    }
}
