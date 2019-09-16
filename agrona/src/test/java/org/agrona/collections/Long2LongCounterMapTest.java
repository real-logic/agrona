package org.agrona.collections;

import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongUnaryOperator;
import java.util.stream.LongStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;


public class Long2LongCounterMapTest
{
    private static final int INITIAL_VALUE = 0;

    private final Long2LongCounterMap map = new Long2LongCounterMap(INITIAL_VALUE);

    @Test
    public void shouldInitiallyBeEmpty()
    {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void getShouldReturnInitialValueWhenEmpty()
    {
        assertEquals(INITIAL_VALUE, map.get(1));
    }

    @Test
    public void getShouldReturnInitialValueWhenThereIsNoElement()
    {
        map.put(3000000000L, 3000000000L);

        assertEquals(INITIAL_VALUE, map.get(2));
    }

    @Test
    public void getShouldReturnPutValues()
    {
        map.put(3000000000L, 3000000000L);

        assertEquals(3000000000L, map.get(3000000000L));
    }

    @Test
    public void putShouldReturnOldValue()
    {
        map.put(3000000000L, 3000000000L);

        assertEquals(3000000000L, map.put(3000000000L, 2));
    }

    @Test
    public void clearShouldResetSize()
    {
        map.put(3000000000L, 3000000000L);
        map.put(4000000000L, 4000000000L);

        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void clearShouldRemoveValues()
    {
        map.put(3000000000L, 3000000000L);
        map.put(4000000000L, 4000000000L);

        map.clear();

        assertEquals(INITIAL_VALUE, map.get(3000000000L));
        assertEquals(INITIAL_VALUE, map.get(4000000000L));
    }

    @Test
    public void forEachShouldLoopOverEveryElement()
    {
        map.put(3000000000L, 3000000000L);
        map.put(4000000000L, 4000000000L);

        final LongLongConsumer mockConsumer = mock(LongLongConsumer.class);
        map.forEach(mockConsumer);

        final InOrder inOrder = inOrder(mockConsumer);
        inOrder.verify(mockConsumer).accept(3000000000L, 3000000000L);
        inOrder.verify(mockConsumer).accept(4000000000L, 4000000000L);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldNotContainKeyOfAMissingKey()
    {
        assertFalse(map.containsKey(1));
    }

    @Test
    public void shouldContainKeyOfAPresentKey()
    {
        map.put(20000000000L, 1L);

        assertTrue(map.containsKey(20000000000L));
    }

    @Test
    public void shouldNotContainValueForAMissingEntry()
    {
        assertFalse(map.containsValue(1));
    }

    @Test
    public void shouldContainValueForAPresentEntry()
    {
        map.put(5000000000L, 20000000000L);

        assertTrue(map.containsValue(20000000000L));
    }

    @Test
    public void removeShouldReturnMissing()
    {
        assertEquals(INITIAL_VALUE, map.remove(1));
    }

    @Test
    public void removeShouldReturnValueRemoved()
    {
        map.put(10000000000L, 20000000000L);

        assertEquals(20000000000L, map.remove(10000000000L));
    }

    @Test
    public void removeShouldRemoveEntry()
    {
        map.put(10000000000L, 20000000000L);

        map.remove(10000000000L);

        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(10000000000L));
        assertFalse(map.containsValue(20000000000L));
    }

    @Test
    public void shouldOnlyRemoveTheSpecifiedEntry()
    {
        LongStream.range(1L, 8L).forEach((i) -> map.put(i, i * 2L));

        map.remove(5);

        LongStream
            .range(1L, 8L)
            .filter((i) -> i != 5L)
            .forEach((i) ->
            {
                assertTrue(map.containsKey(i));
                assertTrue(map.containsValue(2L * i));
            });
    }

    @Test
    public void shouldResizeWhenMoreElementsAreAdded()
    {
        LongStream
            .range(1L, 100L)
            .forEach((key) ->
            {
                final long value = key * 2;
                assertEquals(INITIAL_VALUE, map.put(key, value));
                assertEquals(value, map.get(key));
            });
    }

    @Test
    public void shouldHaveNoMinValueForEmptyCollection()
    {
        assertEquals(INITIAL_VALUE, map.minValue());
    }

    @Test
    public void shouldFindMinValue()
    {
        map.put(10000000000L, 20000000000L);
        map.put(20000000000L, 100000000000L);
        map.put(30000000000L, -50000000000L);

        assertEquals(-50000000000L, map.minValue());
    }

    @Test
    public void shouldHaveNoMaxValueForEmptyCollection()
    {
        assertEquals(INITIAL_VALUE, map.maxValue());
    }

    @Test
    public void shouldFindMaxValue()
    {
        map.put(10000000000L, 20000000000L);
        map.put(20000000000L, 100000000000L);
        map.put(30000000000L, -50000000000L);

        assertEquals(100000000000L, map.maxValue());
    }

    @Test
    public void sizeShouldReturnNumberOfEntries()
    {
        final long count = 100000L;
        for (int key = 0; key < count; key++)
        {
            map.put(key, 1);
        }

        assertEquals(count, map.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotSupportLoadFactorOfGreaterThanOne()
    {
        new Long2LongHashMap(4, 2, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotSupportLoadFactorOfOne()
    {
        new Long2LongHashMap(4, 1, 0);
    }

    @Test
    public void correctSizeAfterRehash()
    {
        final Long2LongHashMap map = new Long2LongHashMap(16, 0.6f, -1);

        LongStream.range(1L, 17L).forEach((i) -> map.put(i, i));
        assertEquals("Map has correct size", 16L, map.size());

        final List<Long> keys = new ArrayList<>(map.keySet());
        keys.forEach(map::remove);

        assertTrue("Map isn't empty", map.isEmpty());
    }

    @Test
    public void shouldComputeIfAbsent()
    {
        final long testKey = 7000000000L;
        final long testValue = 7000000000L;

        final LongUnaryOperator function = (i) -> testValue;

        assertEquals(map.initialValue(), map.get(testKey));

        assertThat(map.computeIfAbsent(testKey, function), is(testValue));
        assertThat(map.get(testKey), is(testValue));
    }

    @Test
    public void shouldContainValueForIncAndDecEntries()
    {
        map.incrementAndGet(10000000000L);
        map.getAndIncrement(20000000000L);
        map.getAndAdd(30000000000L, 20000000000L);
        map.addAndGet(40000000000L, 30000000000L);
        map.decrementAndGet(50000000000L);
        map.getAndDecrement(60000000000L);

        assertTrue(map.containsKey(10000000000L));
        assertTrue(map.containsKey(20000000000L));
        assertTrue(map.containsKey(30000000000L));
        assertTrue(map.containsKey(40000000000L));
        assertTrue(map.containsKey(50000000000L));
        assertTrue(map.containsKey(60000000000L));
    }

    @Test
    public void shouldResultInEmptyAfterIncAndDecWhenEmpty()
    {
        map.incrementAndGet(10000000000L);
        map.decrementAndGet(10000000000L);

        assertTrue(map.isEmpty());

        map.getAndIncrement(10000000000L);
        map.getAndDecrement(10000000000L);

        assertTrue(map.isEmpty());

        map.incrementAndGet(10000000000L);
        map.getAndDecrement(10000000000L);

        assertTrue(map.isEmpty());

        map.getAndIncrement(10000000000L);
        map.decrementAndGet(10000000000L);

        assertTrue(map.isEmpty());
    }

    @Test
    public void shouldResultInNotEmptyAfterIncAndDecWhenHaveKey()
    {
        map.put(10000000000L, 10000000000L);

        map.incrementAndGet(10000000000L);
        map.decrementAndGet(10000000000L);

        assertEquals(10000000000L, map.get(10000000000L));
        assertEquals(1, map.size());
    }

    @Test
    public void shouldReturnInitialValueForGetAndAdd0IfKeyMissing()
    {
        final long val = map.getAndAdd(10000000000000L, 0);

        assertEquals(INITIAL_VALUE, val);
        assertTrue(map.isEmpty());
    }

    @Test
    public void shouldReturnOldValueForGetAndAdd0IfKeyExists()
    {
        map.put(10000000000L, 10000000000L);
        final long val = map.getAndAdd(10000000000L, 0);

        assertEquals(10000000000L, val);
        assertEquals(1, map.size());
    }

    @Test
    public void shouldReturnOldValueForGetAndAddNot0IfKeyExists()
    {
        map.put(10000000000L, 10000000000L);
        int amount;
        do
        {
            amount = ThreadLocalRandom.current().nextInt();
        }
        while (amount == 0);
        final long val = map.getAndAdd(10000000000L, amount);

        assertEquals(10000000000L, val);
        assertEquals(1, map.size());
    }

    @Test
    public void shouldRemoveEntryAfterDecToInitialVal()
    {
        map.put(10000000000L, INITIAL_VALUE + 1);

        map.decrementAndGet(10000000000L);

        assertEquals(INITIAL_VALUE, map.get(10000000000L));
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(10000000000L));
    }

    @Test
    public void shouldRemoveEntryAfterIncToInitialVal()
    {
        map.put(10000000000L, INITIAL_VALUE - 1);

        map.incrementAndGet(10000000000L);

        assertEquals(INITIAL_VALUE, map.get(1));
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(10000000000L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowInitialValueAsValue()
    {
        map.put(10000000000L, INITIAL_VALUE);
    }

    @Test
    public void shouldAllowInitialValueAsKey()
    {
        map.put(INITIAL_VALUE, 10000000000L);

        assertEquals(10000000000L, map.get(INITIAL_VALUE));
        assertTrue(map.containsKey(INITIAL_VALUE));
        assertEquals(1, map.size());

        final long[] tuple = new long[2];
        map.forEach((k, v) ->
        {
            tuple[0] = k;
            tuple[1] = v;
        });

        assertEquals(INITIAL_VALUE, tuple[0]);
        assertEquals(10000000000L, tuple[1]);

        assertEquals(10000000000L, map.remove(INITIAL_VALUE));
        assertEquals(0, map.size());
        assertEquals(INITIAL_VALUE, map.get(INITIAL_VALUE));
    }

    @Test
    public void shouldNotContainInitialValue()
    {
        assertFalse(map.containsValue(INITIAL_VALUE));
        map.put(INITIAL_VALUE, 1);
        assertFalse(map.containsValue(INITIAL_VALUE));
    }
}