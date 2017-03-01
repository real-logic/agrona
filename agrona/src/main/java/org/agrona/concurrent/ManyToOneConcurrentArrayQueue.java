/*
 * Copyright 2014-2017 Real Logic Ltd.
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
 * Many producer to one consumer concurrent queue that is array backed. The algorithm is a variation of Fast Flow
 * consumer adapted to work with the Java Memory Model on arrays by using {@link sun.misc.Unsafe}.
 *
 * @param <E> type of the elements stored in the {@link java.util.Queue}.
 */
public class ManyToOneConcurrentArrayQueue<E> extends AbstractConcurrentArrayQueue<E>
{
    public ManyToOneConcurrentArrayQueue(final int requestedCapacity)
    {
        super(requestedCapacity);
    }

    public boolean offer(final E e)
    {
        if (null == e)
        {
            throw new NullPointerException("element cannot be null");
        }

        final int capacity = this.capacity;
        long currentHead = sharedHeadCache;
        long bufferLimit = currentHead + capacity;
        long currentTail;
        do
        {
            currentTail = tail;
            if (currentTail >= bufferLimit)
            {
                currentHead = head;
                bufferLimit = currentHead + capacity;
                if (currentTail >= bufferLimit)
                {
                    return false;
                }

                UNSAFE.putOrderedLong(this, SHARED_HEAD_CACHE_OFFSET, currentHead);
            }
        }
        while (!UNSAFE.compareAndSwapLong(this, TAIL_OFFSET, currentTail, currentTail + 1));

        UNSAFE.putOrderedObject(buffer, sequenceToBufferOffset(currentTail, capacity - 1), e);

        return true;
    }

    @SuppressWarnings("unchecked")
    public E poll()
    {
        final long currentHead = head;
        final long elementOffset = sequenceToBufferOffset(currentHead, capacity - 1);
        final Object[] buffer = this.buffer;
        final Object e = UNSAFE.getObjectVolatile(buffer, elementOffset);

        if (null != e)
        {
            UNSAFE.putObject(buffer, elementOffset, null);
            UNSAFE.putOrderedLong(this, HEAD_OFFSET, currentHead + 1);
        }

        return (E)e;
    }

    @SuppressWarnings("unchecked")
    public int drain(final Consumer<E> elementHandler)
    {
        final Object[] buffer = this.buffer;
        final long mask = this.capacity - 1;
        final long currentHead = head;
        long nextSequence = currentHead;
        final long limit = nextSequence + mask + 1;

        while (nextSequence < limit)
        {
            final long elementOffset = sequenceToBufferOffset(nextSequence, mask);
            final Object item = UNSAFE.getObjectVolatile(buffer, elementOffset);

            if (null == item)
            {
                break;
            }

            UNSAFE.putOrderedObject(buffer, elementOffset, null);
            nextSequence++;
            UNSAFE.putOrderedLong(this, HEAD_OFFSET, nextSequence);
            elementHandler.accept((E)item);
        }

        return (int)(nextSequence - currentHead);
    }

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
            final Object e = UNSAFE.getObjectVolatile(buffer, elementOffset);
            if (null == e)
            {
                break;
            }

            UNSAFE.putOrderedObject(buffer, elementOffset, null);
            nextSequence++;
            UNSAFE.putOrderedLong(this, HEAD_OFFSET, nextSequence);
            count++;
            target.add((E)e);
        }

        return count;
    }
}
