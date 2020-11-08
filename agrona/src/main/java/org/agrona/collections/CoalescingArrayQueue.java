/*
 * Copyright 2014-2020 Real Logic Limited.
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

import org.agrona.BitUtil;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Basic queue that coalesces duplicate entries without loss of time priority.
 * <p>
 * The {@link ValueIterator} is cached by default to avoid allocation unless directed to do so in the constructor.
 * <p>
 * <b>Note:</b> This class is not threadsafe.
 * <p>
 * Implementation:
 * Bulk of the code replicates {@link IntArrayQueue } to create an optimum queue implementation.
 * The coalescing function is managed via queued keys from a key supplier mapped to the latest value instance.
 *
 * @param <V>
 */
public class CoalescingArrayQueue<V> extends AbstractQueue<V>
{
    /**
     * Supplies a unique Key/Id for an instance of the queues object type.
     *
     * @param <V> type of the value.
     */
    public interface KeySupplier<V>
    {
        /**
         * key supplier.
         * @param v value instance.
         * @return unique id for instance.
         */
        int getKey(V v);
    }

    /**
     * Minimum capacity for the queue which must also be a power of 2.
     */
    public static final int MIN_CAPACITY = 8;

    private final KeySupplier<V> keySupplier;
    private final boolean shouldAvoidAllocation;
    private int head;
    private int tail;
    private final int nullValue;
    private int[] values;
    private final Int2ObjectHashMap<V> valueFromKey;
    private ValueIterator iterator;

    /**
     * Construct a new queue defaulting to {@link #MIN_CAPACITY} capacity and cached iterator.
     *
     * @param keySupplier required supplier of unique key for value instances.
     */
    public CoalescingArrayQueue(final KeySupplier<V> keySupplier)
    {
        this(keySupplier, MIN_CAPACITY);
    }

    /**
     * Construct a new queue default to cached iterators.
     *
     * @param keySupplier required supplier of unique key for value instances.
     * @param initialCapacity for the queue which will be rounded up to the nearest power of 2.
     */
    public CoalescingArrayQueue(
        final KeySupplier<V> keySupplier,
        final int initialCapacity)
    {
        this(keySupplier, initialCapacity, true);
    }

    /**
     * Construct a new queue providing all the config options.
     *
     * @param keySupplier required supplier of unique key for value instances.
     * @param initialCapacity for the queue which will be rounded up to the nearest power of 2.
     * @param shouldAvoidAllocation true to cache the iterator otherwise false to allocate a new iterator each time.
     */
    public CoalescingArrayQueue(
        final KeySupplier<V> keySupplier,
        final int initialCapacity,
        final boolean shouldAvoidAllocation)
    {
        if (keySupplier == null)
        {
            throw new IllegalArgumentException("Key Supplier is missing");
        }

        this.keySupplier = keySupplier;
        this.nullValue = IntArrayQueue.DEFAULT_NULL_VALUE;
        this.shouldAvoidAllocation = shouldAvoidAllocation;

        if (initialCapacity < MIN_CAPACITY)
        {
            throw new IllegalArgumentException("initial capacity < MIN_INITIAL_CAPACITY : " + initialCapacity);
        }

        final int capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        if (capacity < MIN_CAPACITY)
        {
            throw new IllegalArgumentException("invalid initial capacity: " + initialCapacity);
        }

        values = new int[capacity];
        Arrays.fill(values, nullValue);

        valueFromKey = new Int2ObjectHashMap<>(initialCapacity, Hashing.DEFAULT_LOAD_FACTOR, shouldAvoidAllocation);
    }

    /**
     * The current capacity for the collection.
     *
     * @return the current capacity for the collection.
     */
    public int capacity()
    {
        return values.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return (tail - head) & (values.length - 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return head == tail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueIterator iterator()
    {
        ValueIterator iterator = this.iterator;
        if (null == iterator)
        {
            iterator = new ValueIterator();

            if (shouldAvoidAllocation)
            {
                this.iterator = iterator;
            }
        }

        return iterator.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forEach(final Consumer<? super V> action)
    {
        requireNonNull(action, "action cannot be null.");
        // Warn: a slow implementation
        for (int i = head; i != tail; )
        {
            action.accept(valueFromKey.get(values[i]));
            i = (i + 1) & (values.length - 1);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        if (head != tail)
        {
            Arrays.fill(values, nullValue);
            head = 0;
            tail = 0;
        }

        valueFromKey.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean offer(final V v)
    {
        requireNonNull(v, "value cannot be null");

        final int key = keySupplier.getKey(v);

        if (null == valueFromKey.put(key, v))
        {
            values[tail] = key;
            tail = (tail + 1) & (values.length - 1);

            if (tail == head)
            {
                increaseCapacity();
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V poll()
    {
        final int key = values[head];

        if (nullValue == key)
        {
            return null;
        }

        values[head] = nullValue;
        head = (head + 1) & (values.length - 1);

        return valueFromKey.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V peek()
    {
        final int key = values[head];

        return key == nullValue ? null : valueFromKey.get(key);
    }

    private void increaseCapacity()
    {
        final int oldHead = head;
        final int oldCapacity = values.length;
        final int toEndOfArray = oldCapacity - oldHead;
        final int newCapacity = oldCapacity << 1;

        if (newCapacity < MIN_CAPACITY)
        {
            throw new IllegalStateException("max capacity reached");
        }

        final int[] array = new int[newCapacity];
        Arrays.fill(array, oldCapacity, newCapacity, nullValue);
        System.arraycopy(values, oldHead, array, 0, toEndOfArray);
        System.arraycopy(values, 0, array, toEndOfArray, oldHead);

        values = array;
        head = 0;
        tail = oldCapacity;
    }

    public final class ValueIterator implements Iterator<V>
    {
        private int index;

        ValueIterator reset()
        {
            index = CoalescingArrayQueue.this.head;
            return this;
        }

        public boolean hasNext()
        {
            return index != tail;
        }

        public V next()
        {
            if (index == tail)
            {
                throw new NoSuchElementException();
            }

            final int key = values[index];
            index = (index + 1) & (values.length - 1);

            return valueFromKey.get(key);
        }
    }
}
