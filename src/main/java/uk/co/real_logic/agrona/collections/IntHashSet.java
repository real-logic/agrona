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
package uk.co.real_logic.agrona.collections;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.Verify;
import uk.co.real_logic.agrona.generation.DoNotSub;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;

import static java.util.stream.Collectors.joining;
import static uk.co.real_logic.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * Open-addressing with linear-probing expandable hash set. Allocation free in steady state use when expanded.
 * <p>
 * By storing elements as int primitives this significantly reduces memory consumption compared with Java's builtin
 * <code>HashSet&lt;Integer&gt;</code>. It implements <code>Set&lt;Integer&gt;</code> for convenience, but calling
 * functionality via those methods can add boxing overhead to your usage.
 * <p>
 * Not Threadsafe.
 * <p>
 * This HashSet caches its iterator object, so nested iteration is not supported.
 *
 * @see IntIterator
 * @see Set
 */
public final class IntHashSet implements Set<Integer>
{
    /**
     * The load factor used when none is specified in the constructor.
     */
    public static final double DEFAULT_LOAD_FACTOR = 0.67;

    /**
     * The initial capacity used when none is specified in the constructor.
     */
    @DoNotSub public static final int DEFAULT_INITIAL_CAPACITY = 8;

    private final double loadFactor;
    private final int missingValue;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size;

    private int[] values;
    private final IntIterator iterator;

    public IntHashSet(final int missingValue)
    {
        this(DEFAULT_INITIAL_CAPACITY, missingValue);
    }

    public IntHashSet(
        @DoNotSub final int proposedCapacity,
        final int missingValue)
    {
        this(proposedCapacity, missingValue, DEFAULT_LOAD_FACTOR);
    }

    public IntHashSet(
        @DoNotSub final int initialCapacity,
        final int missingValue,
        final double loadFactor)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        size = 0;
        this.missingValue = missingValue;
        @DoNotSub final int capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        resizeThreshold = (int)(capacity * loadFactor); // @DoNotSub
        values = new int[capacity];
        Arrays.fill(values, missingValue);

        // NB: references values in the constructor, so must be assigned after values
        iterator = new IntIterator(missingValue, values);
    }

    /**
     * The value to be used as a null marker in the set.
     *
     * @return value to be used as a null marker in the set.
     */
    public int missingValue()
    {
        return missingValue;
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
     */
    public boolean add(final int value)
    {
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(value, mask);

        while (values[index] != missingValue)
        {
            if (values[index] == value)
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
        }

        return true;
    }

    private void increaseCapacity()
    {
        @DoNotSub final int newCapacity = values.length * 2;
        if (newCapacity < 0)
        {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        rehash(newCapacity);
    }

    private void rehash(@DoNotSub final int newCapacity)
    {
        @DoNotSub final int capacity = newCapacity;
        @DoNotSub final int mask = newCapacity - 1;
        resizeThreshold = (int)(newCapacity * loadFactor); // @DoNotSub

        final int[] tempValues = new int[capacity];
        final int missingValue = this.missingValue;
        Arrays.fill(tempValues, missingValue);

        for (final int value : values)
        {
            if (value != missingValue)
            {
                @DoNotSub int newHash = Hashing.hash(value, mask);
                while (tempValues[newHash] != missingValue)
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
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(value, mask);

        while (values[index] != missingValue)
        {
            if (values[index] == value)
            {
                values[index] = missingValue;
                compactChain(index);
                size--;
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

    @DoNotSub private void compactChain(int deleteIndex)
    {
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;

        @DoNotSub int index = deleteIndex;
        while (true)
        {
            index = next(index, mask);
            if (values[index] == missingValue)
            {
                return;
            }

            @DoNotSub final int hash = Hashing.hash(values[index], mask);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                values[deleteIndex] = values[index];

                values[index] = missingValue;
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
        @DoNotSub final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
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
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = Hashing.hash(value, mask);

        while (values[index] != missingValue)
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
     * Get the load factor beyond which the set will increase size.
     *
     * @return load factor for when the set should increase size.
     */
    public double loadFactor()
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
        Arrays.fill(values, missingValue);
        size = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean addAll(final Collection<? extends Integer> coll)
    {
        return conjunction(coll, (x) -> add(x));
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsAll(final Collection<?> coll)
    {
        return conjunction(coll, this::contains);
    }

    /**
     * IntHashSet specialised variant of {this#containsAll(Collection)}.
     *
     * @param other int hash set to compare against.
     * @return true if every element in other is in this.
     */
    public boolean containsAll(final IntHashSet other)
    {
        boolean containsAll = true;

        final int missingValue = this.missingValue;
        for (final int value : values)
        {
            if (value != missingValue && !other.contains(value))
            {
                containsAll = false;
                break;
            }
        }

        return containsAll;
    }

    /**
     * Fast Path set difference for comparison with another IntHashSet.
     * <p>
     * NB: garbage free in the identical case, allocates otherwise.
     *
     * @param other the other set to subtract
     * @return null if identical, otherwise the set of differences
     */
    public IntHashSet difference(final IntHashSet other)
    {
        Objects.requireNonNull(other);

        IntHashSet difference = null;

        final int missingValue = this.missingValue;
        for (final int value : values)
        {
            if (value != missingValue && !other.contains(value))
            {
                if (difference == null)
                {
                    difference = new IntHashSet(size, missingValue);
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
        return conjunction(coll, this::remove);
    }

    private static <T> boolean conjunction(final Collection<T> collection, final Predicate<T> predicate)
    {
        Objects.requireNonNull(collection);

        boolean acc = false;
        for (final T t : collection)
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
        iterator.reset();

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

        if (this.missingValue != that.missingValue)
        {
            throw new IllegalArgumentException("Cannot copy object: missingValues not equal");
        }

        System.arraycopy(that.values, 0, this.values, 0, this.values.length);
        this.size = that.size;
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
        Verify.notNull(into, "into");

        final Class<?> componentType = into.getClass().getComponentType();
        if (!componentType.isAssignableFrom(Integer.class))
        {
            throw new ArrayStoreException("Cannot store Integers in array of type " + componentType);
        }

        @DoNotSub final int size = this.size;
        final T[] arrayCopy = into.length >= size ? into : (T[])Array.newInstance(componentType, size);
        copyValues(arrayCopy);

        return arrayCopy;
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray()
    {
        final Object[] arrayCopy = new Object[size];
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

            return otherSet.missingValue == missingValue
                && otherSet.size == size
                && containsAll(otherSet);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @DoNotSub public int hashCode()
    {
        return Arrays.hashCode(values);
    }

    // --- Unimplemented below here

    public boolean retainAll(final Collection<?> coll)
    {
        throw new UnsupportedOperationException("Not implemented");
    }
}
