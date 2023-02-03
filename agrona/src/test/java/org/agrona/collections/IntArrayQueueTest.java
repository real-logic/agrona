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
package org.agrona.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntArrayQueueTest
{
    @Test
    void shouldDefaultInitialise()
    {
        final IntArrayQueue queue = new IntArrayQueue();

        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
        assertEquals(IntArrayQueue.MIN_CAPACITY, queue.capacity());
    }

    @Test
    void shouldOfferThenPoll()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final Integer element = 7;

        assertTrue(queue.offer(element));
        assertEquals(1, queue.size());

        assertEquals(element, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    void shouldForEachWithoutBoxing()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final IntArrayList expected = new IntArrayList();

        for (int i = 0; i < 20; i++)
        {
            assertTrue(queue.offerInt(i));
            expected.addInt(i);
        }

        final IntArrayList actual = new IntArrayList();
        queue.forEachInt(actual::addInt);

        assertEquals(expected, actual);
    }

    @Test
    void shouldClear()
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
    void shouldOfferThenPollWithoutBoxing()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final int count = 20;

        for (int i = 0; i < count; i++)
        {
            assertTrue(queue.offerInt(i));
        }

        assertFalse(queue.isEmpty());
        assertEquals(count, queue.size());

        for (int i = 0; i < count; i++)
        {
            assertEquals(i, queue.pollInt());
        }

        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    void shouldPeek()
    {
        final int nullValue = -1;
        final IntArrayQueue queue = new IntArrayQueue(nullValue);
        assertEquals(nullValue, queue.nullValue());
        assertNull(queue.peek());

        final Integer element = 7;
        assertTrue(queue.offer(element));
        assertTrue(queue.offer(element));
        assertEquals(element, queue.peek());
    }

    @Test
    void shouldPeekWithoutBoxing()
    {
        final int nullValue = -1;
        final IntArrayQueue queue = new IntArrayQueue(nullValue);
        assertEquals(nullValue, queue.peekInt());

        final int element = 7;
        assertTrue(queue.offerInt(element));
        assertEquals(element, queue.peekInt());
    }

    @Test
    void shouldIterate()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final int count = 20;

        for (int i = 0; i < count; i++)
        {
            assertTrue(queue.offerInt(i));
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
    void shouldIterateWithoutBoxing()
    {
        final IntArrayQueue queue = new IntArrayQueue();
        final int count = 20;

        for (int i = 0; i < count; i++)
        {
            assertTrue(queue.offerInt(i));
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
    void shouldIterateEmptyQueue()
    {
        final IntArrayQueue queue = new IntArrayQueue();

        final IntArrayQueue.IntIterator iteratorOne = queue.iterator();
        assertFalse(iteratorOne.hasNext());

        final int count = 20;
        for (int i = 0; i < count; i++)
        {
            assertTrue(queue.offerInt(i));
            assertEquals(i, queue.removeInt());
        }

        final IntArrayQueue.IntIterator iteratorTwo = queue.iterator();
        assertFalse(iteratorTwo.hasNext());
    }
}
