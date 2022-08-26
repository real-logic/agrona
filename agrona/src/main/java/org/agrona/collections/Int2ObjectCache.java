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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;
import static org.agrona.collections.CollectionUtil.validatePositivePowerOfTwo;

/**
 * A cache implementation specialised for int keys using open addressing to probe a set of fixed size.
 * <p>
 * The eviction strategy is to remove the oldest in a set if the key is not found, or if found then that item.
 * The newly inserted item becomes the youngest in the set. Sets are evicted on a first in, first out, manner unless
 * replacing a matching key.
 * <p>
 * A good set size would be in the range of 2 to 16 so that the references/keys can fit in a cache-line (assuming
 * references are 32-bit references and 64-byte cache lines, YMMV). A linear search within a cache line is much
 * less costly than a cache-miss to another line.
 * <p>
 * Null values are not supported by this cache.
 *
 * @param <V> type of values stored in the {@link Map}
 */
public class Int2ObjectCache<V> implements Map<Integer, V>
{
    private long cachePuts = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;

    @DoNotSub private int size;
    @DoNotSub private final int capacity;
    @DoNotSub private final int setSize;
    @DoNotSub private final int setSizeShift;
    @DoNotSub private final int mask;

    private final int[] keys;
    private final Object[] values;
    private final Consumer<V> evictionConsumer;

    private ValueCollection valueCollection;
    private KeySet keySet;
    private EntrySet entrySet;

