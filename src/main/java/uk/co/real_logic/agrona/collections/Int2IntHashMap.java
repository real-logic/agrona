/*
 * Copyright 2015 Real Logic Ltd.
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
import uk.co.real_logic.agrona.generation.DoNotSub;

import java.util.*;
import java.util.function.BiConsumer;

import static uk.co.real_logic.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * A open addressing with linear probing hash map specialised for primitive key and value pairs.
 */
public class Int2IntHashMap implements Map<Integer, Integer>
{
    private final Set<Integer> keySet;
    private final Collection<Integer> values;
    private final Set<Entry<Integer, Integer>> entrySet;

    @DoNotSub private final double loadFactor;
    private final int missingValue;

    private int[] entries;

    @DoNotSub private int capacity;
    @DoNotSub private int mask;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size = 0;

    public Int2IntHashMap(final int missingValue)
    {
        this(16, 0.67, missingValue);
    }

    @SuppressWarnings("unchecked")
    public Int2IntHashMap(
        @DoNotSub final int initialCapacity,
        @DoNotSub final double loadFactor,
        final int missingValue)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        this.missingValue = missingValue;

        capacity(BitUtil.findNextPositivePowerOfTwo(initialCapacity));

        final PrimitiveIterator keyIterator = new PrimitiveIterator(0);
        final PrimitiveIterator valueIterator = new PrimitiveIterator(1);
        keySet = new MapDelegatingSet<>(this, keyIterator::reset, this::containsValue);
        values = new MapDelegatingSet<>(this, valueIterator::reset, this::containsKey);

        final EntryIterator entryIterator = new EntryIterator();
        entrySet = new MapDelegatingSet<>(this, entryIterator::reset, (e) -> containsKey(((Entry)e).getKey()));
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

    public int get(final int key)
    {
        final int[] entries = this.entries;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int candidateKey;
        while ((candidateKey = entries[index]) != missingValue)
        {
            if (candidateKey == key)
            {
                return entries[index + 1];
            }

            index = next(index);
        }

        return missingValue;
    }

    public int put(final int key, final int value)
    {
        int oldValue = missingValue;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int candidateKey;
        while ((candidateKey = entries[index]) != missingValue)
        {
            if (candidateKey == key)
            {
                oldValue = entries[index + 1];
                break;
            }

            index = next(index);
        }

        if (oldValue == missingValue)
        {
            ++size;
            entries[index] = key;
        }

        entries[index + 1] = value;

        checkResize();

        return oldValue;
    }

    private void checkResize()
    {
        if (size > resizeThreshold)
        {
            @DoNotSub final int newCapacity = capacity << 1;
            if (newCapacity < 0)
            {
                throw new IllegalStateException("Max capacity reached at size=" + size);
            }

            rehash(newCapacity);
        }
    }

    private void rehash(@DoNotSub final int newCapacity)
    {
        final int[] oldEntries = entries;
        @DoNotSub final int length = entries.length;

        capacity(newCapacity);

        for (@DoNotSub int i = 0; i < length; i += 2)
        {
            final int key = oldEntries[i];
            if (key != missingValue)
            {
                put(key, oldEntries[i + 1]);
            }
        }
    }

