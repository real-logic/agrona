/*
 * Copyright 2014-2025 Real Logic Limited.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Int2IntHashMapTest
{
    static final int MISSING_VALUE = -1;

    Int2IntHashMap map;

    @BeforeEach
    void before()
    {
        map = new Int2IntHashMap(MISSING_VALUE);
    }

    @Test
    void shouldInitiallyBeEmpty()
    {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    void getShouldReturnMissingValueWhenEmpty()
    {
        assertEquals(MISSING_VALUE, map.get(1));
    }

    @Test
    void boxedGetShouldReturnNull()
    {
        assertNull(map.get((Integer)1));
    }

    @Test
    void getShouldReturnMissingValueWhenThereIsNoElement()
    {
        map.put(1, 1);

        assertEquals(MISSING_VALUE, map.get(2));
    }

    @Test
    void getShouldReturnPutValues()
    {
        map.put(1, 1);

        assertEquals(1, map.get(1));
    }

    @Test
    void putShouldReturnOldValue()
    {
        map.put(1, 1);

        assertEquals(1, map.put(1, 2));
    }

    @Test
    void clearShouldResetSize()
    {
        map.put(1, 1);
        map.put(100, 100);

        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    void clearShouldRemoveValues()
    {
        map.put(1, 1);
        map.put(100, 100);

        map.clear();

        assertEquals(MISSING_VALUE, map.get(1));
        assertEquals(MISSING_VALUE, map.get(100));
    }

    @Test
    void forEachShouldLoopOverEveryElement()
    {
        map.put(1, 1);
        map.put(100, 100);

        final IntIntConsumer mockConsumer = mock(IntIntConsumer.class);
        map.forEachInt(mockConsumer);

        final InOrder inOrder = inOrder(mockConsumer);
        inOrder.verify(mockConsumer).accept(100, 100);
        inOrder.verify(mockConsumer).accept(1, 1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldNotContainKeyOfAMissingKey()
    {
        assertFalse(map.containsKey(1));
    }

    @Test
    void shouldContainKeyOfAPresentKey()
    {
        map.put(1, 1);

        assertTrue(map.containsKey(1));
    }

    @Test
    void shouldNotContainValueForAMissingEntry()
    {
        assertFalse(map.containsValue(1));
    }

    @Test
    void shouldContainValueForAPresentEntry()
    {
        map.put(1, 1);

        assertTrue(map.containsValue(1));
    }

    @Test
    void shouldExposeValidKeySet()
    {
        map.put(1, 1);
        map.put(2, 2);

        assertCollectionContainsElements(map.keySet());
    }

    @Test
    void shouldExposeValidValueSet()
    {
        map.put(1, 1);
        map.put(2, 2);

        assertCollectionContainsElements(map.values());
    }

    @Test
    void shouldPutAllMembersOfAnotherHashMap()
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
    void shouldIterateKeys()
    {
        addTwoElements();

        assertIteratesKeys();
    }

    @Test
    void shouldIterateKeysFromBeginningEveryTime()
    {
        shouldIterateKeys();

        assertIteratesKeys();
    }

    @Test
    void shouldIterateKeysWithoutHasNext()
    {
        addTwoElements();

        assertIterateKeysWithoutHasNext();
    }

    @Test
    void shouldIterateKeysWithoutHasNextFromBeginningEveryTime()
    {
        shouldIterateKeysWithoutHasNext();

        assertIterateKeysWithoutHasNext();
    }

    @Test
    void shouldExceptionForEmptyIteration()
    {
        assertThrows(NoSuchElementException.class, () -> keyIterator().next());
    }

    @Test
    void shouldExceptionWhenRunningOutOfElements()
    {
        addTwoElements();

        final Iterator<Integer> iterator = keyIterator();
        iterator.next();
        iterator.next();

        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldIterateValues()
    {
        addTwoElements();

        assertIteratesValues();
    }

    @Test
    void shouldIterateValuesFromBeginningEveryTime()
    {
        shouldIterateValues();

        assertIteratesValues();
    }

    @Test
    void entrySetShouldContainEntries()
    {
        addTwoElements();

        entrySetContainsTwoElements();
    }

    @Test
    void entrySetIteratorShouldContainEntriesEveryIteration()
    {
        addTwoElements();

        entrySetContainsTwoElements();

        entrySetContainsTwoElements();
    }

    @Test
    void removeShouldReturnMissing()
    {
        assertEquals(MISSING_VALUE, map.remove(1));
    }

    @Test
    void removeShouldReturnValueRemoved()
    {
        map.put(1, 2);

        assertEquals(2, map.remove(1));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void removeShouldRemoveEntry()
    {
        map.put(1, 2);

        map.remove(1);

        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(1));
        assertFalse(map.containsValue(2));
    }

    @Test
    void shouldOnlyRemoveTheSpecifiedEntry()
    {
        IntStream.range(0, 8).forEach((i) -> map.put(i, i * 2));

        map.remove(5);

        IntStream
            .range(0, 8)
            .filter((i) -> i != 5L)
            .forEach((i) ->
            {
                assertTrue(map.containsKey(i));
                assertTrue(map.containsValue(2 * i));
            });
    }

    @Test
    void shouldResizeWhenMoreElementsAreAdded()
    {
        IntStream
            .range(0, 100)
            .forEach((key) ->
            {
                final int value = key * 2;
                assertEquals(MISSING_VALUE, map.put(key, value));
                assertEquals(value, map.get(key));
            });
    }

    @Test
    void shouldHaveNoMinValueForEmptyCollection()
    {
        assertEquals(MISSING_VALUE, map.minValue());
    }

    @Test
    void shouldFindMinValue()
    {
        addValues(map);

        assertEquals(-5, map.minValue());
    }

    @Test
    void shouldHaveNoMaxValueForEmptyCollection()
    {
        assertEquals(MISSING_VALUE, map.maxValue());
    }

    @Test
    void shouldFindMaxValue()
    {
        addValues(map);

        assertEquals(10, map.maxValue());
    }

    @Test
    void sizeShouldReturnNumberOfEntries()
    {
        final int count = 100;
        for (int key = 0; key < count; key++)
        {
            map.put(key, 1);
        }

        assertEquals(count, map.size());
    }

    @Test
    void shouldNotSupportLoadFactorOfGreaterThanOne()
    {
        assertThrows(IllegalArgumentException.class, () -> new Int2IntHashMap(4, 2, 0));
    }

    @Test
    void shouldNotSupportLoadFactorOfOne()
    {
        assertThrows(IllegalArgumentException.class, () -> new Int2IntHashMap(4, 1, 0));
    }

    @Test
    void correctSizeAfterRehash()
    {
        final Int2IntHashMap map = new Int2IntHashMap(16, 0.6f, -1);

        IntStream.range(1, 17).forEach((i) -> map.put(i, i));
        assertEquals(16, map.size(), "Map has correct size");

        final List<Integer> keys = new ArrayList<>(map.keySet());
        keys.forEach(map::remove);

        assertTrue(map.isEmpty(), "Map isn't empty");
    }

    @Test
    void shouldComputeIfAbsent()
    {
        final int testKey = 7;
        final int testValue = 7;
        final int testValue2 = 8;

        assertEquals(map.missingValue(), map.get(testKey));

        assertThat(map.computeIfAbsent(testKey, (i) -> testValue), is(testValue));
        assertThat(map.get(testKey), is(testValue));

        assertThat(map.computeIfAbsent(testKey, (i) -> testValue2), is(testValue));
        assertThat(map.get(testKey), is(testValue));
    }

    @Test
    void shouldComputeIfAbsentBoxed()
    {
        final Map<Integer, Integer> map = this.map;

        final int testKey = 7;
        final int testValue = 7;
        final int testValue2 = 8;

        assertThat(map.computeIfAbsent(testKey, (i) -> testValue), is(testValue));
        assertThat(map.get(testKey), is(testValue));

        assertThat(map.computeIfAbsent(testKey, (i) -> testValue2), is(testValue));
        assertThat(map.get(testKey), is(testValue));
    }

    @Test
    void shouldComputeIfPresent()
    {
        final int testKey = 7;
        final int testValue = 7;
        final int testValue2 = 8;

        assertThat(map.computeIfPresent(testKey, (k, v) -> testValue), is(map.missingValue()));
        assertThat(map.get(testKey), is(map.missingValue()));

        map.put(testKey, testValue);
        assertThat(map.computeIfPresent(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(map.get(testKey), is(testValue2));
    }

    @Test
    void shouldComputeIfPresentBoxed()
    {
        final Map<Integer, Integer> map = this.map;

        final int testKey = 7;
        final int testValue = 7;
        final int testValue2 = 8;

        assertNull(map.computeIfPresent(testKey, (k, v) -> testValue));
        assertNull(map.get(testKey));

        map.put(testKey, testValue);
        assertThat(map.computeIfPresent(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(map.get(testKey), is(testValue2));
    }

    @Test
    void computeIfPresentShouldDeleteEntryIfMissingValue()
    {
        final int key = 707070707;
        final int value = Integer.MIN_VALUE;
        map.put(key, value);

        assertEquals(MISSING_VALUE, map.computeIfPresent(key, (k, v) -> MISSING_VALUE));

        assertEquals(0, map.size());
        assertEquals(MISSING_VALUE, map.get(key));
    }

    @Test
    void shouldCompute()
    {
        final int testKey = 7;
        final int testValue = 7;
        final int testValue2 = 8;

        assertEquals(map.missingValue(), map.get(testKey));
        assertThat(map.compute(testKey, (k, v) -> testValue), is(testValue));
        assertThat(map.get(testKey), is(testValue));

        assertThat(map.compute(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(map.get(testKey), is(testValue2));
    }

    @Test
    void shouldComputeBoxed()
    {
        final Map<Integer, Integer> map = this.map;

        final int testKey = 7;
        final int testValue = 7;
        final int testValue2 = 8;

        assertThat(map.compute(testKey, (k, v) -> testValue), is(testValue));
        assertThat(map.get(testKey), is(testValue));
        assertEquals(1, map.size());

        assertThat(map.compute(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(map.get(testKey), is(testValue2));
        assertEquals(1, map.size());
    }

    @Test
    void computeShouldDeleteKeyMappingIfMissingValue()
    {
        final int key = MISSING_VALUE;
        final int value = 0;
        map.put(key, value);

        assertEquals(MISSING_VALUE, map.compute(key, (k, v) -> MISSING_VALUE));

        assertEquals(0, map.size());
    }

    @Test
    void computeIsANoOpIfKeyIsUnknownAndMissingValue()
    {
        final int key = -303;
        final int value = 404;
        map.put(key, value);

        assertEquals(MISSING_VALUE, map.compute(999, (k, v) -> MISSING_VALUE));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
    }

    @Test
    void shouldNotAllowMissingValueAsValue()
    {
        assertThrows(IllegalArgumentException.class, () -> map.put(1, MISSING_VALUE));
    }

    @Test
    void shouldAllowMissingValueAsKey()
    {
        map.put(MISSING_VALUE, 1);

        assertEquals(1, map.get(MISSING_VALUE));
        assertTrue(map.containsKey(MISSING_VALUE));
        assertEquals(1, map.size());

        final int[] tuple = new int[2];
        map.forEachInt((k, v) ->
        {
            tuple[0] = k;
            tuple[1] = v;
        });

        assertEquals(MISSING_VALUE, tuple[0]);
        assertEquals(1, tuple[1]);

        assertEquals(1, map.remove(MISSING_VALUE));
        assertEquals(0, map.size());
        assertEquals(MISSING_VALUE, map.get(MISSING_VALUE));
    }

    @Test
    void shouldNotContainMissingValue()
    {
        assertFalse(map.containsValue(MISSING_VALUE));
        map.put(MISSING_VALUE, 1);
        assertFalse(map.containsValue(MISSING_VALUE));
    }

    @Test
    void emptyMapsShouldBeEqual()
    {
        assertEquals(map, new Int2IntHashMap(MISSING_VALUE));
        assertEquals(map, new HashMap<Integer, Integer>());
    }

    @Test
    void shouldEqualPrimitiveMapWithSameContents()
    {
        final Int2IntHashMap otherMap = new Int2IntHashMap(MISSING_VALUE);

        addValues(map);
        addValues(otherMap);

        assertEquals(map, otherMap);
    }

    @Test
    void shouldEqualPrimitiveMapWithSameContentsAndDifferentMissingValue()
    {
        final Int2IntHashMap otherMap = new Int2IntHashMap(-2);

        addValues(map);
        addValues(otherMap);

        assertEquals(map, otherMap);
    }

    @Test
    void shouldEqualHashMapWithSameContents()
    {
        final Map<Integer, Integer> otherMap = new HashMap<>();

        addValues(map);
        addValues(otherMap);

        assertEquals(map, otherMap);
    }

    @Test
    void shouldNotEqualPrimitiveMapWithDifferentContents()
    {
        final Int2IntHashMap otherMap = new Int2IntHashMap(MISSING_VALUE);

        addValues(map);
        addAValue(otherMap);

        assertNotEquals(map, otherMap);
    }

    @Test
    void shouldNotEqualHashMapWithDifferentContents()
    {
        final Map<Integer, Integer> otherMap = new HashMap<>();

        addValues(map);
        addAValue(otherMap);

        assertNotEquals(map, otherMap);
    }

    @Test
    void emptyMapsShouldHaveEqualHashCodes()
    {
        assertHashcodeEquals(map, new Int2IntHashMap(MISSING_VALUE));
        assertHashcodeEquals(map, new HashMap<Integer, Integer>());
    }

    @Test
    void shouldHaveEqualHashcodePrimitiveMapWithSameContents()
    {
        final Int2IntHashMap otherMap = new Int2IntHashMap(MISSING_VALUE);

        addValues(map);
        addValues(otherMap);

        assertHashcodeEquals(map, otherMap);
    }

    @Test
    void shouldHaveEqualHashcodePrimitiveMapWithSameContentsAndDifferentMissingValue()
    {
        final Int2IntHashMap otherMap = new Int2IntHashMap(-2);

        addValues(map);
        addValues(otherMap);

        assertHashcodeEquals(map, otherMap);
    }

    @Test
    void shouldHaveEqualHashcodeHashMapWithSameContents()
    {
        final Map<Integer, Integer> otherMap = new HashMap<>();

        addValues(map);
        addValues(otherMap);

        assertHashcodeEquals(map, otherMap);
    }

    @Test
    void shouldNotHaveEqualHashcodePrimitiveMapWithDifferentContents()
    {
        final Int2IntHashMap otherMap = new Int2IntHashMap(MISSING_VALUE);

        addValues(map);
        addAValue(otherMap);

        assertHashcodeNotEquals(map, otherMap);
    }

    @Test
    void shouldNotHaveEqualHashcodeHashMapWithDifferentContents()
    {
        final Map<Integer, Integer> otherMap = new HashMap<>();

        addValues(map);
        addAValue(otherMap);

        assertHashcodeNotEquals(map, otherMap);
    }

    @Test
    void shouldComputeIfAbsentUsingImplementation()
    {
        final Int2IntHashMap int2IntHashMap = new Int2IntHashMap(-1);
        final int key = 0;
        final int result = int2IntHashMap.computeIfAbsent(key, (k) -> k);
        assertEquals(key, result);
    }

    @Test
    void shouldComputeIfAbsentUsingInterface()
    {
        final Map<Integer, Integer> map = new Int2IntHashMap(-1);
        final int key = 0;
        final int result = map.computeIfAbsent(key, (k) -> k);
        assertEquals(key, result);
    }

    @Test
    void shouldGenerateStringRepresentation()
    {
        final int[] testEntries = { 3, 1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            map.put(testEntry, testEntry + 1000);
        }

        final String mapAsAString = "{19=1019, 1=1001, 11=1011, 7=1007, 3=1003, 12=1012}";
        assertEquals(mapAsAString, map.toString());
    }

    @Test
    void shouldIterateEntriesBySpecialisedType()
    {
        final Map<Integer, Integer> expected = new HashMap<>();
        final Int2IntHashMap map = new Int2IntHashMap(Integer.MIN_VALUE);

        IntStream.range(1, 10).forEachOrdered((i) ->
        {
            map.put(i, -i);
            expected.put(i, -i);
        });

        final Map<Integer, Integer> actual = new HashMap<>();
        final Int2IntHashMap.EntryIterator iter = map.entrySet().iterator();
        while (iter.hasNext())
        {
            iter.next();
            actual.put(iter.getIntKey(), iter.getIntValue());
        }

        assertEquals(expected, actual);
    }

    @Test
    void shouldIterateEntriesBySpecialisedTypeAndSetValue()
    {
        final Map<Integer, Integer> expected = new HashMap<>();
        final Int2IntHashMap map = new Int2IntHashMap(Integer.MIN_VALUE);

        IntStream.range(1, 10).forEachOrdered((i) ->
        {
            map.put(i, -i);
            expected.put(i, -i * 10);
        });

        final Int2IntHashMap.EntryIterator iter = map.entrySet().iterator();
        while (iter.hasNext())
        {
            iter.next();
            iter.setValue(iter.getIntValue() * 10);
        }

        assertEquals(expected, map);
    }

    @Test
    void shouldCopyConstructAndBeEqual()
    {
        final int[] testEntries = { 3, 1, 19, 7, 11, 12, 7 };

        final Int2IntHashMap map = new Int2IntHashMap(Integer.MIN_VALUE);
        for (final int testEntry : testEntries)
        {
            map.put(testEntry, testEntry + 1);
        }

        final Int2IntHashMap mapCopy = new Int2IntHashMap(map);
        assertEquals(map, mapCopy);
    }

    @Test
    void shouldToArray()
    {
        final Int2IntHashMap map = new Int2IntHashMap(-127);
        map.put(1, 11);
        map.put(2, 12);
        map.put(3, 13);

        final Object[] array = map.entrySet().toArray();
        for (final Object entry : array)
        {
            map.remove(((Int2IntHashMap.EntryIterator.MapEntry)entry).getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void shouldToArrayTyped()
    {
        final Int2IntHashMap map = new Int2IntHashMap(-127);
        map.put(1, 11);
        map.put(2, 12);
        map.put(3, 13);

        final Entry<?, ?>[] type = new Entry[1];
        final Entry<?, ?>[] array = map.entrySet().toArray(type);
        for (final Entry<?, ?> entry : array)
        {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void shouldToArrayWithArrayListConstructor()
    {
        final Int2IntHashMap map = new Int2IntHashMap(-127);
        map.put(1, 11);
        map.put(2, 12);
        map.put(3, 13);

        final List<Map.Entry<Integer, Integer>> list = new ArrayList<>(map.entrySet());
        for (final Map.Entry<Integer, Integer> entry : list)
        {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void shouldCompactMap()
    {
        final Int2IntHashMap map = new Int2IntHashMap(2, 0.9f, Integer.MIN_VALUE);
        assertEquals(8, map.capacity());
        assertEquals(7, map.resizeThreshold());
        assertEquals(0, map.size());

        for (int i = 1; i <= 16; i++)
        {
            map.put(i, i * -123);
        }
        assertEquals(32, map.capacity());
        assertEquals(28, map.resizeThreshold());
        assertEquals(16, map.size());

        for (int i = 6; i <= 16; i++)
        {
            assertEquals(-123 * i, map.remove(i));
        }
        assertEquals(32, map.capacity());
        assertEquals(28, map.resizeThreshold());
        assertEquals(5, map.size());

        map.compact();
        assertEquals(8, map.capacity());
        assertEquals(7, map.resizeThreshold());
        assertEquals(5, map.size());
    }

    @Test
    void getOrDefaultShouldReturnADefaultValueWhenNoMappingExists()
    {
        final int key = 42;
        final int defaultValue = 8;

        assertEquals(defaultValue, map.getOrDefault(key, defaultValue));
    }

    @Test
    void getOrDefaultShouldReturnAValueWhenExists()
    {
        final int key = 42;
        final int value = 21;
        final int defaultValue = 8;
        map.put(key, value);

        assertEquals(value, map.getOrDefault(key, defaultValue));
    }

    @Test
    void mergeThrowsIllegalArgumentExceptionIfValueIsMissingValue()
    {
        final int missingValue = 42;
        final Int2IntHashMap map = new Int2IntHashMap(missingValue);
        final int key = -9;
        final IntIntFunction remappingFunction = mock(IntIntFunction.class);

        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> map.merge(key, missingValue, remappingFunction));
        assertEquals("cannot accept missingValue", exception.getMessage());
    }

    @Test
    void mergeShouldPutANewKeyIntoTheMap()
    {
        final int key = -9;
        final int value = 113;
        final IntIntFunction remappingFunction = mock(IntIntFunction.class);

        assertEquals(value, map.merge(key, value, remappingFunction));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
        verifyNoInteractions(remappingFunction);
    }

    @Test
    void mergeShouldReplaceAnExistingValueInTheMap()
    {
        final int key = 42;
        final int oldValue = 0;
        final int value = 5;
        final IntIntFunction remappingFunction = (val1, val2) -> val1 + val2 + 3;
        map.put(key, oldValue);

        assertEquals(8, map.merge(key, value, remappingFunction));
        assertEquals(1, map.size());
        assertEquals(8, map.get(key));

        assertEquals(-6, map.merge(key, -17, remappingFunction));
        assertEquals(1, map.size());
        assertEquals(-6, map.get(key));
    }

    @Test
    void mergeShouldRemoveTheExistingMappingIfRemappingToAMissingValue()
    {
        final int key = 8;
        final int oldValue = -17;
        final int value = 5;
        final IntIntFunction remappingFunction = (val1, val2) -> MISSING_VALUE;
        map.put(key, oldValue);

        assertEquals(MISSING_VALUE, map.merge(key, value, remappingFunction));

        assertEquals(0, map.size());
    }

    @Test
    void mergeShouldRejectAMissingValue()
    {
        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> map.merge(42, MISSING_VALUE, (v1, v2) -> 1000));
        assertEquals("cannot accept missingValue", exception.getMessage());
    }

    @Test
    void putIfAbsentReturnsAnExistingValue()
    {
        final int key = 52;
        final int value = -19;
        final int newValue = Integer.MIN_VALUE;
        map.put(key, value);

        assertEquals(value, map.putIfAbsent(key, newValue));

        assertEquals(value, map.get(key));
    }

    @Test
    void putIfAbsentPutANewMappingForANewKey()
    {
        final int key = 52;
        final int newValue = Integer.MIN_VALUE;

        assertEquals(MISSING_VALUE, map.putIfAbsent(key, newValue));

        assertEquals(newValue, map.get(key));
    }

    @Test
    void replaceShouldReturnMissingValueForAnUnknownKey()
    {
        final int key = 42;
        final int value = 8;
        map.put(1, 2);

        assertEquals(MISSING_VALUE, map.replace(key, value));

        assertEquals(1, map.size());
        assertEquals(2, map.get(1));
    }

    @Test
    void replaceShouldOverwriteAnExistingValue()
    {
        final int key = 42;
        final int value = 8;
        final int newValue = Integer.MAX_VALUE;
        map.put(key, value);

        assertEquals(value, map.replace(key, newValue));

        assertEquals(1, map.size());
        assertEquals(newValue, map.get(key));
    }

    @Test
    void replaceReturnsFalseForAnUnknownKey()
    {
        final int key = 42;
        final int value = 8;
        final int newValue = 0;
        map.put(1, 2);
        map.put(2, 3);

        assertFalse(map.replace(key, value, newValue));

        assertEquals(2, map.size());
        assertEquals(2, map.get(1));
        assertEquals(3, map.get(2));
    }

    @Test
    void replaceReturnsFalseForIfOldValueIsNotCorrect()
    {
        final int key = 42;
        final int value = 8;
        final int oldValue = -value;
        final int newValue = 0;
        map.put(key, value);

        assertFalse(map.replace(key, oldValue, newValue));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
    }

    @Test
    void replaceReturnsTrueAfterUpdatingTheValueOfAnExistingKeyIfTheOldValueMatches()
    {
        final int key = 42;
        final int value = 8;
        final int newValue = 100;
        map.put(key, value);

        assertTrue(map.replace(key, value, newValue));

        assertEquals(newValue, map.get(key));
    }

    @Test
    void replaceAllIntShouldThrowIllegalArgumentExceptionIfANewValueIsAMissingValue()
    {
        final IntIntFunction function = (key, value) -> MISSING_VALUE;
        map.put(1, 2);

        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> map.replaceAllInt(function));
        assertEquals("cannot replace with a missingValue", exception.getMessage());
    }

    @Test
    void replaceAllIntShouldUpdateAllExistingValues()
    {
        final IntIntFunction function = (key, value) -> value / 10;
        map.put(1, 10);
        map.put(2, 20);
        map.put(-4, 40);

        map.replaceAllInt(function);

        assertEquals(3, map.size());
        assertEquals(1, map.get(1));
        assertEquals(2, map.get(2));
        assertEquals(4, map.get(-4));
    }

    @Test
    void replaceAllShouldThrowIllegalArgumentExceptionIfANewValueIsAMissingValue()
    {
        final BiFunction<Integer, Integer, Integer> function = (key, value) -> MISSING_VALUE;
        map.put(1, 2);

        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> map.replaceAll(function));
        assertEquals("cannot replace with a missingValue", exception.getMessage());
    }

    @Test
    void replaceAllShouldUpdateAllExistingValues()
    {
        final BiFunction<Integer, Integer, Integer> function = (key, value) -> value / 10;
        map.put(1, 10);
        map.put(2, 20);
        map.put(-4, 40);

        map.replaceAll(function);

        assertEquals(3, map.size());
        assertEquals(1, map.get(1));
        assertEquals(2, map.get(2));
        assertEquals(4, map.get(-4));
    }

    @Test
    void removeIsANoOpIfTheValueIsAMissingValue()
    {
        final int key = 5;
        final int value = 18;
        map.put(key, value);

        assertFalse(map.remove(key, MISSING_VALUE));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
    }

    @Test
    void removeIsANoOpIfTheValueIsWrong()
    {
        final int key = 5;
        final int value = 18;
        map.put(key, value);

        assertFalse(map.remove(key, 34));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
    }

    @Test
    void removeDeleteAMappingWhenAKeyAndValueMatch()
    {
        final int key = 5;
        final int value = 18;
        map.put(key, value);

        assertTrue(map.remove(key, value));

        assertEquals(0, map.size());
        assertEquals(MISSING_VALUE, map.get(key));
    }

    @Test
    void putAllCopiesAllValuesFromTheSourceMap()
    {
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        final Int2IntHashMap otherMap = new Int2IntHashMap(Integer.MIN_VALUE);
        otherMap.put(-100, -100);
        otherMap.put(1, 10);
        otherMap.put(3, 30);
        otherMap.put(5, 50);

        map.putAll(otherMap);

        assertEquals(5, map.size());
        assertEquals(10, map.get(1));
        assertEquals(2, map.get(2));
        assertEquals(30, map.get(3));
        assertEquals(50, map.get(5));
        assertEquals(-100, map.get(-100));
    }

    @Test
    void putAllThrowsIllegalArgumentExceptionIfOtherMapContainsMissingValue()
    {
        map.put(1, 1);
        final Int2IntHashMap otherMap = new Int2IntHashMap(Integer.MIN_VALUE);
        otherMap.put(MISSING_VALUE, MISSING_VALUE);

        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> map.putAll(otherMap));
        assertEquals("cannot accept missingValue", exception.getMessage());
    }

    @Test
    void removeIfIntOnKeySet()
    {
        final IntPredicate filter = (v) -> v < 3;
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        map.put(4, 4);

        assertTrue(map.keySet().removeIfInt(filter));

        assertEquals(2, map.size());
        assertEquals(3, map.get(3));
        assertEquals(4, map.get(4));

        assertFalse(map.keySet().removeIfInt(filter));
        assertEquals(2, map.size());
    }

    @Test
    void removeIfIntOnValuesCollection()
    {
        final IntPredicate filter = (v) -> v >= 20;
        map.put(1, 10);
        map.put(2, 20);
        map.put(3, 30);
        map.put(4, 40);

        assertTrue(map.values().removeIfInt(filter));

        assertEquals(1, map.size());
        assertEquals(10, map.get(1));

        assertFalse(map.values().removeIfInt(filter));
        assertEquals(1, map.size());
    }

    @Test
    void removeIfIntOnEntrySet()
    {
        final IntIntPredicate filter = (k, v) -> k >= 2 && v <= 30;
        map.put(1, 10);
        map.put(2, 20);
        map.put(3, 30);
        map.put(4, 40);

        assertTrue(map.entrySet().removeIfInt(filter));

        assertEquals(2, map.size());
        assertEquals(10, map.get(1));
        assertEquals(40, map.get(4));

        assertFalse(map.entrySet().removeIfInt(filter));
        assertEquals(2, map.size());
    }

    @Test
    void shouldRemoveAnExistingKeyMapping()
    {
        final int key = 42;
        final int value = -1_000;
        map.put(key, value);

        assertEquals(value, map.remove(key));

        assertEquals(0, map.size());
    }

    @Test
    void shouldNotRemoveAnNonExistingKey()
    {
        final int key = 42;
        final int value = -1_000;
        map.put(key, value);

        assertEquals(MISSING_VALUE, map.remove(0));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
    }

    @Test
    void shouldRemoveByKeyAndValue()
    {
        final int key = 42;
        final int value = -1_000;
        map.put(key, value);

        assertTrue(map.remove(key, value));

        assertEquals(0, map.size());
    }

    @Test
    void shouldNotRemoveIfKeyDoesNotMatch()
    {
        final int key = 42;
        final int value = -1_000;
        map.put(key, value);

        assertFalse(map.remove(0, value));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
    }

    @Test
    void shouldNotRemoveIfValueDoesNotMatch()
    {
        final int key = 42;
        final int value = -1_000;
        map.put(key, value);

        assertFalse(map.remove(key, 42));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
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

    private void addValues(final Map<Integer, Integer> map)
    {
        map.put(1, 2);
        map.put(2, 10);
        map.put(3, -5);
    }

    private void addAValue(final Map<Integer, Integer> map)
    {
        map.put(5, 10);
    }

    private void assertHashcodeEquals(final Object expected, final Object value)
    {
        assertEquals(
            expected.hashCode(),
            value.hashCode(),
            value + " should have the same hashcode as " + expected);
    }

    private void assertHashcodeNotEquals(final Object unexpected, final Object value)
    {
        assertNotEquals(
            unexpected.hashCode(),
            value.hashCode(),
            value + " should not have the same hashcode as " + unexpected);
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
}
