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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;
import static org.agrona.collections.Hashing.compoundKey;

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

    /**
     * Creates a new value based upon keys.
     *
     * @param <V> type of the value.
     */
    public interface EntryRemap<V, V1>
    {
        /**
         * A map entry.
         *
         * @param keyPartA for the key
         * @param keyPartB for the key
         * @param oldValue to be remapped
         * @return value for the entry
         */
        V1 apply(int keyPartA, int keyPartB, V oldValue);
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
     *
     * @see Map#clear()
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
     * @see Map#put(Object, Object)
     */
    @SuppressWarnings("unchecked")
    public V put(final int keyPartA, final int keyPartB, final V value)
    {
        final V val = (V)mapNullValue(value);
        final long key = compoundKey(keyPartA, keyPartB);
        requireNonNull(val, "value cannot be null");

        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object oldValue;
        while (null != (oldValue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        if (null == oldValue)
        {
            ++size;
            keys[index] = key;
        }

        values[index] = val;

        if (size > resizeThreshold)
        {
            increaseCapacity();
        }

        return unmapNullValue(oldValue);
    }

    /**
     * Interceptor for masking null values.
     *
     * @param value value to mask.
     * @return masked value.
     */
    protected Object mapNullValue(final Object value)
    {
        return value;
    }

    /**
     * Interceptor for unmasking null values.
     *
     * @param value value to unmask.
     * @return unmasked value.
     */
    @SuppressWarnings("unchecked")
    protected V unmapNullValue(final Object value)
    {
        return (V)value;
    }

    @SuppressWarnings("unchecked")
    private V getMapping(final int keyPartA, final int keyPartB)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        final long[] keys = this.keys;
        final Object[] values = this.values;
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
     * Retrieve a value from the map.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @return value matching the key if found or null if not found.
     * @see Map#get(Object)
     */
    public V get(final int keyPartA, final int keyPartB)
    {
        return unmapNullValue(getMapping(keyPartA, keyPartB));
    }

    /**
     * Retrieve a value from the map or <code>defaultValue</code> if this map contains not mapping for the key.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @param defaultValue the default mapping of the key
     * @return value matching the key if found or <code>defaultValue</code> if not found.
     * @see java.util.Map#getOrDefault(Object, Object)
     */
    public V getOrDefault(final int keyPartA, final int keyPartB, final V defaultValue)
    {
        final V val = getMapping(keyPartA, keyPartB);
        return unmapNullValue(null != val ? val : defaultValue);
    }

    /**
     * Returns true if this map contains a mapping for the specified key.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @return <code>true</code> if this map contains a mapping for the specified key
     * @see java.util.Map#containsKey(Object)
     */
    public boolean containsKey(final int keyPartA, final int keyPartB)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        boolean found = false;
        while (null != values[index])
        {
            if (key == keys[index])
            {
                found = true;
                break;
            }

            index = ++index & mask;
        }

        return found;
    }

    /**
     * Remove a value from the map and return the value.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @return the previous value if found otherwise null
     * @see Map#remove(Object)
     */
    @SuppressWarnings("unchecked")
    public V remove(final int keyPartA, final int keyPartB)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        final long[] keys = this.keys;
        final Object[] values = this.values;
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
     * @see Map#computeIfAbsent(Object, Function)
     */
    public V computeIfAbsent(final int keyPartA, final int keyPartB, final EntryFunction<? extends V> mappingFunction)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        requireNonNull(mappingFunction);
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object mappedValue;
        while (null != (mappedValue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        V value = unmapNullValue(mappedValue);

        if (null == value && (value = mappingFunction.apply(keyPartA, keyPartB)) != null)
        {
            values[index] = value;
            if (null == mappedValue)
            {
                keys[index] = key;
                if (++size > resizeThreshold)
                {
                    increaseCapacity();
                }
            }
        }

        return value;
    }

    /**
     * If the value for the specified key is present and non-null, attempts to compute a new mapping given the key and
     * its current mapped value.
     * <p>
     * If the function returns null, the mapping is removed. If the function itself throws an (unchecked) exception,
     * the exception is rethrown, and the current mapping is left unchanged.
     *
     * @param keyPartA          for the key
     * @param keyPartB          for the key
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @see Map#computeIfPresent(Object, BiFunction)
     */
    public V computeIfPresent(
        final int keyPartA,
        final int keyPartB,
        final EntryRemap<? super V, ? extends V> remappingFunction)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        requireNonNull(remappingFunction);
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object mappedValue;
        while (null != (mappedValue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        V value = unmapNullValue(mappedValue);

        if (null != value)
        {
            value = remappingFunction.apply(keyPartA, keyPartB, value);
            values[index] = value;
            if (null == value)
            {
                --size;
                compactChain(index);
            }
        }

        return value;
    }

    /**
     * Attempts to compute a mapping for the specified key and its current mapped value (or null if there is no current
     * mapping).
     *
     * @param keyPartA          for the key
     * @param keyPartB          for the key
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @see Map#compute(Object, BiFunction)
     */
    public V compute(final int keyPartA, final int keyPartB, final EntryRemap<? super V, ? extends V> remappingFunction)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        requireNonNull(remappingFunction);
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object mappedvalue;
        while (null != (mappedvalue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        final V newValue = remappingFunction.apply(keyPartA, keyPartB, unmapNullValue(mappedvalue));
        if (null != newValue)
        {
            values[index] = newValue;
            if (null == mappedvalue)
            {
                keys[index] = key;
                if (++size > resizeThreshold)
                {
                    increaseCapacity();
                }
            }
        }
        else if (null != mappedvalue)
        {
            values[index] = null;
            size--;
            compactChain(index);
        }

        return newValue;
    }


    /**
     * If the specified key is not already associated with a value or is associated with null, associates it with the
     * given non-null value. Otherwise, replaces the associated value with the results of the given remapping function,
     * or removes if the result is null.
     *
     * @param keyPartA          for the key
     * @param keyPartB          for the key
     * @param value             the non-null value to be merged with the existing value associated with the key or, if
     *                          no existing value or a null value is associated with the key, to be associated with the
     *                          key
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if no value is associated with the key
     * @see Map#merge(Object, Object, BiFunction)
     */
    public V merge(
        final int keyPartA,
        final int keyPartB,
        final V value,
        final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        requireNonNull(value);
        requireNonNull(remappingFunction);
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object mappedvalue;
        while (null != (mappedvalue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        final V oldValue = unmapNullValue(mappedvalue);
        final V newValue = null == oldValue ? value : remappingFunction.apply(oldValue, value);

        if (null != newValue)
        {
            values[index] = newValue;
            if (null == mappedvalue)
            {
                keys[index] = key;
                if (++size > resizeThreshold)
                {
                    increaseCapacity();
                }
            }
        }
        else if (null != mappedvalue)
        {
            values[index] = null;
            size--;
            compactChain(index);
        }

        return newValue;
    }

    /**
     * Iterate over the contents of the map
     *
     * @param consumer to apply to each value in the map
     */
    @SuppressWarnings("unchecked")
    public void forEach(final Consumer<V> consumer)
    {
        int remaining = this.size;
        final Object[] values = this.values;

        for (int i = 0, length = values.length; remaining > 0 && i < length; i++)
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
    public void forEach(final EntryConsumer<V> consumer)
    {
        int remaining = this.size;
        final long[] keys = this.keys;
        final Object[] values = this.values;

        for (int i = 0, length = values.length; remaining > 0 && i < length; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                final long compoundKey = keys[i];
                final int keyPartA = (int)(compoundKey >>> 32);
                final int keyPartB = (int)(compoundKey & 0xFFFF_FFFFL);

                consumer.accept(keyPartA, keyPartB, unmapNullValue(value));
                --remaining;
            }
        }
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or null if there was no mapping for the key.
     * (A null return can also indicate that the map previously associated null with the key, if the implementation
     * supports null values.)
     * @see Map#replace(Object, Object)
     */
    @SuppressWarnings("unchecked")
    public V replace(final int keyPartA, final int keyPartB, final V value)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        final V val = (V)mapNullValue(value);
        requireNonNull(val, "value cannot be null");

        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object oldValue;
        while (null != (oldValue = values[index]))
        {
            if (key == keys[index])
            {
                values[index] = val;
                break;
            }

            index = ++index & mask;
        }

        return unmapNullValue(oldValue);
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @param oldValue value expected to be associated with the specified key
     * @param newValue to be associated with the specified key
     * @return true if the value was replaced
     */
    @SuppressWarnings("unchecked")
    public boolean replace(final int keyPartA, final int keyPartB, final V oldValue, final V newValue)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        final V val = (V)mapNullValue(newValue);
        requireNonNull(val, "value cannot be null");

        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object mappedValue;
        while (null != (mappedValue = values[index]))
        {
            if (key == keys[index])
            {
                if (Objects.equals(unmapNullValue(mappedValue), oldValue))
                {
                    values[index] = val;
                    return true;
                }
                break;
            }

            index = ++index & mask;
        }

        return false;
    }

    /**
     * If the specified key is not already associated with a value (or is mapped to null) associates it with the given
     * value and returns null, else returns the current value.
     *
     * @param keyPartA for the key
     * @param keyPartB for the key
     * @param value    to put into the map
     * @return the previous value if found otherwise null
     */
    @SuppressWarnings("unchecked")
    public V putIfAbsent(final int keyPartA, final int keyPartB, final V value)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        final V val = (V)mapNullValue(value);
        requireNonNull(val, "value cannot be null");

        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = Hashing.hash(key, mask);

        Object mappedValue;
        while (null != (mappedValue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        final V oldValue = unmapNullValue(mappedValue);
        if (null == oldValue)
        {
            if (null == mappedValue)
            {
                ++size;
                keys[index] = key;
            }

            values[index] = val;

            if (size > resizeThreshold)
            {
                increaseCapacity();
            }
        }

        return oldValue;
    }

    /**
     * Removes the entry for the specified key only if it is currently mapped to the specified value.
     *
     * @param keyPartA  for the key
     * @param keyPartB  for the key
     * @param value     value expected to be associated with the specified key
     * @return true if the value was removed
     * @see Map#remove(Object, Object)
     */
    public boolean remove(final int keyPartA, final int keyPartB, final V value)
    {
        final long key = compoundKey(keyPartA, keyPartB);
        final Object val = mapNullValue(value);
        if (null != val)
        {
            final long[] keys = this.keys;
            final Object[] values = this.values;
            final int mask = values.length - 1;
            int index = Hashing.hash(key, mask);

            Object mappedValue;
            while (null != (mappedValue = values[index]))
            {
                if (key == keys[index])
                {
                    if (Objects.equals(unmapNullValue(mappedValue), value))
                    {
                        values[index] = null;
                        --size;

                        compactChain(index);
                        return true;
                    }
                    break;
                }

                index = ++index & mask;
            }
        }
        return false;
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

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');

        final long[] keys = this.keys;
        final Object[] values = this.values;
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

        final long[] keys = this.keys;
        final Object[] values = this.values;
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

        this.keys = tempKeys;
        this.values = tempValues;
    }

    @SuppressWarnings("FinalParameters")
    private void compactChain(int deleteIndex)
    {
        final int mask = values.length - 1;
        int index = deleteIndex;
        final long[] keys = this.keys;
        final Object[] values = this.values;
        while (true)
        {
            index = ++index & mask;
            final Object value = values[index];
            if (null == value)
            {
                break;
            }

            final long key = keys[index];
            final int hash = Hashing.hash(key, mask);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                keys[deleteIndex] = key;
                values[deleteIndex] = value;

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
