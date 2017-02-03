/*
 * Copyright 2014 - 2016 Real Logic Ltd.
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
import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * {@link java.util.Map} implementation specialised for int keys using open addressing and
 * linear probing for cache efficient access.
 *
 * @param <V> type of values stored in the {@link java.util.Map}
 */
public class Int2ObjectHashMap<V>
    implements Map<Integer, V>
{
    private final float loadFactor;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size;

    private int[] keys;
    private Object[] values;

    private final ValueCollection<V> valueCollection;
    private final KeySet keySet;
    private final EntrySet<V> entrySet;

    public Int2ObjectHashMap()
    {
        this(8, 0.67f);
    }

    /**
     * Construct a new map allowing a configuration for initial capacity and load factor.
     *
     * @param initialCapacity for the backing array
     * @param loadFactor      limit for resizing on puts
     */
    public Int2ObjectHashMap(
        @DoNotSub final int initialCapacity,
        final float loadFactor)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        /* @DoNotSub */ final int capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        /* @DoNotSub */ resizeThreshold = (int)(capacity * loadFactor);

        keys = new int[capacity];
        values = new Object[capacity];

        // Cached to avoid allocation.
        valueCollection = new ValueCollection<>();
        keySet = new KeySet();
        entrySet = new EntrySet<>();
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
     * Get the total capacity for the map to which the load factor with be a fraction of.
     *
     * @return the total capacity for the map.
     */
    @DoNotSub public int capacity()
    {
        return values.length;
    }

    /**
     * Get the actual threshold which when reached the map resize.
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
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

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
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

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
     * Get a value for a given key, or if it does not exist then default the value via a
     * {@link java.util.function.IntFunction} and put it in the map.
     *
     * Primitive specialized version of {@link java.util.Map#computeIfAbsent}.
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
     * @return the previous value if found otherwise null
     */
    @SuppressWarnings("unchecked")
    public V put(final int key, final V value)
    {
        requireNonNull(value, "Value cannot be null");

        V oldValue = null;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

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
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

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
     * {@inheritDoc}
     */
    public void clear()
    {
        size = 0;
        Arrays.fill(values, null);
    }

    /**
     * Compact the {@link Map} backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        @DoNotSub final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
        rehash(BitUtil.findNextPositivePowerOfTwo(idealCapacity));
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

    /**
     * Primitive specialised version of {@link #replace(Object, Object)}
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     */
    public V replace(final int key, final V value)
    {
        V curValue = get(key);
        if (curValue != null)
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
    public boolean replace(final int key, final V oldValue, final V newValue)
    {
        final Object curValue = get(key);
        if (curValue == null || !Objects.equals(curValue, oldValue))
        {
            return false;
        }

        put(key, newValue);

        return true;
    }

    private void increaseCapacity()
    {
        @DoNotSub final int newCapacity = values.length << 1;
        if (newCapacity < 0)
        {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        rehash(newCapacity);
    }

    private void rehash(@DoNotSub final int newCapacity)
    {
        @DoNotSub final int mask = newCapacity - 1;
        /* @DoNotSub */ resizeThreshold = (int)(newCapacity * loadFactor);

        final int[] tempKeys = new int[newCapacity];
        final Object[] tempValues = new Object[newCapacity];

        for (@DoNotSub int i = 0, size = values.length; i < size; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                final int key = keys[i];
                @DoNotSub int newHash = Hashing.hash(key, mask);
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
    private void compactChain(@DoNotSub int deleteIndex)
    {
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = deleteIndex;
        while (true)
        {
            index = ++index & mask;
            if (null == values[index])
            {
                break;
            }

            @DoNotSub final int hash = Hashing.hash(keys[index], mask);

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

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Internal Sets and Collections
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public class KeySet extends AbstractSet<Integer>
    {
        private final KeyIterator iterator = new KeyIterator();

        @DoNotSub public int size()
        {
            return Int2ObjectHashMap.this.size();
        }

        public boolean contains(final Object o)
        {
            return Int2ObjectHashMap.this.containsKey(o);
        }

        public boolean contains(final int key)
        {
            return Int2ObjectHashMap.this.containsKey(key);
        }

        public KeyIterator iterator()
        {
            iterator.reset();

            return iterator;
        }

        public boolean remove(final Object o)
        {
            return null != Int2ObjectHashMap.this.remove(o);
        }

        public boolean remove(final int key)
        {
            return null != Int2ObjectHashMap.this.remove(key);
        }

        public void clear()
        {
            Int2ObjectHashMap.this.clear();
        }
    }

    private class ValueCollection<V> extends AbstractCollection<V>
    {
        private final ValueIterator<V> iterator = new ValueIterator<V>();

        @DoNotSub public int size()
        {
            return Int2ObjectHashMap.this.size();
        }

        public boolean contains(final Object o)
        {
            return Int2ObjectHashMap.this.containsValue(o);
        }

        public ValueIterator<V> iterator()
        {
            iterator.reset();

            return iterator;
        }

        public void clear()
        {
            Int2ObjectHashMap.this.clear();
        }
    }

    private class EntrySet<V> extends AbstractSet<Entry<Integer, V>>
    {
        private final EntryIterator<V> iterator = new EntryIterator<V>();

        @DoNotSub public int size()
        {
            return Int2ObjectHashMap.this.size();
        }

        public Iterator<Entry<Integer, V>> iterator()
        {
            iterator.reset();

            return iterator;
        }

        public void clear()
        {
            Int2ObjectHashMap.this.clear();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Iterators
    ///////////////////////////////////////////////////////////////////////////////////////////////

    abstract class AbstractIterator<T> implements Iterator<T>
    {
        @DoNotSub private int posCounter;
        @DoNotSub private int stopCounter;
        private boolean isPositionValid = false;
        protected int[] keys;
        protected Object[] values;

        protected AbstractIterator()
        {
            reset();
        }

        @DoNotSub protected int position()
        {
            return posCounter & values.length - 1;
        }

        public boolean hasNext()
        {
            @DoNotSub final int mask = values.length - 1;
            boolean hasNext = false;
            for (@DoNotSub int i = posCounter - 1; i >= stopCounter; i--)
            {
                @DoNotSub final int index = i & mask;
                if (null != values[index])
                {
                    hasNext = true;
                    break;
                }
            }

            return hasNext;
        }

        protected void findNext()
        {
            @DoNotSub final int mask = values.length - 1;
            isPositionValid = false;

            for (@DoNotSub int i = posCounter - 1; i >= stopCounter; i--)
            {
                @DoNotSub final int index = i & mask;
                if (null != values[index])
                {
                    posCounter = i;
                    isPositionValid = true;
                    return;
                }
            }

            throw new NoSuchElementException();
        }

        public abstract T next();

        public void remove()
        {
            if (isPositionValid)
            {
                @DoNotSub final int position = position();
                values[position] = null;
                --size;

                compactChain(position);

                isPositionValid = false;
            }
            else
            {
                throw new IllegalStateException();
            }
        }

        void reset()
        {
            keys = Int2ObjectHashMap.this.keys;
            values = Int2ObjectHashMap.this.values;
            @DoNotSub final int capacity = values.length;

            @DoNotSub int i = capacity;
            if (null != values[capacity - 1])
            {
                i = 0;
                for (@DoNotSub int size = capacity; i < size; i++)
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
            requireNonNull(value);

            @DoNotSub final int pos = position();
            final Object oldValue = values[pos];
            values[pos] = value;

            return (V)oldValue;
        }
    }
}