    /**
     * Primitive specialised forEach implementation.
     * <p>
     * NB: Renamed from forEach to avoid overloading on parameter types of lambda
     * expression, which doesn't interplay well with type inference in lambda expressions.
     *
     * @param consumer a callback called for each key/value pair in the map.
     */
    public void intForEach(final IntIntConsumer consumer)
    {
        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int i = 0; i < length; i += 2)
        {
            final int key = entries[i];
            if (key != missingValue)
            {
                consumer.accept(entries[i], entries[i + 1]);
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

    public boolean containsValue(final int value)
    {
        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int i = 1; i < length; i += 2)
        {
            final int entryValue = entries[i];
            if (entryValue == value)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        Arrays.fill(entries, missingValue);
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

    // ---------------- Boxed Versions Below ----------------

    /**
     * {@inheritDoc}
     */
    public Integer get(final Object key)
    {
        return get((int)key);
    }

    /**
     * {@inheritDoc}
     */
    public Integer put(final Integer key, final Integer value)
    {
        return put((int)key, (int)value);
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
        map.forEach(this::put);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> keySet()
    {
        return keySet;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Integer> values()
    {
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Entry<Integer, Integer>> entrySet()
    {
        return entrySet;
    }

    /**
     * {@inheritDoc}
     */
    public Integer remove(final Object key)
    {
        return remove((int)key);
    }

    public int remove(final int key)
    {
        final int[] entries = this.entries;

        @DoNotSub int index = Hashing.evenHash(key, mask);

        int candidateKey;
        while ((candidateKey = entries[index]) != missingValue)
        {
            if (candidateKey == key)
            {
                @DoNotSub final int valueIndex = index + 1;
                final int oldValue = entries[valueIndex];
                entries[index] = missingValue;
                entries[valueIndex] = missingValue;
                size--;

                compactChain(index);

                return oldValue;
            }

            index = next(index);
        }

        return missingValue;
    }

    private void compactChain(@DoNotSub int deleteIndex)
    {
        final int[] entries = this.entries;

        @DoNotSub int index = deleteIndex;
        while (true)
        {
            index = next(index);
            if (entries[index] == missingValue)
            {
                return;
            }

            @DoNotSub final int hash = Hashing.evenHash(entries[index], mask);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                entries[deleteIndex] = entries[index];
                entries[deleteIndex + 1] = entries[index + 1];

                entries[index] = missingValue;
                entries[index + 1] = missingValue;
                deleteIndex = index;
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
        int min = size == 0 ? missingValue : Integer.MAX_VALUE;

        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int i = 1; i < length; i += 2)
        {
            final int value = entries[i];
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
        int max = size == 0 ? missingValue : Integer.MIN_VALUE;

        final int[] entries = this.entries;
        @DoNotSub final int length = entries.length;

        for (@DoNotSub int i = 1; i < length; i += 2)
        {
            final int value = entries[i];
            if (value != missingValue)
            {
                max = Math.max(max, value);
            }
        }

        return max;
    }

    // ---------------- Utility Classes ----------------

    abstract class AbstractIterator
    {
        @DoNotSub private int capacity;
        @DoNotSub private int mask;
        @DoNotSub private int positionCounter;
        @DoNotSub private int stopCounter;

        AbstractIterator()
        {
            reset();
        }

        private void reset()
        {
            final int[] entries = Int2IntHashMap.this.entries;
            capacity = entries.length;
            mask = capacity - 1;

            @DoNotSub int i = capacity;
            if (entries[capacity - 2] != missingValue)
            {
                i = 0;
                for (@DoNotSub int size = capacity; i < size; i += 2)
                {
                    if (entries[i] == missingValue)
                    {
                        break;
                    }
                }
            }

            stopCounter = i;
            positionCounter = i + capacity;
        }

        @DoNotSub protected int getKeyPosition()
        {
            return positionCounter & mask;
        }

        public boolean hasNext()
        {
            final int[] entries = Int2IntHashMap.this.entries;
            for (@DoNotSub int i = positionCounter - 2; i >= stopCounter; i -= 2)
            {
                @DoNotSub final int index = i & mask;
                if (entries[index] != missingValue)
                {
                    return true;
                }
            }

            return false;
        }

        protected void findNext()
        {
            final int[] entries = Int2IntHashMap.this.entries;

            for (@DoNotSub int i = positionCounter - 2; i >= stopCounter; i -= 2)
            {
                @DoNotSub final int index = i & mask;
                if (entries[index] != missingValue)
                {
                    positionCounter = i;
                    return;
                }
            }

            throw new NoSuchElementException();
        }
    }

    final class PrimitiveIterator extends AbstractIterator implements Iterator<Integer>
    {
        @DoNotSub private final int offset;

        private PrimitiveIterator(@DoNotSub final int offset)
        {
            this.offset = offset;
        }

        public Integer next()
        {
            return nextValue();
        }

        public int nextValue()
        {
            findNext();

            return entries[getKeyPosition() + offset];
        }

        public PrimitiveIterator reset()
        {
            super.reset();

            return this;
        }
    }

    final class EntryIterator
        extends AbstractIterator
        implements Iterator<Entry<Integer, Integer>>, Entry<Integer, Integer>
    {
        private int key;
        private int value;

        private EntryIterator()
        {
            super();
        }

        public Integer getKey()
        {
            return key;
        }

        public Integer getValue()
        {
            return value;
        }

        public Integer setValue(final Integer value)
        {
            throw new UnsupportedOperationException();
        }

        public Entry<Integer, Integer> next()
        {
            findNext();

            @DoNotSub final int keyPosition = getKeyPosition();
            key = entries[keyPosition];
            value = entries[keyPosition + 1];

            return this;
        }

        public EntryIterator reset()
        {
            super.reset();
            key = missingValue;
            value = missingValue;

            return this;
        }
    }

    @DoNotSub private int next(final int index)
    {
        return (index + 2) & mask;
    }

    private void capacity(@DoNotSub final int newCapacity)
    {
        capacity = newCapacity;
        /*@DoNotSub*/ resizeThreshold = (int)(newCapacity * loadFactor);
        mask = (newCapacity * 2) - 1;
        entries = new int[newCapacity * 2];
        size = 0;
        Arrays.fill(entries, missingValue);
    }
}
