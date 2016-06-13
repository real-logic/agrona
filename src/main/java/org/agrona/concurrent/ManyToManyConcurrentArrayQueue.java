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
package org.agrona.concurrent;

import java.util.Collection;
import java.util.function.Consumer;

import static org.agrona.UnsafeAccess.UNSAFE;

/**
 * Many producer to many consumer concurrent queue that is array backed.
 *
 *
 * This is a Java port of the
 * <a href="http://www.1024cores.net/home/lock-free-algorithms/queues/bounded-mpmc-queue">MPMC queue</a> by Dmitry Vyukov.
 *
 * <b>Note:</b> This queue breaks the contract for peek and poll in that it can return null when the queue has no node available
 * but size could be greater than zero if an offer is in progress. This is a conflated design issue in the Queue implementation.
 * If you wish to check for empty then call {@link #isEmpty()} rather than {@link #size() checking for zero}.
 *
 * @param <E> type of the elements stored in the {@link java.util.Queue}.
 */
public class ManyToManyConcurrentArrayQueue<E> extends AbstractConcurrentArrayQueue<E>
{
    private static final int SEQUENCES_ARRAY_BASE;

    static
    {
        try
        {
            SEQUENCES_ARRAY_BASE = UNSAFE.arrayBaseOffset(long[].class);
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private final long[] sequences;

    /**
     * Create a new queue with a bounded capacity. The requested capacity will be rounded up to the next positive
     * power of two in size. That is if you request a capacity of 1000 then you will get 1024. If you request 1024
     * then that is what you will get.
     *
     * @param requestedCapacity of the queue which must be &gt;= 2.
     * @throws IllegalArgumentException if the requestedCapacity &lt; 2.
     */
    public ManyToManyConcurrentArrayQueue(final int requestedCapacity)
    {
        super(requestedCapacity);

        if (requestedCapacity < 2)
        {
            throw new IllegalArgumentException("requestCapacity must be >= 2: requestedCapacity=" + requestedCapacity);
        }

        final long[] sequences = new long[capacity];

        for (int i = 0, size = capacity; i < size; i++)
        {
            final long sequenceOffset = sequenceArrayOffset(i, capacity - 1);
            UNSAFE.putOrderedLong(sequences, sequenceOffset, i);
        }

        this.sequences = sequences;
    }

    public boolean offer(final E e)
    {
        if (null == e)
        {
            throw new NullPointerException("element cannot be null");
        }

        final long mask = this.capacity - 1;
        final long[] sequences = this.sequences;

        do
        {
            final long currentTail = tail;
            final long sequenceOffset = sequenceArrayOffset(currentTail, mask);
            final long sequence = UNSAFE.getLongVolatile(sequences, sequenceOffset);

            if (sequence < currentTail)
            {
                return false;
            }

            if (UNSAFE.compareAndSwapLong(this, TAIL_OFFSET, currentTail, currentTail + 1L))
            {
                UNSAFE.putObject(buffer, sequenceToBufferOffset(currentTail, mask), e);
                UNSAFE.putOrderedLong(sequences, sequenceOffset, currentTail + 1L);

                return true;
            }
        }
        while (true);
    }

    @SuppressWarnings("unchecked")
    public E poll()
    {
        final long[] sequences = this.sequences;
        final long mask = this.capacity - 1;

        do
        {
            final long currentHead = head;
            final long sequenceOffset = sequenceArrayOffset(currentHead, mask);
            final long sequence = UNSAFE.getLongVolatile(sequences, sequenceOffset);
            final long attemptedHead = currentHead + 1L;

            if (sequence < attemptedHead)
            {
                return null;
            }

            if (UNSAFE.compareAndSwapLong(this, HEAD_OFFSET, currentHead, attemptedHead))
            {
                final long elementOffset = sequenceToBufferOffset(currentHead, mask);

                final Object e = UNSAFE.getObject(buffer, elementOffset);
                UNSAFE.putObject(buffer, elementOffset, null);
                UNSAFE.putOrderedLong(sequences, sequenceOffset, attemptedHead + mask);

                return (E)e;
            }
        }
        while (true);
    }

    public int drain(final Consumer<E> elementHandler)
    {
        final int size = size();
        int count = 0;

        E e;
        while (count < size && null != (e = poll()))
        {
            elementHandler.accept(e);
            ++count;
        }

        return count;
    }

    public int drainTo(final Collection<? super E> target, final int limit)
    {
        int count = 0;

        while (count < limit)
        {
            final E e = poll();
            if (null == e)
            {
                break;
            }

            target.add(e);
            ++count;
        }

        return count;
    }

    private static long sequenceArrayOffset(final long sequence, final long mask)
    {
        return SEQUENCES_ARRAY_BASE + ((sequence & mask) << 3);
    }
}
