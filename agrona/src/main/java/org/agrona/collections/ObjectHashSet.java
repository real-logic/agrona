/*
 * Copyright 2014-2019 Real Logic Ltd.
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

import java.io.Serializable;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * Open-addressing with linear-probing expandable hash set. Allocation free in steady state use when expanded.
 * Ability to be notified when resizing occurs so that appropriate sizing can be implemented.
 * <p>
 * Not Threadsafe.
 * <p>
 * This HashSet caches its iterator object by default, which can be overridden, so nested iteration is not supported.
 * You can override this behaviour at construction by indicating that the iterator should not be cached.
 *
 * @param <T> type of values stored in the {@link java.util.Set}
 * @see ObjectIterator
 * @see Set
 */
public class ObjectHashSet<T> extends AbstractSet<T> implements Serializable
{
    /**
     * The initial capacity used when none is specified in the constructor.
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 8;

    static final Object MISSING_VALUE = null;

    private final boolean shouldAvoidAllocation;
    private final float loadFactor;
    private int resizeThreshold;
    private int size;

    private T[] values;
    private ObjectIterator iterator;
    private IntConsumer resizeNotifier;

    /**
     * Construct a hash set with {@link #DEFAULT_INITIAL_CAPACITY}, {@link Hashing#DEFAULT_LOAD_FACTOR},
     * and iterator caching support.
     */
    public ObjectHashSet()
    {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Construct a hash set with a proposed initial capacity, {@link Hashing#DEFAULT_LOAD_FACTOR},
     * and iterator caching support.
     *
     * @param proposedCapacity for the initial capacity of the set.
     */
    public ObjectHashSet(final int proposedCapacity)
    {
        this(proposedCapacity, Hashing.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Construct a hash set with a proposed initial capacity, load factor, and iterator caching support.
     *
     * @param proposedCapacity for the initial capacity of the set.
     * @param loadFactor       to be used for resizing.
     */
    public ObjectHashSet(final int proposedCapacity, final float loadFactor)
    {
        this(proposedCapacity, loadFactor, true);
    }

    /**
     * Construct a hash set with a proposed initial capacity, load factor, and indicated iterator caching support.
     *
     * @param proposedCapacity      for the initial capacity of the set.
     * @param loadFactor            to be used for resizing.
     * @param shouldAvoidAllocation should the iterator be cached to avoid further allocation.
     */
    @SuppressWarnings("unchecked")
    public ObjectHashSet(final int proposedCapacity, final float loadFactor, final boolean shouldAvoidAllocation)
    {
        validateLoadFactor(loadFactor);

        this.shouldAvoidAllocation = shouldAvoidAllocation;
        this.loadFactor = loadFactor;
        size = 0;

        final int capacity = findNextPositivePowerOfTwo(Math.max(DEFAULT_INITIAL_CAPACITY, proposedCapacity));
        resizeThreshold = (int)(capacity * loadFactor);
        values = (T[])new Object[capacity];
    }

    /**
     * Get the load factor beyond which the set will increase size.
     *
     * @return load factor for when the set should increase size.
     */
    public float loadFactor()
    {
        return loadFactor;
    }

    /**
     * Get the total capacity for the set to which the load factor with be a fraction of.
     *
     * @return the total capacity for the set.
     */
    public int capacity()
    {
        return values.length;
    }

    /**
     * Get the actual threshold which when reached the map will resize.
     * This is a function of the current capacity and load factor.
     *
     * @return the threshold when the map will resize.
     */
    public int resizeThreshold()
    {
        return resizeThreshold;
    }

    /**
     * Add a Consumer that will be called when the collection is re-sized.
     *
     * @param resizeNotifier IntConsumer containing the new resizeThreshold
     */
    public void resizeNotifier(final IntConsumer resizeNotifier)
    {
        this.resizeNotifier = resizeNotifier;
    }

    /**
     * @param value the value to add
     * @return true if the collection has changed, false otherwise
     * @throws NullPointerException if the value is null
     */
    public boolean add(final T value)
    {
        Objects.requireNonNull(value);
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = value.hashCode() & mask;

        while (values[index] != MISSING_VALUE)
        {
            if (values[index].equals(value))
            {
                return false;
            }

            index = next(index, mask);
        }

        values[index] = value;
        size++;

        if (size > resizeThreshold)
        {
            increaseCapacity();
            if (resizeNotifier != null)
            {
                resizeNotifier.accept(resizeThreshold);
            }
        }

        return true;
    }

    private void increaseCapacity()
    {
        final int newCapacity = values.length * 2;
        if (newCapacity < 0)
        {
            throw new IllegalStateException("max capacity reached at size=" + size);
        }

        rehash(newCapacity);
    }

    @SuppressWarnings("unchecked")
    private void rehash(final int newCapacity)
    {
        final int mask = newCapacity - 1;
        resizeThreshold = (int)(newCapacity * loadFactor);

        final T[] tempValues = (T[])new Object[newCapacity];
        Arrays.fill(tempValues, MISSING_VALUE);

        for (final T value : values)
        {
            if (value != MISSING_VALUE)
            {
                int newHash = value.hashCode() & mask;
                while (tempValues[newHash] != MISSING_VALUE)
                {
                    newHash = ++newHash & mask;
                }

                tempValues[newHash] = value;
            }
        }

        values = tempValues;
    }

    /**
     * @param value the value to remove
     * @return true if the value was present, false otherwise
     */
    public boolean remove(final Object value)
    {
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = value.hashCode() & mask;

        while (values[index] != MISSING_VALUE)
        {
            if (values[index].equals(value))
            {
                values[index] = MISSING_VALUE;
                compactChain(index);
                size--;
                return true;
            }

            index = next(index, mask);
        }

        return false;
    }

    private static int next(final int index, final int mask)
    {
        return (index + 1) & mask;
    }

    @SuppressWarnings("FinalParameters")
    void compactChain(int deleteIndex)
    {
        final Object[] values = this.values;
        final int mask = values.length - 1;

        int index = deleteIndex;
        while (true)
        {
            index = next(index, mask);
            if (values[index] == MISSING_VALUE)
            {
                return;
            }

            final int hash = values[index].hashCode() & mask;

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                values[deleteIndex] = values[index];

                values[index] = MISSING_VALUE;
                deleteIndex = index;
            }
        }
    }

    /**
     * Compact the backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        final int idealCapacity = (int)Math.round(size() * (1.0 / loadFactor));
        rehash(findNextPositivePowerOfTwo(Math.max(DEFAULT_INITIAL_CAPACITY, idealCapacity)));
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(final Object value)
    {
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = value.hashCode() & mask;

        while (values[index] != MISSING_VALUE)
        {
            if (values[index].equals(value))
            {
                return true;
            }

            index = next(index, mask);
        }

        return false;
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
        return size == 0;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        if (size > 0)
        {
            Arrays.fill(values, MISSING_VALUE);
            size = 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsAll(final Collection<?> coll)
    {
        for (final Object t : coll)
        {
            if (!contains(t))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean addAll(final Collection<? extends T> coll)
    {
        return disjunction(coll, this::add);
    }

    /**
     * Alias for {@link #addAll(Collection)} for the specialized case when adding another ObjectHashSet,
     * avoids boxing and allocations
     *
     * @param coll containing the values to be added.
     * @return {@code true} if this set changed as a result of the call
     */
    public boolean addAll(final ObjectHashSet<T> coll)
    {
        boolean acc = false;

        for (final T value : coll.values)
        {
            if (value != MISSING_VALUE)
            {
                acc |= add(value);
            }
        }

        return acc;
    }

    /**
     * Fast Path set difference for comparison with another ObjectHashSet.
     * <p>
     * NB: garbage free in the identical case, allocates otherwise.
     *
     * @param other the other set to subtract
     * @return null if identical, otherwise the set of differences
     */
    public ObjectHashSet<T> difference(final ObjectHashSet<T> other)
    {
        ObjectHashSet<T> difference = null;

        for (final T value : values)
        {
            if (value != MISSING_VALUE && !other.contains(value))
            {
                if (difference == null)
                {
                    difference = new ObjectHashSet<>(size);
                }

                difference.add(value);
            }
        }

        return difference;
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeAll(final Collection<?> coll)
    {
        return disjunction(coll, this::remove);
    }

    /**
     * Alias for {@link #removeAll(Collection)} for the specialized case when removing another ObjectHashSet,
     * avoids boxing and allocations
     *
     * @param coll containing the values to be removed.
     * @return {@code true} if this set changed as a result of the call
     */
    public boolean removeAll(final ObjectHashSet<T> coll)
    {
        boolean acc = false;

        for (final T value : coll.values)
        {
            if (value != MISSING_VALUE)
            {
                acc |= remove(value);
            }
        }

        return acc;
    }

    private static <T> boolean disjunction(final Collection<T> coll, final Predicate<T> predicate)
    {
        boolean acc = false;
        for (final T t : coll)
        {
            // Deliberate strict evaluation
            acc |= predicate.test(t);
        }

        return acc;
    }

    /**
     * {@inheritDoc}
     */
    public ObjectIterator iterator()
    {
        ObjectIterator iterator = this.iterator;
        if (null == iterator)
        {
            iterator = new ObjectIterator();

            if (shouldAvoidAllocation)
            {
                this.iterator = iterator;
            }
        }

        return iterator.reset();
    }

    public void copy(final ObjectHashSet<T> that)
    {
        if (this.values.length != that.values.length)
        {
            throw new IllegalArgumentException("cannot copy object: lengths not equal");
        }

        System.arraycopy(that.values, 0, this.values, 0, this.values.length);
        this.size = that.size;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');

        for (final Object value : values)
        {
            if (value != MISSING_VALUE)
            {
                sb.append(value).append(", ");
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
    public boolean equals(final Object other)
    {
        if (other == this)
        {
            return true;
        }

        if (other instanceof ObjectHashSet)
        {
            final ObjectHashSet otherSet = (ObjectHashSet)other;

            return otherSet.size == size && containsAll(otherSet);
        }

        if (!(other instanceof Set))
        {
            return false;
        }

        final Set<?> c = (Set<?>)other;
        if (c.size() != size())
        {
            return false;
        }

        try
        {
            return containsAll(c);
        }
        catch (final ClassCastException | NullPointerException ignore)
        {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        int hashCode = 0;

        for (final Object value : values)
        {
            if (value != MISSING_VALUE)
            {
                hashCode += value.hashCode();
            }
        }

        return hashCode;
    }

    public final class ObjectIterator implements Iterator<T>, Serializable
    {
        private int remaining;
        private int positionCounter;
        private int stopCounter;
        private boolean isPositionValid = false;

        ObjectIterator reset()
        {
            this.remaining = size;
            final T[] values = ObjectHashSet.this.values;
            final int length = values.length;
            int i = length;

            if (values[length - 1] != MISSING_VALUE)
            {
                i = 0;
                for (; i < length; i++)
                {
                    if (values[i] == MISSING_VALUE)
                    {
                        break;
                    }
                }
            }

            stopCounter = i;
            positionCounter = i + length;
            isPositionValid = false;

            return this;
        }

        public int remaining()
        {
            return remaining;
        }

        public boolean hasNext()
        {
            return remaining > 0;
        }

        public T next()
        {
            return nextValue();
        }

        /**
         * @return the next int value.
         */
        public T nextValue()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }

            final T[] values = ObjectHashSet.this.values;
            final int mask = values.length - 1;
            isPositionValid = false;

            for (int i = positionCounter - 1; i >= stopCounter; i--)
            {
                final int index = i & mask;
                final T value = values[index];
                if (value != MISSING_VALUE)
                {
                    positionCounter = i;
                    isPositionValid = true;
                    --remaining;

                    return value;
                }
            }

            throw new IllegalStateException();
        }

        @SuppressWarnings("unchecked")
        public void remove()
        {
            if (isPositionValid)
            {
                final T[] values = ObjectHashSet.this.values;
                final int position = position(values);
                values[position] = (T)MISSING_VALUE;
                --size;

                compactChain(position);

                isPositionValid = false;
            }
            else
            {
                throw new IllegalStateException();
            }
        }

        private int position(final T[] values)
        {
            return positionCounter & (values.length - 1);
        }
    }
}
