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

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * An open-addressing with linear probing hash map specialised for primitive key and value pairs.
 */
public class Int2IntHashMap implements Map<Integer, Integer>
{
    @DoNotSub static final int MIN_CAPACITY = 8;

    private final float loadFactor;
    private final int missingValue;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size = 0;
    private final boolean shouldAvoidAllocation;

    private int[] entries;
    private KeySet keySet;
    private ValueCollection values;
    private EntrySet entrySet;

    /**
     * @param missingValue for the map that represents null.
     */
    public Int2IntHashMap(final int missingValue)
    {
        this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, missingValue);
    }

    /**
     * @param initialCapacity for the map to override {@link #MIN_CAPACITY}
     * @param loadFactor      for the map to override {@link Hashing#DEFAULT_LOAD_FACTOR}.
     * @param missingValue    for the map that represents null.
     */
    public Int2IntHashMap(
        @DoNotSub final int initialCapacity,
        @DoNotSub final float loadFactor,
        final int missingValue)
    {
        this(initialCapacity, loadFactor, missingValue, true);
    }

    /**
     * @param initialCapacity       for the map to override {@link #MIN_CAPACITY}
     * @param loadFactor            for the map to override {@link Hashing#DEFAULT_LOAD_FACTOR}.
     * @param missingValue          for the map that represents null.
     * @param shouldAvoidAllocation should allocation be avoided by caching iterators and map entries.
     */
    public Int2IntHashMap(
        @DoNotSub final int initialCapacity,
        @DoNotSub final float loadFactor,
        final int missingValue,
        final boolean shouldAvoidAllocation)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        this.missingValue = missingValue;
        this.shouldAvoidAllocation = shouldAvoidAllocation;

        capacity(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity)));
    }

    /**
     * The value to be used as a null marker in the map.
     *
     * @return value to be used as a null marker in the map.
     */
    public int missingValue()
    {
        return missingValue;
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
     * Get the total capacity for the map to which the load factor will be a fraction of.
     *
     * @return the total capacity for the map.
     */
    @DoNotSub public int capacity()
    {
        return entries.length >> 1;
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
     * {@inheritDoc}
     */
    @DoNotSub public int size()
    {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return 0 == size;
    }

    /**
     * Returns the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key.
     *
     * @param key          whose associated value is to be returned.
     * @param defaultValue to be returned if there is no value in the map for a given {@code key}.
     * @return the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key.
     */
    public int getOrDefault(final int key, final int defaultValue)
    {
        final int value = get(key);
        return missingValue != value ? value : defaultValue;
    }

    /**
     * Get a value using provided key avoiding boxing.
     *
     * @param key lookup key.
     * @return value associated with the key or {@link #missingValue()} if key is not found in the map.
     */
    public int get(final int key)
    {
        final int missingValue = this.missingValue;
        final int[] entries = this.entries;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int value;
        while (missingValue != (value = entries[index + 1]))
        {
            if (key == entries[index])
            {
                break;
            }

            index = next(index, mask);
        }

        return value;
    }

    /**
     * Put a key value pair in the map.
     *
     * @param key   lookup key
     * @param value new value, must not be {@link #missingValue()}
     * @return previous value associated with the key, or {@link #missingValue()} if none found
     * @throws IllegalArgumentException if value is {@link #missingValue()}
     */
    public int put(final int key, final int value)
    {
        final int missingValue = this.missingValue;
        if (missingValue == value)
        {
            throw new IllegalArgumentException("cannot accept missingValue");
        }

        final int[] entries = this.entries;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int oldValue;
        while (missingValue != (oldValue = entries[index + 1]))
        {
            if (key == entries[index])
            {
                break;
            }

            index = next(index, mask);
        }

        if (missingValue == oldValue)
        {
            ++size;
            entries[index] = key;
        }

        entries[index + 1] = value;

        increaseCapacity();

        return oldValue;
    }

    /**
     * Primitive specialised version of {@link #putIfAbsent(Integer, Integer)} method.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     * {@link #missingValue()} if there was no mapping for the key.
     * @throws IllegalArgumentException if value is {@link #missingValue()}
     */
    public int putIfAbsent(final int key, final int value)
    {
        final int existingValue = get(key);
        if (missingValue != existingValue)
        {
            return existingValue;
        }
        return put(key, value);
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
        final int missingValue = this.missingValue;
        final int[] oldEntries = entries;
        @DoNotSub final int length = oldEntries.length;

        capacity(newCapacity);

        final int[] newEntries = entries;
        @DoNotSub final int mask = newEntries.length - 1;

        for (@DoNotSub int keyIndex = 0; keyIndex < length; keyIndex += 2)
        {
            final int value = oldEntries[keyIndex + 1]; // lgtm[java/index-out-of-bounds]
            if (missingValue != value)
            {
                final int key = oldEntries[keyIndex];
                @DoNotSub int index = Hashing.evenHash(key, mask);

                while (missingValue != newEntries[index + 1])
                {
                    index = next(index, mask);
                }

                newEntries[index] = key;
                newEntries[index + 1] = value;
            }
        }
    }

    /**
     * Use {@link #forEachInt(IntIntConsumer)} instead.
     *
     * @param consumer a callback called for each key/value pair in the map.
     * @deprecated Use {@link #forEachInt(IntIntConsumer)} instead.
     */
    @Deprecated
    public void intForEach(final IntIntConsumer consumer)
    {
        forEachInt(consumer);
    }

    /**
     * Primitive specialised forEach implementation.
     * <p>
     * NB: Renamed from forEach to avoid overloading on parameter types of lambda
     * expression, which doesn't play well with type inference in lambda expressions.
     *
     * @param consumer a callback called for each key/value pair in the map.
     */
    public void forEachInt(final IntIntConsumer consumer)
    {
        final int missingValue = this.missingValue;
        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;
        @DoNotSub int remaining = size;

        for (@DoNotSub int valueIndex = 1; remaining > 0 && valueIndex < length; valueIndex += 2)
        {
            if (missingValue != entries[valueIndex])
            {
                consumer.accept(entries[valueIndex - 1], entries[valueIndex]);
                --remaining;
            }
        }
    }

    /**
     * Int primitive specialised containsKey.
     *
     * @param key the key to check.
     * @return true if the map contains key as a key, false otherwise.
     */
    public boolean containsKey(final int key)
    {
        return missingValue != get(key);
    }

    /**
     * Does the map contain the value.
     *
     * @param value to be tested against contained values.
     * @return true if contained otherwise value.
     */
    public boolean containsValue(final int value)
    {
        boolean found = false;
        final int missingValue = this.missingValue;
        if (missingValue != value)
        {
            final int[] entries = this.entries;
            @DoNotSub final int length = entries.length;
            @DoNotSub int remaining = size;

            for (@DoNotSub int valueIndex = 1; remaining > 0 && valueIndex < length; valueIndex += 2)
            {
                final int existingValue = entries[valueIndex];
                if (missingValue != existingValue)
                {
                    if (existingValue == value)
                    {
                        found = true;
                        break;
                    }
                    --remaining;
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
        if (size > 0)
        {
            Arrays.fill(entries, missingValue);
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
     * Primitive specialised version of {@link #computeIfAbsent(Object, Function)}.
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns null.
     * @return the value if found otherwise the missing value.
     */
    public int computeIfAbsent(final int key, final IntUnaryOperator mappingFunction)
    {
        final int missingValue = this.missingValue;
        final int[] entries = this.entries;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int value;
        while (missingValue != (value = entries[index + 1]))
        {
            if (key == entries[index])
            {
                break;
            }

            index = next(index, mask);
        }

        if (value == missingValue && (value = mappingFunction.applyAsInt(key)) != missingValue)
        {
            entries[index] = key;
            entries[index + 1] = value;
            size++;
            increaseCapacity();
        }

        return value;
    }

    /**
     * Primitive specialised version of {@link java.util.Map#computeIfPresent}.
     *
     * @param key               to search on.
     * @param remappingFunction to compute a value if a mapping is found.
     * @return the updated value if a mapping was found, otherwise the missing value.
     */
    public int computeIfPresent(final int key, final IntBinaryOperator remappingFunction)
    {
        final int missingValue = this.missingValue;
        final int[] entries = this.entries;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int value;
        while (missingValue != (value = entries[index + 1]))
        {
            if (key == entries[index])
            {
                break;
            }

            index = next(index, mask);
        }

        if (value != missingValue)
        {
            value = remappingFunction.applyAsInt(key, value);
            entries[index + 1] = value;
            if (value == missingValue)
            {
                size--;
                compactChain(index);
            }
        }

        return value;
    }

    /**
     * Primitive specialised version of {@link java.util.Map#compute}.
     *
     * @param key               to search on.
     * @param remappingFunction to compute a value.
     * @return the updated value.
     */
    public int compute(final int key, final IntBinaryOperator remappingFunction)
    {
        final int missingValue = this.missingValue;
        final int[] entries = this.entries;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int oldValue;
        while (missingValue != (oldValue = entries[index + 1]))
        {
            if (key == entries[index])
            {
                break;
            }

            index = next(index, mask);
        }

        final int newValue = remappingFunction.applyAsInt(key, oldValue);
        if (newValue != missingValue)
        {
            entries[index + 1] = newValue;
            if (oldValue == missingValue)
            {
                entries[index] = key;
                size++;
                increaseCapacity();
            }
        }
        else if (oldValue != missingValue)
        {
            entries[index + 1] = missingValue;
            size--;
            compactChain(index);
        }

        return newValue;
    }

    // ---------------- Boxed Versions Below ----------------

    /**
     * {@inheritDoc}
     */
    public Integer get(final Object key)
    {
        return valOrNull(get((int)key));
    }

    /**
     * {@inheritDoc}
     */
    public Integer put(final Integer key, final Integer value)
    {
        return valOrNull(put((int)key, (int)value));
    }

    /**
     * {@inheritDoc}
     */
    public void forEach(final BiConsumer<? super Integer, ? super Integer> action)
    {
        forEachInt(action::accept);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(final Object key)
    {
        return containsKey((int)key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(final Object value)
    {
        return containsValue((int)value);
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(final Map<? extends Integer, ? extends Integer> map)
    {
        for (final Map.Entry<? extends Integer, ? extends Integer> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Integer putIfAbsent(final Integer key, final Integer value)
    {
        return valOrNull(putIfAbsent((int)key, (int)value));
    }

    /**
     * {@inheritDoc}
     */
    public Integer replace(final Integer key, final Integer value)
    {
        return valOrNull(replace((int)key, (int)value));
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(final Integer key, final Integer oldValue, final Integer newValue)
    {
        return replace((int)key, (int)oldValue, (int)newValue);
    }

    /**
     * {@inheritDoc}
     */
    public void replaceAll(final BiFunction<? super Integer, ? super Integer, ? extends Integer> function)
    {
        replaceAllInt(function::apply);
    }

    /**
     * {@inheritDoc}
     */
    public KeySet keySet()
    {
        if (null == keySet)
        {
            keySet = new KeySet();
        }

        return keySet;
    }

    /**
     * {@inheritDoc}
     */
    public ValueCollection values()
    {
        if (null == values)
        {
            values = new ValueCollection();
        }

        return values;
    }

    /**
     * {@inheritDoc}
     */
    public EntrySet entrySet()
    {
        if (null == entrySet)
        {
            entrySet = new EntrySet();
        }

        return entrySet;
    }

    /**
     * {@inheritDoc}
     */
    public Integer remove(final Object key)
    {
        return valOrNull(remove((int)key));
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Object key, final Object value)
    {
        return remove((int)key, (int)value);
    }

    /**
     * Remove value from the map using given key avoiding boxing.
     *
     * @param key whose mapping is to be removed from the map.
     * @return removed value or {@link #missingValue()} if key was not found in the map.
     */
    public int remove(final int key)
    {
        final int missingValue = this.missingValue;
        final int[] entries = this.entries;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int keyIndex = Hashing.evenHash(key, mask);

        int oldValue;
        while (missingValue != (oldValue = entries[keyIndex + 1]))
        {
            if (key == entries[keyIndex])
            {
                entries[keyIndex + 1] = missingValue;
                size--;

                compactChain(keyIndex);

                break;
            }

            keyIndex = next(keyIndex, mask);
        }

        return oldValue;
    }

    /**
     * Primitive specialised version of {@link #remove(Object, Object)}.
     *
     * @param key   with which the specified value is associated.
     * @param value expected to be associated with the specified key.
     * @return {@code true} if the value was removed.
     */
    public boolean remove(final int key, final int value)
    {
        if (missingValue != value && value == get(key))
        {
            remove(key);
            return true;
        }
        return false;
    }

    /**
     * Primitive specialised version of {@link #merge(Object, Object, BiFunction)}.
     *
     * @param key               with which the resulting value is to be associated.
     * @param value             to be merged with the existing value associated with the key or, if no existing value or a null
     *                          value is associated with the key, to be associated with the key.
     * @param remappingFunction the function to recompute a value if present.
     * @return the new value associated with the specified key, or {@link #missingValue()} if no value is associated
     * with the key as the result of this operation.
     */
    public int merge(final int key, final int value, final IntIntFunction remappingFunction)
    {
        final int oldValue = get(key);
        final int newValue = missingValue == oldValue ? value : remappingFunction.apply(oldValue, value);
        if (missingValue == newValue)
        {
            remove(key);
        }
        else
        {
            put(key, newValue);
        }
        return newValue;
    }

    @SuppressWarnings("FinalParameters")
    private void compactChain(@DoNotSub int deleteKeyIndex)
    {
        final int missingValue = this.missingValue;
        final int[] entries = this.entries;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int keyIndex = deleteKeyIndex;

        while (true)
        {
            keyIndex = next(keyIndex, mask);
            final int value = entries[keyIndex + 1];
            if (missingValue == value)
            {
                break;
            }

            final int key = entries[keyIndex];
            @DoNotSub final int hash = Hashing.evenHash(key, mask);

            if ((keyIndex < hash && (hash <= deleteKeyIndex || deleteKeyIndex <= keyIndex)) ||
                (hash <= deleteKeyIndex && deleteKeyIndex <= keyIndex))
            {
                entries[deleteKeyIndex] = key;
                entries[deleteKeyIndex + 1] = value;

                entries[keyIndex + 1] = missingValue;
                deleteKeyIndex = keyIndex;
            }
        }
    }

    /**
     * Get the minimum value stored in the map. If the map is empty then it will return {@link #missingValue()}.
     *
     * @return the minimum value stored in the map.
     */
    public int minValue()
    {
        final int missingValue = this.missingValue;
        int min = 0 == size ? missingValue : Integer.MAX_VALUE;
        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int valueIndex = 1; valueIndex < length; valueIndex += 2)
        {
            final int value = entries[valueIndex];
            if (missingValue != value)
            {
                min = Math.min(min, value);
            }
        }

        return min;
    }

    /**
     * Get the maximum value stored in the map. If the map is empty then it will return {@link #missingValue()}.
     *
     * @return the maximum value stored in the map.
     */
    public int maxValue()
    {
        final int missingValue = this.missingValue;
        int max = 0 == size ? missingValue : Integer.MIN_VALUE;
        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int valueIndex = 1; valueIndex < length; valueIndex += 2)
        {
            final int value = entries[valueIndex];
            if (missingValue != value)
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
        if (isEmpty())
        {
            return "{}";
        }

        final EntryIterator entryIterator = new EntryIterator();
        entryIterator.reset();

        final StringBuilder sb = new StringBuilder().append('{');
        while (true)
        {
            entryIterator.next();
            sb.append(entryIterator.getIntKey()).append('=').append(entryIterator.getIntValue());
            if (!entryIterator.hasNext())
            {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }

    /**
     * Primitive specialised version of {@link #replace(Integer, Integer)}.
     *
     * @param key   key with which the specified value is associated.
     * @param value value to be associated with the specified key.
     * @return the previous value associated with the specified key, or
     * {@link #missingValue()} if there was no mapping for the key.
     */
    public int replace(final int key, final int value)
    {
        int currentValue = get(key);
        if (missingValue != currentValue)
        {
            currentValue = put(key, value);
        }

        return currentValue;
    }

    /**
     * Primitive specialised version of {@link #replace(Integer, Integer, Integer)}.
     *
     * @param key      key with which the specified value is associated.
     * @param oldValue value expected to be associated with the specified key.
     * @param newValue value to be associated with the specified key.
     * @return {@code true} if the value was replaced.
     */
    public boolean replace(final int key, final int oldValue, final int newValue)
    {
        final int curValue = get(key);
        if (curValue != oldValue || missingValue == curValue)
        {
            return false;
        }

        put(key, newValue);

        return true;
    }

    /**
     * Primitive specialised version of {@link #replaceAll(BiFunction)}.
     * <p>
     * NB: Renamed from replaceAll to avoid overloading on parameter types of lambda
     * expression, which doesn't play well with type inference in lambda expressions.
     *
     * @param function to apply to each entry.
     */
    public void replaceAllInt(final IntIntFunction function)
    {
        final int missingValue = this.missingValue;
        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;
        @DoNotSub int remaining = size;

        for (@DoNotSub int valueIndex = 1; remaining > 0 && valueIndex < length; valueIndex += 2)
        {
            final int existingValue = entries[valueIndex];
            if (missingValue != existingValue)
            {
                final int newValue = function.apply(entries[valueIndex - 1], existingValue);
                if (missingValue == newValue)
                {
                    throw new IllegalArgumentException("cannot replace with a missingValue");
                }
                entries[valueIndex] = newValue;
                --remaining;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof Map))
        {
            return false;
        }

        final Map<?, ?> that = (Map<?, ?>)o;

        return size == that.size() && entrySet().equals(that.entrySet());
    }

    /**
     * {@inheritDoc}
     */
    @DoNotSub public int hashCode()
    {
        return entrySet().hashCode();
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
            throw new IllegalStateException("max capacity reached at size=" + size);
        }

        /*@DoNotSub*/ resizeThreshold = (int)(newCapacity * loadFactor);
        entries = new int[entriesLength];
        Arrays.fill(entries, missingValue);
    }

    private Integer valOrNull(final int value)
    {
        return value == missingValue ? null : value;
    }

    // ---------------- Utility Classes ----------------

    /**
     * Base iterator implementation.
     */
    abstract class AbstractIterator
    {
        /**
         * Is current position valid.
         */
        protected boolean isPositionValid = false;
        @DoNotSub private int remaining;
        @DoNotSub private int positionCounter;
        @DoNotSub private int stopCounter;

        final void reset()
        {
            isPositionValid = false;
            remaining = Int2IntHashMap.this.size;
            final int missingValue = Int2IntHashMap.this.missingValue;
            final int[] entries = Int2IntHashMap.this.entries;
            @DoNotSub final int capacity = entries.length;

            @DoNotSub int keyIndex = capacity;
            if (missingValue != entries[capacity - 1])
            {
                for (@DoNotSub int i = 1; i < capacity; i += 2)
                {
                    if (missingValue == entries[i])
                    {
                        keyIndex = i - 1;
                        break;
                    }
                }
            }

            stopCounter = keyIndex;
            positionCounter = keyIndex + capacity;
        }

        /**
         * Returns position of the key of the current entry.
         *
         * @return key position.
         */
        @DoNotSub protected final int keyPosition()
        {
            return positionCounter & entries.length - 1;
        }

        /**
         * Number of remaining elements.
         *
         * @return number of remaining elements.
         */
        @DoNotSub public int remaining()
        {
            return remaining;
        }

        /**
         * Check if there are more elements remaining.
         *
         * @return {@code true} if {@code remaining > 0}.
         */
        public boolean hasNext()
        {
            return remaining > 0;
        }

        /**
         * Advance to the next entry.
         *
         * @throws NoSuchElementException if no more entries available.
         */
        protected final void findNext()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }

            final int[] entries = Int2IntHashMap.this.entries;
            final int missingValue = Int2IntHashMap.this.missingValue;
            @DoNotSub final int mask = entries.length - 1;

            for (@DoNotSub int keyIndex = positionCounter - 2, stop = stopCounter; keyIndex >= stop; keyIndex -= 2)
            {
                @DoNotSub final int index = keyIndex & mask;
                if (missingValue != entries[index + 1])
                {
                    isPositionValid = true;
                    positionCounter = keyIndex;
                    --remaining;
                    return;
                }
            }

            isPositionValid = false;
            throw new IllegalStateException();
        }

        /**
         * {@inheritDoc}
         */
        public void remove()
        {
            if (isPositionValid)
            {
                @DoNotSub final int position = keyPosition();
                entries[position + 1] = missingValue;
                --size;

                compactChain(position);

                isPositionValid = false;
            }
            else
            {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Iterator over keys which supports access to unboxed keys via {@link #nextValue()}.
     */
    public final class KeyIterator extends AbstractIterator implements Iterator<Integer>
    {
        /**
         * {@inheritDoc}
         */
        public Integer next()
        {
            return nextValue();
        }

        /**
         * Return next key.
         *
         * @return next key.
         */
        public int nextValue()
        {
            findNext();
            return entries[keyPosition()];
        }
    }

    /**
     * Iterator over values which supports access to unboxed values.
     */
    public final class ValueIterator extends AbstractIterator implements Iterator<Integer>
    {
        /**
         * {@inheritDoc}
         */
        public Integer next()
        {
            return nextValue();
        }

        /**
         * Return next value.
         *
         * @return next value.
         */
        public int nextValue()
        {
            findNext();
            return entries[keyPosition() + 1];
        }
    }

    /**
     * Iterator over entries which supports access to unboxed keys and values.
     */
    public final class EntryIterator
        extends AbstractIterator
        implements Iterator<Entry<Integer, Integer>>, Entry<Integer, Integer>
    {
        /**
         * {@inheritDoc}
         */
        public Integer getKey()
        {
            return getIntKey();
        }

        /**
         * Returns the key of the current entry.
         *
         * @return the key.
         */
        public int getIntKey()
        {
            return entries[keyPosition()];
        }

        /**
         * {@inheritDoc}
         */
        public Integer getValue()
        {
            return getIntValue();
        }

        /**
         * Returns the value of the current entry.
         *
         * @return the value.
         */
        public int getIntValue()
        {
            return entries[keyPosition() + 1];
        }

        /**
         * {@inheritDoc}
         */
        public Integer setValue(final Integer value)
        {
            return setValue(value.intValue());
        }

        /**
         * Sets the value of the current entry.
         *
         * @param value to be set.
         * @return previous value of the entry.
         */
        public int setValue(final int value)
        {
            if (!isPositionValid)
            {
                throw new IllegalStateException();
            }

            if (missingValue == value)
            {
                throw new IllegalArgumentException("cannot accept missingValue");
            }

            @DoNotSub final int keyPosition = keyPosition();
            final int[] entries = Int2IntHashMap.this.entries;
            final int prevValue = entries[keyPosition + 1];
            entries[keyPosition + 1] = value;
            return prevValue;
        }

        /**
         * {@inheritDoc}
         */
        public Entry<Integer, Integer> next()
        {
            findNext();

            if (shouldAvoidAllocation)
            {
                return this;
            }

            return allocateDuplicateEntry();
        }

        private Entry<Integer, Integer> allocateDuplicateEntry()
        {
            return new MapEntry(getIntKey(), getIntValue());
        }

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int hashCode()
        {
            return Integer.hashCode(getIntKey()) ^ Integer.hashCode(getIntValue());
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }

            if (!(o instanceof Entry))
            {
                return false;
            }

            final Entry<?, ?> that = (Entry<?, ?>)o;

            return Objects.equals(getKey(), that.getKey()) && Objects.equals(getValue(), that.getValue());
        }

        /**
         * An {@link java.util.Map.Entry} implementation.
         */
        public final class MapEntry implements Entry<Integer, Integer>
        {
            private final int k;
            private final int v;

            /**
             * Constructs entry with given key and value.
             *
             * @param k key.
             * @param v value.
             */
            public MapEntry(final int k, final int v)
            {
                this.k = k;
                this.v = v;
            }

            /**
             * {@inheritDoc}
             */
            public Integer getKey()
            {
                return k;
            }

            /**
             * {@inheritDoc}
             */
            public Integer getValue()
            {
                return v;
            }

            /**
             * {@inheritDoc}
             */
            public Integer setValue(final Integer value)
            {
                return Int2IntHashMap.this.put(k, value.intValue());
            }

            /**
             * {@inheritDoc}
             */
            @DoNotSub public int hashCode()
            {
                return Integer.hashCode(getIntKey()) ^ Integer.hashCode(getIntValue());
            }

            /**
             * {@inheritDoc}
             */
            @DoNotSub public boolean equals(final Object o)
            {
                if (!(o instanceof Map.Entry))
                {
                    return false;
                }

                final Entry<?, ?> e = (Entry<?, ?>)o;

                return (e.getKey() != null && e.getValue() != null) && (e.getKey().equals(k) && e.getValue().equals(v));
            }

            /**
             * {@inheritDoc}
             */
            public String toString()
            {
                return k + "=" + v;
            }
        }
    }

    /**
     * Set of keys which supports optional cached iterators to avoid allocation.
     */
    public final class KeySet extends AbstractSet<Integer>
    {
        private final KeyIterator keyIterator = shouldAvoidAllocation ? new KeyIterator() : null;

        /**
         * {@inheritDoc}
         */
        public KeyIterator iterator()
        {
            KeyIterator keyIterator = this.keyIterator;
            if (null == keyIterator)
            {
                keyIterator = new KeyIterator();
            }

            keyIterator.reset();

            return keyIterator;
        }

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int size()
        {
            return Int2IntHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isEmpty()
        {
            return Int2IntHashMap.this.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Int2IntHashMap.this.clear();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return contains((int)o);
        }

        /**
         * Checks if key is contained in the map without boxing.
         *
         * @param key to check.
         * @return {@code true} if key is contained in this map.
         */
        public boolean contains(final int key)
        {
            return containsKey(key);
        }
    }

    /**
     * Collection of values which supports optionally cached iterators to avoid allocation.
     */
    public final class ValueCollection extends AbstractCollection<Integer>
    {
        private final ValueIterator valueIterator = shouldAvoidAllocation ? new ValueIterator() : null;

        /**
         * {@inheritDoc}
         */
        public ValueIterator iterator()
        {
            ValueIterator valueIterator = this.valueIterator;
            if (null == valueIterator)
            {
                valueIterator = new ValueIterator();
            }

            valueIterator.reset();

            return valueIterator;
        }

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int size()
        {
            return Int2IntHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return contains((int)o);
        }

        /**
         * Checks if the value is contained in the map.
         *
         * @param value to be checked.
         * @return {@code true} if value is contained in this map.
         */
        public boolean contains(final int value)
        {
            return containsValue(value);
        }
    }

    /**
     * Set of entries which supports optionally cached iterators to avoid allocation.
     */
    public final class EntrySet extends AbstractSet<Map.Entry<Integer, Integer>>
    {
        private final EntryIterator entryIterator = shouldAvoidAllocation ? new EntryIterator() : null;

        /**
         * {@inheritDoc}
         */
        public EntryIterator iterator()
        {
            EntryIterator entryIterator = this.entryIterator;
            if (null == entryIterator)
            {
                entryIterator = new EntryIterator();
            }

            entryIterator.reset();

            return entryIterator;
        }

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int size()
        {
            return Int2IntHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isEmpty()
        {
            return Int2IntHashMap.this.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Int2IntHashMap.this.clear();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            if (!(o instanceof Entry))
            {
                return false;
            }
            final Entry<?, ?> entry = (Entry<?, ?>)o;
            final Integer value = get(entry.getKey());

            return value != null && value.equals(entry.getValue());
        }

        /**
         * {@inheritDoc}
         */
        public Object[] toArray()
        {
            return toArray(new Object[size()]);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(final T[] a)
        {
            final T[] array = a.length >= size ?
                a : (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            final EntryIterator it = iterator();

            for (@DoNotSub int i = 0; i < array.length; i++)
            {
                if (it.hasNext())
                {
                    it.next();
                    array[i] = (T)it.allocateDuplicateEntry();
                }
                else
                {
                    array[i] = null;
                    break;
                }
            }

            return array;
        }
    }
}
