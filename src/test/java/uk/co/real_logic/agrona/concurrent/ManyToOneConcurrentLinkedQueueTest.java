package uk.co.real_logic.agrona.concurrent;

import org.junit.Test;

import java.util.Queue;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ManyToOneConcurrentLinkedQueueTest
{
    private final Queue<Integer> queue = new ManyToOneConcurrentLinkedQueue<>();

    @Test
    public void shouldBeEmpty()
    {
        assertTrue(queue.isEmpty());
        assertThat(queue.size(), is(0));
    }

    @Test
    public void shouldNotBeEmpty()
    {
        queue.offer(1);

        assertFalse(queue.isEmpty());
        assertThat(queue.size(), is(1));
    }

    @Test
    public void shouldFailToPoll()
    {
        assertNull(queue.poll());
    }

    @Test
    public void shouldOfferItem()
    {
        assertTrue(queue.offer(7));
        assertThat(queue.size(), is(1));
    }

    @Test
    public void shouldExchangeItem()
    {
        final int testItem = 1;
        queue.offer(testItem);

        assertThat(queue.poll(), is(testItem));
    }

    @Test
    public void shouldExchangeInFifoOrder()
    {
        final int numItems = 7;

        for (int i = 0; i < numItems; i++)
        {
            queue.offer(i);
        }

        assertThat(queue.size(), is(numItems));

        for (int i = 0; i < numItems; i++)
        {
            assertThat(queue.poll(), is(i));
        }

        assertThat(queue.size(), is(0));
    }

    @Test
    public void shouldToString()
    {
        assertThat(queue.toString(), is("{}"));

        for (int i = 0; i < 5; i++)
        {
            queue.offer(i);
        }

        assertThat(queue.toString(), is("{0, 1, 2, 3, 4}"));
    }
}