/*
 * Copyright 2014-2017 Real Logic Ltd.
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
package org.agrona.collections;

import org.agrona.BitUtil;
import org.agrona.generation.DoNotSub;

import java.util.*;
import java.util.function.IntUnaryOperator;

import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * A open addressing with linear probing hash map specialised for primitive key and counter pairs. A counter map views
 * counters which hit {@link #initialValue} as deleted. This means that changing a counter may impact {@link #size()}.
 */
public class Int2IntCounterMap
{
    @DoNotSub private final float loadFactor;
    private final int initialValue;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size = 0;

    private int[] entries;

    /**
     * Construct a new counter map with the initial value for the counter provided.
     *
     * @param initialValue to be used for each counter.
     */
    public Int2IntCounterMap(final int initialValue)
    {
        this(8, 0.67f, initialValue);
    }

    @SuppressWarnings("unchecked")
    public Int2IntCounterMap(
        @DoNotSub final int initialCapacity,
        @DoNotSub final float loadFactor,
        final int initialValue)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        this.initialValue = initialValue;

        capacity(BitUtil.findNextPositivePowerOfTwo(initialCapacity));
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
     * @return map size, counters at {@link #initialValue()} are not counted
     */
    @DoNotSub public int size()
    {
        return size;
    }

    /**
     * @return size == 0
     */
    public boolean isEmpty()
    {
        return size == 0;
    }

    /**
     * @param key lookup key
     * @return counter value associated with key, or initialValue if none found
     */
    public int get(final int key)
    {
        final int[] entries = this.entries;
        final int initialValue = this.initialValue;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int value = initialValue;
        while (entries[index + 1] != initialValue)
        {
            if (entries[index] == key)
            {
                value = entries[index + 1];
                break;
            }

            index = next(index, mask);
        }

        return value;
    }

    /**
     * @param key   lookup key
     * @param value new value, must not be initialValue
     * @return current counter value associated with key, or initialValue if none found
     * @throws IllegalArgumentException if value is initialValue
     */
    public int put(final int key, final int value)
    {
        if (value == initialValue)
        {
            throw new IllegalArgumentException("Cannot accept initialValue");
        }

        final int[] entries = this.entries;
        final int initialValue = this.initialValue;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);
        int oldValue = initialValue;

        while (entries[index + 1] != initialValue)
        {
            if (entries[index] == key)
            {
                oldValue = entries[index + 1];
                break;
            }

            index = next(index, mask);
        }

        if (oldValue == initialValue)
        {
            ++size;
            entries[index] = key;
        }

        entries[index + 1] = value;

        increaseCapacity();

        return oldValue;
    }

    /**
     * Convenience version of {@link #addAndGet(int, int)} (key, 1).
     *
     * @param key for the counter.
     * @return the new value.
     */
    public int incrementAndGet(final int key)
    {
        return addAndGet(key, 1);
    }

    /**
     * Convenience version of {@link #addAndGet(int, int)} (key, -1).
     *
     * @param key for the counter.
     * @return the new value.
     */
    public int decrementAndGet(final int key)
    {
        return addAndGet(key, -1);
    }

    /**
     * Add amount to the current value associated with this key. If no such value exists use {@link #initialValue()} as
     * current value and associate key with {@link #initialValue()} + amount unless amount is 0, in which case map
     * remains unchanged.
     *
     * @param key    new or existing
     * @param amount to be added
     * @return the new value associated with the specified key, or
     *         {@link #initialValue()} + amount if there was no mapping for the key.
     */
    public int addAndGet(final int key, final int amount)
    {
        return getAndAdd(key, amount) + amount;
    }

    /**
     * Convenience version of {@link #getAndAdd(int, int)} (key, 1).
     *
     * @param key for the counter.
     * @return the old value.
     */
    public int getAndIncrement(final int key)
    {
        return getAndAdd(key, 1);
    }

    /**
     * Convenience version of {@link #getAndAdd(int, int)} (key, -1).
     *
     * @param key for the counter.
     * @return the old value.
     */
    public int getAndDecrement(final int key)
    {
        return getAndAdd(key, -1);
    }
    /**
     * Add amount to the current value associated with this key. If no such value exists use {@link #initialValue()} as
     * current value and associate key with {@link #initialValue()} + amount unless amount is 0, in which case map
     * remains unchanged.
     *
     * @param key    new or existing
     * @param amount to be added
     * @return the previous value associated with the specified key, or
     *         {@link #initialValue()} if there was no mapping for the key.
     */
    public int getAndAdd(final int key, final int amount)
    {
        final int[] entries = this.entries;
        final int initialValue = this.initialValue;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);
        int oldValue = initialValue;

        while (entries[index + 1] != initialValue)
        {
            if (entries[index] == key)
            {
                oldValue = entries[index + 1];
                break;
            }

            index = next(index, mask);
        }

        if (amount != 0)
        {
            final int newValue = oldValue + amount;
            entries[index + 1] = newValue;

            if (oldValue == initialValue)
            {
                ++size;
                entries[index] = key;
                increaseCapacity();
            }
            else if (newValue == initialValue)
            {
                size--;
                compactChain(index);
            }
        }

