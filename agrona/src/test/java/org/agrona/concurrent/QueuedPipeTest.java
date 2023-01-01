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
package org.agrona.concurrent;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class QueuedPipeTest
{
    private static final int QUEUE_CAPACITY = 8;

    private static Stream<QueuedPipe<Integer>> data()
    {
        return Stream.of(
            new OneToOneConcurrentArrayQueue<>(QUEUE_CAPACITY),
            new ManyToOneConcurrentArrayQueue<>(QUEUE_CAPACITY),
            new ManyToManyConcurrentArrayQueue<>(QUEUE_CAPACITY)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldGetSizeWhenEmpty(final QueuedPipe<Integer> queue)
    {
        assertThat(queue.size(), is(0));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldThrowExceptionWhenNullOffered(final QueuedPipe<Integer> queue)
    {
        assertThrows(NullPointerException.class, () -> queue.offer(null));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldThrowExceptionWhenNullAdded(final QueuedPipe<Integer> queue)
    {
        assertThrows(NullPointerException.class, () -> queue.add(null));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldOfferAndPollToEmptyQueue(final QueuedPipe<Integer> queue)
    {
        final Integer testValue = 7;

        final boolean success = queue.offer(testValue);
        assertTrue(success);
        assertThat(queue.size(), is(1));

        final Integer polledValue = queue.poll();
        assertEquals(testValue, polledValue);
        assertThat(polledValue, is(testValue));
        assertThat(queue.size(), is(0));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldAddAndRemoveFromEmptyQueue(final QueuedPipe<Integer> queue)
    {
        final Integer testValue = 7;

        final boolean success = queue.add(testValue);
        assertTrue(success);
        assertThat(queue.size(), is(1));

        final Integer removedValue = queue.remove();
        assertThat(removedValue, is(testValue));
        assertThat(queue.size(), is(0));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldFailToOfferToFullQueue(final QueuedPipe<Integer> queue)
    {
        fillQueue(queue);

        assertThat(queue.size(), is(queue.capacity()));
        final boolean success = queue.offer(0);
        assertFalse(success);
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldPollSingleElementFromFullQueue(final QueuedPipe<Integer> queue)
    {
        fillQueue(queue);

        final Integer polledValue = queue.poll();
        assertThat(polledValue, is(0));
        assertThat(queue.size(), is(queue.capacity() - 1));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldPollNullFromEmptyQueue(final QueuedPipe<Integer> queue)
    {
        final Integer polledValue = queue.poll();
        assertNull(polledValue);
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldPeakQueueHead(final QueuedPipe<Integer> queue)
    {
        fillQueue(queue);

        final Integer headValue = queue.peek();
        assertThat(headValue, is(0));
        assertThat(queue.size(), is(queue.capacity()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldReturnNullForPeekOnEmptyQueue(final QueuedPipe<Integer> queue)
    {
        final Integer value = queue.peek();
        assertNull(value);
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldReturnElementQueueHead(final QueuedPipe<Integer> queue)
    {
        fillQueue(queue);

        final Integer headValue = queue.element();
        assertThat(headValue, is(0));
        assertThat(queue.size(), is(queue.capacity()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldThrowExceptionForElementOnEmptyQueue(final QueuedPipe<Integer> queue)
    {
        assertThrows(NoSuchElementException.class, queue::element);
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldRemoveSingleElementFromFullQueue(final QueuedPipe<Integer> queue)
    {
        fillQueue(queue);

        assertEquals(queue.capacity(), queue.size());
        final Integer removedValue = queue.remove();
        assertThat(removedValue, is(0));
        assertThat(queue.size(), is(queue.capacity() - 1));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldThrowExceptionForRemoveOnEmptyQueue(final QueuedPipe<Integer> queue)
    {
        assertThrows(NoSuchElementException.class, queue::remove);
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldClearFullQueue(final QueuedPipe<Integer> queue)
    {
        fillQueue(queue);

        queue.clear();
        assertThat(queue.size(), is(0));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldDrainFullQueue(final QueuedPipe<Integer> queue)
    {
        fillQueue(queue);

        final int[] counter = new int[1];
        final Consumer<Integer> elementHandler = (e) -> ++counter[0];

        final int elementsDrained = queue.drain(elementHandler);

        assertThat(elementsDrained, is(queue.capacity()));
        assertThat(counter[0], is(queue.capacity()));
        assertThat(queue.size(), is(0));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldHandleExceptionWhenDraining(final QueuedPipe<Integer> queue)
    {
        final String testMessage = "Test Exception";

        fillQueue(queue);

        final int[] counter = new int[1];
        final int exceptionTrigger = 3;
        final Consumer<Integer> elementHandler =
            (e) ->
            {
                ++counter[0];

                if (exceptionTrigger == counter[0])
                {
                    throw new RuntimeException(testMessage);
                }
            };

        RuntimeException exception = null;
        try
        {
            queue.drain(elementHandler);
        }
        catch (final RuntimeException ex)
        {
            exception = ex;
        }

        assertNotNull(exception);
        assertThat(queue.size(), is(queue.capacity() - exceptionTrigger));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldDrainFullQueueToCollection(final QueuedPipe<Integer> queue)
    {
        final Collection<Integer> target = new ArrayList<>();

        fillQueue(queue);

        final int elementsDrained = queue.drainTo(target, Integer.MAX_VALUE);

        assertThat(elementsDrained, is(queue.capacity()));
        assertThat(target.size(), is(queue.capacity()));
        assertThat(queue.size(), is(0));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldDrainQueueWithCountToCollection(final QueuedPipe<Integer> queue)
    {
        final Collection<Integer> target = new ArrayList<>();
        final int count = 3;

        fillQueue(queue);

        final int elementsDrained = queue.drainTo(target, count);

        assertThat(elementsDrained, is(count));
        assertThat(target.size(), is(count));
        assertThat(queue.size(), is(queue.capacity() - count));
    }

    private void fillQueue(final QueuedPipe<Integer> queue)
    {
        for (int i = 0, size = queue.capacity(); i < size; i++)
        {
            final boolean success = queue.offer(i);
            assertTrue(success);
        }
    }
}
