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
package org.agrona.collections;

import org.junit.Test;
import org.mockito.InOrder;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class Int2IntHashMapTest
{
    public static final int MISSING_VALUE = -1;

    private Int2IntHashMap map = new Int2IntHashMap(MISSING_VALUE);

    @Test
    public void shouldInitiallyBeEmpty()
    {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void getShouldReturnMissingValueWhenEmpty()
    {
        assertEquals(MISSING_VALUE, map.get(1));
    }

    @Test
    public void getShouldReturnMissingValueWhenThereIsNoElement()
    {
        map.put(1, 1);

        assertEquals(MISSING_VALUE, map.get(2));
    }

    @Test
    public void getShouldReturnPutValues()
    {
        map.put(1, 1);

        assertEquals(1, map.get(1));
    }

    @Test
    public void putShouldReturnOldValue()
    {
        map.put(1, 1);

        assertEquals(1, map.put(1, 2));
    }

    @Test
    public void clearShouldResetSize()
    {
        map.put(1, 1);
        map.put(100, 100);

        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void clearShouldRemoveValues()
    {
        map.put(1, 1);
        map.put(100, 100);

        map.clear();

        assertEquals(MISSING_VALUE, map.get(1));
        assertEquals(MISSING_VALUE, map.get(100));
    }

    @Test
    public void forEachShouldLoopOverEveryElement()
    {
        map.put(1, 1);
        map.put(100, 100);

        final IntIntConsumer mockConsumer = mock(IntIntConsumer.class);
        map.intForEach(mockConsumer);

        final InOrder inOrder = inOrder(mockConsumer);
        inOrder.verify(mockConsumer).accept(1, 1);
        inOrder.verify(mockConsumer).accept(100, 100);
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
        map.put(1, 1);

        assertTrue(map.containsKey(1));
    }

    @Test
    public void shouldNotContainValueForAMissingEntry()
    {
        assertFalse(map.containsValue(1));
    }

    @Test
    public void shouldContainValueForAPresentEntry()
    {
        map.put(1, 1);

        assertTrue(map.containsValue(1));
    }

    @Test
    public void shouldExposeValidKeySet()
    {
        map.put(1, 1);
        map.put(2, 2);

        assertCollectionContainsElements(map.keySet());
    }

    @Test
    public void shouldExposeValidValueSet()
    {
        map.put(1, 1);
        map.put(2, 2);

        assertCollectionContainsElements(map.values());
    }

    @Test
    public void shouldPutAllMembersOfAnotherHashMap()
    {
        addTwoElements();

        final Map<Integer, Integer> other = new HashMap<>();
        other.put(1, 2);
        other.put(3, 4);

        map.putAll(other);

        assertEquals(3, map.size());

        assertEquals(2, map.get(1));
        assertEquals(3, map.get(2));
        assertEquals(4, map.get(3));
    }

    @Test
    public void shouldIterateKeys()
    {
        addTwoElements();

        assertIteratesKeys();
    }

    @Test
    public void shouldIterateKeysFromBeginningEveryTime()
    {
        shouldIterateKeys();

        assertIteratesKeys();
    }

    @Test
    public void shouldIterateKeysWithoutHasNext()
    {
        addTwoElements();

        assertIterateKeysWithoutHasNext();
    }

    @Test
    public void shouldIterateKeysWithoutHasNextFromBeginningEveryTime()
    {
        shouldIterateKeysWithoutHasNext();

        assertIterateKeysWithoutHasNext();
    }

    @Test(expected = NoSuchElementException.class)
    public void shouldExceptionForEmptyIteration()
    {
        keyIterator().next();
    }

    @Test(expected = NoSuchElementException.class)
    public void shouldExceptionWhenRunningOutOfElements()
    {
        addTwoElements();

        final Iterator<Integer> iterator = keyIterator();
        iterator.next();
        iterator.next();
        iterator.next();
    }

    @Test
    public void shouldIterateValues()
    {
        addTwoElements();

        assertIteratesValues();
    }

    @Test
    public void shouldIterateValuesFromBeginningEveryTime()
    {
        shouldIterateValues();

        assertIteratesValues();
    }

    @Test
    public void entrySetShouldContainEntries()
    {
        addTwoElements();

        entrySetContainsTwoElements();
    }

    @Test
    public void entrySetIteratorShouldContainEntriesEveryIteration()
    {
        addTwoElements();

        entrySetContainsTwoElements();

        entrySetContainsTwoElements();
    }

    private void entrySetContainsTwoElements()
    {
        final Set<Entry<Integer, Integer>> entrySet = map.entrySet();
        assertEquals(2, entrySet.size());
        assertFalse(entrySet.isEmpty());

        final Iterator<Entry<Integer, Integer>> it = entrySet.iterator();
        assertTrue(it.hasNext());
        assertEntryIs(it.next(), 2, 3);
        assertTrue(it.hasNext());
        assertEntryIs(it.next(), 1, 1);
        assertFalse(it.hasNext());
    }

    @Test
    public void removeShouldReturnMissing()
    {
        assertEquals(MISSING_VALUE, map.remove(1));
    }

    @Test
    public void removeShouldReturnValueRemoved()
    {
        map.put(1, 2);

        assertEquals(2, map.remove(1));
    }

    @Test
    public void removeShouldRemoveEntry()
    {
        map.put(1, 2);

        map.remove(1);

        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(1));
        assertFalse(map.containsValue(2));
    }

    @Test
    public void shouldOnlyRemoveTheSpecifiedEntry()
    {
        IntStream.range(0, 8).forEach((i) -> map.put(i, i * 2));

        map.remove(5);

        IntStream
            .range(0, 8)
            .filter((i) -> i != 5L)
            .forEach(
                (i) ->
                {
                    assertTrue(map.containsKey(i));
                    assertTrue(map.containsValue(2 * i));
                });
    }

    @Test
    public void shouldResizeWhenMoreElementsAreAdded()
    {
        IntStream
            .range(0, 100)
            .forEach(
                (key) ->
                {
                    final int value = key * 2;
                    assertEquals(MISSING_VALUE, map.put(key, value));
                    assertEquals(value, map.get(key));
                });
    }

    @Test
    public void shouldHaveNoMinValueForEmptyCollection()
    {
        assertEquals(MISSING_VALUE, map.minValue());
    }

    @Test
    public void shouldFindMinValue()
    {
        map.put(1, 2);
        map.put(2, 10);
        map.put(3, -5);

        assertEquals(-5, map.minValue());
    }

    @Test
    public void shouldHaveNoMaxValueForEmptyCollection()
    {
        assertEquals(MISSING_VALUE, map.maxValue());
    }

    @Test
    public void shouldFindMaxValue()
    {
        map.put(1, 2);
        map.put(2, 10);
        map.put(3, -5);

        assertEquals(10, map.maxValue());
    }

    @Test
    public void sizeShouldReturnNumberOfEntries()
    {
        final int count = 100;
        for (int key = 0; key < count; key++)
        {
            map.put(key, 1);
        }

        assertEquals(count, map.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotSupportLoadFactorOfGreaterThanOne()
    {
        new Int2IntHashMap(4, 2, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotSupportLoadFactorOfOne()
    {
        new Int2IntHashMap(4, 1, 0);
    }

    @Test
    public void correctSizeAfterRehash() throws Exception
    {
        final Int2IntHashMap map = new Int2IntHashMap(16, 0.6f, -1);

        IntStream.range(1, 17).forEach(i -> map.put(i, i));
        assertEquals("Map has correct size", 16, map.size());

        final List<Integer> keys = new ArrayList<>(map.keySet());
        keys.forEach(map::remove);

        assertTrue("Map isn't empty", map.isEmpty());
    }

    private void assertEntryIs(final Entry<Integer, Integer> entry, final int expectedKey, final int expectedValue)
    {
        assertEquals(expectedKey, entry.getKey().intValue());
        assertEquals(expectedValue, entry.getValue().intValue());
    }

    private void assertCollectionContainsElements(final Collection<Integer> keys)
    {
        assertEquals(2, keys.size());
        assertFalse(keys.isEmpty());
        assertTrue(keys.contains(1));
        assertTrue(keys.contains(2));
        assertFalse(keys.contains(3));
        assertThat(keys, hasItems(1, 2));

        assertThat("iterator has failed to be reset", keys, hasItems(1, 2));
    }

    private void assertIteratesKeys()
    {
        final Iterator<Integer> it = keyIterator();
        assertContains(it, 2, 1);
    }

    private void assertIteratesValues()
    {
        final Iterator<Integer> it = map.values().iterator();
        assertContains(it, 3, 1);
    }

    private void assertContains(final Iterator<Integer> it, final int first, final int second)
    {
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(first), it.next());
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(second), it.next());
        assertFalse(it.hasNext());
    }

    private void addTwoElements()
    {
        map.put(1, 1);
        map.put(2, 3);
    }

    private void assertIterateKeysWithoutHasNext()
    {
        final Iterator<Integer> it = keyIterator();
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
    }

    private Iterator<Integer> keyIterator()
    {
        return map.keySet().iterator();
    }
}
