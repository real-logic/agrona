/*
 * Copyright 2014-2023 Real Logic Limited.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.agrona.UnsafeAccess.UNSAFE;

/**
 * Pad out a cacheline to the left of a producer fields to prevent false sharing.
 */
@SuppressWarnings("unused")
abstract class AbstractConcurrentArrayQueuePadding1
{
    byte p000, p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    byte p016, p017, p018, p019, p020, p021, p022, p023, p024, p025, p026, p027, p028, p029, p030, p031;
    byte p032, p033, p034, p035, p036, p037, p038, p039, p040, p041, p042, p043, p044, p045, p046, p047;
    byte p048, p049, p050, p051, p052, p053, p054, p055, p056, p057, p058, p059, p060, p061, p062, p063;
}

/**
 * Value for the producer that are expected to be padded.
 */
abstract class AbstractConcurrentArrayQueueProducer extends AbstractConcurrentArrayQueuePadding1
{
    /**
     * Tail index.
     */
    protected volatile long tail;
    /**
     * Cached head index.
     */
    protected long headCache;
    /**
     * Shared cached head index.
     */
    protected volatile long sharedHeadCache;
}

/**
 * Pad out a cacheline between the producer and consumer fields to prevent false sharing.
 */
@SuppressWarnings("unused")
abstract class AbstractConcurrentArrayQueuePadding2 extends AbstractConcurrentArrayQueueProducer
{
    byte p064, p065, p066, p067, p068, p069, p070, p071, p072, p073, p074, p075, p076, p077, p078, p079;
    byte p080, p081, p082, p083, p084, p085, p086, p087, p088, p089, p090, p091, p092, p093, p094, p095;
    byte p096, p097, p098, p099, p100, p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111;
    byte p112, p113, p114, p115, p116, p117, p118, p119, p120, p121, p122, p123, p124, p125, p126, p127;
}

/**
 * Values for the consumer that are expected to be padded.
 */
abstract class AbstractConcurrentArrayQueueConsumer extends AbstractConcurrentArrayQueuePadding2
{
    /**
     * Head index.
     */
    protected volatile long head;
}

/**
 * Pad out a cacheline between the producer and consumer fields to prevent false sharing.
 */
@SuppressWarnings("unused")
abstract class AbstractConcurrentArrayQueuePadding3 extends AbstractConcurrentArrayQueueConsumer
{
    byte p128, p129, p130, p131, p132, p133, p134, p135, p136, p137, p138, p139, p140, p142, p143, p144;
    byte p145, p146, p147, p148, p149, p150, p151, p152, p153, p154, p155, p156, p157, p158, p159, p160;
    byte p161, p162, p163, p164, p165, p166, p167, p168, p169, p170, p171, p172, p173, p174, p175, p176;
    byte p177, p178, p179, p180, p181, p182, p183, p184, p185, p186, p187, p189, p190, p191, p192, p193;
}

/**
 * Left over immutable queue fields.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractConcurrentArrayQueue<E>
    extends AbstractConcurrentArrayQueuePadding3
    implements QueuedPipe<E>
{
    /**
     * Offset of the {@code tail} field.
     */
    protected static final long TAIL_OFFSET;
    /**
     * Offset of the {@code sharedHeadCache} field.
     */
    protected static final long SHARED_HEAD_CACHE_OFFSET;
    /**
     * Offset of the {@code head} field.
     */
    protected static final long HEAD_OFFSET;
    /**
     * Array base.
     */
    protected static final int BUFFER_ARRAY_BASE;
    /**
     * Shift for scale.
     */
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

    /**
     * Queue capacity.
     */
    protected final int capacity;
    /**
     * Backing array.
     */
    protected final E[] buffer;

    /**
     * Constructs a queue with the requested capacity.
     *
     * @param requestedCapacity of the queue.
     */
    @SuppressWarnings("unchecked")
    public AbstractConcurrentArrayQueue(final int requestedCapacity)
    {
        capacity = BitUtil.findNextPositivePowerOfTwo(requestedCapacity);
        buffer = (E[])new Object[capacity];
    }

    /**
     * {@inheritDoc}
     */
    public long addedCount()
    {
        return tail;
    }

    /**
     * {@inheritDoc}
     */
    public long removedCount()
    {
        return head;
    }

    /**
     * {@inheritDoc}
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * {@inheritDoc}
     */
    public int remainingCapacity()
    {
        return capacity - size();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public E peek()
    {
        return (E)UNSAFE.getObjectVolatile(buffer, sequenceToBufferOffset(head, capacity - 1));
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(final E e)
    {
        if (offer(e))
        {
            return true;
        }

        throw new IllegalStateException("Queue is full");
    }

    /**
     * {@inheritDoc}
     */
    public E remove()
    {
        final E e = poll();
        if (null == e)
        {
            throw new NoSuchElementException("Queue is empty");
        }

        return e;
    }

    /**
     * {@inheritDoc}
     */
    public E element()
    {
        final E e = peek();
        if (null == e)
        {
            throw new NoSuchElementException("Queue is empty");
        }

        return e;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public Iterator<E> iterator()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public <T> T[] toArray(final T[] a)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Object o)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public boolean addAll(final Collection<? extends E> c)
    {
        for (final E e : c)
        {
            add(e);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeAll(final Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean retainAll(final Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        Object value;
        do
        {
            value = poll();
        }
        while (null != value);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return head >= tail;
    }

    /**
     * {@inheritDoc}
     */
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

        final long size = currentTail - currentHeadAfter;
        if (size < 0)
        {
            return 0;
        }
        else if (size > capacity)
        {
            return capacity;
        }

        return (int)size;
    }

    /**
     * Compute buffer offset based on the given sequence and the mask.
     *
     * @param sequence to compute the offset from.
     * @param mask     to apply.
     * @return buffer offset.
     */
    public static long sequenceToBufferOffset(final long sequence, final long mask)
    {
        return BUFFER_ARRAY_BASE + ((sequence & mask) << SHIFT_FOR_SCALE);
    }
}
