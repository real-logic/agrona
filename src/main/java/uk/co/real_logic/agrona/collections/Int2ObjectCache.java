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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;

/**
 * A cache implementation specialised for int keys using open addressing and
 * linear probing for efficient access. The cache can only grow to the maxSize which is less than capacity.
 *
 * The eviction strategy approximates to LRU using a victim replacement policy.
 *
 * @param <V> values stored in the {@link Map}
 */
public class Int2ObjectCache<V>
    implements Map<Integer, V>
{
    public static final double LOAD_FACTOR = 0.67d;
    public static final int SIZE_LIMIT = (int)((1 << 30) * LOAD_FACTOR);

    private long cacheHits = 0;
    private long cacheMisses = 0;

    private int maxSize;
    private int capacity;
    private int mask;
    private int size;

    private final int[] keys;
    private final Object[] values;
    private final Consumer<V> evictionHandler;

    private final ValueCollection<V> valueCollection;
    private final KeySet keySet;
    private final EntrySet<V> entrySet;

    /**
     * Construct a new cache with a maximum size.
     *
     * @param maxSize         beyond which slots are reused.
     * @param evictionHandler to be called when a value is evicted from the cache.
     */
    public Int2ObjectCache(final int maxSize, final Consumer<V> evictionHandler)
    {
        if (maxSize <= 0 || maxSize >= SIZE_LIMIT)
        {
            throw new IllegalArgumentException(String.format(
                "maxSize must be greater than 0 and less than limit : maxSize=%d limit=%d", maxSize, capacity));
        }


        requireNonNull(evictionHandler, "Null values are not permitted");

        this.capacity = BitUtil.findNextPositivePowerOfTwo((int)(maxSize * (1 / LOAD_FACTOR)));
        this.maxSize = maxSize;
        mask = this.capacity - 1;

        keys = new int[this.capacity];
        values = new Object[this.capacity];
        this.evictionHandler = evictionHandler;

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
     * Get the maximum size the cache of values can grow to.
     *
     * @return the maximum size the cache of values can grow to.
     */
    public int maxSize()
    {
        return maxSize;
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
     * {@inheritDoc}
     */
    public int size()
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
        requireNonNull(key, "Null keys are not permitted");

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
        int index = hash(key);

        while (null != values[index])
        {
            if (key == keys[index])
            {
                return true;
            }

            index = ++index & mask;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(final Object value)
    {
        requireNonNull(value, "Null values are not permitted");

        for (final Object v : values)
        {
            if (null != v && value.equals(v))
            {
                return true;
            }
        }

        return false;
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
        int index = hash(key);

        Object value;
        while (null != (value = values[index]))
        {
            if (key == keys[index])
            {
                ++cacheHits;
                return (V)value;
            }

            index = ++index & mask;
        }

        ++cacheMisses;

        return null;
    }

    /**
     * Get a value for a given key, or if it does ot exist then default the value via a {@link java.util.function.IntFunction}
     * and put it in the  cache.
     * <p>
     * Primitive specialized version of {@link Map#computeIfAbsent}.
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns null.
     * @return the value if found otherwise the default.
     */
    public V computeIfAbsent(final int key, final IntFunction<? extends V> mappingFunction)
    {
        requireNonNull(mappingFunction, "mappingFunction cannot be null");
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
     * @return always null
     */
    public V put(final int key, final V value)
    {
        requireNonNull(value, "Value cannot be null");

        if ((size + 1) <= maxSize)
        {
            normalInsert(key, value);
        }
        else
        {
            evictingInsert(key, value);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void normalInsert(final int key, final V value)
    {
        V oldValue = null;
        int index = hash(key);

        while (null != values[index])
        {
            if (key == keys[index])
            {
                oldValue = (V)values[index];
                break;
            }

            index = ++index & mask;
        }

        keys[index] = key;
        values[index] = value;

        if (null == oldValue)
        {
            ++size;
        }
        else
        {
            evictionHandler.accept(oldValue);
        }
    }

    @SuppressWarnings("unchecked")
    private void evictingInsert(final int key, final V value)
    {
        V oldValue = null;
        int index = hash(key);
        final int startingIndex = index;

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
            if (startingIndex != index)
            {
                oldValue = (V)values[startingIndex];
                values[startingIndex] = null;
                compactChain(startingIndex);
            }
            else
            {
                for (int i = 0; i < values.length; i++)
                {
                    final int p = (index + i) & mask;

                    if (null != values[p])
                    {
                        oldValue = (V)values[p];
                        values[p] = null;
                        compactChain(p);
                        break;
                    }
                }
            }
        }

        keys[index] = key;
        values[index] = value;

        evictionHandler.accept(oldValue);
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
        int index = hash(key);

        Object value;
        while (null != (value = values[index]))
        {
            if (key == keys[index])
            {
                values[index] = null;
                --size;

                compactChain(index);

                evictionHandler.accept((V)value);
                return (V)value;
            }

            index = ++index & mask;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        size = 0;
        Arrays.fill(values, null);
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

        for (final Entry<Integer, V> entry : entrySet())
        {
            sb.append(entry.getKey().intValue());
            sb.append('=');
            sb.append(entry.getValue());
            sb.append(", ");
        }

        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 2);
        }

        sb.append('}');

        return sb.toString();
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

            final int hash = hash(keys[index]);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                keys[deleteIndex] = keys[index];
                values[deleteIndex] = values[index];

                values[index] = null;
                deleteIndex = index;
            }
        }
    }

    private int hash(final int key)
    {
        final int hash = key ^ (key >>> 16);

        return hash & mask;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Internal Sets and Collections
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public class KeySet extends AbstractSet<Integer>
    {
        private final KeyIterator iterator = new KeyIterator();

        public int size()
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
            return null != Int2ObjectCache.this.remove(o);
        }

        public boolean remove(final int key)
        {
            return null != Int2ObjectCache.this.remove(key);
        }

        public void clear()
        {
            Int2ObjectCache.this.clear();
        }
    }

    private class ValueCollection<V> extends AbstractCollection<V>
    {
        private final ValueIterator<V> iterator = new ValueIterator<V>();

        public int size()
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

        public int size()
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
        private int posCounter;
        private int stopCounter;
        protected int[] keys;
        protected Object[] values;

        protected AbstractIterator()
        {
            reset();
        }

        protected int getPosition()
        {
            return posCounter & mask;
        }

        public boolean hasNext()
        {
            for (int i = posCounter - 1; i >= stopCounter; i--)
            {
                final int index = i & mask;
                if (null != values[index])
                {
                    return true;
                }
            }

            return false;
        }

        protected void findNext()
        {
            for (int i = posCounter - 1; i >= stopCounter; i--)
            {
                final int index = i & mask;
                if (null != values[index])
                {
                    posCounter = i;
                    return;
                }
            }

            throw new NoSuchElementException();
        }

        public abstract T next();

        public void remove()
        {
            throw new UnsupportedOperationException("Cannot remove on iterator");
        }

        void reset()
        {
            keys = Int2ObjectCache.this.keys;
            values = Int2ObjectCache.this.values;

            int i = capacity;
            if (null != values[capacity - 1])
            {
                i = 0;
                for (int size = capacity; i < size; i++)
                {
                    if (null == values[i])
                    {
                        break;
                    }
                }
            }

            stopCounter = i;
            posCounter = i + capacity;
        }
    }

    public class ValueIterator<T> extends AbstractIterator<T>
    {
        @SuppressWarnings("unchecked")
        public T next()
        {
            findNext();

            return (T)values[getPosition()];
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

            return keys[getPosition()];
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
            return keys[getPosition()];
        }

        public V getValue()
        {
            return (V)values[getPosition()];
        }

        public V setValue(final V value)
        {
            throw new UnsupportedOperationException("Cannot set on iterator");
        }
    }
}
