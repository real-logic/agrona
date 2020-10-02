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

import org.agrona.generation.DoNotSub;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * A open addressing with linear probing hash map specialised for primitive key and value pairs.
 */
@SuppressWarnings("serial")
public class Int2IntHashMap implements Map<Integer, Integer>, Serializable
{
    @DoNotSub static final int MIN_CAPACITY = 8;
    private static final long serialVersionUID = -690554872053575793L;

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
        return size == 0;
    }

    /**
     * Get a value using provided key avoiding boxing.
     *
     * @param key lookup key.
     * @return value associated with the key or {@link #missingValue()} if key is not found in the map.
     */
    public int get(final int key)
    {
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int value = missingValue;
        while (entries[index + 1] != missingValue)
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
     * Put a key value pair in the map.
     *
     * @param key   lookup key
     * @param value new value, must not be initialValue
     * @return current counter value associated with key, or initialValue if none found
     * @throws IllegalArgumentException if value is missingValue
     */
    public int put(final int key, final int value)
    {
        if (value == missingValue)
        {
            throw new IllegalArgumentException("cannot accept missingValue");
        }

        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);
        int oldValue = missingValue;

        while (entries[index + 1] != missingValue)
        {
            if (entries[index] == key)
            {
                oldValue = entries[index + 1];
                break;
            }

            index = next(index, mask);
        }

        if (oldValue == missingValue)
        {
            ++size;
            entries[index] = key;
        }

        entries[index + 1] = value;

        increaseCapacity();

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
        @DoNotSub final int length = entries.length;

        capacity(newCapacity);

        final int[] newEntries = entries;
        @DoNotSub final int mask = entries.length - 1;

        for (@DoNotSub int keyIndex = 0; keyIndex < length; keyIndex += 2)
        {
            final int value = oldEntries[keyIndex + 1];
            if (value != missingValue)
            {
                final int key = oldEntries[keyIndex];
                @DoNotSub int index = Hashing.evenHash(key, mask);

                while (newEntries[index + 1] != missingValue)
                {
                    index = next(index, mask);
                }

                newEntries[index] = key;
                newEntries[index + 1] = value;
            }
        }
    }

    /**
     * Primitive specialised forEach implementation.
     * <p>
     * NB: Renamed from forEach to avoid overloading on parameter types of lambda
     * expression, which doesn't play well with type inference in lambda expressions.
     *
     * @param consumer a callback called for each key/value pair in the map.
     */
    public void intForEach(final IntIntConsumer consumer)
    {
        @DoNotSub final int length = entries.length;
        @DoNotSub int remaining = size;

        for (@DoNotSub int valueIndex = 1; remaining > 0 && valueIndex < length; valueIndex += 2)
        {
            if (entries[valueIndex] != missingValue)
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
        return get(key) != missingValue;
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
        if (value != missingValue)
        {
            @DoNotSub final int length = entries.length;
            @DoNotSub int remaining = size;

            for (@DoNotSub int valueIndex = 1; remaining > 0 && valueIndex < length; valueIndex += 2)
            {
                if (missingValue != entries[valueIndex])
                {
                    if (value == entries[valueIndex])
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
     * Primitive specialised version of {@link #computeIfAbsent(Object, Function)}
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns null.
     * @return the value if found otherwise the missing value.
     */
    public int computeIfAbsent(final int key, final IntUnaryOperator mappingFunction)
    {
        int value = get(key);
        if (value == missingValue)
        {
            value = mappingFunction.applyAsInt(key);
            if (value != missingValue)
            {
                put(key, value);
            }
        }

        return value;
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
        intForEach(action::accept);
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
     * Remove value from the map using given key avoiding boxing.
     *
     * @param key whose mapping is to be removed from the map.
     * @return removed value or {@link #missingValue()} if key was not found in the map.
     */
    public int remove(final int key)
    {
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int keyIndex = Hashing.evenHash(key, mask);

        int oldValue = missingValue;
        while (entries[keyIndex + 1] != missingValue)
        {
            if (entries[keyIndex] == key)
            {
                oldValue = entries[keyIndex + 1];
                entries[keyIndex + 1] = missingValue;
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
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int keyIndex = deleteKeyIndex;

        while (true)
        {
            keyIndex = next(keyIndex, mask);
            if (entries[keyIndex + 1] == missingValue)
            {
                break;
            }

            @DoNotSub final int hash = Hashing.evenHash(entries[keyIndex], mask);

            if ((keyIndex < hash && (hash <= deleteKeyIndex || deleteKeyIndex <= keyIndex)) ||
                (hash <= deleteKeyIndex && deleteKeyIndex <= keyIndex))
            {
                entries[deleteKeyIndex] = entries[keyIndex];
                entries[deleteKeyIndex + 1] = entries[keyIndex + 1];

                entries[keyIndex + 1] = missingValue;
                deleteKeyIndex = keyIndex;
            }
        }
    }

    /**
     * Get the minimum value stored in the map. If the map is empty then it will return {@link #missingValue()}
     *
     * @return the minimum value stored in the map.
     */
    public int minValue()
    {
        final int missingValue = this.missingValue;
        int min = size == 0 ? missingValue : Integer.MAX_VALUE;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int valueIndex = 1; valueIndex < length; valueIndex += 2)
        {
            final int value = entries[valueIndex];
            if (value != missingValue)
            {
                min = Math.min(min, value);
            }
        }

        return min;
    }

    /**
     * Get the maximum value stored in the map. If the map is empty then it will return {@link #missingValue()}
     *
     * @return the maximum value stored in the map.
     */
    public int maxValue()
    {
        final int missingValue = this.missingValue;
        int max = size == 0 ? missingValue : Integer.MIN_VALUE;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int valueIndex = 1; valueIndex < length; valueIndex += 2)
        {
            final int value = entries[valueIndex];
            if (value != missingValue)
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
     * Primitive specialised version of {@link #replace(Object, Object)}
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@link #missingValue()} if there was no mapping for the key.
     */
    public int replace(final int key, final int value)
    {
        int currentValue = get(key);
        if (currentValue != missingValue)
        {
            currentValue = put(key, value);
        }

        return currentValue;
    }

    /**
     * Primitive specialised version of {@link #replace(Object, Object, Object)}
     *
     * @param key      key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     */
    public boolean replace(final int key, final int oldValue, final int newValue)
    {
        final int curValue = get(key);
        if (curValue != oldValue || curValue == missingValue)
        {
            return false;
        }

        put(key, newValue);

        return true;
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
    abstract class AbstractIterator implements Serializable
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
            if (entries[capacity - 1] != missingValue)
            {
                for (@DoNotSub int i = 1; i < capacity; i += 2)
                {
                    if (entries[i] == missingValue)
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

            for (@DoNotSub int keyIndex = positionCounter - 2; keyIndex >= stopCounter; keyIndex -= 2)
            {
                @DoNotSub final int index = keyIndex & mask;
                if (entries[index + 1] != missingValue)
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
    public final class KeyIterator extends AbstractIterator implements Iterator<Integer>, Serializable
    {
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
    public final class ValueIterator extends AbstractIterator implements Iterator<Integer>, Serializable
    {
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
        implements Iterator<Entry<Integer, Integer>>, Entry<Integer, Integer>, Serializable
    {
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
                throw new IllegalArgumentException();
            }

            @DoNotSub final int keyPosition = keyPosition();
            final int prevValue = entries[keyPosition + 1];
            entries[keyPosition + 1] = value;
            return prevValue;
        }

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

            public Integer getKey()
            {
                return k;
            }

            public Integer getValue()
            {
                return v;
            }

            public Integer setValue(final Integer value)
            {
                return Int2IntHashMap.this.put(k, value.intValue());
            }

            @DoNotSub public int hashCode()
            {
                return Integer.hashCode(getIntKey()) ^ Integer.hashCode(getIntValue());
            }

            @DoNotSub public boolean equals(final Object o)
            {
                if (!(o instanceof Map.Entry))
                {
                    return false;
                }

                final Entry<?, ?> e = (Entry<?, ?>)o;

                return (e.getKey() != null && e.getValue() != null) && (e.getKey().equals(k) && e.getValue().equals(v));
            }

            public String toString()
            {
                return k + "=" + v;
            }
        }
    }

    /**
     * Set of keys which supports optional cached iterators to avoid allocation.
     */
    public final class KeySet extends AbstractSet<Integer> implements Serializable
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
    public final class ValueCollection extends AbstractCollection<Integer> implements Serializable
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
         * @return  {@code true} if value is contained in this map.
         */
        public boolean contains(final int value)
        {
            return containsValue(value);
        }
    }

    /**
     * Set of entries which supports optionally cached iterators to avoid allocation.
     */
    public final class EntrySet extends AbstractSet<Map.Entry<Integer, Integer>> implements Serializable
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
