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
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/**
 * A {@link List} implementation that stores int values with the ability to not have them boxed.
 */
public class IntArrayList extends AbstractList<Integer> implements List<Integer>, RandomAccess
{
    /**
     * The default value that will be used in place of null for an element.
     */
    public static final int DEFAULT_NULL_VALUE = Integer.MIN_VALUE;

    /**
     * Initial capacity to which the array will be sized.
     */
    @DoNotSub public static final int INITIAL_CAPACITY = 10;

    private final int nullValue;
    @DoNotSub private int size = 0;
    private int[] elements;

    /**
     * Constructs a new list with the {@link #INITIAL_CAPACITY} using {@link #DEFAULT_NULL_VALUE}.
     */
    public IntArrayList()
    {
        this(INITIAL_CAPACITY, DEFAULT_NULL_VALUE);
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
        elements = new int[Math.max(initialCapacity, INITIAL_CAPACITY)];
    }

    /**
     * Create a new list that wraps an existing arrays without copying it.
     *
     * @param initialElements to be wrapped.
     * @param initialSize     of the array to wrap.
     * @param nullValue       to be used to represent a null element.
     */
    public IntArrayList(
        final int[] initialElements,
        @DoNotSub final int initialSize,
        final int nullValue)
    {
        wrap(initialElements, initialSize);
        this.nullValue = nullValue;
    }

