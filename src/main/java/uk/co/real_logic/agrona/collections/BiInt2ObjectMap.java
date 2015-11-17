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

import uk.co.real_logic.agrona.BitUtil;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import static uk.co.real_logic.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * Map that takes two part int key and associates with an object.
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
         * @param value    for the entry
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

    private final double loadFactor;
    private int resizeThreshold;
    private int capacity;
    private int mask;
    private int size;

    private long[] keys;
    private Object[] values;

    /**
     * Construct an empty map
     */
    public BiInt2ObjectMap()
    {
        this(8, 0.6);
    }

    /**
     * See {@link Long2ObjectHashMap#Long2ObjectHashMap(int, double)}.
     *
     * @param initialCapacity for the underlying hash map
     * @param loadFactor      for the underlying hash map
     */
    public BiInt2ObjectMap(final int initialCapacity, final double loadFactor)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        mask = capacity - 1;
        resizeThreshold = (int)(capacity * loadFactor);

        keys = new long[capacity];
        values = new Object[capacity];
    }

    /**
     * Get the total capacity for the map to which the load factor with be a fraction of.
     *
     * @return the total capacity for the map.
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * Get the load factor beyond which the map will increase size.
     *
     * @return load factor for when the map should increase size.
     */
    public double loadFactor()
    {
        return loadFactor;
    }

    /**
     * Clear out the map of all entries.
     */
    public void clear()
    {
        size = 0;
        Arrays.fill(values, null);
    }

    /**
     * Compact the backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
        rehash(BitUtil.findNextPositivePowerOfTwo(idealCapacity));
    }

    /**
     * Put a value into the map.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @param value    to put into the map
     * @return the previous value if found otherwise null
     */
    @SuppressWarnings("unchecked")
    public V put(final int keyPartA, final int keyPartB, final V value)
    {
        final long key = compoundKey(keyPartA, keyPartB);

        V oldValue = null;
        int index = Hashing.hash(keyPartA, keyPartB, mask);

        while (null != values[index])
        {
            if (key == keys[index])
            {
                oldValue = (V)values[index];
                break;
            }

            index = ++index & mask;
        }

        if (null == oldValue)
        {
            ++size;
            keys[index] = key;
        }

        values[index] = value;

        if (size > resizeThreshold)
        {
            increaseCapacity();
        }

        return oldValue;
    }

    /**
     * Retrieve a value from the map.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @return value matching the key if found or null if not found.
     */
    @SuppressWarnings("unchecked")
    public V get(final int keyPartA, final int keyPartB)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        int index = Hashing.hash(keyPartA, keyPartB, mask);

        Object value;
        while (null != (value = values[index]))
        {
            if (key == keys[index])
            {
                return (V)value;
            }

            index = ++index & mask;
        }

        return null;
    }

    /**
     * Remove a value from the map and return the value.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @return the previous value if found otherwise null
     */
    @SuppressWarnings("unchecked")
    public V remove(final int keyPartA, final int keyPartB)
    {
        final long key = compoundKey(keyPartA, keyPartB);

        int index = Hashing.hash(keyPartA, keyPartB, mask);

        Object value;
        while (null != (value = values[index]))
        {
            if (key == keys[index])
            {
                values[index] = null;
                --size;

                compactChain(index);

                return (V)value;
            }

            index = ++index & mask;
        }

        return null;
    }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}), attempts to compute its value using the given mapping
     * function and enters it into this map unless {@code null}.
     *
     * @param keyPartA        for the key
     * @param keyPartB        for the key
     * @param mappingFunction creates values based upon keys if the key pair is missing
     * @return the newly created or stored value.
     */
    public V computeIfAbsent(final int keyPartA, final int keyPartB, final EntryFunction<? extends V> mappingFunction)
    {
        Objects.requireNonNull(mappingFunction);

        final V value = get(keyPartA, keyPartB);
        if (value == null)
        {
            final V newValue = mappingFunction.apply(keyPartA, keyPartB);
            if (newValue != null)
            {
                put(keyPartA, keyPartB, newValue);
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
    @SuppressWarnings("unchecked")
    public void forEach(final Consumer<V> consumer)
    {
        for (final Object value : values)
        {
            if (null != value)
            {
                consumer.accept((V)value);
            }
        }
    }

    /**
     * Iterate over the contents of the map
     *
     * @param consumer to apply to each value in the map
     */
    @SuppressWarnings("unchecked")
    public void forEach(final EntryConsumer<V> consumer)
    {
        for (int i = 0, size = values.length; i < size; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                final long compoundKey = keys[i];
                final int keyPartA = (int)(compoundKey >>> 32);
                final int keyPartB = (int)(compoundKey & 0xFFFF_FFFFL);

                consumer.accept(keyPartA, keyPartB, (V)value);
            }
        }
    }

    /**
     * Return the number of unique entries in the map.
     *
     * @return number of unique entries in the map.
     */
    public int size()
    {
        return size;
    }

    /**
     * Is map empty or not.
     *
     * @return boolean indicating empty map or not
     */
    public boolean isEmpty()
    {
        return 0 == size;
    }

    private static long compoundKey(final int keyPartA, final int keyPartB)
    {
        return ((long)keyPartA << 32) | keyPartB;
    }

    private void rehash(final int newCapacity)
    {
        if (1 != Integer.bitCount(newCapacity))
        {
            throw new IllegalStateException("New capacity must be a power of two");
        }

        capacity = newCapacity;
        mask = newCapacity - 1;
        resizeThreshold = (int)(newCapacity * loadFactor);

        final long[] tempKeys = new long[capacity];
        final Object[] tempValues = new Object[capacity];

        for (int i = 0, size = values.length; i < size; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                final long key = keys[i];
                int newHash = Hashing.hash(key, mask);

                while (null != tempValues[newHash])
                {
                    newHash = ++newHash & mask;
                }

                tempKeys[newHash] = key;
                tempValues[newHash] = value;
            }
        }

        keys = tempKeys;
        values = tempValues;
    }

    private void compactChain(int deleteIndex)
    {
        int index = deleteIndex;
        while (true)
        {
            index = ++index & mask;
            if (null == values[index])
            {
                return;
            }

            final long key = keys[index];
            final int hash = Hashing.hash(key, mask);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                keys[deleteIndex] = key;
                values[deleteIndex] = values[index];

                values[index] = null;
                deleteIndex = index;
            }
        }
    }

    private void increaseCapacity()
    {
        final int newCapacity = capacity << 1;
        if (newCapacity < 0)
        {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        rehash(newCapacity);
    }
}
