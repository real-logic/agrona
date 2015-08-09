/*
 * Copyright 2015 Real Logic Ltd.
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
package uk.co.real_logic.agrona.concurrent;

import uk.co.real_logic.agrona.BitUtil;

import java.util.*;

import static uk.co.real_logic.agrona.UnsafeAccess.UNSAFE;

/**
 * Pad out a cacheline to the left of a tail to prevent false sharing.
 */
class AbstractConcurrentArrayQueuePadding1
{
    @SuppressWarnings("unused")
    protected long p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;
}

/**
 * Value for the tail that is expected to be padded.
 */
class AbstractConcurrentArrayQueueTail extends AbstractConcurrentArrayQueuePadding1
{
    protected volatile long tail;
}

/**
 * Pad out a cacheline between the tail and the head to prevent false sharing.
 */
class AbstractConcurrentArrayQueuePadding2 extends AbstractConcurrentArrayQueueTail
{
    @SuppressWarnings("unused")
    protected long p16, p17, p18, p19, p20, p21, p22, p23, p24, p25, p26, p27, p28, p29, p30;
}

/**
 * Value for the head that is expected to be padded.
 */
class AbstractConcurrentArrayQueueHead extends AbstractConcurrentArrayQueuePadding2
{
    protected volatile long head;
}

/**
 * Pad out a cacheline between the tail and the head to prevent false sharing.
 */
class AbstractConcurrentArrayQueuePadding3 extends AbstractConcurrentArrayQueueHead
{
    @SuppressWarnings("unused")
    protected long p31, p32, p33, p34, p35, p36, p37, p38, p39, p40, p41, p42, p43, p44, p45;
}

/**
 * Left over immutable queue fields.
 */
public abstract class AbstractConcurrentArrayQueue<E>
    extends AbstractConcurrentArrayQueuePadding3
    implements QueuedPipe<E>
{
    protected static final long TAIL_OFFSET;
    protected static final long HEAD_OFFSET;
    protected static final int ARRAY_BASE;
    protected static final int SHIFT_FOR_SCALE;

    static
    {
        try
        {
            ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class);
            SHIFT_FOR_SCALE = BitUtil.calculateShiftForScale(UNSAFE.arrayIndexScale(Object[].class));
            TAIL_OFFSET = UNSAFE.objectFieldOffset(AbstractConcurrentArrayQueueTail.class.getDeclaredField("tail"));
            HEAD_OFFSET = UNSAFE.objectFieldOffset(AbstractConcurrentArrayQueueHead.class.getDeclaredField("head"));
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    protected final long mask;
    protected final int capacity;
    protected final E[] buffer;

    @SuppressWarnings("unchecked")
    public AbstractConcurrentArrayQueue(final int requestedCapacity)
    {
        capacity = BitUtil.findNextPositivePowerOfTwo(requestedCapacity);
        mask = capacity - 1;
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
        return (E)UNSAFE.getObjectVolatile(buffer, sequenceToOffset(head, mask));
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
        return tail == head;
    }

    public boolean contains(final Object o)
    {
        if (null == o)
        {
            return false;
        }

        final Object[] buffer = this.buffer;

        for (long i = head, limit = tail; i < limit; i++)
        {
            final Object e = UNSAFE.getObjectVolatile(buffer, sequenceToOffset(i, mask));
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

    public static long sequenceToOffset(final long sequence, final long mask)
    {
        return ARRAY_BASE + ((sequence & mask) << SHIFT_FOR_SCALE);
    }
}
