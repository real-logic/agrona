/*
 * Copyright 2014-2020 Real Logic Ltd.
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
package org.agrona.concurrent;

import org.agrona.BitUtil;

import java.util.*;

import static org.agrona.UnsafeAccess.UNSAFE;

/**
 * Pad out a cacheline to the left of a producer fields to prevent false sharing.
 */
class AbstractConcurrentArrayQueuePadding1
{
    @SuppressWarnings("unused")
    long p1, p2, p3, p4, p5, p6, p7;
}

/**
 * Value for the producer that are expected to be padded.
 */
class AbstractConcurrentArrayQueueProducer extends AbstractConcurrentArrayQueuePadding1
{
    protected volatile long tail;
    protected long headCache;
    protected volatile long sharedHeadCache;
}

/**
 * Pad out a cacheline between the producer and consumer fields to prevent false sharing.
 */
class AbstractConcurrentArrayQueuePadding2 extends AbstractConcurrentArrayQueueProducer
{
    @SuppressWarnings("unused")
    long p1, p2, p3, p4, p5, p6, p7;
}

/**
 * Values for the consumer that are expected to be padded.
 */
class AbstractConcurrentArrayQueueConsumer extends AbstractConcurrentArrayQueuePadding2
{
    protected volatile long head;
}

/**
 * Pad out a cacheline between the producer and consumer fields to prevent false sharing.
 */
class AbstractConcurrentArrayQueuePadding3 extends AbstractConcurrentArrayQueueConsumer
{
    @SuppressWarnings("unused")
    long p1, p2, p3, p4, p5, p6, p7;
}

/**
 * Left over immutable queue fields.
 */
public abstract class AbstractConcurrentArrayQueue<E>
    extends AbstractConcurrentArrayQueuePadding3
    implements QueuedPipe<E>
{
    protected static final long TAIL_OFFSET;
    protected static final long SHARED_HEAD_CACHE_OFFSET;
    protected static final long HEAD_OFFSET;
    protected static final int BUFFER_ARRAY_BASE;
    protected static final int SHIFT_FOR_SCALE;

    static
    {
        try
        {
            BUFFER_ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class);
            SHIFT_FOR_SCALE = BitUtil.calculateShiftForScale(UNSAFE.arrayIndexScale(Object[].class));
            TAIL_OFFSET = UNSAFE.objectFieldOffset(AbstractConcurrentArrayQueueProducer.class.getDeclaredField("tail"));
            SHARED_HEAD_CACHE_OFFSET = UNSAFE.objectFieldOffset(
                AbstractConcurrentArrayQueueProducer.class.getDeclaredField("sharedHeadCache"));
            HEAD_OFFSET = UNSAFE.objectFieldOffset(AbstractConcurrentArrayQueueConsumer.class.getDeclaredField("head"));
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    protected final int capacity;
    protected final E[] buffer;

    @SuppressWarnings("unchecked")
    public AbstractConcurrentArrayQueue(final int requestedCapacity)
    {
        capacity = BitUtil.findNextPositivePowerOfTwo(requestedCapacity);
        buffer = (E[])new Object[capacity];
    }

    public long addedCount()
    {
        return tail;
    }

    public long removedCount()
    {
        return head;
    }

    public int capacity()
    {
        return capacity;
    }

    public int remainingCapacity()
    {
        return capacity() - size();
    }

    @SuppressWarnings("unchecked")
    public E peek()
    {
        return (E)UNSAFE.getObjectVolatile(buffer, sequenceToBufferOffset(head, capacity - 1));
    }

    public boolean add(final E e)
    {
        if (offer(e))
        {
            return true;
        }

        throw new IllegalStateException("Queue is full");
    }

    public E remove()
    {
        final E e = poll();
        if (null == e)
        {
            throw new NoSuchElementException("Queue is empty");
        }

        return e;
    }

    public E element()
    {
        final E e = peek();
        if (null == e)
        {
            throw new NoSuchElementException("Queue is empty");
        }

        return e;
    }

    public boolean isEmpty()
    {
        return head == tail;
    }

    public boolean contains(final Object o)
    {
        if (null == o)
        {
            return false;
        }

        final Object[] buffer = this.buffer;
        final int mask = this.capacity - 1;

        for (long i = head, limit = tail; i < limit; i++)
        {
            final Object e = UNSAFE.getObjectVolatile(buffer, sequenceToBufferOffset(i, mask));
            if (o.equals(e))
            {
                return true;
            }
        }

        return false;
    }

    public Iterator<E> iterator()
    {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray()
    {
        throw new UnsupportedOperationException();
    }

    public <T> T[] toArray(final T[] a)
    {
        throw new UnsupportedOperationException();
    }

    public boolean remove(final Object o)
    {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(final Collection<?> c)
    {
        for (final Object o : c)
        {
            if (!contains(o))
            {
                return false;
            }
        }

        return true;
    }

    public boolean addAll(final Collection<? extends E> c)
    {
        for (final E e : c)
        {
            add(e);
        }

        return true;
    }

    public boolean removeAll(final Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(final Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    public void clear()
    {
        Object value;
        do
        {
            value = poll();
        }
        while (null != value);
    }

    public int size()
    {
        long currentHeadBefore;
        long currentTail;
        long currentHeadAfter = head;

        do
        {
            currentHeadBefore = currentHeadAfter;
            currentTail = tail;
            currentHeadAfter = head;
        }
        while (currentHeadAfter != currentHeadBefore);

        return (int)(currentTail - currentHeadAfter);
    }

    public static long sequenceToBufferOffset(final long sequence, final long mask)
    {
        return BUFFER_ARRAY_BASE + ((sequence & mask) << SHIFT_FOR_SCALE);
    }
}
