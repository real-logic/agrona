/*
 * Copyright 2014-2018 Real Logic Ltd.
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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import static org.agrona.UnsafeAccess.UNSAFE;

/**
 * Pad out a cache line to the left of a tail to prevent false sharing.
 */
class ManyToOneConcurrentLinkedQueuePadding1
{
    protected static final long HEAD_OFFSET;
    protected static final long TAIL_OFFSET;
    protected static final long NEXT_OFFSET;

    static final class Node<E>
    {
        E value;
        volatile Node<E> next;

        Node(final E value)
        {
            this.value = value;
        }

        void nextOrdered(final Node<E> next)
        {
            UNSAFE.putOrderedObject(this, NEXT_OFFSET, next);
        }
    }

    static
    {
        try
        {
            HEAD_OFFSET = UNSAFE.objectFieldOffset(ManyToOneConcurrentLinkedQueueHead.class.getDeclaredField("head"));
            TAIL_OFFSET = UNSAFE.objectFieldOffset(ManyToOneConcurrentLinkedQueueTail.class.getDeclaredField("tail"));
            NEXT_OFFSET = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unused")
    protected long p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;
}

/**
 * Value for the tail that is expected to be padded.
 */
class ManyToOneConcurrentLinkedQueueTail<E> extends ManyToOneConcurrentLinkedQueuePadding1
{
    protected volatile ManyToOneConcurrentLinkedQueue.Node<E> tail;
}

/**
 * Pad out a cache line between the tail and the head to prevent false sharing.
 */
class ManyToOneConcurrentLinkedQueuePadding2<E> extends ManyToOneConcurrentLinkedQueueTail<E>
{
    @SuppressWarnings("unused")
    protected long p16, p17, p18, p19, p20, p21, p22, p23, p24, p25, p26, p27, p28, p29, p30;
}

/**
 * Value for the head that is expected to be padded.
 */
class ManyToOneConcurrentLinkedQueueHead<E> extends ManyToOneConcurrentLinkedQueuePadding2<E>
{
    protected volatile ManyToOneConcurrentLinkedQueue.Node<E> head;
}

/**
 * Concurrent linked {@link Queue} that can be used from many producers and a single consumer.
 * <p>
 * This is a Java port of Dmitry Vyukov's
 * <a href="http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue">
 * MPSC linked queue</a>.
 * <p>
 * <b>Note:</b> This queue breaks the contract for peek and poll in that it can return null when the queue has no item
 * available but size could be greater than zero if an offer is in progress. This is a conflated design issue in the
 * {@link java.util.Queue} implementation.
 * <p>
 * If you wish to check for empty then call {@link #isEmpty()} rather than {@link #size()} checking for zero.
 *
 * @param <E> element type in the queue.
 */
public class ManyToOneConcurrentLinkedQueue<E> extends ManyToOneConcurrentLinkedQueueHead<E> implements Queue<E>
{
    @SuppressWarnings("unused")
    protected long p31, p32, p33, p34, p35, p36, p37, p38, p39, p40, p41, p42, p43, p44, p45;

    private final Node<E> empty = new Node<>(null);

    public ManyToOneConcurrentLinkedQueue()
    {
        headOrdered(empty);
        UNSAFE.putOrderedObject(this, TAIL_OFFSET, empty);
    }

    public boolean add(final E e)
    {
        return offer(e);
    }

    public boolean offer(final E e)
    {
        if (null == e)
        {
            throw new NullPointerException("element cannot be null");
        }

        final Node<E> tail = new Node<>(e);
        final Node<E> previousTail = swapTail(tail);
        previousTail.nextOrdered(tail);

        return true;
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

    public E poll()
    {
        E value = null;
        final Node<E> head = this.head;
        Node<E> next = head.next;

        if (null != next)
        {
            value = next.value;
            next.value = null;
            head.nextOrdered(null);

            if (null == next.next)
            {
                final Node<E> tail = this.tail;
                if (tail == next && casTail(tail, empty))
                {
                    next = empty;
                }
            }

            headOrdered(next);
        }

        return value;
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

    public E peek()
    {
        final Node<E> next = head.next;
        return null != next ? next.value : null;
    }

    /**
     * Size can be considered an approximation on a moving list.
     * It is only really stable when the consumer is inactive.
     * If you want to check for size() == 0 then {@link #isEmpty()} is a better alternative.
     * <p>
     * This operation is O(n) on the length of the linked chain.
     *
     * @return an approximation for the size of the list.
     */
    public int size()
    {
        Node<E> head = this.head;
        final Node<E> tail = this.tail;
        int size = 0;

        while (tail != head && size < Integer.MAX_VALUE)
        {
            final Node<E> next = head.next;
            if (null == next)
            {
                break;
            }

            head = next;
            ++size;
        }

        return size;
    }

    public boolean isEmpty()
    {
        return head == tail;
    }

    public boolean contains(final Object o)
    {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    public boolean addAll(final Collection<? extends E> c)
    {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');

        Node<E> head = this.head;
        final Node<E> tail = this.tail;

        while (head != tail)
        {
            final Node<E> next = head.next;
            if (null == next)
            {
                break;
            }

            head = next;

            sb.append(head.value);
            sb.append(", ");
        }

        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 2);
        }

        sb.append('}');

        return sb.toString();
    }

    private void headOrdered(final Node<E> head)
    {
        UNSAFE.putOrderedObject(this, HEAD_OFFSET, head);
    }

    @SuppressWarnings("unchecked")
    private Node<E> swapTail(final Node<E> newTail)
    {
        return (Node<E>)UNSAFE.getAndSetObject(this, TAIL_OFFSET, newTail);
    }

    private boolean casTail(final Node<E> expectedNode, final Node<E> updateNode)
    {
        return UNSAFE.compareAndSwapObject(this, TAIL_OFFSET, expectedNode, updateNode);
    }
}
