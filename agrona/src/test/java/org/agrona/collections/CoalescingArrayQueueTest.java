/*
 * Copyright 2014-2020 Real Logic Limited.
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

import java.util.ArrayList;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CoalescingArrayQueueTest
{
    @Test
    void shouldFailCreateWithInvalidKeySupplier()
    {
        assertThrows(IllegalArgumentException.class, () -> new CoalescingArrayQueue<>(null));
    }

    @Test
    void shouldFailCreateWithInvalidCapacity()
    {
        assertThrows(IllegalArgumentException.class, () -> new CoalescingArrayQueue<>(Versioned::getKey, 1));
    }

    @Test
    public void shouldDefaultInitialise()
    {
        final CoalescingArrayQueue<Versioned> queue = create();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertEquals(IntArrayQueue.MIN_CAPACITY, queue.capacity());
    }

    @Test
    public void shouldOfferThenPoll()
    {
        final CoalescingArrayQueue<Versioned> queue = create();
        final Versioned element = withKey(7);

        queue.offer(element);
        assertEquals(1, queue.size());

        assertEquals(element, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    public void shouldForEach()
    {
        final CoalescingArrayQueue<Versioned> queue = create();
        final ArrayList<Versioned> expected = new ArrayList<>();

        for (int i = 0; i < 20; i++)
        {
            queue.offer(withKey(i));
            expected.add(withKey(i));
        }

        final ArrayList<Versioned> actual = new ArrayList<>();
        queue.forEach(actual::add);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldClear()
    {
        final CoalescingArrayQueue<Versioned> queue = create();

        for (int i = 0; i < 7; i++)
        {
            queue.offer(withKey(i));
        }

        queue.clear();
        assertEquals(0, queue.size());
    }

    @Test
    public void shouldPeek()
    {
        final CoalescingArrayQueue<Versioned> queue = create();
        assertNull(queue.peek());

        final Versioned element = withKey(7);
        queue.offer(element);
        assertEquals(element, queue.peek());
    }

    @Test
    public void shouldIterate()
    {
        final CoalescingArrayQueue<Versioned> queue = create();
        final int count = 20;

        for (int i = 0; i < count; i++)
        {
            queue.offer(withKey(i));
        }

        final CoalescingArrayQueue<Versioned>.ValueIterator iterator = queue.iterator();
        for (int i = 0; i < count; i++)
        {
            assertTrue(iterator.hasNext());
            assertEquals(withKey(i), iterator.next());
        }

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldIterateEmptyQueue()
    {
        final CoalescingArrayQueue<Versioned> queue = create();

        for (final Versioned ignore : queue)
        {
            fail("Should be empty");
        }

        final int count = 20;
        for (int i = 0; i < count; i++)
        {
            queue.offer(withKey(i));
            queue.poll();
        }

        for (final Versioned ignore : queue)
        {
            fail("Should be empty");
        }
    }

    @Test
    void shouldCoalesce()
    {
        final CoalescingArrayQueue<Versioned> queue = create();
        final Versioned element = withKey(7);
        final Versioned updated = withKeyVersion(7, 1);

        queue.offer(element);
        assertEquals(1, queue.size());

        queue.offer(updated);
        assertEquals(1, queue.size());

        assertEquals(updated, queue.poll());
        assertNull(queue.poll());
    }

    private CoalescingArrayQueue<Versioned> create()
    {
        return new CoalescingArrayQueue<>(Versioned::getKey);
    }

    private Versioned withKey(final int key)
    {
        return new Versioned(key);
    }

    private Versioned withKeyVersion(final int key, final int version)
    {
        return new Versioned(key, version);
    }

    private static final class Versioned
    {
        private final int key;
        private int version;

        private Versioned(final int key)
        {
            this(key, 0);
        }

        private Versioned(final int key, final int version)
        {
            this.key = key;
            this.version = version;
        }

        public int getKey()
        {
            return key;
        }

        public int getVersion()
        {
            return version;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final Versioned versioned = (Versioned)o;

            return key == versioned.key && version == versioned.version;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(key, version);
        }
    }
}