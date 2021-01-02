/*
 * Copyright 2014-2021 Real Logic Limited.
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
package org.agrona.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntArrayQueueTest
{
    @Test
    public void shouldDefaultInitialise()
    {
        final IntArrayQueue queue = new IntArrayQueue();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertEquals(IntArrayQueue.MIN_CAPACITY, queue.capacity());
    }

    @Test
    public void shouldOfferThenPoll()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final Integer element = 7;

        queue.offer(7);
        assertEquals(1, queue.size());

        assertEquals(element, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    public void shouldForEachWithoutBoxing()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final IntArrayList expected = new IntArrayList();

        for (int i = 0; i < 20; i++)
        {
            queue.offerInt(i);
            expected.addInt(i);
        }

        final IntArrayList actual = new IntArrayList();
        queue.forEachInt(actual::addInt);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldClear()
    {
        final IntArrayQueue queue = new IntArrayQueue();

        for (int i = 0; i < 7; i++)
        {
            queue.offerInt(i);
        }
        queue.removeInt();

        queue.clear();
        assertEquals(0, queue.size());
    }

    @Test
    public void shouldOfferThenPollWithoutBoxing()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final int count = 20;

        for (int i = 0; i < count; i++)
        {
            queue.offerInt(i);
        }

        assertFalse(queue.isEmpty());
        assertEquals(count, queue.size());

        for (int i = 0; i < count; i++)
        {
            assertEquals(i, queue.pollInt());
        }

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    public void shouldPeek()
    {
        final int nullValue = -1;
        final IntArrayQueue queue = new IntArrayQueue(nullValue);
        assertEquals(nullValue, queue.nullValue());
        assertNull(queue.peek());

        final Integer element = 7;
        queue.offer(element);
        assertEquals(element, queue.peek());
    }

    @Test
    public void shouldPeekWithoutBoxing()
    {
        final int nullValue = -1;
        final IntArrayQueue queue = new IntArrayQueue(nullValue);
        assertEquals(nullValue, queue.peekInt());

        final int element = 7;
        queue.offerInt(element);
        assertEquals(element, queue.peekInt());
    }

    @Test
    public void shouldIterate()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final int count = 20;

        for (int i = 0; i < count; i++)
        {
            queue.offerInt(i);
        }

        final IntArrayQueue.IntIterator iterator = queue.iterator();
        for (int i = 0; i < count; i++)
        {
            assertTrue(iterator.hasNext());
            assertEquals(Integer.valueOf(i), iterator.next());
        }

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldIterateWithoutBoxing()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final int count = 20;

        for (int i = 0; i < count; i++)
        {
            queue.offerInt(i);
        }

        final IntArrayQueue.IntIterator iterator = queue.iterator();
        for (int i = 0; i < count; i++)
        {
            assertTrue(iterator.hasNext());
            assertEquals(i, iterator.nextValue());
        }

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldIterateEmptyQueue()
    {
        final IntArrayQueue queue = new IntArrayQueue();

        for (final int ignore : queue)
        {
            fail("Should be empty");
        }

        final int count = 20;
        for (int i = 0; i < count; i++)
        {
            queue.offerInt(i);
            queue.removeInt();
        }

        for (final int ignore : queue)
        {
            fail("Should be empty");
        }
    }
}