    /**
     * Constructs cache with provided configuration.
     *
     * @param numSets          number of sets, must be power or two.
     * @param setSize          size of a single set, must be power or two.
     * @param evictionConsumer consumer to be notified when entry is being evicted from the cache.
     */
    public Int2ObjectCache(
        @DoNotSub final int numSets,
        @DoNotSub final int setSize,
        final Consumer<V> evictionConsumer)
    {
        validatePositivePowerOfTwo(numSets);
        validatePositivePowerOfTwo(setSize);
        requireNonNull(evictionConsumer, "null values are not permitted");

        if (((long)numSets) * setSize > (Integer.MAX_VALUE - 8))
        {
            throw new IllegalArgumentException(
                "total capacity must be <= max array size: numSets=" + numSets + " setSize=" + setSize);
        }

        this.setSize = setSize;
        this.setSizeShift = Integer.numberOfTrailingZeros(setSize);
        capacity = numSets << setSizeShift;
        mask = numSets - 1;

        keys = new int[capacity];
        values = new Object[capacity];
        this.evictionConsumer = evictionConsumer;
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
     * Get the total capacity for the map to which the load factor will be a fraction of.
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
        return containsKey((int)key);
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
        @DoNotSub final int setBeginIndex = setNumber << setSizeShift;

        final int[] keys = this.keys;
        final Object[] values = this.values;
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
            final Object[] values = this.values;
            for (final Object v : values)
            {
                if (Objects.equals(v, value))
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
        return get((int)key);
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
        @DoNotSub final int setNumber = Hashing.hash(key, mask);
        @DoNotSub final int setBeginIndex = setNumber << setSizeShift;

        final int[] keys = this.keys;
        final Object[] values = this.values;
        for (@DoNotSub int i = setBeginIndex, setEndIndex = setBeginIndex + setSize; i < setEndIndex; i++)
        {
            final Object value = values[i];
            if (null == value)
            {
                break;
            }

            if (key == keys[i])
            {
                cacheHits++;
                return (V)value;
            }
        }

        cacheMisses++;
        return null;
    }

    /**
     * Returns the value to which the specified key is mapped, or defaultValue if this map contains no mapping for the
     * key.
     *
     * @param key          whose associated value is to be returned.
     * @param defaultValue the default mapping of the key.
     * @return the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key.
     */
    public V getOrDefault(final int key, final V defaultValue)
    {
        final V value = get(key);
        return null != value ? value : defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public void forEach(final BiConsumer<? super Integer, ? super V> action)
    {
        forEachInt(action::accept);
    }

    /**
     * Implementation of the {@link #forEach(BiConsumer)} that avoids boxing of keys.
     *
     * @param consumer to be called for each key/value pair.
     */
    @SuppressWarnings("unchecked")
    public void forEachInt(final IntObjConsumer<? super V> consumer)
    {
        requireNonNull(consumer);
        final int[] keys = this.keys;
        final Object[] values = this.values;
        @DoNotSub final int length = values.length;
        @DoNotSub int remaining = size;

        for (@DoNotSub int index = 0; remaining > 0 && index < length; index++)
        {
            final Object value = values[index];
            if (null != value)
            {
                consumer.accept(keys[index], (V)value);
                --remaining;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public V computeIfAbsent(final Integer key, final Function<? super Integer, ? extends V> mappingFunction)
    {
        return computeIfAbsent((int)key, mappingFunction::apply);
    }

    /**
     * Get a value for a given key, or if it does ot exist then default the value via a
     * {@link java.util.function.IntFunction} and put it in the  cache.
     * <p>
     * Primitive specialized version of {@link Map#computeIfAbsent}.
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns null.
     * @return the value if found otherwise the default.
     */
    public V computeIfAbsent(final int key, final IntFunction<? extends V> mappingFunction)
    {
        requireNonNull(mappingFunction);
        V value = get(key);
        if (null == value)
        {
            value = mappingFunction.apply(key);
            if (null != value)
            {
                put(key, value);
            }
        }

        return value;
    }

    /**
     * {@inheritDoc}
     */
    public V computeIfPresent(
        final Integer key, final BiFunction<? super Integer, ? super V, ? extends V> remappingFunction)
    {
        return computeIfPresent((int)key, remappingFunction::apply);
    }

    /**
     * If the value for the specified key is present attempts to compute a new mapping given the key and its current
     * mapped value.
     * <p>
     * Primitive specialized version of {@link Map#computeIfPresent(Object, BiFunction)}.
     *
     * @param key               with which the specified value is to be associated.
     * @param remappingFunction the function to compute a value.
     * @return the new value associated with the specified key, or null if none.
     */
    public V computeIfPresent(
        final int key, final IntObjectToObjectFunction<? super V, ? extends V> remappingFunction)
    {
        requireNonNull(remappingFunction);
        final V oldValue = get(key);
        if (null != oldValue)
        {
            final V newValue = remappingFunction.apply(key, oldValue);
            if (null != newValue)
            {
                put(key, newValue);
                return newValue;
            }
            else
            {
                remove(key);
                return null;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public V compute(final Integer key, final BiFunction<? super Integer, ? super V, ? extends V> remappingFunction)
    {
        return compute((int)key, remappingFunction::apply);
    }

    /**
     * Attempts to compute a mapping for the specified key and its current mapped value (or {@code null} if there is no
     * current mapping).
     * <p>
     * Primitive specialized version of {@link Map#compute(Object, BiFunction)}.
     *
     * @param key               with which the specified value is to be associated.
     * @param remappingFunction the function to compute a value.
     * @return the new value associated with the specified key, or null if none.
     */
    public V compute(final int key, final IntObjectToObjectFunction<? super V, ? extends V> remappingFunction)
    {
        final V oldValue = get(key);
        final V newValue = remappingFunction.apply(key, oldValue);
        if (null != newValue)
        {
            put(key, newValue);
            return newValue;
        }
        else
        {
            if (null != oldValue)
            {
                remove(key);
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public V merge(
        final Integer key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        return merge((int)key, value, remappingFunction);
    }

    /**
     * If the specified key is not already associated with a value or is associated with null, associates it with the
     * given non-null value. Otherwise, replaces the associated value with the results of the given remapping function,
     * or removes if the result is {@code null}.
     * <p>
     * Primitive specialized version of {@link Map#merge(Object, Object, BiFunction)}.
     *
     * @param key               with which the resulting value is to be associated.
     * @param value             the non-null value to be merged with the existing value associated with the key or,
     *                          if no existing value or a null value is associated with the key, to be associated with
     *                          the key.
     * @param remappingFunction the function to recompute a value if present.
     * @return the new value associated with the specified key, or null if no
     * value is associated with the key.
     */
    public V merge(final int key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        requireNonNull(value);
        requireNonNull(remappingFunction);
        final V oldValue = get(key);
        final V newValue = null == oldValue ? value : remappingFunction.apply(oldValue, value);
        if (null != newValue)
        {
            put(key, newValue);
            return newValue;
        }
        else
        {
            remove(key);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public V putIfAbsent(final Integer key, final V value)
    {
        return putIfAbsent((int)key, value);
    }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associates it with the given value and returns
     * {@code null}, else returns the current value.
     * <p>
     * Primitive specialized version of {@link Map#putIfAbsent(Object, Object)}.
     *
     * @param key with which the specified value is to be associated.
     * @param value to be associated with the specified key.
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     */
    public V putIfAbsent(final int key, final V value)
    {
        final V existingValue = get(key);
        if (null == existingValue)
        {
            put(key, value);
            return null;
        }
        return existingValue;
    }

    /**
     * {@inheritDoc}
     */
    public V put(final Integer key, final V value)
    {
        return put((int)key, value);
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

        @DoNotSub final int setNumber = Hashing.hash(key, mask);
        @DoNotSub final int setBeginIndex = setNumber << setSizeShift;
        @DoNotSub int i = setBeginIndex;

        final Object[] values = this.values;
        final int[] keys = this.keys;
        Object evictedValue = null;
        for (@DoNotSub int nextSetIndex = setBeginIndex + setSize; i < nextSetIndex; i++)
        {
            evictedValue = values[i];
            if (null == evictedValue)
            {
                break;
            }

            if (key == keys[i])
            {
                shuffleUp(i, nextSetIndex - 1);

                break;
            }
        }

        if (null == evictedValue)
        {
            evictedValue = values[setBeginIndex + (setSize - 1)];
        }

        shuffleDown(setBeginIndex);

        keys[setBeginIndex] = key;
        values[setBeginIndex] = value;

        cachePuts++;

        if (null != evictedValue)
        {
            evictionConsumer.accept((V)evictedValue);
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
    @SuppressWarnings("unchecked")
    public boolean remove(final Object key, final Object value)
    {
        return remove((int)key, (V)value);
    }

    /**
     * Removes the entry for the specified key only if it is currently mapped to the specified value.
     * <p>
     * Primitive specialized version of {@link Map#remove(Object, Object)}.
     *
     * @param key key with which the specified value is associated.
     * @param value expected to be associated with the specified key.
     * @return {@code true} if the value was removed.
     */
    public boolean remove(final int key, final V value)
    {
        final V existingValue = get(key);
        if (null != existingValue && Objects.equals(existingValue, value))
        {
            remove(key);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public V remove(final Object key)
    {
        return remove((int)key);
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
        @DoNotSub final int setNumber = Hashing.hash(key, mask);
        @DoNotSub final int setBeginIndex = setNumber << setSizeShift;

        final int[] keys = this.keys;
        final Object[] values = this.values;
        Object value = null;
        for (@DoNotSub int i = setBeginIndex, nextSetIndex = setBeginIndex + setSize; i < nextSetIndex; i++)
        {
            value = values[i];
            if (null == value)
            {
                break;
            }

            if (key == keys[i])
            {
                shuffleUp(i, nextSetIndex - 1);
                --size;

                evictionConsumer.accept((V)value);
                break;
            }
        }

        return (V)value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(final Integer key, final V oldValue, final V newValue)
    {
        return replace((int)key, oldValue, newValue);
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     * <p>
     * Primitive specialized version of {@link Map#replace(Object, Object, Object)}.
     *
     * @param key with which the specified value is associated.
     * @param oldValue expected to be associated with the specified key.
     * @param newValue to be associated with the specified key.
     * @return {@code true} if the value was replaced.
     */
    public boolean replace(final int key, final V oldValue, final V newValue)
    {
        final V existingValue = get(key);
        if (null != existingValue && Objects.equals(existingValue, oldValue))
        {
            put(key, newValue);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public V replace(final Integer key, final V value)
    {
        return replace((int)key, value);
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     * <p>
     * Primitive specialized version of {@link Map#replace(Object, Object)}.
     *
     * @param key with which the specified value is associated.
     * @param value to be associated with the specified key.
     * @return the previous value associated with the specified key.
     */
    public V replace(final int key, final V value)
    {
        final V oldValue = get(key);
        if (null != oldValue)
        {
            put(key, value);
        }
        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    public void replaceAll(final BiFunction<? super Integer, ? super V, ? extends V> function)
    {
        replaceAllInt(function::apply);
    }

    /**
     * Replaces each entry's value with the result of invoking the given function on that entry until all entries have
     * been processed or the function throws an exception.
     * <p>
     * Primitive specialized version of {@link Map#replaceAll(BiFunction)}.
     * <p>
     * NB: Renamed from forEach to avoid overloading on parameter types of lambda
     * expression, which doesn't play well with type inference in lambda expressions.
     *
     * @param function the function to apply to each entry.
     */
    @SuppressWarnings("unchecked")
    public void replaceAllInt(final IntObjectToObjectFunction<? super V, ? extends V> function)
    {
        requireNonNull(function);
        final int[] keys = this.keys;
        final Object[] values = this.values;
        @DoNotSub final int length = values.length;
        @DoNotSub int remaining = size;

        for (@DoNotSub int index = 0; remaining > 0 && index < length; index++)
        {
            final Object oldValue = values[index];
            if (null != oldValue)
            {
                final V newValue = function.apply(keys[index], (V)oldValue);
                requireNonNull(newValue, "null values are not supported");
                values[index] = newValue;
                --remaining;
            }
        }
    }

    @DoNotSub private void shuffleUp(final int fromIndex, final int toIndex)
    {
        final int[] keys = this.keys;
        final Object[] values = this.values;
        values[toIndex] = null;

        for (@DoNotSub int i = fromIndex; i < toIndex; i++)
        {
            values[i] = values[i + 1];
            keys[i] = keys[i + 1];
        }
    }

    @DoNotSub private void shuffleDown(final int setBeginIndex)
    {
        final int[] keys = this.keys;
        final Object[] values = this.values;
        for (@DoNotSub int i = setBeginIndex + (setSize - 1); i > setBeginIndex; i--)
        {
            values[i] = values[i - 1];
            keys[i] = keys[i - 1];
        }

        values[setBeginIndex] = null;
    }

    /**
     * Clear down all items in the cache.
     * <p>
     * If an exception occurs during the eviction function callback then clear may need to be called again to complete.
     * <p>
     * If an exception occurs the cache should only be used when {@link #size()} reports zero.
     */
    @SuppressWarnings("unchecked")
    public void clear()
    {
        final Object[] values = this.values;
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
        final StringBuilder sb = new StringBuilder();
        sb.append('{');

        final int[] keys = this.keys;
        final Object[] values = this.values;
        for (@DoNotSub int i = 0, length = values.length; i < length; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                sb.append(keys[i]).append('=').append(value).append(", ");
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
                if (!thisValue.equals(thatValue))
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

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Sets and Collections
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * A key set implementation which supports cached iterator to avoid allocation.
     */
    public final class KeySet extends AbstractSet<Integer>
    {
        private final KeyIterator iterator = new KeyIterator();

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int size()
        {
            return Int2ObjectCache.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return Int2ObjectCache.this.containsKey(o);
        }

        /**
         * Check if the given key contained in the set without boxing.
         *
         * @param key to be checked.
         * @return {@code true} if key is contained in the cache.
         */
        public boolean contains(final int key)
        {
            return Int2ObjectCache.this.containsKey(key);
        }

        /**
         * {@inheritDoc}
         */
        public KeyIterator iterator()
        {
            iterator.reset();

            return iterator;
        }

        /**
         * {@inheritDoc}
         */
        public boolean remove(final Object o)
        {
            throw new UnsupportedOperationException("Cannot remove on iterator");
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Int2ObjectCache.this.clear();
        }
    }

    /**
     * Collection of values which supports cached iterator to avoid allocation.
     */
    public final class ValueCollection extends AbstractCollection<V>
    {
        private final ValueIterator iterator = new ValueIterator();

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int size()
        {
            return Int2ObjectCache.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return Int2ObjectCache.this.containsValue(o);
        }

        /**
         * {@inheritDoc}
         */
        public ValueIterator iterator()
        {
            iterator.reset();

            return iterator;
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Int2ObjectCache.this.clear();
        }
    }

    /**
     * Set of entries which supports cached iterator to avoid allocation.
     */
    public final class EntrySet extends AbstractSet<Map.Entry<Integer, V>>
    {
        private final EntryIterator iterator = new EntryIterator();

        /**
         * {@inheritDoc}
         */
        @DoNotSub public int size()
        {
            return Int2ObjectCache.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public EntryIterator iterator()
        {
            iterator.reset();

            return iterator;
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Int2ObjectCache.this.clear();
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
        @DoNotSub private int remaining;
        @DoNotSub private int position = -1;

        /**
         * Position of the current element.
         *
         * @return position of the current element.
         */
        @DoNotSub protected final int position()
        {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext()
        {
            return remaining > 0;
        }

        /**
         * Find next element.
         */
        protected final void findNext()
        {
            boolean found = false;
            final Object[] values = Int2ObjectCache.this.values;

            for (@DoNotSub int i = position + 1, size = capacity; i < size; i++)
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

        /**
         * {@inheritDoc}
         */
        public abstract T next();

        /**
         * {@inheritDoc}
         */
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

    /**
     * An iterator over values.
     */
    public final class ValueIterator extends AbstractIterator<V>
    {
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public V next()
        {
            findNext();
            return (V)values[position()];
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

            return this;
        }

        /**
         * {@inheritDoc}
         */
        public Integer getKey()
        {
            return getIntKey();
        }

        /**
         * Get key of the current entry.
         *
         * @return key of the current entry.
         */
        public int getIntKey()
        {
            return keys[position()];
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public V getValue()
        {
            return (V)values[position()];
        }

        /**
         * {@inheritDoc}
         */
        public V setValue(final V value)
        {
            throw new UnsupportedOperationException("no set on this iterator");
        }
    }
}
