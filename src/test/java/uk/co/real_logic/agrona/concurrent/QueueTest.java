/*
 * Copyright 2015 Real Logic Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Theories.class)
public class QueueTest
{
    private static final int QUEUE_CAPACITY = 8;

    interface Fixture
    {
        SequencedContainerQueue<Integer> newInstance();
    }

    @DataPoint
    public static final Fixture ONE_TO_ONE_QUEUE = () -> new OneToOneConcurrentArrayQueue<>(QUEUE_CAPACITY);

    @DataPoint
    public static final Fixture MANY_TO_ONE_QUEUE = () -> new ManyToOneConcurrentArrayQueue<>(QUEUE_CAPACITY);

    @Theory
    public void shouldGetSizeWhenEmpty(final Fixture fixture)
    {
        final Queue<Integer> queue = fixture.newInstance();
        assertThat(queue.size(), is(0));
    }

    @Theory
    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenNullOffered(final Fixture fixture)
    {
        final Queue<Integer> queue = fixture.newInstance();
        queue.offer(null);
    }

    @Theory
    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenNullAdded(final Fixture fixture)
    {
        final Queue<Integer> queue = fixture.newInstance();
        queue.add(null);
    }

    @Theory
    public void shouldOfferAndPollToEmptyQueue(final Fixture fixture)
    {
        final Queue<Integer> queue = fixture.newInstance();

        final Integer testValue = 7;

        final boolean success = queue.offer(testValue);
        assertTrue(success);
        assertThat(queue.size(), is(1));

        final Integer polledValue = queue.poll();
        Assert.assertEquals(testValue, polledValue);
        assertThat(polledValue, is(testValue));
        assertThat(queue.size(), is(0));
    }

    @Theory
    public void shouldAddAndRemoveFromEmptyQueue(final Fixture fixture)
    {
        final Queue<Integer> queue = fixture.newInstance();

        final Integer testValue = 7;

        final boolean success = queue.add(testValue);
        assertTrue(success);
        assertThat(queue.size(), is(1));

        final Integer removedValue = queue.remove();
        assertThat(removedValue, is(testValue));
        assertThat(queue.size(), is(0));
    }

    @Theory
    public void shouldFailToOfferToFullQueue(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();

        fillQueue(queue);

        assertThat(queue.size(), is(queue.capacity()));
        final boolean success = queue.offer(0);
        Assert.assertFalse(success);
    }

    @Theory
    public void shouldPollSingleElementFromFullQueue(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();
        fillQueue(queue);

        final Integer polledValue = queue.poll();
        assertThat(polledValue, is(0));
        assertThat(queue.size(), is(queue.capacity() - 1));
    }

    @Theory
    public void shouldPollNullFromEmptyQueue(final Fixture fixture)
    {
        final Queue<Integer> queue = fixture.newInstance();

        final Integer polledValue = queue.poll();
        Assert.assertNull(polledValue);
    }

    @Theory
    public void shouldPeakQueueHead(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();

        fillQueue(queue);

        final Integer headValue = queue.peek();
        assertThat(headValue, is(0));
        assertThat(queue.size(), is(queue.capacity()));
    }

    @Theory
    public void shouldReturnNullForPeekOnEmptyQueue(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();

        final Integer value = queue.peek();
        Assert.assertNull(value);
    }

    @Theory
    public void shouldReturnElementQueueHead(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();

        fillQueue(queue);

        final Integer headValue = queue.element();
        assertThat(headValue, is(0));
        assertThat(queue.size(), is(queue.capacity()));
    }

    @Theory
    @Test(expected = NoSuchElementException.class)
    public void shouldThrowExceptionForElementOnEmptyQueue(final Fixture fixture)
    {
        final Queue<Integer> queue = fixture.newInstance();

        queue.element();
    }

    @Theory
    public void shouldRemoveSingleElementFromFullQueue(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();

        fillQueue(queue);

        Assert.assertEquals(queue.capacity(), queue.size());
        final Integer removedValue = queue.remove();
        assertThat(removedValue, is(0));
        assertThat(queue.size(), is(queue.capacity() - 1));
    }

    @Theory
    @Test(expected = NoSuchElementException.class)
    public void shouldThrowExceptionForRemoveOnEmptyQueue(final Fixture fixture)
    {
        final Queue<Integer> queue = fixture.newInstance();
        queue.remove();
    }

    @Theory
    public void shouldClearFullQueue(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();

        fillQueue(queue);

        queue.clear();
        assertThat(queue.size(), is(0));
    }

    @Theory
    public void shouldDrainFullQueue(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();

        fillQueue(queue);

        final int[] counter = new int[1];
        final Consumer<Integer> elementHandler = (e) -> ++counter[0];

        final int elementsDrained = queue.drain(elementHandler);

        assertThat(elementsDrained, is(queue.capacity()));
        assertThat(counter[0], is(queue.capacity()));
        assertThat(queue.size(), is(0));
    }

    @Theory
    public void shouldHandleExceptionWhenDraining(final Fixture fixture)
    {
        final String testMessage = "Test Exception";
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();

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

        Assert.assertNotNull(exception);
        assertThat(queue.size(), is(queue.capacity() - exceptionTrigger));
    }

    @Theory
    public void shouldDrainFullQueueToCollection(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();
        final Collection<Integer> target = new ArrayList<>();

        fillQueue(queue);

        final int elementsDrained = queue.drainTo(target, Integer.MAX_VALUE);

        assertThat(elementsDrained, is(queue.capacity()));
        assertThat(target.size(), is(queue.capacity()));
        assertThat(queue.size(), is(0));
    }

    @Theory
    public void shouldDrainQueueWithCountToCollection(final Fixture fixture)
    {
        final SequencedContainerQueue<Integer> queue = fixture.newInstance();
        final Collection<Integer> target = new ArrayList<>();
        final int count = 3;

        fillQueue(queue);

        int elementsDrained = queue.drainTo(target, count);

        assertThat(elementsDrained, is(count));
        assertThat(target.size(), is(count));
        assertThat(queue.size(), is(queue.capacity() - count));
    }

    private void fillQueue(final SequencedContainerQueue<Integer> queue)
    {
        for (int i = 0, size = queue.capacity(); i < size; i++)
        {
            final boolean success = queue.offer(i);
            assertTrue(success);
        }
    }
}