    /**
     * Wrap an existing array without copying it.
     *
     * @param initialElements to be wrapped.
     * @param initialSize     of the array to wrap.
     * @throws IllegalArgumentException if the initialSize is less than 0 or greater than the length of the
     *                                  initial array.
     */
    public void wrap(
        final int[] initialElements,
        final @DoNotSub int initialSize)
    {
        if (initialSize < 0 || initialSize > initialElements.length)
        {
            throw new IllegalArgumentException(
                "illegal initial size " + initialSize + " for array length of " + initialElements.length);
        }

        elements = initialElements;
        size = initialSize;
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

    /**
     * {@inheritDoc}
     */
    public @DoNotSub int size()
    {
        return size;
    }

    /**
     * The current capacity for the collection.
     *
     * @return the current capacity for the collection.
     */
    @DoNotSub public int capacity()
    {
        return elements.length;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        size = 0;
    }

    /**
     * Trim the underlying array to be the current size, or {@link #INITIAL_CAPACITY} if size is less.
     */
    public void trimToSize()
    {
        if (elements.length != size && elements.length > INITIAL_CAPACITY)
        {
            elements = Arrays.copyOf(elements, Math.max(INITIAL_CAPACITY, size));
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
        ensureCapacityPrivate(size + 1);

        elements[size] = element;
        size++;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void add(
        @DoNotSub final int index,
        final Integer element)
    {
        addInt(index, null == element ? nullValue : element);
    }

    /**
     * Add an element without boxing at a given index.
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
        ensureCapacityPrivate(requiredSize);

        if (index < size)
        {
            System.arraycopy(elements, index, elements, index + 1, size - index);
        }

        elements[index] = element;
        size++;
    }

    /**
     * {@inheritDoc}
     */
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
     * {@inheritDoc}
     */
    public boolean contains(final Object o)
    {
        return containsInt((int)o);
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
    public @DoNotSub int indexOf(
        final int value)
    {
        final int[] elements = this.elements;
        for (@DoNotSub int i = 0, size = this.size; i < size; i++)
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
    public @DoNotSub int lastIndexOf(
        final int value)
    {
        final int[] elements = this.elements;
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
     * Appends all the elements in the specified list to the end of this list, in the order that they are stored in the
     * specified list.
     *
     * @param list containing elements to be added to this list.
     * @return {@code true} if this list changed as a result of the call.
     */
    public boolean addAll(final IntArrayList list)
    {
        @DoNotSub final int numElements = list.size;
        if (numElements > 0)
        {
            ensureCapacityPrivate(size + numElements);
            System.arraycopy(list.elements, 0, elements, size, numElements);
            size += numElements;
            return true;
        }
        return false;
    }

    /**
     * Inserts all the elements from the specified list to this list at the specified position. Shifts the element
     * currently at that position (if any) and any subsequent elements to the right (increases their indices). The new
     * elements will appear in this list in the order that they are stored in the specified list.
     *
     * @param index at which to insert the first element from the specified collection.
     * @param list  containing elements to be added to this list.
     * @return {@code true} if this list changed as a result of the call.
     */
    public boolean addAll(
        @DoNotSub final int index,
        final IntArrayList list)
    {
        checkIndexForAdd(index);

        @DoNotSub final int numElements = list.size;
        if (numElements > 0)
        {
            @DoNotSub final int size = this.size;
            ensureCapacityPrivate(size + numElements);
            final int[] elements = this.elements;
            for (@DoNotSub int i = size - 1; i >= index; i--)
            {
                elements[i + numElements] = elements[i];
            }
            System.arraycopy(list.elements, 0, elements, index, numElements);
            this.size += numElements;
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this list contains all the elements of the specified list.
     *
     * @param list to be checked for containment in this list.
     * @return {@code true} if this list contains all the elements of the specified list.
     */
    public boolean containsAll(final IntArrayList list)
    {
        return super.containsAll(list);
    }

    /**
     * Retains only the elements in this list that are contained in the specified list. In other words, removes from
     * this list all of its elements that are not contained in the specified list.
     *
     * @param list containing elements to be removed from this list.
     * @return {@code true} if this list changed as a result of the call.
     */
    public boolean retainAll(final IntArrayList list)
    {
        return super.retainAll(list);
    }

    /**
     * Removes all of this collection's elements that are also contained in the specified list. After this call
     * returns, this list will contain no elements in common with the specified list.
     *
     * @param list whose elements are to be removed from this list.
     * @return {@code true} if this list changed as a result of the call.
     */
    public boolean removeAll(final IntArrayList list)
    {
        return super.removeAll(list);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Object o)
    {
        return removeInt((int)o);
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
        return removeAt(index);
    }

    /**
     * Remove at a given index.
     *
     * @param index of the element to be removed.
     * @return the existing value at this index.
     */
    public int removeAt(
        @DoNotSub final int index)
    {
        checkIndex(index);

        final int value = elements[index];

        @DoNotSub final int moveCount = size - index - 1;
        if (moveCount > 0)
        {
            System.arraycopy(elements, index + 1, elements, index, moveCount);
        }

        size--;

        return value;
    }

    /**
     * Removes element at index, but instead of copying all elements to the left,
     * it replaces the item in the slot with the last item in the list. This avoids the copy
     * costs at the expense of preserving list order. If index is the last element it is just removed.
     *
     * @param index of the element to be removed.
     * @return the existing value at this index.
     * @throws IndexOutOfBoundsException if index is out of bounds.
     */
    public int fastUnorderedRemove(
        @DoNotSub final int index)
    {
        checkIndex(index);

        final int value = elements[index];
        elements[index] = elements[--size];

        return value;
    }

    /**
     * Remove the first instance of a value if found in the list.
     * <p>
     * Primitive specialization of the {@link List#remove(Object)} method.
     *
     * @param value to be removed.
     * @return true if successful otherwise false.
     */
    public boolean removeInt(final int value)
    {
        @DoNotSub final int index = indexOf(value);
        if (-1 != index)
        {
            removeAt(index);

            return true;
        }

        return false;
    }

    /**
     * Remove the first instance of a value if found in the list and replaces it with the last item
     * in the list. This saves a copy down of all items at the expense of not preserving list order.
     *
     * @param value to be removed.
     * @return true if successful otherwise false.
     */
    public boolean fastUnorderedRemoveInt(final int value)
    {
        @DoNotSub final int index = indexOf(value);
        if (-1 != index)
        {
            elements[index] = elements[--size];

            return true;
        }

        return false;
    }

    /**
     * Push an element onto the end of the array like a stack.
     *
     * @param element to be pushed onto the end of the array.
     */
    public void pushInt(final int element)
    {
        ensureCapacityPrivate(size + 1);

        elements[size] = element;
        size++;
    }

    /**
     * Pop a value off the end of the array as a stack operation.
     *
     * @return the value at the end of the array.
     * @throws NoSuchElementException if the array is empty.
     */
    public int popInt()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException();
        }

        return elements[--size];
    }

    /**
     * For each element in order provide the int value to a {@link IntConsumer}.
     *
     * @param action to be taken for each element.
     */
    public void forEachOrderedInt(final IntConsumer action)
    {
        final int[] elements = this.elements;
        for (@DoNotSub int i = 0, size = this.size; i < size; i++)
        {
            action.accept(elements[i]);
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

    /**
     * Ensure the backing array has a required capacity.
     *
     * @param requiredCapacity for the backing array.
     */
    public void ensureCapacity(@DoNotSub final int requiredCapacity)
    {
        ensureCapacityPrivate(Math.max(requiredCapacity, INITIAL_CAPACITY));
    }

    /**
     * Type-safe overload of the {@link #equals(Object)} method.
     *
     * @param that other list.
     * @return {@code true} if lists are equal.
     */
    public boolean equals(final IntArrayList that)
    {
        if (that == this)
        {
            return true;
        }

        boolean isEqual = false;

        final int size = this.size;
        if (size == that.size)
        {
            isEqual = true;

            final int[] elements = this.elements;
            final int[] thatElements = that.elements;
            for (@DoNotSub int i = 0; i < size; i++)
            {
                final int thisValue = elements[i];
                final int thatValue = thatElements[i];

                if (thisValue != thatValue)
                {
                    if (thisValue != this.nullValue || thatValue != that.nullValue)
                    {
                        isEqual = false;
                        break;
                    }
                }
            }
        }

        return isEqual;
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

        boolean isEqual = false;

        if (other instanceof IntArrayList)
        {
            return equals((IntArrayList)other);
        }
        else if (other instanceof List)
        {
            final List<?> that = (List<?>)other;

            if (size == that.size())
            {
                isEqual = true;
                @DoNotSub int i = 0;

                for (final Object o : that)
                {
                    if (null == o || o instanceof Integer)
                    {
                        final Integer thisValue = get(i++);
                        final Integer thatValue = (Integer)o;

                        if (Objects.equals(thisValue, thatValue))
                        {
                            continue;
                        }
                    }

                    isEqual = false;
                    break;
                }
            }
        }

        return isEqual;
    }

    /**
     * {@inheritDoc}
     */
    @DoNotSub public int hashCode()
    {
        @DoNotSub int hashCode = 1;
        final int nullValue = this.nullValue;
        final int[] elements = this.elements;
        for (@DoNotSub int i = 0, size = this.size; i < size; i++)
        {
            final int value = elements[i];
            hashCode = 31 * hashCode + (nullValue == value ? 0 : Integer.hashCode(value));
        }

        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    public void forEach(final Consumer<? super Integer> action)
    {
        final int nullValue = this.nullValue;
        final int[] elements = this.elements;
        for (@DoNotSub int i = 0, size = this.size; i < size; i++)
        {
            final int value = elements[i];
            action.accept(nullValue != value ? value : null);
        }
    }

    /**
     * Iterate over the collection without boxing.
     *
     * @param action to be taken for each element.
     */
    public void forEachInt(final IntConsumer action)
    {
        final int[] elements = this.elements;
        for (@DoNotSub int i = 0, size = this.size; i < size; i++)
        {
            action.accept(elements[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');

        final int nullValue = this.nullValue;
        final int[] elements = this.elements;
        for (@DoNotSub int i = 0, size = this.size; i < size; i++)
        {
            final int value = elements[i];
            sb.append(value != nullValue ? value : null).append(", ");
        }

        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 2);
        }

        sb.append(']');

        return sb.toString();
    }

    private void ensureCapacityPrivate(@DoNotSub final int requiredCapacity)
    {
        @DoNotSub final int currentCapacity = elements.length;
        if (requiredCapacity > currentCapacity)
        {
            if (requiredCapacity > ArrayUtil.MAX_CAPACITY)
            {
                throw new IllegalStateException("max capacity: " + ArrayUtil.MAX_CAPACITY);
            }

            @DoNotSub int newCapacity = currentCapacity > INITIAL_CAPACITY ? currentCapacity : INITIAL_CAPACITY;

            while (newCapacity < requiredCapacity)
            {
                newCapacity = newCapacity + (newCapacity >> 1);

                if (newCapacity < 0 || newCapacity >= ArrayUtil.MAX_CAPACITY)
                {
                    newCapacity = ArrayUtil.MAX_CAPACITY;
                }
            }

            final int[] newElements = new int[newCapacity];
            System.arraycopy(elements, 0, newElements, 0, currentCapacity);
            elements = newElements;
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
}
