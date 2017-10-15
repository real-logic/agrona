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

import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.agrona.generation.DoNotSub;

/**
 * {@link java.util.Map} implementation specialised for int values using open addressing and
 * linear probing for cache efficient access. the implementation is mirror copy of {@link org.agrona.collections.Int2ObjectHashMap}
 * and it also relies on missing value concept from {@link org.agrona.collections.Int2IntHashMap}
 *
 * @param <K> type of keys stored in the {@link java.util.Map}
 */
public class Object2IntHashMap<K>
    implements Map<K, Integer>, Serializable
{
    @DoNotSub private static final int MIN_CAPACITY = 8;

    private final float loadFactor;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size;

    private int[] values;
    private K[] keys;

    private final ValueCollection valueCollection;
    private final KeySet<K> keySet;
    private final EntrySet<K> entrySet;
    private final int missingValue;

    public Object2IntHashMap(final int missingValue)
    {
        this(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, missingValue);
    }

    /**
     * Construct a new map allowing a configuration for initial capacity and load factor.
     *
     * @param initialCapacity for the backing array
     * @param loadFactor      limit for resizing on puts
     * @param missingValue    value to be used as a null marker in the map
     */
    @SuppressWarnings("unchecked")
    public Object2IntHashMap(
        @DoNotSub final int initialCapacity,
        final float loadFactor, final int missingValue)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        /* @DoNotSub */ final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));
        /* @DoNotSub */ resizeThreshold = (int)(capacity * loadFactor);

        this.missingValue = missingValue;
        keys = (K[])new Object[capacity];
        values = new int[capacity];
        Arrays.fill(values, missingValue);

        // Cached to avoid allocation.
        valueCollection = new ValueCollection();
        keySet = new KeySet<>();
        entrySet = new EntrySet<>();
    }

    /**
     * Copy construct a new map from an existing one.
     *
     * @param mapToCopy for construction.
     */
    public Object2IntHashMap(final Object2IntHashMap<K> mapToCopy)
    {
        this.loadFactor = mapToCopy.loadFactor;
        this.resizeThreshold = mapToCopy.resizeThreshold;
        this.size = mapToCopy.size;
        this.missingValue = mapToCopy.missingValue;

        keys = mapToCopy.keys.clone();
        values = mapToCopy.values.clone();

        // Cached to avoid allocation.
        valueCollection = new ValueCollection();
        keySet = new KeySet<>();
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
     * Overloaded version of {@link Map#containsKey(Object)} that takes a primitive int key.
     *
     * @param key for indexing the {@link Map}
     * @return true if the key is found otherwise false.
     */
    public boolean containsKey(final Object key)
    {
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        boolean found = false;
        while (missingValue != values[index])
        {
            if (key.equals(keys[index]))
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
        final int valueInt = ((Integer)value).intValue();
        if (valueInt == missingValue)
        {
            return false;
        }
        boolean found = false;
        if (missingValue != valueInt)
        {
            for (final int v : values)
            {
                if (valueInt == v)
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
    @SuppressWarnings("unchecked")
    public Integer get(final Object key)
    {
        return getValue((K)key);
    }

    /**
     * Overloaded version of {@link Map#get(Object)} that takes a primitive int key.
     * Due to type erasure have to rename the method
     *
     * @param key for indexing the {@link Map}
     * @return the value if found otherwise missingValue
     */
    public int getValue(final K key)
    {
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        int value;
        while (missingValue != (value = values[index]))
        {
            if (key.equals(keys[index]))
            {
                break;
            }

            index = ++index & mask;
        }

        return value;
    }

    /**
     * Get a value for a given key, or if it does not exist then default the value via a
     * {@link java.util.function.IntFunction} and put it in the map.
     * <p>
     * Primitive specialized version of {@link java.util.Map#computeIfAbsent}.
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns missingValue.
     * @return the value if found otherwise the default.
     */
    public int computeIfAbsent(final K key, final ToIntFunction<? super K> mappingFunction)
    {
        int value = getValue(key);
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

    /**
     * {@inheritDoc}
     */
    public Integer put(final K key, final Integer value)
    {
        return put(key, value.intValue());
    }

    /**
     * Overloaded version of {@link Map#put(Object, Object)} that takes a primitive int key.
     *
     * @param key   for indexing the {@link Map}
     * @param value to be inserted in the {@link Map}
     * @return the previous value if found otherwise missingValue
     */
    public int put(final K key, final int value)
    {
        if (value == missingValue)
        {
            throw new IllegalArgumentException("Cannot accept missingValue");
        }


        int oldValue = missingValue;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        while (missingValue != values[index])
        {
            if (key.equals(keys[index]))
            {
                oldValue = values[index];
                break;
            }

            index = ++index & mask;
        }

        if (missingValue == oldValue)
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
    @SuppressWarnings("unchecked")
    public Integer remove(final Object key)
    {
        return removeKey(((K)key));
    }

    /**
     * Overloaded version of {@link Map#remove(Object)} that takes a primitive int key.
     * Due to type erasure have to rename the method
     *
     * @param key for indexing the {@link Map}
     * @return the value if found otherwise missingValue
     */
    public int removeKey(final K key)
    {
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(key, mask);

        int value;
        while (missingValue != (value = values[index]))
        {
            if (key.equals(keys[index]))
            {
                values[index] = missingValue;
                --size;

                compactChain(index);
                break;
            }

            index = ++index & mask;
        }

        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        size = 0;
        Arrays.fill(values, missingValue);
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
    public void putAll(final Map<? extends K, ? extends Integer> map)
    {
        for (final Entry<? extends K, ? extends Integer> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<K> keySet()
    {
        return keySet;
    }

    /**
     * {@inheritDoc}
     */
    public ValueCollection values()
    {
        return valueCollection;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Entry<K, Integer>> entrySet()
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
            final int value = values[i];
            if (missingValue != value)
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
            final int thisValue = values[i];
            if (missingValue != thisValue)
            {
                final Object thatValueObject = that.get(keys[i]);
                if (!(thatValueObject instanceof Integer))
                {
                    return false;
                }
                final int thatValue = ((Integer)thatValueObject).intValue();
                if (missingValue == thatValue || thisValue != thatValue)
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
                result += (keys[i].hashCode() ^ Integer.hashCode(value.hashCode()));
            }
        }

        return result;
    }

    /**
     * Primitive specialised version of {@link #replace(Object, Object)}
     *
     * @param key   key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     * {@code null} if there was no mapping for the key.
     */
    public int replace(final K key, final int value)
    {
        int curValue = getValue(key);
        if (curValue != missingValue)
        {
            curValue = put(key, value);
        }

        return curValue;
    }

    /**
     * Primitive specialised version of {@link #replace(Object, Object, Object)}
     *
     * @param key      key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     */
    public boolean replace(final K key, final int oldValue, final int newValue)
    {
        final int curValue = getValue(key);
        if (curValue == missingValue || curValue != oldValue)
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

        @SuppressWarnings("unchecked")
        final K[] tempKeys = (K[])new Object[newCapacity];
        final int[] tempValues = new int[newCapacity];
        Arrays.fill(tempValues, missingValue);

        for (@DoNotSub int i = 0, size = values.length; i < size; i++)
        {
            final int value = values[i];
            if (missingValue != value)
            {
                final K key = keys[i];
                @DoNotSub int newHash = Hashing.hash(key, mask);
                while (missingValue != tempValues[newHash])
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
            if (missingValue == values[index])
            {
                break;
            }

            @DoNotSub final int hash = Hashing.hash(keys[index], mask);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                keys[deleteIndex] = keys[index];
                values[deleteIndex] = values[index];

                values[index] = missingValue;
                deleteIndex = index;
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Internal Sets and Collections
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private class KeySet<T> extends AbstractSet<T> implements Serializable
    {
        private final KeyIterator<T> iterator = new KeyIterator<>();

        @DoNotSub public int size()
        {
            return Object2IntHashMap.this.size();
        }

        public boolean contains(final Object o)
        {
            return Object2IntHashMap.this.containsKey(o);
        }

        public KeyIterator<T> iterator()
        {
            iterator.reset();

            return iterator;
        }

        @SuppressWarnings("unchecked")
        public boolean remove(final Object o)
        {
            return missingValue != Object2IntHashMap.this.removeKey((K)o);
        }

        public void clear()
        {
            Object2IntHashMap.this.clear();
        }
    }

    public class ValueCollection extends AbstractCollection<Integer> implements Serializable
    {
        private final ValueIterator iterator = new ValueIterator();

        @DoNotSub public int size()
        {
            return Object2IntHashMap.this.size();
        }

        public boolean contains(final Object o)
        {
            return Object2IntHashMap.this.containsValue(o);
        }

        public ValueIterator iterator()
        {
            iterator.reset();

            return iterator;
        }

        public void clear()
        {
            Object2IntHashMap.this.clear();
        }
    }

    private class EntrySet<T> extends AbstractSet<Entry<T, Integer>> implements Serializable
    {
        private final EntryIterator<T> iterator = new EntryIterator<>();

        @DoNotSub public int size()
        {
            return Object2IntHashMap.this.size();
        }

        public Iterator<Entry<T, Integer>> iterator()
        {
            iterator.reset();

            return iterator;
        }

        public void clear()
        {
            Object2IntHashMap.this.clear();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Iterators
    ///////////////////////////////////////////////////////////////////////////////////////////////

    abstract class AbstractIterator<T> implements Iterator<T>, Serializable
    {
        @DoNotSub private int posCounter;
        @DoNotSub private int stopCounter;
        @DoNotSub private int remaining;
        private boolean isPositionValid = false;
        protected Object[] keys;
        protected int[] values;

        protected AbstractIterator()
        {
            reset();
        }

        @DoNotSub protected int position()
        {
            return posCounter & (values.length - 1);
        }

        public boolean hasNext()
        {
            return remaining > 0;
        }

        protected void findNext()
        {
            @DoNotSub final int mask = values.length - 1;
            isPositionValid = false;

            for (@DoNotSub int i = posCounter - 1; i >= stopCounter; i--)
            {
                @DoNotSub final int index = i & mask;
                if (missingValue != values[index])
                {
                    posCounter = i;
                    isPositionValid = true;
                    --remaining;
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
                values[position] = missingValue;
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
            remaining = Object2IntHashMap.this.size;
            keys = Object2IntHashMap.this.keys;
            values = Object2IntHashMap.this.values;
            @DoNotSub final int capacity = values.length;

            @DoNotSub int i = capacity;
            if (missingValue != values[capacity - 1])
            {
                for (i = 0; i < capacity; i++)
                {
                    if (missingValue == values[i])
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

    public class ValueIterator extends AbstractIterator<Integer>
    {
        public Integer next()
        {
            return nextInt();
        }

        public int nextInt()
        {
            findNext();

            return values[position()];
        }
    }

    public class KeyIterator<T> extends AbstractIterator<T>
    {
        @SuppressWarnings("unchecked")
        public T next()
        {
            findNext();

            return (T)keys[position()];
        }
    }

    @SuppressWarnings("unchecked")
    public class EntryIterator<T>
        extends AbstractIterator<Entry<T, Integer>>
        implements Entry<T, Integer>
    {
        public Entry<T, Integer> next()
        {
            findNext();

            return this;
        }

        public T getKey()
        {
            return (T)keys[position()];
        }

        public Integer getValue()
        {
            return values[position()];
        }

        public Integer setValue(final Integer value)
        {
            return setValue(value.intValue());
        }

        public int setValue(final int value)
        {
            if (value == missingValue)
            {
                throw new IllegalArgumentException("Cannot accept missingValue");
            }

            @DoNotSub final int pos = position();
            final int oldValue = values[pos];
            values[pos] = value;

            return oldValue;
        }
    }
}
