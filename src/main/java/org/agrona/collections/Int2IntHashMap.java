/*
 *  Copyright 2014 - 2016 Real Logic Ltd.
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
import java.util.function.BiConsumer;

import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * A open addressing with linear probing hash map specialised for primitive key and value pairs.
 */
public class Int2IntHashMap implements Map<Integer, Integer>
{
    @DoNotSub private final float loadFactor;
    private final int missingValue;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size = 0;

    private int[] entries;
    private final KeySet keySet;
    private final Values values;
    private final Set<Entry<Integer, Integer>> entrySet;

    public Int2IntHashMap(final int missingValue)
    {
        this(16, 0.67f, missingValue);
    }

    @SuppressWarnings("unchecked")
    public Int2IntHashMap(
        @DoNotSub final int initialCapacity,
        @DoNotSub final float loadFactor,
        final int missingValue)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        this.missingValue = missingValue;

        capacity(BitUtil.findNextPositivePowerOfTwo(initialCapacity));

        keySet = new KeySet();
        values = new Values();
        entrySet = new EntrySet();
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
        final int missingValue = this.missingValue;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int value = missingValue;
        int candidateKey;
        while ((candidateKey = entries[index]) != missingValue)
        {
            if (candidateKey == key)
            {
                value = entries[index + 1];
                break;
            }

            index = next(index, mask);
        }

        return value;
    }

    public int put(final int key, final int value)
    {
        final int[] entries = this.entries;
        final int missingValue = this.missingValue;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);
        int oldValue = missingValue;

        int candidateKey;
        while ((candidateKey = entries[index]) != missingValue)
        {
            if (candidateKey == key)
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
        final int missingValue = this.missingValue;
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
        final int missingValue = this.missingValue;
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

        boolean found = false;
        for (@DoNotSub int i = 1; i < length; i += 2)
        {
            if (value == entries[i])
            {
                found = true;
                break;
            }
        }

        return found;
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
        return keySet;
    }

    /**
     * {@inheritDoc}
     */
    public Values values()
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
        final int missingValue = this.missingValue;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = Hashing.evenHash(key, mask);

        int oldValue = missingValue;
        int candidateKey;
        while ((candidateKey = entries[index]) != missingValue)
        {
            if (candidateKey == key)
            {
                @DoNotSub final int valueIndex = index + 1;
                oldValue = entries[valueIndex];
                entries[index] = missingValue;
                entries[valueIndex] = missingValue;
                size--;

                compactChain(index);

                break;
            }

            index = next(index, mask);
        }

        return oldValue;
    }

    private void compactChain(@DoNotSub int deleteIndex)
    {
        final int[] entries = this.entries;
        final int missingValue = this.missingValue;
        @DoNotSub final int mask = entries.length - 1;
        @DoNotSub int index = deleteIndex;

        while (true)
        {
            index = next(index, mask);
            if (entries[index] == missingValue)
            {
                break;
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
        final int missingValue = this.missingValue;
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
        final int missingValue = this.missingValue;
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

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');

        for (final Entry<Integer, Integer> entry : entrySet())
        {
            sb.append(entry.getKey().intValue());
            sb.append('=');
            sb.append(entry.getValue().intValue());
            sb.append(", ");
        }

        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 2);
        }

        sb.append('}');

        return sb.toString();
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
        int curValue = get(key);
        if (curValue != missingValue)
        {
            curValue = put(key, value);
        }
        return curValue;
    }

    /**
     * Primitive specialised version of {@link #replace(Object, Object, Object)}
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     */
    public boolean replace(final int key, int oldValue, int newValue)
    {
        final int curValue = get(key);
        if (curValue != oldValue || curValue == missingValue)
        {
            return false;
        }
        put(key, newValue);
        return true;
    }

    @DoNotSub private static int next(final int index, final int mask)
    {
        return (index + 2) & mask;
    }

    private void capacity(@DoNotSub int newCapacity)
    {
        @DoNotSub final int entriesLength = newCapacity * 2;
        if (entriesLength < 0)
        {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        /*@DoNotSub*/ resizeThreshold = (int)(newCapacity * loadFactor);
        entries = new int[entriesLength];
        size = 0;
        Arrays.fill(entries, missingValue);
    }

    // ---------------- Utility Classes ----------------

    abstract class AbstractIterator
    {
        @DoNotSub private int positionCounter;
        @DoNotSub private int stopCounter;

        AbstractIterator()
        {
            reset();
        }

        private void reset()
        {
            final int missingValue = Int2IntHashMap.this.missingValue;
            final int[] entries = Int2IntHashMap.this.entries;
            @DoNotSub final int capacity = entries.length;

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

        @DoNotSub protected int keyPosition()
        {
            return positionCounter & entries.length - 1;
        }

        public boolean hasNext()
        {
            final int[] entries = Int2IntHashMap.this.entries;
            final int missingValue = Int2IntHashMap.this.missingValue;
            @DoNotSub final int mask = entries.length - 1;
            boolean hasNext = false;
            for (@DoNotSub int i = positionCounter - 2; i >= stopCounter; i -= 2)
            {
                @DoNotSub final int index = i & mask;
                if (entries[index] != missingValue)
                {
                    hasNext = true;
                    break;
                }
            }

            return hasNext;
        }

        protected void findNext()
        {
            final int[] entries = Int2IntHashMap.this.entries;
            final int missingValue = Int2IntHashMap.this.missingValue;
            @DoNotSub final int mask = entries.length - 1;

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

    public final class IntIterator extends AbstractIterator implements Iterator<Integer>
    {
        @DoNotSub private final int offset;

        private IntIterator(
            @DoNotSub final int offset)
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

            return entries[keyPosition() + offset];
        }

        public IntIterator reset()
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

            @DoNotSub final int keyPosition = keyPosition();
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

    public final class KeySet extends MapDelegatingSet<Integer>
    {
        private final IntIterator keyIterator = new IntIterator(0);

        private KeySet()
        {
            super(Int2IntHashMap.this);
        }

        /**
         * {@inheritDoc}
         */
        public IntIterator iterator()
        {
            return keyIterator.reset();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return contains((int)o);
        }

        public boolean contains(final int key)
        {
            return containsKey(key);
        }
    }

    public final class Values extends AbstractCollection<Integer>
    {
        private final IntIterator valueIterator = new IntIterator(1);

        private Values()
        {
        }

        /**
         * {@inheritDoc}
         */
        public IntIterator iterator()
        {
            return valueIterator.reset();
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

        public boolean contains(final int key)
        {
            return containsValue(key);
        }
    }

    private final class EntrySet extends MapDelegatingSet<Entry<Integer, Integer>>
    {
        private final EntryIterator entryIterator = new EntryIterator();

        private EntrySet()
        {
            super(Int2IntHashMap.this);
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<Entry<Integer, Integer>> iterator()
        {
            return entryIterator.reset();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return containsKey(((Entry)o).getKey());
        }
    }
}
