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
package uk.co.real_logic.agrona.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import static uk.co.real_logic.agrona.UnsafeAccess.UNSAFE;

/**
 * Pad out a cacheline to the left of a tail to prevent false sharing.
 */
class ManyToOneConcurrentLinkedQueuePadding1
{
    protected static final long TAIL_OFFSET;
    protected static final long NODE_NEXT_OFFSET;

    static final class Node<E>
    {
        E value;
        volatile Node<E> next;

        Node(final E value)
        {
            this.value = value;
        }

        void setNextOrdered(final Node<E> nextNode)
        {
            UNSAFE.putOrderedObject(this, NODE_NEXT_OFFSET, nextNode);
        }
    }

    static
    {
        try
        {
            TAIL_OFFSET = UNSAFE.objectFieldOffset(ManyToOneConcurrentLinkedQueueTail.class.getDeclaredField("tail"));
            NODE_NEXT_OFFSET = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
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
 * Pad out a cacheline between the tail and the head to prevent false sharing.
 */
class ManyToOneConcurrentLinkedQueuePadding2<E> extends ManyToOneConcurrentLinkedQueueTail<E>
{
    @SuppressWarnings("unused")
    protected long p16, p17, p18, p19, p20, p21, p22, p23, p24, p25, p26, p27, p28, p29, p30;
}

/**
 * Concurrent linked {@link Queue} that can be used from many producers and a single consumer.
 *
 * This is a Java port of the
 * <a href="http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue">MPSC queue</a>
 * by Dmitry Vyukov.
 *
 * <b>Note:</b> This queue breaks the contract for peek and poll in that it can return null when the queue has no node available
 * but is not empty. This is a conflated design issue in the Queue implementation. If you wish to check for empty then call
 * {@link ManyToOneConcurrentLinkedQueue#isEmpty()}.
 *
 * @param <E> element type in the queue.
 */
public class ManyToOneConcurrentLinkedQueue<E> extends ManyToOneConcurrentLinkedQueuePadding2<E> implements Queue<E>
{
    private Node<E> head;

    public ManyToOneConcurrentLinkedQueue()
    {
        head = new Node<>(null);
        UNSAFE.putOrderedObject(this, TAIL_OFFSET, head);
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

        final Node<E> newTail = new Node<>(e);
        final Node<E> prevTail = swapTail(newTail);
        prevTail.setNextOrdered(newTail);

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
        final Node<E> node = head.next;

        if (null != node)
        {
            value = node.value;
            node.value = null;
            head = node;
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
        final Node<E> node = head.next;
        return null != node ? node.value : null;
    }

    public int size()
    {
        final Node<E> tail = this.tail;
        Node<E> head = this.head;

        int size = 0;
        while (head != tail && size < Integer.MAX_VALUE)
        {
            Node<E> next = head.next;

            while (null == next)
            {
                next = head.next;
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

    @SuppressWarnings("unchecked")
    private Node<E> swapTail(final Node<E> newTail)
    {
        return (Node<E>)UNSAFE.getAndSetObject(this, TAIL_OFFSET, newTail);
    }
}
