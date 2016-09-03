/*
 * Copyright 2016 Real Logic Ltd.
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
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/**
 * A {@link List} implementation that stores int values with the ability to have them not boxed.
 */
public class IntArrayList extends AbstractList<Integer> implements List<Integer>, RandomAccess
{
    public static final int DEFAULT_NULL_VALUE = Integer.MIN_VALUE;
    @DoNotSub public static final int DEFAULT_INITIAL_CAPACITY = 10;
    @DoNotSub public static final int MAX_SIZE = Integer.MAX_VALUE - 8;

    private final int nullValue;
    @DoNotSub private int size = 0;
    private int[] elements;

    public IntArrayList()
    {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_NULL_VALUE);
    }

    /**
     * Construct a new list.
     *
     * @param initialCapacity for the backing array.
     * @param nullValue       to be used to represent a null element.
     */
    public IntArrayList(
        @DoNotSub final int initialCapacity,
        final int nullValue)
    {
        this.nullValue = nullValue;
        elements = new int[initialCapacity];
    }

    /**
     * The value representing a null element.
     *
     * @return value representing a null element.
     */
    public int nullValue()
    {
        return nullValue;
    }

    public @DoNotSub int size()
    {
        return size;
    }

    public void clear()
    {
        size = 0;
    }

    /**
     * Trim the underlying array to be the same capacity as the current size.
     */
    public void trimToSize()
    {
        if (elements.length != size)
        {
            elements = Arrays.copyOf(elements, size);
        }
    }

    public Integer get(
        @DoNotSub final int index)
    {
        final int value = getInt(index);

        return value == nullValue ? null : value;
    }

    /**
     * Get the element at a given index without boxing.
     *
     * @param index to get.
     * @return the unboxed element.
     */
    public int getInt(
        @DoNotSub final int index)
    {
        checkIndex(index);

        return elements[index];
    }

    public boolean add(final Integer element)
    {
        return addInt(null == element ? nullValue : element);
    }

    /**
     * Add an element without boxing.
     *
     * @param element to be added.
     * @return true
     */
    public boolean addInt(final int element)
    {
        ensureCapacity(size + 1);

        elements[size] = element;
        size++;

        return true;
    }

    public void add(
        @DoNotSub final int index,
        final Integer element)
    {
        addInt(index, null == element ? nullValue : element);
    }

    /**
     * Add a element without boxing at a given index.
     *
     * @param index   at which the element should be added.
     * @param element to be added.
     */
    public void addInt(
        @DoNotSub final int index,
        final int element)
    {
        checkIndexForAdd(index);

        @DoNotSub final int requiredSize = size + 1;
        ensureCapacity(requiredSize);

        if (index < size)
        {
            System.arraycopy(elements, index, elements, index + 1, requiredSize - index);
        }

        elements[index] = element;
        size++;
    }

    public Integer set(
        @DoNotSub final int index,
        final Integer element)
    {
        final int previous = setInt(index, null == element ? nullValue : element);

        return nullValue == previous ? null : previous;
    }

    /**
     * Set an element at a given index without boxing.
     *
     * @param index   at which to set the element.
     * @param element to be added.
     * @return the previous element at the index.
     */
    public int setInt(
        @DoNotSub final int index,
        final int element)
    {
        checkIndex(index);

        final int previous = elements[index];
        elements[index] = element;

        return previous;
    }

    /**
     * Does the list contain this element value.
     *
     * @param value of the element.
     * @return true if present otherwise false.
     */
    public boolean containsInt(final int value)
    {
        return -1 != indexOf(value);
    }

    /**
     * Index of the first element with this value.
     *
     * @param value for the element.
     * @return the index if found otherwise -1.
     */
    public int indexOf(final int value)
    {
        for (@DoNotSub int i = 0; i < size; i++)
        {
            if (value == elements[i])
            {
                return i;
            }
        }

        return -1;
    }

    /**
     * Index of the last element with this value.
     *
     * @param value for the element.
     * @return the index if found otherwise -1.
     */
    public int lastIndexOf(final int value)
    {
        for (@DoNotSub int i = size - 1; i >= 0; i--)
        {
            if (value == elements[i])
            {
                return i;
            }
        }

        return -1;
    }

    /**
     * Remove at a given index.
     *
     * @param index of the element to be removed.
     * @return the existing value at this index.
     */
    public Integer remove(
        @DoNotSub final int index)
    {
        checkIndex(index);

        final int value = elements[index];
        System.arraycopy(elements, index + 1, elements, index, size - index - 1);
        size--;

        return value;
    }

    /**
     * For each element in order provide the int value to a {@link IntConsumer}.
     *
     * @param consumer for each element.
     */
    public void forEachOrderedInt(final IntConsumer consumer)
    {
        for (@DoNotSub int i = 0; i < size; i++)
        {
            consumer.accept(elements[i]);
        }
    }

    /**
     * Create a {@link IntStream} over the elements of underlying array.
     *
     * @return a {@link IntStream} over the elements of underlying array.
     */
    public IntStream intStream()
    {
        return Arrays.stream(elements, 0, size);
    }

    /**
     * Create a new array that is a copy of the elements.
     *
     * @return a copy of the elements.
     */
    public int[] toIntArray()
    {
        return Arrays.copyOf(elements, size);
    }

    /**
     * Create a new array that is a copy of the elements.
     *
     * @param dst destination array for the copy if it is the correct size.
     * @return a copy of the elements.
     */
    public int[] toIntArray(final int[] dst)
    {
        if (dst.length == size)
        {
            System.arraycopy(elements, 0, dst, 0, dst.length);
            return dst;
        }
        else
        {
            return Arrays.copyOf(elements, size);
        }
    }

    private void checkIndex(@DoNotSub final int index)
    {
        if (index >= size || index < 0)
        {
            throw new IndexOutOfBoundsException("index=" + index + " size=" + size);
        }
    }

    private void checkIndexForAdd(@DoNotSub final int index)
    {
        if (index > size || index < 0)
        {
            throw new IndexOutOfBoundsException("index=" + index + " size=" + size);
        }
    }

    private void ensureCapacity(@DoNotSub final int requiredCapacity)
    {
        @DoNotSub final int capacity = elements.length;
        if (requiredCapacity > capacity)
        {
            @DoNotSub int newCapacity = capacity * 2;
            if (newCapacity < 0)
            {
                if (MAX_SIZE == capacity)
                {
                    throw new IndexOutOfBoundsException("Max size reached: " + MAX_SIZE);
                }

                newCapacity = MAX_SIZE;
            }

            final int[] newElements = new int[newCapacity];

            System.arraycopy(elements, 0, newElements, 0, capacity);
            elements = newElements;
        }
    }
}
