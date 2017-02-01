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

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;

import static java.util.stream.Collectors.joining;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * Open-addressing with linear-probing expandable hash set. Allocation free in steady state use when expanded.
 *
 * By storing elements as int primitives this significantly reduces memory consumption compared with Java's builtin
 * <code>HashSet&lt;Integer&gt;</code>. It implements <code>Set&lt;Integer&gt;</code> for convenience, but calling
 * functionality via those methods can add boxing overhead to your usage.
 *
 * Not Threadsafe.
 *
 * This HashSet caches its iterator object, so nested iteration is not supported.
 *
 * @see IntIterator
 * @see Set
 */
public final class IntHashSet extends AbstractSet<Integer>
{
    /**
     * The load factor used when none is specified in the constructor.
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.67f;

    /**
     * The initial capacity used when none is specified in the constructor.
     */
    @DoNotSub public static final int DEFAULT_INITIAL_CAPACITY = 8;

    static final int MISSING_VALUE = -1;

    private final float loadFactor;
    @DoNotSub private int resizeThreshold;
    // NB: excludes missing value
    @DoNotSub private int sizeOfArrayValues;

    private int[] values;
    private final IntIterator iterator = new IntHashSetIterator();
    private boolean containsMissingValue;

    public IntHashSet()
    {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public IntHashSet(
        @DoNotSub final int proposedCapacity)
    {
        this(proposedCapacity, DEFAULT_LOAD_FACTOR);
    }

    public IntHashSet(
        @DoNotSub final int initialCapacity,
        final float loadFactor)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        sizeOfArrayValues = 0;
        @DoNotSub final int capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        resizeThreshold = (int)(capacity * loadFactor); // @DoNotSub
        values = new int[capacity];
        Arrays.fill(values, MISSING_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(final Integer value)
    {
        return add(value.intValue());
    }

    /**
     * Primitive specialised overload of {this#add(Integer)}
     *
     * @param value the value to add
     * @return true if the collection has changed, false otherwise
     * @throws IllegalArgumentException if value is missingValue
     */
    public boolean add(final int value)
    {
        if (value == MISSING_VALUE)
        {
            final boolean previousContainsMissingValue = this.containsMissingValue;
            containsMissingValue = true;
            return !previousContainsMissingValue;
        }

        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(value, mask);

        while (values[index] != MISSING_VALUE)
        {
            if (values[index] == value)
            {
                return false;
            }

            index = next(index, mask);
        }

        values[index] = value;
        sizeOfArrayValues++;

        if (sizeOfArrayValues > resizeThreshold)
        {
            increaseCapacity();
        }

        return true;
    }

    private void increaseCapacity()
    {
        @DoNotSub final int newCapacity = values.length * 2;
        if (newCapacity < 0)
        {
            throw new IllegalStateException("Max capacity reached at size=" + size());
        }

        rehash(newCapacity);
    }

    private void rehash(@DoNotSub final int newCapacity)
    {
        @DoNotSub final int capacity = newCapacity;
        @DoNotSub final int mask = newCapacity - 1;
        resizeThreshold = (int)(newCapacity * loadFactor); // @DoNotSub

        final int[] tempValues = new int[capacity];
        Arrays.fill(tempValues, MISSING_VALUE);

        for (final int value : values)
        {
            if (value != MISSING_VALUE)
            {
                @DoNotSub int newHash = Hashing.hash(value, mask);
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
     * {@inheritDoc}
     */
    public boolean remove(final Object value)
    {
        return value instanceof Integer && remove(((Integer)value).intValue());
    }

    /**
     * An int specialised version of {this#remove(Object)}.
     *
     * @param value the value to remove
     * @return true if the value was present, false otherwise
     */
    public boolean remove(final int value)
    {
        if (value == MISSING_VALUE)
        {
            final boolean previousContainsMissingValue = this.containsMissingValue;
            containsMissingValue = false;
            return previousContainsMissingValue;
        }

        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(value, mask);

        while (values[index] != MISSING_VALUE)
        {
            if (values[index] == value)
            {
                values[index] = MISSING_VALUE;
                compactChain(index);
                sizeOfArrayValues--;
                return true;
            }

            index = next(index, mask);
        }

        return false;
    }

    @DoNotSub private static int next(final int index, final int mask)
    {
        return (index + 1) & mask;
    }

    @DoNotSub void compactChain(int deleteIndex)
    {
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;

        @DoNotSub int index = deleteIndex;
        while (true)
        {
            index = next(index, mask);
            if (values[index] == MISSING_VALUE)
            {
                return;
            }

            @DoNotSub final int hash = Hashing.hash(values[index], mask);

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
        @DoNotSub final int idealCapacity = (int)Math.round(size() * (1.0 / loadFactor));
        rehash(BitUtil.findNextPositivePowerOfTwo(idealCapacity));
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(final Object value)
    {
        return value instanceof Integer && contains(((Integer)value).intValue());
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(final int value)
    {
        if (value == MISSING_VALUE)
        {
            return containsMissingValue;
        }

        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(value, mask);

        while (values[index] != MISSING_VALUE)
        {
            if (values[index] == value)
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
    @DoNotSub public int size()
    {
        return sizeOfArrayValues + (containsMissingValue ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return size() == 0;
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
     * {@inheritDoc}
     */
    public void clear()
    {
        Arrays.fill(values, MISSING_VALUE);
        sizeOfArrayValues = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean addAll(final Collection<? extends Integer> coll)
    {
        return disjunction(coll, this::add);
    }

    /**
     * Alias for {@link #addAll(Collection)} for the specialized case when adding another IntHashSet,
     * avoids boxing and allocations
     *
     * @param coll containing the values to be added.
     * @return <tt>true</tt> if this set changed as a result of the call
     */
    public boolean addAll(final IntHashSet coll)
    {
        boolean acc = false;

        for (final int value : coll.values)
        {
            if (value != MISSING_VALUE)
            {
                acc |= add(value);
            }
        }

        return acc;
    }

    /**
     * IntHashSet specialised variant of {this#containsAll(Collection)}.
     *
     * @param other int hash set to compare against.
     * @return true if every element in other is in this.
     */
    public boolean containsAll(final IntHashSet other)
    {
        for (final int value : other.values)
        {
            if (value != MISSING_VALUE && !contains(value))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Fast Path set difference for comparison with another IntHashSet.
     *
     * NB: garbage free in the identical case, allocates otherwise.
     *
     * @param other the other set to subtract
     * @return null if identical, otherwise the set of differences
     */
    public IntHashSet difference(final IntHashSet other)
    {
        IntHashSet difference = null;

        for (final int value : values)
        {
            if (value != MISSING_VALUE && !other.contains(value))
            {
                if (difference == null)
                {
                    difference = new IntHashSet(sizeOfArrayValues);
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
     * Alias for {@link #removeAll(Collection)} for the specialized case when removing another IntHashSet,
     * avoids boxing and allocations
     *
     * @param coll containing the values to be removed.
     * @return <tt>true</tt> if this set changed as a result of the call
     */
    public boolean removeAll(final IntHashSet coll)
    {
        boolean acc = false;

        for (final int value : coll.values)
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
    public IntIterator iterator()
    {
        iterator.reset(values, containsMissingValue);

        return iterator;
    }

    /**
     * {@inheritDoc}
     */
    public void copy(final IntHashSet that)
    {
        if (this.values.length != that.values.length)
        {
            throw new IllegalArgumentException("Cannot copy object: masks not equal");
        }

        System.arraycopy(that.values, 0, this.values, 0, this.values.length);
        this.sizeOfArrayValues = that.sizeOfArrayValues;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return
            stream()
            .map((x) -> Integer.toString(x))
            .collect(joining(",", "{", "}"));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(final T[] into)
    {
        final Class<?> componentType = into.getClass().getComponentType();
        if (!componentType.isAssignableFrom(Integer.class))
        {
            throw new ArrayStoreException("Cannot store Integers in array of type " + componentType);
        }

        @DoNotSub final int size = size();
        final T[] arrayCopy = into.length >= size ? into : (T[])Array.newInstance(componentType, size);
        copyValues(arrayCopy);

        return arrayCopy;
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray()
    {
        final Object[] arrayCopy = new Object[size()];
        copyValues(arrayCopy);
        return arrayCopy;
    }

    private void copyValues(final Object[] arrayCopy)
    {
        final IntIterator iterator = iterator();
        for (@DoNotSub int i = 0; iterator.hasNext(); i++)
        {
            arrayCopy[i] = iterator.next();
        }

        if (containsMissingValue)
        {
            arrayCopy[sizeOfArrayValues] = MISSING_VALUE;
        }
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

        if (other instanceof IntHashSet)
        {
            final IntHashSet otherSet = (IntHashSet)other;

            return otherSet.containsMissingValue == containsMissingValue &&
                   otherSet.sizeOfArrayValues == sizeOfArrayValues &&
                   containsAll(otherSet);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @DoNotSub public int hashCode()
    {
        @DoNotSub int hashCode = 0;
        for (final int value : values)
        {
            if (value != MISSING_VALUE)
            {
                hashCode = 31 * hashCode + Hashing.hash(value);
            }
        }

        hashCode = 31 * hashCode + (containsMissingValue ? 1 : 0);

        return hashCode;
    }

    public final class IntHashSetIterator extends IntIterator
    {
        public void remove()
        {
            if (isPositionValid)
            {
                @DoNotSub final int position = position();
                values[position] = MISSING_VALUE;
                --sizeOfArrayValues;

                compactChain(position);

                isPositionValid = false;
            }
            else
            {
                if (containsMissingValue)
                {
                    containsMissingValue = false;
                }
                else
                {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
