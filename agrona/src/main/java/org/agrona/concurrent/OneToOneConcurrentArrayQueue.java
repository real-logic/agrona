/*
 * Copyright 2014-2025 Real Logic Limited.
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

import org.agrona.UnsafeApi;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * One producer to one consumer concurrent queue that is array backed. The algorithm is a variation of Fast Flow
 * adapted to work with the Java Memory Model on arrays by using {@link sun.misc.Unsafe}.
 *
 * @param <E> type of the elements stored in the {@link java.util.Queue}.
 */
@SuppressWarnings("removal")
public class OneToOneConcurrentArrayQueue<E> extends AbstractConcurrentArrayQueue<E>
{
    /**
     * Constructs queue with the requested capacity.
     *
     * @param requestedCapacity of the queue.
     */
    public OneToOneConcurrentArrayQueue(final int requestedCapacity)
    {
        super(requestedCapacity);
    }

    /**
     * {@inheritDoc}
     */
    public boolean offer(final E e)
    {
        if (null == e)
        {
            throw new NullPointerException("Null is not a valid element");
        }

        final int capacity = this.capacity;
        long currentHead = headCache;
        long bufferLimit = currentHead + capacity;
        final long currentTail = tail;
        if (currentTail >= bufferLimit)
        {
            currentHead = head;
            bufferLimit = currentHead + capacity;
            if (currentTail >= bufferLimit)
            {
                return false;
            }

            headCache = currentHead;
        }

        final long elementOffset = sequenceToBufferOffset(currentTail, capacity - 1);

        UnsafeApi.putReferenceRelease(buffer, elementOffset, e);
        UnsafeApi.putLongRelease(this, TAIL_OFFSET, currentTail + 1);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public E poll()
    {
        final Object[] buffer = this.buffer;
        final long currentHead = head;
        final long elementOffset = sequenceToBufferOffset(currentHead, capacity - 1);

        final Object e = UnsafeApi.getReferenceVolatile(buffer, elementOffset);
        if (null != e)
        {
            UnsafeApi.putReferenceRelease(buffer, elementOffset, null);
            UnsafeApi.putLongRelease(this, HEAD_OFFSET, currentHead + 1);
        }

        return (E)e;
    }

    /**
     * {@inheritDoc}
     */
    public int drain(final Consumer<E> elementConsumer)
    {
        return drain(elementConsumer, (int)(tail - head));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public int drain(final Consumer<E> elementConsumer, final int limit)
    {
        final Object[] buffer = this.buffer;
        final long mask = this.capacity - 1;
        final long currentHead = head;
        long nextSequence = currentHead;
        final long limitSequence = nextSequence + limit;

        while (nextSequence < limitSequence)
        {
            final long elementOffset = sequenceToBufferOffset(nextSequence, mask);
            final Object item = UnsafeApi.getReferenceVolatile(buffer, elementOffset);

            if (null == item)
            {
                break;
            }

            UnsafeApi.putReferenceRelease(buffer, elementOffset, null);
            nextSequence++;
            UnsafeApi.putLongRelease(this, HEAD_OFFSET, nextSequence);
            elementConsumer.accept((E)item);
        }

        return (int)(nextSequence - currentHead);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public int drainTo(final Collection<? super E> target, final int limit)
    {
        final Object[] buffer = this.buffer;
        final long mask = this.capacity - 1;
        long nextSequence = head;
        int count = 0;

        while (count < limit)
        {
            final long elementOffset = sequenceToBufferOffset(nextSequence, mask);
            final Object item = UnsafeApi.getReferenceVolatile(buffer, elementOffset);
            if (null == item)
            {
                break;
            }

            UnsafeApi.putReferenceRelease(buffer, elementOffset, null);
            nextSequence++;
            UnsafeApi.putLongRelease(this, HEAD_OFFSET, nextSequence);
            count++;
            target.add((E)item);
        }

        return count;
    }
}