        return oldValue;
    }

    private void increaseCapacity()
    {
        if (size > resizeThreshold)
        {
            // entries.length = 2 * capacity
            @DoNotSub final int newCapacity = entries.length;
            rehash(newCapacity);
        }
    }

    private void rehash(@DoNotSub final int newCapacity)
    {
        final int[] oldEntries = entries;
        final int initialValue = this.initialValue;
        @DoNotSub final int length = entries.length;

        capacity(newCapacity);

        for (@DoNotSub int i = 0; i < length; i += 2)
        {
            final int key = oldEntries[i];
            if (oldEntries[i + 1] != initialValue)
            {
                put(key, oldEntries[i + 1]);
            }
        }
    }

    /**
     * @param consumer a callback called for each key/value pair in the map.
     */
    public void forEach(final IntIntConsumer consumer)
    {
        final int[] entries = this.entries;
        final int initialValue = this.initialValue;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int i = 0; i < length; i += 2)
        {
            if (entries[i + 1] != initialValue)
            {
                consumer.accept(entries[i], entries[i + 1]);
            }
        }
    }

    /**
     * @param key the key to check.
     * @return true if the map contains key as a mapped key, false otherwise.
     */
    public boolean containsKey(final int key)
    {
        return get(key) != initialValue;
    }

    /**
     * @param value the key to check.
     * @return true if the map contains value as a mapped value, false otherwise.
     */
    public boolean containsValue(final int value)
    {
        boolean found = false;
        if (value != initialValue)
        {
            final int[] entries = this.entries;
            @DoNotSub final int length = entries.length;

            for (@DoNotSub int i = 1; i < length; i += 2)
            {
                if (value == entries[i])
                {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        Arrays.fill(entries, initialValue);
        size = 0;
    }

    /**
     * Compact the backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        @DoNotSub final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
        rehash(BitUtil.findNextPositivePowerOfTwo(idealCapacity));
    }

    /**
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns null.
     * @return the value if found otherwise the missing value.
     */
    public int computeIfAbsent(final int key, final IntUnaryOperator mappingFunction)
    {
        int value = get(key);
        if (value == initialValue)
        {
            value = mappingFunction.applyAsInt(key);
            if (value != initialValue)
            {
                put(key, value);
            }
        }

        return value;
    }

    /**
     * @param key to be removed
     * @return old value for key
     */
    public int remove(final int key)
    {
        final int[] entries = this.entries;
        final int initialValue = this.initialValue;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int keyIndex = Hashing.evenHash(key, mask);

        int oldValue = initialValue;
        while (entries[keyIndex + 1] != initialValue)
        {
            if (entries[keyIndex] == key)
            {
                @DoNotSub final int valueIndex = keyIndex + 1;
                oldValue = entries[keyIndex + 1];
                entries[keyIndex + 1] = initialValue;
                size--;

                compactChain(keyIndex);

                break;
            }

            keyIndex = next(keyIndex, mask);
        }

        return oldValue;
    }

    @SuppressWarnings("FinalParameters")
    private void compactChain(@DoNotSub int deleteKeyIndex)
    {
        final int[] entries = this.entries;
        final int initialValue = this.initialValue;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = deleteKeyIndex;

        while (true)
        {
            index = next(index, mask);
            if (entries[index + 1] == initialValue)
            {
                break;
            }

            @DoNotSub final int hash = Hashing.evenHash(entries[index], mask);

            if ((index < hash && (hash <= deleteKeyIndex || deleteKeyIndex <= index)) ||
                (hash <= deleteKeyIndex && deleteKeyIndex <= index))
            {
                entries[deleteKeyIndex] = entries[index];
                entries[deleteKeyIndex + 1] = entries[index + 1];
                entries[index + 1] = initialValue;
                deleteKeyIndex = index;
            }
        }
    }

    /**
     * Get the minimum value stored in the map. If the map is empty then it will return {@link #initialValue()}
     *
     * @return the minimum value stored in the map.
     */
    public int minValue()
    {
        final int initialValue = this.initialValue;
        int min = size == 0 ? initialValue : Integer.MAX_VALUE;

        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int i = 1; i < length; i += 2)
        {
            final int value = entries[i];
            if (value != initialValue)
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
        int max = size == 0 ? initialValue : Integer.MIN_VALUE;

        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int i = 1; i < length; i += 2)
        {
            final int value = entries[i];
            if (value != initialValue)
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
        forEach(
            (k, v) ->
            {
                sb.append(k);
                sb.append('=');
                sb.append(v);
                sb.append(", ");
            }
        );

        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 2);
        }

        sb.append('}');

        return sb.toString();
    }

    @DoNotSub private static int next(final int index, final int mask)
    {
        return (index + 2) & mask;
    }

    private void capacity(@DoNotSub final int newCapacity)
    {
        @DoNotSub final int entriesLength = newCapacity * 2;
        if (entriesLength < 0)
        {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        /*@DoNotSub*/ resizeThreshold = (int)(newCapacity * loadFactor);
        entries = new int[entriesLength];
        size = 0;
        Arrays.fill(entries, initialValue);
    }
}
