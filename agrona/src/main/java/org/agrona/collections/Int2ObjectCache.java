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

import org.agrona.generation.DoNotSub;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;
import static org.agrona.collections.CollectionUtil.validatePositivePowerOfTwo;

/**
 * A cache implementation specialised for int keys using open addressing to probe a set of fixed size.
 *
 * The eviction strategy is to remove the oldest in a set if the key is not found, or if found then that item.
 * The newly inserted item becomes the youngest in the set. Sets are evicted on a first in, first out, manner unless
 * replacing a matching key.
 *
 * A good set size would be in the range of 2 to 16 so that the references/keys can fit in a cache-line (assuming
 * references are 32 bit references and 64 byte cache lines, YMMV). A linear search within a cache line is much much
 * less costly than a cache-miss to another line.
 *
 * Null values are not supported by this cache.
 *
 * @param <V> type of values stored in the {@link Map}
 */
public class Int2ObjectCache<V>
    implements Map<Integer, V>
{
    private long cachePuts = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;

    @DoNotSub private int size;
    @DoNotSub private final int capacity;
    @DoNotSub private final int setSize;
    @DoNotSub private final int mask;

    private final int[] keys;
    private final Object[] values;
    private final Consumer<V> evictionConsumer;

    private final ValueCollection<V> valueCollection;
    private final KeySet keySet;
    private final EntrySet<V> entrySet;

    public Int2ObjectCache(
        @DoNotSub final int numSets,
        @DoNotSub final int setSize,
        final Consumer<V> evictionConsumer)
    {
        validatePositivePowerOfTwo(numSets);
        requireNonNull(evictionConsumer, "Null values are not permitted");

        if (((long)numSets) * setSize > (Integer.MAX_VALUE - 8))
        {
            throw new IllegalArgumentException(
                "Total capacity must be <= max array size: numSets=" + numSets + " setSize=" + setSize);
        }

        this.setSize = setSize;
        capacity = numSets * setSize;
        mask = numSets - 1;

        keys = new int[capacity];
        values = new Object[capacity];
        this.evictionConsumer = evictionConsumer;

        // Cached to avoid allocation.
        valueCollection = new ValueCollection<>();
        keySet = new KeySet();
        entrySet = new EntrySet<>();
    }

    /**
     * The number of times a cache hit has occurred on the {@link #get(int)} method.
     *
     * @return the number of times a cache hit has occurred on the {@link #get(int)} method.
     */
    public long cacheHits()
    {
        return cacheHits;
    }

    /**
     * The number of times a cache miss has occurred on the {@link #get(int)} method.
     *
     * @return the number of times a cache miss has occurred on the {@link #get(int)} method.
     */
    public long cacheMisses()
    {
        return cacheMisses;
    }

    /**
     * The number of items that have been put in the cache.
     *
     * @return number of items that have been put in the cache.
     */
    public long cachePuts()
    {
        return cachePuts;
    }

    /**
     * Reset the cache statistics counters to zero.
     */
    public void resetCounters()
    {
        cacheHits = 0;
        cacheMisses = 0;
        cachePuts = 0;
    }

    /**
     * Get the total capacity for the map to which the load factor with be a fraction of.
     *
     * @return the total capacity for the map.
     */
    @DoNotSub public int capacity()
    {
        return capacity;
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
     * {@inheritDoc}
     */
    public boolean containsKey(final Object key)
    {
        return containsKey(((Integer)key).intValue());
    }

    /**
     * Overloaded version of {@link Map#containsKey(Object)} that takes a primitive int key.
     *
     * @param key for indexing the {@link Map}
     * @return true if the key is found otherwise false.
     */
    public boolean containsKey(final int key)
    {
        boolean found = false;
        @DoNotSub final int setNumber = Hashing.hash(key, mask);
        @DoNotSub final int setBeginIndex = setNumber * setSize;

        for (@DoNotSub int i = setBeginIndex, setEndIndex = setBeginIndex + setSize; i < setEndIndex; i++)
        {
            if (null == values[i])
            {
                break;
            }

            if (key == keys[i])
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
    public boolean containsValue(final Object value)
    {
        boolean found = false;
        if (null != value)
        {
            for (final Object v : values)
            {
                if (value.equals(v))
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
    public V get(final Object key)
    {
        return get(((Integer)key).intValue());
    }

    /**
     * Overloaded version of {@link Map#get(Object)} that takes a primitive int key.
     *
     * @param key for indexing the {@link Map}
     * @return the value if found otherwise null
     */
    @SuppressWarnings("unchecked")
    public V get(final int key)
    {
        V value = null;
        @DoNotSub final int setNumber = Hashing.hash(key, mask);
        @DoNotSub final int setBeginIndex = setNumber * setSize;

        for (@DoNotSub int i = setBeginIndex, setEndIndex = setBeginIndex + setSize; i < setEndIndex; i++)
        {
            if (null == values[i])
            {
                break;
            }

            if (key == keys[i])
            {
                value = (V)values[i];
                break;
            }
        }

        if (null == value)
        {
            cacheMisses++;
        }
        else
        {
            cacheHits++;
        }

        return value;
    }

    /**
     * Get a value for a given key, or if it does ot exist then default the value via a {@link java.util.function.IntFunction}
     * and put it in the  cache.
     *
     * Primitive specialized version of {@link Map#computeIfAbsent}.
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns null.
     * @return the value if found otherwise the default.
     */
    public V computeIfAbsent(final int key, final IntFunction<? extends V> mappingFunction)
    {
        V value = get(key);
        if (value == null)
        {
            value = mappingFunction.apply(key);
            if (value != null)
            {
                put(key, value);
            }
        }

        return value;
    }

    /**
     * {@inheritDoc}
     */
    public V put(final Integer key, final V value)
    {
        return put(key.intValue(), value);
    }

    /**
     * Overloaded version of {@link Map#put(Object, Object)} that takes a primitive int key.
     *
     * @param key   for indexing the {@link Map}
     * @param value to be inserted in the {@link Map}
     * @return always null (as per JCache API, as opposed to {@link Map})
     */
    @SuppressWarnings("unchecked")
    public V put(final int key, final V value)
    {
        requireNonNull(value, "null values are not supported");

        V evictedValue = null;
        @DoNotSub final int setNumber = Hashing.hash(key, mask);
        @DoNotSub final int setBeginIndex = setNumber * setSize;
        @DoNotSub int i = setBeginIndex;

        for (@DoNotSub int nextSetIndex = setBeginIndex + setSize; i < nextSetIndex; i++)
        {
            if (null == values[i])
            {
                break;
            }

            if (key == keys[i])
            {
                evictedValue = (V)values[i];
                shuffleUp(i, nextSetIndex - 1);

                break;
            }
        }

        if (null == evictedValue)
        {
            evictedValue = (V)values[setBeginIndex + (setSize - 1)];
        }

        shuffleDown(setBeginIndex);

        keys[setBeginIndex] = key;
        values[setBeginIndex] = value;

        cachePuts++;

        if (null != evictedValue)
        {
            evictionConsumer.accept(evictedValue);
        }
        else
        {
            ++size;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public V remove(final Object key)
    {
        return remove(((Integer)key).intValue());
    }

    /**
     * Overloaded version of {@link Map#remove(Object)} that takes a primitive int key.
     *
     * @param key for indexing the {@link Map}
     * @return the value if found otherwise null
     */
    @SuppressWarnings("unchecked")
    public V remove(final int key)
    {
        V value = null;
        @DoNotSub final int setNumber = Hashing.hash(key, mask);
        @DoNotSub final int setBeginIndex = setNumber * setSize;

        for (@DoNotSub int i = setBeginIndex, nextSetIndex = setBeginIndex + setSize; i < nextSetIndex; i++)
        {
            if (null == values[i])
            {
                break;
            }

            if (key == keys[i])
            {
                value = (V)values[i];
                shuffleUp(i, nextSetIndex - 1);
                --size;

                evictionConsumer.accept(value);
                break;
            }
        }

        return value;
    }

    @DoNotSub private void shuffleUp(final int fromIndex, final int toIndex)
    {
        values[toIndex] = null;

        for (@DoNotSub int i = fromIndex; i < toIndex; i++)
        {
            values[i] = values[i + 1];
            keys[i] = keys[i + 1];
        }
    }

    @DoNotSub private void shuffleDown(final int setBeginIndex)
    {
        for (@DoNotSub int i = setBeginIndex + (setSize - 1); i > setBeginIndex; i--)
        {
            values[i] = values[i - 1];
            keys[i] = keys[i - 1];
        }

        values[setBeginIndex] = null;
    }

    /**
     * Clear down all items in the cache.
     *
     * If an exception occurs during the eviction function callback then clear may need to be called again to complete.
     * If an exception occurs the cache should only be used when {@link #size()} reports zero.
     */
    @SuppressWarnings("unchecked")
    public void clear()
    {
        for (@DoNotSub int i = 0, size = values.length; i < size; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                values[i] = null;
                this.size--;

                evictionConsumer.accept((V)value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(final Map<? extends Integer, ? extends V> map)
    {
        for (final Entry<? extends Integer, ? extends V> entry : map.entrySet())
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
    public Collection<V> values()
    {
        return valueCollection;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Entry<Integer, V>> entrySet()
    {
        return entrySet;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');

        for (@DoNotSub int i = 0, length = values.length; i < length; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                sb.append(keys[i]);
                sb.append('=');
                sb.append(value);
                sb.append(", ");
            }
        }

        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 2);
        }

        sb.append('}');

        return sb.toString();
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

        if (o == null || !(o instanceof Map))
        {
            return false;
        }

        final Map<?, ?> that = (Map<?, ?>)o;

        if (size != that.size())
        {
            return false;
        }

        for (@DoNotSub int i = 0, length = values.length; i < length; i++)
        {
            final Object thisValue = values[i];
            if (null != thisValue)
            {
                final Object thatValue = that.get(keys[i]);
                if (null == thatValue || !thisValue.equals(thatValue))
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @DoNotSub public int hashCode()
    {
        @DoNotSub int result = 0;

        for (@DoNotSub int i = 0, length = values.length; i < length; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                result += (Integer.hashCode(keys[i]) ^ value.hashCode());
            }
        }

        return result;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Internal Sets and Collections
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public class KeySet extends AbstractSet<Integer>
    {
        private final KeyIterator iterator = new KeyIterator();

        @DoNotSub public int size()
        {
            return Int2ObjectCache.this.size();
        }

        public boolean contains(final Object o)
        {
            return Int2ObjectCache.this.containsKey(o);
        }

        public boolean contains(final int key)
        {
            return Int2ObjectCache.this.containsKey(key);
        }

        public KeyIterator iterator()
        {
            iterator.reset();

            return iterator;
        }

        public boolean remove(final Object o)
        {
            throw new UnsupportedOperationException("Cannot remove on iterator");
        }

        public void clear()
        {
            Int2ObjectCache.this.clear();
        }
    }

    private class ValueCollection<V> extends AbstractCollection<V>
    {
        private final ValueIterator<V> iterator = new ValueIterator<V>();

        @DoNotSub public int size()
        {
            return Int2ObjectCache.this.size();
        }

        public boolean contains(final Object o)
        {
            return Int2ObjectCache.this.containsValue(o);
        }

        public ValueIterator<V> iterator()
        {
            iterator.reset();

            return iterator;
        }

        public void clear()
        {
            Int2ObjectCache.this.clear();
        }
    }

    private class EntrySet<V> extends AbstractSet<Entry<Integer, V>>
    {
        private final EntryIterator<V> iterator = new EntryIterator<V>();

        @DoNotSub public int size()
        {
            return Int2ObjectCache.this.size();
        }

        public Iterator<Entry<Integer, V>> iterator()
        {
            iterator.reset();

            return iterator;
        }

        public void clear()
        {
            Int2ObjectCache.this.clear();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Iterators
    ///////////////////////////////////////////////////////////////////////////////////////////////

    abstract class AbstractIterator<T> implements Iterator<T>
    {
        @DoNotSub private int remaining;
        @DoNotSub private int position = -1;

        protected AbstractIterator()
        {
            reset();
        }

        @DoNotSub protected int position()
        {
            return position;
        }

        public boolean hasNext()
        {
            return remaining > 0;
        }

        protected void findNext()
        {
            boolean found = false;
            for (@DoNotSub int i = position + 1; i < capacity; i++)
            {
                if (null != values[i])
                {
                    found = true;
                    position = i;
                    --remaining;
                    break;
                }
            }

            if (!found)
            {
                throw new NoSuchElementException();
            }
        }

        public abstract T next();

        public void remove()
        {
            throw new UnsupportedOperationException("Remove not supported on Iterator");
        }

        void reset()
        {
            remaining = size;
            position = -1;
        }
    }

    public class ValueIterator<T> extends AbstractIterator<T>
    {
        @SuppressWarnings("unchecked")
        public T next()
        {
            findNext();

            return (T)values[position()];
        }
    }

    public class KeyIterator extends AbstractIterator<Integer>
    {
        public Integer next()
        {
            return nextInt();
        }

        public int nextInt()
        {
            findNext();

            return keys[position()];
        }
    }

    @SuppressWarnings("unchecked")
    public class EntryIterator<V>
        extends AbstractIterator<Entry<Integer, V>>
        implements Entry<Integer, V>
    {
        public Entry<Integer, V> next()
        {
            findNext();

            return this;
        }

        public Integer getKey()
        {
            return keys[position()];
        }

        public V getValue()
        {
            return (V)values[position()];
        }

        public V setValue(final V value)
        {
            throw new UnsupportedOperationException("Cannot set on this iterator");
        }
    }
}
