/*
 * Copyright 2014 Real Logic Ltd.
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

import java.util.Collection;
import java.util.function.Consumer;

import static uk.co.real_logic.agrona.UnsafeAccess.UNSAFE;

/**
 * One producer to one consumer concurrent queue that is array backed. The algorithm is a variation of Fast Flow
 * adapted to work with the Java Memory Model on arrays by using {@link sun.misc.Unsafe}.
 *
 * @param <E> type of the elements stored in the {@link java.util.Queue}.
 */
public class OneToOneConcurrentArrayQueue<E> extends AbstractConcurrentArrayQueue<E>
{
    public OneToOneConcurrentArrayQueue(final int requestedCapacity)
    {
        super(requestedCapacity);
    }

    public boolean offer(final E e)
    {
        if (null == e)
        {
            throw new NullPointerException("Null is not a valid element");
        }

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

        final long elementOffset = sequenceToBufferOffset(currentTail, mask);

        UNSAFE.putOrderedObject(buffer, elementOffset, e);
        UNSAFE.putOrderedLong(this, TAIL_OFFSET, currentTail + 1);

        return true;
    }

    @SuppressWarnings("unchecked")
    public E poll()
    {
        final Object[] buffer = this.buffer;
        final long currentHead = head;
        final long elementOffset = sequenceToBufferOffset(currentHead, mask);

        final Object e = UNSAFE.getObjectVolatile(buffer, elementOffset);
        if (null != e)
        {
            UNSAFE.putOrderedObject(buffer, elementOffset, null);
            UNSAFE.putOrderedLong(this, HEAD_OFFSET, currentHead + 1);
        }

        return (E)e;
    }

    @SuppressWarnings("unchecked")
    public int drain(final Consumer<E> elementHandler)
    {
        final Object[] buffer = this.buffer;
        final long mask = this.mask;
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
        final long mask = this.mask;
        long nextSequence = head;
        int count = 0;

        while (count < limit)
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
            count++;
            target.add((E)item);
        }

        return count;
    }
}
