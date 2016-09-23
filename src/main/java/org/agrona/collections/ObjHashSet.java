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
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import static java.util.stream.Collectors.joining;
import static org.agrona.collections.CollectionUtil.validateLoadFactor;

/**
 * Open-addressing with linear-probing expandable hash set. Allocation free in steady state use when expanded.
 * Ability to be notified when resizing occurs so that appropriate sizing can be implemented.
 * <p>
 * Not Threadsafe.
 * <p>
 * This HashSet caches its iterator object, so nested iteration is not supported.
 *
 * @see ObjIterator
 * @see Set
 */
public final class ObjHashSet<T> implements Set<T>
{
    /**
     * The load factor used when none is specified in the constructor.
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.67f;

    /**
     * The initial capacity used when none is specified in the constructor.
     */
    @DoNotSub public static final int DEFAULT_INITIAL_CAPACITY = 8;

    private final float loadFactor;
    //Using the missingValue variable name rather than null to make the code less obtuse
    private static final Object missingValue = null;
    @DoNotSub private int resizeThreshold;
    @DoNotSub private int size;

    private T[] values;
    private final ObjIterator<T> iterator;
    private IntConsumer resizeNotifier;

    public ObjHashSet()
    {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public ObjHashSet(
        @DoNotSub final int proposedCapacity)
    {
        this(proposedCapacity, DEFAULT_LOAD_FACTOR);
    }

    public ObjHashSet(
        @DoNotSub final int initialCapacity,
        final float loadFactor)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        size = 0;
        @DoNotSub final int capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        resizeThreshold = (int)(capacity * loadFactor); // @DoNotSub
        values = (T[])new Object[capacity];
        Arrays.fill(values, missingValue);

        // NB: references values in the constructor, so must be assigned after values
        iterator = new ObjHashSetIterator(values);
    }

    /**
     * Add a Consumer that will be called when the collection is resized.
     * @param resizeNotifier IntConsumer containing the new resizeThreshold
     */
    public void setReziseNotifier(IntConsumer resizeNotifier){
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
        final T[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = value.hashCode() & mask;

        while (values[index] != missingValue)
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
            if(resizeNotifier!=null)
                resizeNotifier.accept(resizeThreshold);
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

        final T[] tempValues = (T[])new Object[capacity];
        Arrays.fill(tempValues, missingValue);

        for (final T value : values)
        {
            if (value != missingValue)
            {
                @DoNotSub int newHash = value.hashCode() & mask;
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
     * @param value the value to remove
     * @return true if the value was present, false otherwise
     */
    @Override
    public boolean remove(final Object value)
    {
        final T[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = value.hashCode() & mask;

        while (values[index] != missingValue)
        {
            if (values[index].equals(value))
            {
                values[index] = (T)missingValue;
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

    @DoNotSub void compactChain(int deleteIndex)
    {
        final T[] values = this.values;
        @DoNotSub final int mask = values.length - 1;

        @DoNotSub int index = deleteIndex;
        while (true)
        {
            index = next(index, mask);
            if (values[index] == missingValue)
            {
                return;
            }

            @DoNotSub final int hash = values[index].hashCode() &mask;

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                values[deleteIndex] = values[index];

                values[index] = (T)missingValue;
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
    @Override
    public boolean contains(final Object value)
    {
        final T[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        @DoNotSub int index = value.hashCode()& mask;

        while (values[index] != missingValue)
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
        Arrays.fill(values, missingValue);
        size = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsAll(final Collection<?> coll)
    {
        Objects.requireNonNull(coll);

        for (final Object t : coll)
        {
            if (!contains(t))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public boolean addAll(final Collection<? extends T> coll) {
        return disjunction(coll, this::add);
    }

    /**
     * Fast Path set difference for comparison with another ObjHashSet.
     * <p>
     * NB: garbage free in the identical case, allocates otherwise.
     *
     * @param other the other set to subtract
     * @return null if identical, otherwise the set of differences
     */
    public ObjHashSet difference(final ObjHashSet other)
    {
        Objects.requireNonNull(other);

        ObjHashSet difference = null;

        for (final T value : values)
        {
            if (value != missingValue && !other.contains(value))
            {
                if (difference == null)
                {
                    difference = new ObjHashSet(size);
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

    private static <T> boolean disjunction(final Collection<T> coll, final Predicate<T> predicate)
    {
        Objects.requireNonNull(coll);

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
    public ObjIterator iterator()
    {
        iterator.reset();

        return iterator;
    }

    /**
     * {@inheritDoc}
     */
    public void copy(final ObjHashSet that)
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
            .map((x) -> x.toString())
            .collect(joining(",", "{", "}"));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(final T[] into)
    {
        Objects.requireNonNull(into, "into");

        final Class<?> componentType = into.getClass().getComponentType();

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
        final ObjIterator iterator = iterator();
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

        if (other instanceof ObjHashSet)
        {
            final ObjHashSet otherSet = (ObjHashSet)other;

            return otherSet.size == size
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

    private final class ObjHashSetIterator extends ObjIterator
    {
        private ObjHashSetIterator(final T[] values)
        {
            super(values);
        }

        public void remove()
        {
            if (isPositionValid)
            {
                @DoNotSub final int position = position();
                values[position] = (T)missingValue;
                --size;

                compactChain(position);

                isPositionValid = false;
            }
            else
            {
                throw new IllegalStateException();
            }
        }
    }

    // --- Unimplemented below here

    public boolean retainAll(final Collection<?> coll)
    {
        throw new UnsupportedOperationException("Not implemented");
    }
}
