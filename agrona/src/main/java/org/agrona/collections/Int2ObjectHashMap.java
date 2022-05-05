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
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;
import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * {@link java.util.Map} implementation specialised for int keys using open addressing and
 * linear probing for cache efficient access.
 *
 * @param <V> type of values stored in the {@link java.util.Map}
 */
public class Int2ObjectHashMap<V> implements Map<Integer, V>
{
    @DoNotSub static final int MIN_CAPACITY = 8;

    private final float loadFactor;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size;
    private final boolean shouldAvoidAllocation;

    private int[] keys;
    private Object[] values;

    private ValueCollection valueCollection;
    private KeySet keySet;
    private EntrySet entrySet;

    /**
     * Constructs map with {@link #MIN_CAPACITY}, {@link Hashing#DEFAULT_LOAD_FACTOR} and enables caching of iterators.
     */
    public Int2ObjectHashMap()
    {
        this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, true);
    }

    /**
     * Constructs map with given initial capacity and load factory and enables caching of iterators.
     *
     * @param initialCapacity for the backing array
     * @param loadFactor      limit for resizing on puts
     */
    public Int2ObjectHashMap(
        @DoNotSub final int initialCapacity,
        final float loadFactor)
    {
        this(initialCapacity, loadFactor, true);
    }

    /**
     * Construct a new map allowing a configuration for initial capacity and load factor.
     *
     * @param initialCapacity       for the backing array
     * @param loadFactor            limit for resizing on puts
     * @param shouldAvoidAllocation should allocation be avoided by caching iterators and map entries.
     */
    public Int2ObjectHashMap(
        @DoNotSub final int initialCapacity,
        final float loadFactor,
        final boolean shouldAvoidAllocation)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        this.shouldAvoidAllocation = shouldAvoidAllocation;

        /* @DoNotSub */ final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));
        /* @DoNotSub */ resizeThreshold = (int)(capacity * loadFactor);

        keys = new int[capacity];
        values = new Object[capacity];
    }

    /**
     * Copy construct a new map from an existing one.
     *
     * @param mapToCopy for construction.
     */
    public Int2ObjectHashMap(final Int2ObjectHashMap<V> mapToCopy)
    {
        this.loadFactor = mapToCopy.loadFactor;
        this.resizeThreshold = mapToCopy.resizeThreshold;
        this.size = mapToCopy.size;
        this.shouldAvoidAllocation = mapToCopy.shouldAvoidAllocation;

        keys = mapToCopy.keys.clone();
        values = mapToCopy.values.clone();
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
     * Get the total capacity for the map to which the load factor will be a fraction of.
     *
     * @return the total capacity for the map.
     */
    @DoNotSub public int capacity()
    {
        return values.length;
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
        final int[] keys = this.keys;
        final Object[] values = this.values;
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
        final Object val = mapNullValue(value);

        if (null != val)
        {
            @DoNotSub int remaining =
                Int2ObjectHashMap.this.size;

            final Object[] values = this.values;
            for (@DoNotSub int i = 0, length = values.length; remaining > 0 && i < length; i++)
            {
                final Object existingValue = values[i];
                if (null != existingValue)
                {
                    if (val.equals(existingValue))
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
    public V get(final int key)
    {
        return unmapNullValue(getMapped(key));
    }

    /**
     * Get mapped value without boxing the key.
     *
     * @param key to get value by.
     * @return mapped value or {@code null}.
     */
    @SuppressWarnings("unchecked")
    protected V getMapped(final int key)
    {
        final int[] keys = this.keys;
        final Object[] values = this.values;
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
     * <p>
     * Primitive specialized version of {@link java.util.Map#computeIfAbsent}.
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns null.
     * @return the value if found otherwise the default.
     */
    public V computeIfAbsent(final int key, final IntFunction<? extends V> mappingFunction)
    {
        V value = getMapped(key);
        if (null == value)
        {
            value = mappingFunction.apply(key);
            if (null != value)
            {
                put(key, value);
            }
        }
        else
        {
            value = unmapNullValue(value);
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
        final V val = (V)mapNullValue(value);
        requireNonNull(val, "value cannot be null");

        final int[] keys = this.keys;
        final Object[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

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
    public V remove(final int key)
    {
        final int[] keys = this.keys;
        final Object[] values = this.values;
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

        return unmapNullValue(value);
    }

    /**
     * {@inheritDoc}
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
     * Compact the {@link Map} backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        @DoNotSub final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
        rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
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
        if (null == valueCollection)
        {
            valueCollection = new ValueCollection();
        }

        return valueCollection;
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
            sb.append(entryIterator.getIntKey()).append('=').append(unmapNullValue(entryIterator.getValue()));
            if (!entryIterator.hasNext())
            {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
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

        if (size != that.size())
        {
            return false;
        }

        final int[] keys = this.keys;
        final Object[] values = this.values;
        for (@DoNotSub int i = 0, length = values.length; i < length; i++)
        {
            final Object thisValue = values[i];
            if (null != thisValue)
            {
                final Object thatValue = that.get(keys[i]);
                if (!thisValue.equals(mapNullValue(thatValue)))
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

        final int[] keys = this.keys;
        final Object[] values = this.values;
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

    /**
     * Primitive specialised version of {@link #replace(Object, Object)}
     *
     * @param key   key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     * {@code null} if there was no mapping for the key.
     */
    public V replace(final int key, final V value)
    {
        V currentValue = get(key);
        if (currentValue != null)
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
    public boolean replace(final int key, final V oldValue, final V newValue)
    {
        final Object curValue = get(key);
        if (curValue == null || !Objects.equals(unmapNullValue(curValue), oldValue))
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
            throw new IllegalStateException("max capacity reached at size=" + size);
        }

        rehash(newCapacity);
    }

    private void rehash(@DoNotSub final int newCapacity)
    {
        @DoNotSub final int mask = newCapacity - 1;
        /* @DoNotSub */ resizeThreshold = (int)(newCapacity * loadFactor);

        final int[] tempKeys = new int[newCapacity];
        final Object[] tempValues = new Object[newCapacity];

        final int[] keys = this.keys;
        final Object[] values = this.values;
        for (@DoNotSub int i = 0, size = values.length; i < size; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                final int key = keys[i];
                @DoNotSub int index = Hashing.hash(key, mask);
                while (null != tempValues[index])
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

    @SuppressWarnings("FinalParameters")
    private void compactChain(@DoNotSub int deleteIndex)
    {
        final int[] keys = this.keys;
        final Object[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = deleteIndex;
        while (true)
        {
            index = ++index & mask;
            final Object value = values[index];
            if (null == value)
            {
                break;
            }

            final int key = keys[index];
            @DoNotSub final int hash = Hashing.hash(key, mask);

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

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Sets and Collections
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Set of keys which supports optionally cached iterators to avoid allocation.
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
            return Int2ObjectHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return Int2ObjectHashMap.this.containsKey(o);
        }

        /**
         * Checks if the key is contained in the map.
         *
         * @param key to check.
         * @return {@code true} if the key is contained in the map.
         */
        public boolean contains(final int key)
        {
            return Int2ObjectHashMap.this.containsKey(key);
        }

        /**
         * {@inheritDoc}
         */
        public boolean remove(final Object o)
        {
            return null != Int2ObjectHashMap.this.remove(o);
        }

        /**
         * Removes key and the corresponding value from the map.
         *
         * @param key to be removed.
         * @return {@code true} if the mapping was removed.
         */
        public boolean remove(final int key)
        {
            return null != Int2ObjectHashMap.this.remove(key);
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Int2ObjectHashMap.this.clear();
        }
    }

    /**
     * Collection of values which supports optionally cached iterators to avoid allocation.
     */
    public final class ValueCollection extends AbstractCollection<V>
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
            return Int2ObjectHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return Int2ObjectHashMap.this.containsValue(o);
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Int2ObjectHashMap.this.clear();
        }

        /**
         * {@inheritDoc}
         */
        public void forEach(final Consumer<? super V> action)
        {
            @DoNotSub int remaining =
                Int2ObjectHashMap.this.size;

            final Object[] values = Int2ObjectHashMap.this.values;
            for (@DoNotSub int i = 0, length = values.length; remaining > 0 && i < length; i++)
            {
                final Object value = values[i];
                if (null != value)
                {
                    action.accept(unmapNullValue(value));
                    --remaining;
                }
            }
        }
    }

    /**
     * Set of entries which supports access via an optionally cached iterator to avoid allocation.
     */
    public final class EntrySet extends AbstractSet<Map.Entry<Integer, V>>
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
            return Int2ObjectHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Int2ObjectHashMap.this.clear();
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
            final int key = (Integer)entry.getKey();
            final V value = getMapped(key);
            return value != null && value.equals(mapNullValue(entry.getValue()));
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

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Iterators
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Base iterator implementation that contains basic logic of traversing the element in the backing array.
     *
     * @param <T> type of elements.
     */
    abstract class AbstractIterator<T> implements Iterator<T>
    {
        @DoNotSub private int posCounter;
        @DoNotSub private int stopCounter;
        @DoNotSub private int remaining;
        boolean isPositionValid = false;

        /**
         * Position of the current element.
         *
         * @return position of the current element.
         */
        @DoNotSub protected final int position()
        {
            return posCounter & (values.length - 1);
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
         * {@inheritDoc}
         */
        public boolean hasNext()
        {
            return remaining > 0;
        }

        /**
         * Find the next element.
         *
         * @throws NoSuchElementException if no more elements.
         */
        protected final void findNext()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }

            final Object[] values = Int2ObjectHashMap.this.values;
            @DoNotSub final int mask = values.length - 1;

            for (@DoNotSub int i = posCounter - 1, stop = stopCounter; i >= stop; i--)
            {
                @DoNotSub final int index = i & mask;
                if (null != values[index])
                {
                    posCounter = i;
                    isPositionValid = true;
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
        public abstract T next();

        /**
         * {@inheritDoc}
         */
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

        final void reset()
        {
            remaining = Int2ObjectHashMap.this.size;
            final Object[] values = Int2ObjectHashMap.this.values;
            @DoNotSub final int capacity = values.length;

            @DoNotSub int i = capacity;
            if (null != values[capacity - 1])
            {
                for (i = 0; i < capacity; i++)
                {
                    if (null == values[i])
                    {
                        break;
                    }
                }
            }

            stopCounter = i;
            posCounter = i + capacity;
            isPositionValid = false;
        }
    }

    /**
     * Iterator over values.
     */
    public final class ValueIterator extends AbstractIterator<V>
    {
        /**
         * {@inheritDoc}
         */
        public V next()
        {
            findNext();

            return unmapNullValue(values[position()]);
        }
    }

    /**
     * Iterator over keys which supports access to unboxed keys via {@link #nextInt()}.
     */
    public final class KeyIterator extends AbstractIterator<Integer>
    {
        /**
         * {@inheritDoc}
         */
        public Integer next()
        {
            return nextInt();
        }

        /**
         * Return next key without boxing.
         *
         * @return next key.
         */
        public int nextInt()
        {
            findNext();

            return keys[position()];
        }
    }

    /**
     * Iterator over entries which supports access to unboxed keys via {@link #getIntKey()}.
     */
    public final class EntryIterator
        extends AbstractIterator<Entry<Integer, V>>
        implements Entry<Integer, V>
    {
        /**
         * {@inheritDoc}
         */
        public Entry<Integer, V> next()
        {
            findNext();
            if (shouldAvoidAllocation)
            {
                return this;
            }

            return allocateDuplicateEntry();
        }

        private Entry<Integer, V> allocateDuplicateEntry()
        {
            return new MapEntry(getIntKey(), getValue());
        }

        /**
         * {@inheritDoc}
         */
        public Integer getKey()
        {
            return getIntKey();
        }

        /**
         * Get key without boxing.
         *
         * @return key.
         */
        public int getIntKey()
        {
            return keys[position()];
        }

        /**
         * {@inheritDoc}
         */
        public V getValue()
        {
            return unmapNullValue(values[position()]);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public V setValue(final V value)
        {
            final V val = (V)mapNullValue(value);
            requireNonNull(val, "value cannot be null");

            if (!this.isPositionValid)
            {
                throw new IllegalStateException();
            }

            @DoNotSub final int pos = position();
            final Object[] values = Int2ObjectHashMap.this.values;
            final Object oldValue = values[pos];
            values[pos] = val;

            return (V)oldValue;
        }

        /**
         * An {@link java.util.Map.Entry} implementation.
         */
        public final class MapEntry implements Entry<Integer, V>
        {
            private final int k;
            private final V v;

            /**
             * @param k key.
             * @param v value.
             */
            public MapEntry(final int k, final V v)
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
            public V getValue()
            {
                return v;
            }

            /**
             * {@inheritDoc}
             */
            public V setValue(final V value)
            {
                return Int2ObjectHashMap.this.put(k, value);
            }

            /**
             * {@inheritDoc}
             */
            @DoNotSub public int hashCode()
            {
                return Integer.hashCode(getIntKey()) ^ (v != null ? v.hashCode() : 0);
            }

            /**
             * {@inheritDoc}
             */
            public boolean equals(final Object o)
            {
                if (!(o instanceof Map.Entry))
                {
                    return false;
                }

                final Entry<?, ?> e = (Entry<?, ?>)o;

                return (e.getKey() != null && e.getKey().equals(k)) &&
                    ((e.getValue() == null && v == null) || e.getValue().equals(v));
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
}
