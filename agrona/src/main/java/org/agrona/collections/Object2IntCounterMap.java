/*
 * Copyright 2014-2022 Real Logic Limited.
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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * An open-addressing with linear probing hash map specialised for object and primitive counter pairs.
 * A counter map views counters which hit {@link #initialValue} as deleted.
 * This means that changing a counter may impact {@link #size()}.
 */
public class Object2IntCounterMap<K>
{
    @DoNotSub private static final int MIN_CAPACITY = 8;

    @DoNotSub private final float loadFactor;
    private final int initialValue;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size = 0;

    private K[] keys;
    private int[] values;

    /**
     * Construct a new counter map with the initial value for the counter provided.
     *
     * @param initialValue to be used for each counter.
     */
    public Object2IntCounterMap(final int initialValue)
    {
        this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, initialValue);
    }

    /**
     * Construct a new counter map with the initial value for the counter provided.
     *
     * @param initialCapacity of the map.
     * @param loadFactor      applied for resize operations.
     * @param initialValue    to be used for each counter.
     */
    @SuppressWarnings("unchecked")
    public Object2IntCounterMap(
        @DoNotSub final int initialCapacity,
        @DoNotSub final float loadFactor,
        final int initialValue)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        this.initialValue = initialValue;

        @DoNotSub final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));

        keys = (K[])new Object[capacity];
        values = new int[capacity];
        Arrays.fill(values, initialValue);

        /* @DoNotSub */ resizeThreshold = (int)(capacity * loadFactor);
    }

    /**
     * The value to be used as a null marker in the map.
     *
     * @return value to be used as a null marker in the map.
     */
    public int initialValue()
    {
        return initialValue;
    }

    /**
     * Get the load factor applied for resize operations.
     *
     * @return the load factor applied for resize operations.
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
    @DoNotSub public int resizeThreshold()
    {
        return resizeThreshold;
    }

    /**
     * Get the total capacity for the map to which the load factor will be a fraction of.
     *
     * @return the total capacity for the map.
     */
    @DoNotSub public int capacity()
    {
        return values.length;
    }

    /**
     * The current size of the map which at not at {@link #initialValue()}.
     *
     * @return map size, counters at {@link #initialValue()} are not counted.
     */
    @DoNotSub public int size()
    {
        return size;
    }

    /**
     * Is the map empty.
     *
     * @return size == 0
     */
    public boolean isEmpty()
    {
        return size == 0;
    }

    /**
     * Get the value of a counter associated with a key or {@link #initialValue()} if not found.
     *
     * @param key lookup key.
     * @return counter value associated with key or {@link #initialValue()} if not found.
     */
    public int get(final K key)
    {
        final int initialValue = this.initialValue;
        final K[] keys = this.keys;
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        int value;
        while (initialValue != (value = values[index]))
        {
            if (Objects.equals(keys[index], key))
            {
                break;
            }

            index = ++index & mask;
        }

        return value;
    }

    /**
     * Put the value for a key in the map.
     *
     * @param key   lookup key.
     * @param value new value, must not be initialValue.
     * @return current counter value associated with key, or {@link #initialValue()} if none found.
     * @throws IllegalArgumentException if value is {@link #initialValue()}.
     */
    public int put(final K key, final int value)
    {
        final int initialValue = this.initialValue;
        if (initialValue == value)
        {
            throw new IllegalArgumentException("cannot accept initialValue");
        }

        final K[] keys = this.keys;
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);
        int oldValue = initialValue;

        while (values[index] != initialValue)
        {
            if (Objects.equals(keys[index], key))
            {
                oldValue = values[index];
                break;
            }

            index = ++index & mask;
        }

        if (oldValue == initialValue)
        {
            ++size;
            keys[index] = key;
        }

        values[index] = value;

        increaseCapacity();

        return oldValue;
    }

    /**
     * Convenience version of {@link #addAndGet(Object, int)} (key, 1).
     *
     * @param key for the counter.
     * @return the new value.
     */
    public int incrementAndGet(final K key)
    {
        return addAndGet(key, 1);
    }

    /**
     * Convenience version of {@link #addAndGet(Object, int)} (key, -1).
     *
     * @param key for the counter.
     * @return the new value.
     */
    public int decrementAndGet(final K key)
    {
        return addAndGet(key, -1);
    }

    /**
     * Add amount to the current value associated with this key. If no such value exists use {@link #initialValue()} as
     * current value and associate key with {@link #initialValue()} + amount unless amount is 0, in which case map
     * remains unchanged.
     *
     * @param key    new or existing.
     * @param amount to be added.
     * @return the new value associated with the specified key, or
     *         {@link #initialValue()} + amount if there was no mapping for the key.
     */
    public int addAndGet(final K key, final int amount)
    {
        return getAndAdd(key, amount) + amount;
    }

    /**
     * Convenience version of {@link #getAndAdd(Object, int)} (key, 1).
     *
     * @param key for the counter.
     * @return the old value.
     */
    public int getAndIncrement(final K key)
    {
        return getAndAdd(key, 1);
    }

    /**
     * Convenience version of {@link #getAndAdd(Object, int)} (key, -1).
     *
     * @param key for the counter.
     * @return the old value.
     */
    public int getAndDecrement(final K key)
    {
        return getAndAdd(key, -1);
    }

    /**
     * Add amount to the current value associated with this key. If no such value exists use {@link #initialValue()} as
     * current value and associate key with {@link #initialValue()} + amount unless amount is 0, in which case map
     * remains unchanged.
     *
     * @param key    new or existing.
     * @param amount to be added.
     * @return the previous value associated with the specified key, or
     *         {@link #initialValue()} if there was no mapping for the key.
     */
    public int getAndAdd(final K key, final int amount)
    {
        final int initialValue = this.initialValue;
        final K[] keys = this.keys;
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);
        int oldValue = initialValue;

        while (initialValue != values[index])
        {
            if (Objects.equals(keys[index], key))
            {
                oldValue = values[index];
                break;
            }

            index = ++index & mask;
        }

        if (amount != 0)
        {
            final int newValue = oldValue + amount;
            values[index] = newValue;

            if (initialValue == oldValue)
            {
                ++size;
                keys[index] = key;
                increaseCapacity();
            }
            else if (initialValue == newValue)
            {
                size--;
                compactChain(index);
            }
        }

        return oldValue;
    }

    /**
     * Iterate over all value in the map which are not at {@link #initialValue()}.
     *
     * @param consumer called for each key/value pair in the map.
     */
    public void forEach(final ObjIntConsumer<K> consumer)
    {
        final int initialValue = this.initialValue;
        final K[] keys = this.keys;
        final int[] values = this.values;
        @DoNotSub final int length = values.length;
        @DoNotSub int remaining = size;

        for (@DoNotSub int i = 0; remaining > 0 && i < length; i++)
        {
            if (initialValue != values[i])
            {
                consumer.accept(keys[i], values[i]);
                --remaining;
            }
        }
    }

    /**
     * Does the map contain a value for a given key which is not {@link #initialValue()}.
     *
     * @param key the key to check.
     * @return true if the map contains the key with a value other than {@link #initialValue()}, false otherwise.
     */
    public boolean containsKey(final K key)
    {
        return initialValue != get(key);
    }

    /**
     * Iterate over the values to see if any match the provided value.
     * <p>
     * If value provided is {@link #initialValue()} then it will always return false.
     *
     * @param value the key to check.
     * @return true if the map contains value as a mapped value, false otherwise.
     */
    public boolean containsValue(final int value)
    {
        boolean found = false;
        if (initialValue != value)
        {
            for (final int v : values)
            {
                if (value == v)
                {
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    /**
     * Clear out all entries.
     */
    public void clear()
    {
        if (size > 0)
        {
            Arrays.fill(keys, null);
            Arrays.fill(values, initialValue);
            size = 0;
        }
    }

    /**
     * Compact the backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        @DoNotSub final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
        rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
    }

    /**
     * Try {@link #get(Object)} a value for a key and if not present then apply mapping function.
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns null.
     * @return the value if found otherwise the missing value.
     */
    public int computeIfAbsent(final K key, final ToIntFunction<? super K> mappingFunction)
    {
        int value = get(key);
        if (initialValue == value)
        {
            value = mappingFunction.applyAsInt(key);
            if (initialValue != value)
            {
                put(key, value);
            }
        }

        return value;
    }

    /**
     * Remove a counter value for a given key.
     *
     * @param key to be removed.
     * @return old value for key.
     */
    public int remove(final K key)
    {
        final int initialValue = this.initialValue;
        final K[] keys = this.keys;
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        int oldValue = initialValue;
        while (initialValue != values[index])
        {
            if (Objects.equals(keys[index], key))
            {
                oldValue = values[index];
                values[index] = initialValue;
                size--;

                compactChain(index);

                break;
            }

            index = ++index & mask;
        }

        return oldValue;
    }

    /**
     * Get the minimum value stored in the map. If the map is empty then it will return {@link #initialValue()}
     *
     * @return the minimum value stored in the map.
     */
    public int minValue()
    {
        final int initialValue = this.initialValue;
        int min = 0 == size ? initialValue : Integer.MAX_VALUE;

        for (final int value : values)
        {
            if (initialValue != value)
            {
                min = Math.min(min, value);
            }
        }

        return min;
    }

    /**
     * Get the maximum value stored in the map. If the map is empty then it will return {@link #initialValue()}
     *
     * @return the maximum value stored in the map.
     */
    public int maxValue()
    {
        final int initialValue = this.initialValue;
        int max = 0 == size ? initialValue : Integer.MIN_VALUE;

        for (final int value : values)
        {
            if (initialValue != value)
            {
                max = Math.max(max, value);
            }
        }

        return max;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');

        final int initialValue = this.initialValue;
        final K[] keys = this.keys;
        final int[] values = this.values;
        @DoNotSub final int length = values.length;

        for (@DoNotSub int i = 0; i < length; i++)
        {
            final int value = values[i];
            if (initialValue != value)
            {
                sb.append(keys[i]).append('=').append(value).append(", ");
            }
        }

        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 2);
        }

        sb.append('}');

        return sb.toString();
    }

    @SuppressWarnings("FinalParameters")
    private void compactChain(@DoNotSub int deleteIndex)
    {
        final int initialValue = this.initialValue;
        final K[] keys = this.keys;
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = deleteIndex;

        while (true)
        {
            index = ++index & mask;
            final int value = values[index];
            if (initialValue == value)
            {
                break;
            }

            final K key = keys[index];
            @DoNotSub final int hash = Hashing.hash(key, mask);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                keys[deleteIndex] = key;
                values[deleteIndex] = value;

                keys[index] = null;
                values[index] = initialValue;
                deleteIndex = index;
            }
        }
    }

    private void increaseCapacity()
    {
        if (size > resizeThreshold)
        {
            // entries.length = 2 * capacity
            @DoNotSub final int newCapacity = values.length * 2;
            rehash(newCapacity);
        }
    }

    private void rehash(@DoNotSub final int newCapacity)
    {
        @DoNotSub final int mask = newCapacity - 1;
        /* @DoNotSub */ resizeThreshold = (int)(newCapacity * loadFactor);

        @SuppressWarnings("unchecked")
        final K[] tempKeys = (K[])new Object[newCapacity];
        final int[] tempValues = new int[newCapacity];
        final int initialValue = this.initialValue;
        Arrays.fill(tempValues, initialValue);

        final K[] keys = this.keys;
        final int[] values = this.values;
        for (@DoNotSub int i = 0, size = values.length; i < size; i++)
        {
            final int value = values[i];
            if (initialValue != value)
            {
                final K key = keys[i];
                @DoNotSub int index = Hashing.hash(key, mask);
                while (initialValue != tempValues[index])
                {
                    index = ++index & mask;
                }

                tempKeys[index] = key;
                tempValues[index] = value;
            }
        }

        this.keys = tempKeys;
        this.values = tempValues;
    }
}
