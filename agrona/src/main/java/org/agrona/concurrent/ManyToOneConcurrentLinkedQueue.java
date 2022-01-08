/*
 * Copyright 2014-2022 Real Logic Limited.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import static org.agrona.UnsafeAccess.UNSAFE;

/**
 * Pad out a cache line to the left of a tail to prevent false sharing.
 */
@SuppressWarnings("deprecation")
abstract class ManyToOneConcurrentLinkedQueuePadding1
{
    /**
     * Offset of the {@code head} field.
     */
    protected static final long HEAD_OFFSET;
    /**
     * Offset of the {@code tail} field.
     */
    protected static final long TAIL_OFFSET;
    /**
     * Offset of the {@code next} field.
     */
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

    byte p000, p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    byte p016, p017, p018, p019, p020, p021, p022, p023, p024, p025, p026, p027, p028, p029, p030, p031;
    byte p032, p033, p034, p035, p036, p037, p038, p039, p040, p041, p042, p043, p044, p045, p046, p047;
    byte p048, p049, p050, p051, p052, p053, p054, p055, p056, p057, p058, p059, p060, p061, p062, p063;
}

/**
 * Value for the tail that is expected to be padded.
 */
abstract class ManyToOneConcurrentLinkedQueueTail<E> extends ManyToOneConcurrentLinkedQueuePadding1
{
    /**
     * Tail of the queue.
     */
    protected volatile ManyToOneConcurrentLinkedQueue.Node<E> tail;
}

/**
 * Pad out a cache line between the tail and the head to prevent false sharing.
 */
abstract class ManyToOneConcurrentLinkedQueuePadding2<E> extends ManyToOneConcurrentLinkedQueueTail<E>
{
    byte p064, p065, p066, p067, p068, p069, p070, p071, p072, p073, p074, p075, p076, p077, p078, p079;
    byte p080, p081, p082, p083, p084, p085, p086, p087, p088, p089, p090, p091, p092, p093, p094, p095;
    byte p096, p097, p098, p099, p100, p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111;
    byte p112, p113, p114, p115, p116, p117, p118, p119, p120, p121, p122, p123, p124, p125, p126, p127;
}

/**
 * Value for the head that is expected to be padded.
 */
abstract class ManyToOneConcurrentLinkedQueueHead<E> extends ManyToOneConcurrentLinkedQueuePadding2<E>
{
    /**
     * Head of queue.
     */
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
 * available but size could be greater than zero if an offer is in progress. This is due to the offer being a multi-step
 * process which can start and be interrupted before completion, the thread will later be resumed and the offer process
 * completes. Other methods, such as peek and poll, could spin internally waiting on the offer to complete to provide
 * sequentially consistency across methods but this can have a detrimental effect in a resource starved system. This
 * internal spinning eats up a CPU core and prevents other threads making progress resulting in latency spikes. To
 * avoid this a more relaxed approach is taken in that an in-progress offer is not waited on to complete.
 * <p>
 * If you wish to check for empty then call {@link #isEmpty()} rather than {@link #size()} checking for zero.
 *
 * @param <E> element type in the queue.
 */
public class ManyToOneConcurrentLinkedQueue<E> extends ManyToOneConcurrentLinkedQueueHead<E> implements Queue<E>
{
    byte p128, p129, p130, p131, p132, p133, p134, p135, p136, p137, p138, p139, p140, p142, p143, p144;
    byte p145, p146, p147, p148, p149, p150, p151, p152, p153, p154, p155, p156, p157, p158, p159, p160;
    byte p161, p162, p163, p164, p165, p166, p167, p168, p169, p170, p171, p172, p173, p174, p175, p176;
    byte p177, p178, p179, p180, p181, p182, p183, p184, p185, p186, p187, p189, p190, p191, p192, p193;

    private final Node<E> empty = new Node<>(null);

    /**
     * Constructs an empty queue.
     */
    public ManyToOneConcurrentLinkedQueue()
    {
        headOrdered(empty);
        UNSAFE.putOrderedObject(this, TAIL_OFFSET, empty);
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(final E e)
    {
        return offer(e);
    }

    /**
     * {@inheritDoc}
     */
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
    public E peek()
    {
        final Node<E> next = head.next;
        return null != next ? next.value : null;
    }

    /**
     * Size can be considered an approximation on a moving list.
     * It is only really stable when the consumer is inactive.
     * If you want to check for {@code queue.size() == 0} then {@link #isEmpty()} is a better alternative.
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

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return head == tail;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(final Object o)
    {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean addAll(final Collection<? extends E> c)
    {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
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
