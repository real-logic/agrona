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
package org.agrona.collections;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;
import static org.agrona.collections.Hashing.compoundKey;

/**
 * Map that takes two part int key and associates with an object.
 *
 * @param <V> type of the object stored in the map.
 */
@SuppressWarnings("serial")
public class BiInt2ObjectMap<V> implements Serializable
{
    private static final long serialVersionUID = -4306301811303037776L;

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
     * Creates a new value based upon keys.
     *
     * @param <V> type of the value.
     */
    public interface EntryFunction<V>
    {
        /**
         * A map entry.
         *
         * @param keyPartA for the key
         * @param keyPartB for the key
         * @return value for the entry
         */
        V apply(int keyPartA, int keyPartB);
    }

    private static final int MIN_CAPACITY = 8;

    private final float loadFactor;
    private int resizeThreshold;
    private int size;

    private long[] keys;
    private Object[] values;

    /**
     * Construct an empty map.
     */
    public BiInt2ObjectMap()
    {
        this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Construct a map that sets it initial capacity and load factor.
     *
     * @param initialCapacity for the underlying hash map
     * @param loadFactor      for the underlying hash map
     */
    public BiInt2ObjectMap(final int initialCapacity, final float loadFactor)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));
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
        return values.length;
    }

    /**
     * Get the load factor beyond which the map will increase size.
     *
     * @return load factor for when the map should increase size.
     */
    public float loadFactor()
    {
        return loadFactor;
    }

    /**
     * Get the actual threshold which when reached the map will resize.
     * This is a function of the current capacity and load factor.
     *
     * @return the threshold when the map will resize.
     */
    public int resizeThreshold()
    {
        return resizeThreshold;
    }

    /**
     * Clear out the map of all entries.
     */
    public void clear()
    {
        if (size > 0)
        {
            Arrays.fill(values, null);
            size = 0;
        }
    }

    /**
     * Compact the backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        final int idealCapacity = (int)Math.round(size() * (1.0 / loadFactor));
        rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
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
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

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
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object value;
        while (null != (value = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        return (V)value;
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
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object value;
        while (null != (value = values[index]))
        {
            if (key == keys[index])
            {
                values[index] = null;
                --size;

                compactChain(index);
                break;
            }

            index = ++index & mask;
        }

        return (V)value;
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
        V value = get(keyPartA, keyPartB);
        if (value == null)
        {
            value = mappingFunction.apply(keyPartA, keyPartB);
            if (value != null)
            {
                put(keyPartA, keyPartB, value);
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
        int remaining = size;

        for (int i = 0, size = values.length; remaining > 0 && i < size; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                consumer.accept((V)value);
                --remaining;
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
        int remaining = size;

        for (int i = 0, size = values.length; remaining > 0 && i < size; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                final long compoundKey = keys[i];
                final int keyPartA = (int)(compoundKey >>> 32);
                final int keyPartB = (int)(compoundKey & 0xFFFF_FFFFL);

                consumer.accept(keyPartA, keyPartB, (V)value);
                --remaining;
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

    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');

        for (int i = 0, size = values.length; i < size; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                final long compoundKey = keys[i];
                final int keyPartA = (int)(compoundKey >>> 32);
                final int keyPartB = (int)(compoundKey & 0xFFFF_FFFFL);

                sb.append(keyPartA).append('_').append(keyPartB).append('=').append(value).append(", ");
            }
        }

        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 2);
        }

        sb.append('}');

        return sb.toString();
    }

    private void rehash(final int newCapacity)
    {
        final int mask = newCapacity - 1;
        resizeThreshold = (int)(newCapacity * loadFactor);

        final long[] tempKeys = new long[newCapacity];
        final Object[] tempValues = new Object[newCapacity];

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

    @SuppressWarnings("FinalParameters")
    private void compactChain(int deleteIndex)
    {
        final int mask = values.length - 1;
        int index = deleteIndex;
        while (true)
        {
            index = ++index & mask;
            if (null == values[index])
            {
                break;
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
        final int newCapacity = values.length << 1;
        if (newCapacity < 0)
        {
            throw new IllegalStateException("max capacity reached at size=" + size);
        }

        rehash(newCapacity);
    }
}
