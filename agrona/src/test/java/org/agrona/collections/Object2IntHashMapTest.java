/*
 * Copyright 2014-2024 Real Logic Limited.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Object2IntHashMapTest
{
    static final int MISSING_VALUE = -1;
    final Object2IntHashMap<String> objectToIntMap;

    Object2IntHashMapTest()
    {
        objectToIntMap = newMap(Hashing.DEFAULT_LOAD_FACTOR, Object2IntHashMap.MIN_CAPACITY);
    }

    <T> Object2IntHashMap<T> newMap(final float loadFactor, final int initialCapacity)
    {
        return new Object2IntHashMap<>(initialCapacity, loadFactor, MISSING_VALUE);
    }

    @Test
    void shouldDoPutAndThenGet()
    {
        final String key = "Seven";
        objectToIntMap.put(key, 7);

        assertThat(objectToIntMap.get(key), is(7));
    }

    @Test
    void shouldReplaceExistingValueForTheSameKey()
    {
        final int value = 7;
        final String key = "Seven";
        objectToIntMap.put(key, value);

        final int newValue = 8;
        final int oldValue = objectToIntMap.put(key, newValue);

        assertThat(objectToIntMap.get(key), is(newValue));
        assertThat(oldValue, is(value));
        assertThat(objectToIntMap.size(), is(1));
    }

    @Test
    void replaceReturnMissingValueForAnUnknownKey()
    {
        final int missingValue = -127;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        map.put("one", 1);

        assertEquals(missingValue, map.replace("what is that", 100));
    }

    @Test
    void replaceIsANoOpIfKeyIsUnknown()
    {
        objectToIntMap.put("one", 0);

        assertFalse(objectToIntMap.replace("other", 1, 2));

        assertEquals(1, objectToIntMap.size());
        assertEquals(0, objectToIntMap.getValue("one"));
    }

    @Test
    void replaceIsANoOpIfOldValueIsWrong()
    {
        final String key = "one";
        final int value = 1;
        objectToIntMap.put(key, value);

        assertFalse(objectToIntMap.replace(key, value * 10, 999999999));

        assertEquals(value, objectToIntMap.getValue(key));
    }

    @Test
    void replaceChangesExistingMappingToAGivenValue()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(-1);
        final String key = "one";
        final int value = 1;
        final int newValue = 1111111111;
        map.put(key, value);
        map.put("other", 222);

        assertTrue(map.replace(key, value, newValue));

        assertEquals(2, map.size());
        assertEquals(newValue, map.getValue(key));
        assertEquals(222, map.getValue("other"));
    }

    @Test
    void replaceWithOldAndNewValueThrowsIllegalArgumentExceptionIfNewValueIsAMissingValue()
    {
        final int missingValue = -555;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);

        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> map.replace("key", 1, missingValue));
        assertEquals("cannot accept missingValue", exception.getMessage());
        assertTrue(map.isEmpty());
    }

    @Test
    void replaceThrowsIllegalArgumentExceptionIfNewValueIsAMissingValue()
    {
        final int missingValue = 78;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);

        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> map.replace("key", missingValue));
        assertEquals("cannot accept missingValue", exception.getMessage());
        assertTrue(map.isEmpty());
    }

    @Test
    void shouldGrowWhenThresholdExceeded()
    {
        final float loadFactor = 0.5f;
        final int initialCapacity = 32;
        final Object2IntHashMap<String> map = newMap(loadFactor, initialCapacity);
        for (int i = 0; i < 16; i++)
        {
            map.put(Integer.toString(i), i);
        }

        assertThat(map.resizeThreshold(), is(16));
        assertThat(map.capacity(), is(initialCapacity));
        assertThat(map.size(), is(16));

        map.put("16", 16);

        assertThat(map.resizeThreshold(), is(initialCapacity));
        assertThat(map.capacity(), is(64));
        assertThat(map.size(), is(17));

        assertThat(map.getValue("16"), equalTo(16));
        assertThat((double)loadFactor, closeTo(map.loadFactor(), 0.0f));
    }


    @Test
    void shouldHandleCollisionAndThenLinearProbe()
    {
        final float loadFactor = 0.5f;
        final int initialCapacity = 32;
        final Object2IntHashMap<Integer> map = newMap(loadFactor, initialCapacity);
        final int value = 7;
        final Integer key = 7; //codeql[java/non-null-boxed-variable]

        map.put(key, value);

        final int collisionKey = key + map.capacity();
        final int collisionValue = collisionKey + 1;

        map.put(Integer.valueOf(collisionKey), collisionValue);

        assertThat(map.get(key), is(value));
        assertThat(map.get(collisionKey), is(collisionValue));
        assertThat((double)loadFactor, closeTo(map.loadFactor(), 0.0f));
    }

    @Test
    void shouldClearCollection()
    {
        for (int i = 0; i < 15; i++)
        {
            objectToIntMap.put(Integer.toString(i), i);
        }

        assertThat(objectToIntMap.size(), is(15));
        assertThat(objectToIntMap.getValue(Integer.toString(1)), is(1));

        objectToIntMap.clear();

        assertEquals(MISSING_VALUE, objectToIntMap.getValue("1"));
        assertTrue(objectToIntMap.isEmpty());
    }

    @Test
    void shouldCompactCollection()
    {
        final int totalItems = 50;
        for (int i = 0; i < totalItems; i++)
        {
            objectToIntMap.put(Integer.toString(i), i);
        }

        for (int i = 0, limit = totalItems - 4; i < limit; i++)
        {
            objectToIntMap.remove(Integer.toString(i));
        }

        final int capacityBeforeCompaction = objectToIntMap.capacity();
        objectToIntMap.compact();

        assertThat(objectToIntMap.capacity(), lessThan(capacityBeforeCompaction));
    }

    @Test
    void shouldCompute()
    {
        final String testKey = "Seven";
        final int testValue = 7;
        final int testValue2 = -7;

        assertThat(objectToIntMap.getValue(testKey), is(objectToIntMap.missingValue()));
        assertThat(objectToIntMap.compute(testKey, (final String k, final int v) -> testValue), is(testValue));
        assertThat(objectToIntMap.getValue(testKey), is(testValue));

        assertThat(objectToIntMap.compute(testKey, (final String k, final int v) -> testValue2), is(testValue2));
        assertThat(objectToIntMap.getValue(testKey), is(testValue2));
    }

    @Test
    void shouldComputeBoxed()
    {
        final String testKey = "Seven";
        final int testValue = 17;
        final int testValue2 = -71;

        final BiFunction<String, Integer, Integer> function1 =
            (k, v) ->
            {
                assertEquals(testKey, k);
                assertNull(v);
                return testValue;
            };
        assertThat(objectToIntMap.compute(testKey, function1), is(testValue));
        assertThat(objectToIntMap.get(testKey), is(testValue));

        final BiFunction<String, Integer, Integer> function2 =
            (k, v) ->
            {
                assertEquals(testKey, k);
                assertEquals(testValue, v);
                return testValue2;
            };
        assertThat(objectToIntMap.compute(testKey, function2), is(testValue2));
        assertThat(objectToIntMap.get(testKey), is(testValue2));

        final BiFunction<String, Integer, Integer> function3 =
            (k, v) ->
            {
                assertEquals(testKey, k);
                assertEquals(testValue2, v);
                return null;
            };
        assertNull(objectToIntMap.compute(testKey, function3));
        assertFalse(objectToIntMap.containsKey(testKey));
    }

    @Test
    void computeIsANoOpIfFunctionReturnsMissingValueAndTheKeyDoesNotExist()
    {
        final int missingValue = -127;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final ObjectIntToIntFunction<String> function = (k, v) -> missingValue;
        map.put("one", 1);

        assertEquals(missingValue, map.compute("other", function));

        assertEquals(1, map.size());
        assertEquals(1, map.getValue("one"));
    }

    @Test
    void computeDeletesExistingEntryIfFunctionReturnsMissingValue()
    {
        final int missingValue = 42;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final ObjectIntToIntFunction<String> function = (k, v) -> missingValue;
        final String key = "one";
        final int value = 1;
        map.put(key, value);
        map.put("other", Integer.MAX_VALUE);

        assertEquals(missingValue, map.compute(key, function));

        assertEquals(1, map.size());
        assertEquals(Integer.MAX_VALUE, map.getValue("other"));
        assertFalse(map.containsKey(key));
    }

    @Test
    void computeUpdatesExistingEntry()
    {
        final int missingValue = 42;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final ObjectIntToIntFunction<String> function = (k, v) -> Integer.MAX_VALUE - 1 + v;
        final String key = "one";
        final int oldValue = 1;
        final int newValue = Integer.MAX_VALUE;
        map.put(key, oldValue);
        map.put("two", 2);

        assertEquals(newValue, map.compute(key, function));

        assertEquals(2, map.size());
        assertEquals(newValue, map.getValue(key));
        assertEquals(2, map.getValue("two"));
    }

    @Test
    void computeUpdatesInsertANewEntryIfDoesNotExist()
    {
        final int missingValue = -100;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final String key = "one";
        final int newValue = 42;
        final ObjectIntToIntFunction<String> function = (k, v) -> newValue;
        map.put("two", 2);

        assertEquals(newValue, map.compute(key, function));

        assertEquals(2, map.size());
        assertEquals(newValue, map.getValue(key));
        assertEquals(2, map.getValue("two"));
    }

    @Test
    void shouldComputeIfAbsent()
    {
        final String testKey = "Seven";
        final int testValue = 7;
        final int testValue2 = -7;

        assertThat(objectToIntMap.getValue(testKey), is(objectToIntMap.missingValue()));

        final ToIntFunction<String> function = (i) -> testValue;
        assertThat(objectToIntMap.computeIfAbsent(testKey, function), is(testValue));
        assertThat(objectToIntMap.getValue(testKey), is(testValue));

        final ToIntFunction<String> function2 = (i) -> testValue2;
        assertThat(objectToIntMap.computeIfAbsent(testKey, function2), is(testValue));
        assertThat(objectToIntMap.getValue(testKey), is(testValue));
    }

    @Test
    void shouldComputeIfAbsentBoxed()
    {
        final Map<String, Integer> objectToIntMap = this.objectToIntMap;

        final String testKey = "Seven";
        final int testValue = 7;
        final int testValue2 = -7;

        assertThat(objectToIntMap.computeIfAbsent(testKey, (i) -> testValue), is(testValue));
        assertThat(objectToIntMap.get(testKey), is(testValue));

        assertThat(objectToIntMap.computeIfAbsent(testKey, (i) -> testValue2), is(testValue));
        assertThat(objectToIntMap.get(testKey), is(testValue));
    }

    @Test
    void computeIfAbsentIsANoOpIfTheFunctionReturnsMissingValue()
    {
        final int missingValue = -333;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final ToIntFunction<String> function = (k) -> missingValue;
        map.put("one", 1);

        assertEquals(missingValue, map.computeIfAbsent("two", function));

        assertEquals(1, map.size());
        assertEquals(1, map.getValue("one"));
    }

    @Test
    void shouldComputeIfPresent()
    {
        final String testKey = "Seven";
        final int testValue = 7;
        final int testValue2 = -7;
        final int missingValue = objectToIntMap.missingValue();

        assertThat(objectToIntMap.computeIfPresent(testKey, (final String k, final int v) -> testValue),
            is(missingValue));
        assertThat(objectToIntMap.getValue(testKey), is(missingValue));

        objectToIntMap.put(testKey, testValue);
        assertThat(objectToIntMap.computeIfPresent(testKey, (final String k, final int v) -> testValue2),
            is(testValue2));
        assertThat(objectToIntMap.getValue(testKey), is(testValue2));
    }

    @Test
    void shouldComputeIfPresentBoxed()
    {
        final Map<String, Integer> objectToIntMap = this.objectToIntMap;

        final String testKey = "Seven";
        final int testValue = 7;
        final int testValue2 = -7;

        assertThat(objectToIntMap.computeIfPresent(testKey, (k, v) -> testValue), nullValue());
        assertThat(objectToIntMap.get(testKey), nullValue());

        objectToIntMap.put(testKey, testValue);
        assertThat(objectToIntMap.computeIfPresent(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(objectToIntMap.get(testKey), is(testValue2));
    }

    @Test
    void computeIfPresentIsAnNoOpIfTheKeyIsUnknown()
    {
        final int missingValue = 19;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        map.put("x", 333);
        final ObjectIntToIntFunction<String> function = (k, v) -> 555;

        assertEquals(missingValue, map.computeIfPresent("y", function));

        assertEquals(1, map.size());
        assertEquals(333, map.get("x"));
        assertFalse(map.containsKey("y"));
    }

    @Test
    void computeIfPresentDeletesExistingValueIfFunctionReturnsMissingValue()
    {
        final int missingValue = 1_000_000;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final String key = "x";
        final int value = 333;
        map.put(key, value);
        final ObjectIntToIntFunction<String> function = (k, v) -> missingValue;

        assertEquals(missingValue, map.computeIfPresent(key, function));

        assertEquals(0, map.size());
        assertFalse(map.containsKey(key));
    }

    @Test
    void shouldContainValue()
    {
        final int value = 7;
        final String key = "Seven";

        objectToIntMap.put(key, value);

        assertTrue(objectToIntMap.containsValue(value));
        assertFalse(objectToIntMap.containsValue(8));
    }

    @Test
    void shouldContainKey()
    {
        final int value = 7;
        final String key = "Seven";

        objectToIntMap.put(key, value);

        assertTrue(objectToIntMap.containsKey(key));
        assertFalse(objectToIntMap.containsKey("Eight"));
    }

    @Test
    void shouldRemoveEntry()
    {
        final int value = 7;
        final String key = "Seven";

        objectToIntMap.put(key, value);

        assertTrue(objectToIntMap.containsKey(key));

        objectToIntMap.remove(key);

        assertFalse(objectToIntMap.containsKey(key));
    }

    @Test
    void shouldRemoveEntryAndCompactCollisionChain()
    {
        final float loadFactor = 0.5f;
        final Object2IntHashMap<Integer> objectToIntMap = new Object2IntHashMap<>(32, loadFactor, MISSING_VALUE);

        final int value = 12;
        final Integer key = 12; //codeql[java/non-null-boxed-variable]

        objectToIntMap.put(key, value);
        objectToIntMap.put(Integer.valueOf(13), 13);

        final int collisionKey = key + objectToIntMap.capacity();
        final int collisionValue = collisionKey + 1;

        objectToIntMap.put(Integer.valueOf(collisionKey), collisionValue);
        objectToIntMap.put(Integer.valueOf(14), 14);

        assertThat(objectToIntMap.remove(key), is(value));
    }

    @Test
    void shouldIterateValuesGettingIntAsPrimitive()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
            initialSet.add(i);
        }

        final Collection<Integer> copyToSet = new HashSet<>();

        for (final Object2IntHashMap<String>.ValueIterator iter = objectToIntMap.values().iterator(); iter.hasNext(); )
        {
            copyToSet.add(iter.nextInt());
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    void shouldIterateValues()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
            initialSet.add(i);
        }

        final Collection<Integer> copyToSet = new HashSet<>();

        for (final Integer key : objectToIntMap.values())
        {
            //noinspection UseBulkOperation
            copyToSet.add(key);
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    void shouldIterateKeys()
    {
        final Collection<String> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
            initialSet.add(key);
        }

        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
    }

    @Test
    void shouldIterateAndHandleRemove()
    {
        final Collection<String> initialSet = new HashSet<>();

        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
            initialSet.add(key);
        }

        final Collection<String> copyOfSet = new HashSet<>();

        int i = 0;
        for (final Iterator<String> iter = objectToIntMap.keySet().iterator(); iter.hasNext(); )
        {
            final String item = iter.next();
            if (i++ == 7)
            {
                iter.remove();
            }
            else
            {
                copyOfSet.add(item);
            }
        }

        final int reducedSetSize = count - 1;
        assertThat(initialSet.size(), is(count));
        assertThat(objectToIntMap.size(), is(reducedSetSize));
        assertThat(copyOfSet.size(), is(reducedSetSize));
    }

    @Test
    void shouldIterateEntries()
    {
        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
        }

        iterateEntries();
        iterateEntries();
        iterateEntries();

        final Integer testValue = 100;
        for (final Map.Entry<String, Integer> entry : objectToIntMap.entrySet())
        {
            assertThat(entry.getKey(), equalTo(String.valueOf(entry.getValue())));

            if (entry.getKey().equals("7"))
            {
                entry.setValue(testValue);
            }
        }

        assertThat(objectToIntMap.getValue("7"), equalTo(testValue));
    }


    @Test
    void shouldGenerateStringRepresentation()
    {
        final Object2IntHashMap<ControlledHash> objectToIntMap = new Object2IntHashMap<>(MISSING_VALUE);

        final ControlledHash[] testEntries = ControlledHash.create(3, 1, 19, 7, 11, 12, 7);

        for (final ControlledHash testEntry : testEntries)
        {
            objectToIntMap.put(testEntry, testEntry.value);
        }

        final String mapAsAString = "{7=7, 19=19, 11=11, 1=1, 12=12, 3=3}";
        assertThat(objectToIntMap.toString(), equalTo(mapAsAString));
    }

    @Test
    void shouldCopyConstructAndBeEqual()
    {
        final int[] testEntries = { 3, 1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            objectToIntMap.put(String.valueOf(testEntry), testEntry);
        }

        final Object2IntHashMap<String> mapCopy = new Object2IntHashMap<>(objectToIntMap);
        assertThat(mapCopy, is(objectToIntMap));
    }

    @Test
    void shouldToArray()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(-127);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Object[] array = map.entrySet().toArray();
        for (final Object entry : array)
        {
            map.remove(((Entry<?, ?>)entry).getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void shouldToArrayTyped()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(-127);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

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
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(-127);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        for (final Map.Entry<String, Integer> entry : list)
        {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void putReturnsMissingValueIfNewMappingIsAdded()
    {
        final Object2IntHashMap<TrickyKey> map = new Object2IntHashMap<>(MISSING_VALUE);

        assertEquals(MISSING_VALUE, map.put(new TrickyKey(42, 1), 10));
        assertEquals(MISSING_VALUE, map.put(new TrickyKey(42, 2), 20));
        assertEquals(MISSING_VALUE, map.put(new TrickyKey(42, 3), 30));
        assertEquals(3, map.size());
    }

    @Test
    void removeKeyReturnsMissingValueIfRemoveFails()
    {
        final int missingValue = -14;
        final Object2IntHashMap<TrickyKey> map = new Object2IntHashMap<>(missingValue);
        map.put(new TrickyKey(21, 1), 10);
        map.put(new TrickyKey(8, 2), 20);
        map.put(new TrickyKey(21, 3), 30);

        assertEquals(missingValue, map.removeKey(new TrickyKey(21, 21)));
        assertEquals(3, map.size());
        assertEquals(30, map.getValue(new TrickyKey(21, 3)));
    }

    @Test
    void removeReturnsNullIfRemoveFails()
    {
        final Object2IntHashMap<TrickyKey> map = new Object2IntHashMap<>(Integer.MIN_VALUE);
        map.put(new TrickyKey(21, 1), 10);

        assertNull(map.remove(new TrickyKey(21, 21)));
        assertEquals(1, map.size());
        assertEquals(10, map.getValue(new TrickyKey(21, 1)));
    }

    @Test
    void getValueReturnsMissingValueIfLookupFails()
    {
        final Object2IntHashMap<TrickyKey> map = new Object2IntHashMap<>(MISSING_VALUE);
        map.put(new TrickyKey(1, 1), 1);
        map.put(new TrickyKey(2, 2), 2);
        map.put(new TrickyKey(3, 3), 3);

        assertEquals(MISSING_VALUE, map.getValue(new TrickyKey(3, MISSING_VALUE)));
        assertEquals(2, map.getValue(new TrickyKey(2, 2)));
    }

    @Test
    void removeByKeyAndValueIsANoOpIfMissingValueSpecified()
    {
        final int missingValue = 100;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final String key = "one";
        final int value = 888;
        map.put(key, value);

        assertFalse(map.remove(key, missingValue));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
    }

    @Test
    void removeByKeyAndValueIsANoOpIfValueIsWrong()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);
        final String key = "one";
        final int value = 888;
        map.put(key, value);

        assertFalse(map.remove(key, -value));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
    }

    @Test
    void removeByKeyAndValueIsANoOpIfKeyIsUnknown()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);
        final String key = "one";
        final int value = 42;
        map.put(key, value);

        assertFalse(map.remove("other key", value));

        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
    }

    @Test
    void removeByKeyAndValueDeletesMappingIfValueAndKeyMatch()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);
        final String key = "one";
        final int value = 42;
        map.put(key, value);
        map.put("1", 1);
        map.put("2", 2);

        assertTrue(map.remove(key, value));

        assertEquals(2, map.size());
        assertEquals(1, map.get("1"));
        assertEquals(2, map.get("2"));
        assertFalse(map.containsKey(key));
    }

    @Test
    void putAllShouldAllNonMissingValuesFromTheSourceMap()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);
        map.put("one", 1);
        map.put("two", 2);
        map.put("duo", 2);
        final Object2IntHashMap<String> other = new Object2IntHashMap<>(555);
        other.put("unus", 1);
        other.put("duo", 11);
        other.put("tres", 111);
        other.put("quattuor", 1111);

        map.putAll(other);

        assertEquals(6, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(1, map.get("unus"));
        assertEquals(11, map.get("duo"));
        assertEquals(111, map.get("tres"));
        assertEquals(1111, map.get("quattuor"));
    }

    @Test
    void putAllThrowsIllegalArgumentExceptionIfTheSourceMapContainsOne()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);
        map.put("one", 1);
        final Object2IntHashMap<String> other = new Object2IntHashMap<>(1_000_000);
        other.put("two", 2);
        other.put("broken", MISSING_VALUE);

        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> map.putAll(other));
        assertEquals("cannot accept missingValue", exception.getMessage());
    }

    @Test
    void putIfAbsentReturnsExistingValueWithoutOverwritingIt()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);
        final String key = "one";
        final int value = 1;
        final int newValue = 555;
        map.put(key, value);

        assertEquals(value, map.putIfAbsent(key, newValue));

        assertEquals(value, map.get(key));
    }

    @Test
    void putIfAbsentReturnsMissingValueAfterAddingANewMapping()
    {
        final int missingValue = 1000;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        map.put("one", 1);

        assertEquals(missingValue, map.putIfAbsent("two", 2));

        assertEquals(2, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
    }

    @Test
    void putIfAbsentReturnsNullAfterAddingANewMapping()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(1111);
        map.put("one", 1);

        assertNull(map.putIfAbsent("two", Integer.valueOf(2)));

        assertEquals(2, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
    }

    @Test
    void replaceAllIntThrowsNullPointerExceptionIfFunctionIsNull()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);

        assertThrowsExactly(NullPointerException.class, () -> map.replaceAllInt(null));
    }

    @Test
    void replaceAllIntThrowsIllegalArgumentExceptionIfFunctionReturnsMissingValue()
    {
        final int missingValue = 5;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        map.put("some", 999);
        final ObjectIntToIntFunction<String> function = (k, v) -> missingValue;

        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> map.replaceAllInt(function));
        assertEquals("cannot accept missingValue", exception.getMessage());
    }

    @Test
    void replaceAllIntChangesAllMappedValuesToTheNewOnes()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(Integer.MIN_VALUE);
        map.put("one", 1);
        map.put("zero", 0);
        map.put("max", Integer.MAX_VALUE);
        final ObjectIntToIntFunction<String> function = (k, v) -> -v;

        map.replaceAllInt(function);

        assertEquals(3, map.size());
        assertEquals(-1, map.getValue("one"));
        assertEquals(0, map.getValue("zero"));
        assertEquals(-Integer.MAX_VALUE, map.getValue("max"));
    }

    @Test
    void getOrDefaultReturnsExistingValue()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);
        final String key = "key";
        final int value = 7;
        final int defaultValue = 42;
        map.put(key, value);

        assertEquals(value, map.getOrDefault(key, defaultValue));
        assertEquals(value, map.getValue(key));
    }

    @Test
    void getOrDefaultReturnsDefaultValueIfMappingDoesNotExist()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);
        final String key = "key";
        final int defaultValue = 42;

        assertEquals(defaultValue, map.getOrDefault(key, defaultValue));
        assertEquals(MISSING_VALUE, map.getValue(key));
    }

    @Test
    void forEachIntThrowsNullPointerExceptionIfActionIsNull()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(MISSING_VALUE);

        assertThrowsExactly(NullPointerException.class, () -> map.forEachInt(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void forEachIntIteratesOverAllOfTheValues()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(555);
        map.put("min", Integer.MIN_VALUE);
        map.put("max", Integer.MAX_VALUE);
        map.put("zero", 0);
        map.put("minus one", -1);
        final ObjIntConsumer<String> action = mock(ObjIntConsumer.class);

        map.forEachInt(action);

        assertEquals(4, map.size());
        verify(action).accept("min", Integer.MIN_VALUE);
        verify(action).accept("max", Integer.MAX_VALUE);
        verify(action).accept("zero", 0);
        verify(action).accept("minus one", -1);
        verifyNoMoreInteractions(action);
    }

    @Test
    void mergeThrowsNullPointerExceptionIfKeyIsNull()
    {
        assertThrowsExactly(NullPointerException.class, () -> objectToIntMap.merge(null, 42, (v1, v2) -> 7));
    }

    @Test
    void mergeThrowsNullPointerExceptionIfRemappingFunctionIsNull()
    {
        assertThrowsExactly(NullPointerException.class, () -> objectToIntMap.merge("key", 42, null));
    }

    @Test
    void mergeThrowsIllegalArgumentExceptionIfValueIsMissingValue()
    {
        final int missingValue = 555;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);

        final IllegalArgumentException exception = assertThrowsExactly(
            IllegalArgumentException.class,
            () -> map.merge("key", missingValue, (v1, v2) -> 42));
        assertEquals("cannot accept missingValue", exception.getMessage());
    }

    @Test
    void mergeInsertANewValueIfNoneExist()
    {
        final int missingValue = 555;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final String key = "x";
        final int value = 42;
        final IntIntFunction remappingFunction = (v1, v2) -> fail();

        assertEquals(value, map.merge(key, value, remappingFunction));

        assertEquals(value, map.getValue(key));
    }

    @Test
    void mergeReplacesExistingValue()
    {
        final int missingValue = 555;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final String key = "x";
        final int oldValue = 42;
        final int value = 500;
        final int newValue = 542;
        map.put(key, oldValue);
        final IntIntFunction remappingFunction =
            (v1, v2) ->
            {
                assertEquals(oldValue, v1);
                assertEquals(value, v2);
                return v1 + v2;
            };

        assertEquals(newValue, map.merge(key, value, remappingFunction));

        assertEquals(newValue, map.getValue(key));
    }

    @Test
    void mergeDeletesExistingValue()
    {
        final int missingValue = -100;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        final String key = "x";
        final int oldValue = 42;
        final int value = 500;
        map.put(key, oldValue);
        map.put("other", 22);
        final IntIntFunction remappingFunction =
            (v1, v2) ->
            {
                assertEquals(oldValue, v1);
                assertEquals(value, v2);
                return missingValue;
            };

        assertEquals(missingValue, map.merge(key, value, remappingFunction));

        assertEquals(1, map.size());
        assertFalse(map.containsKey(key));
        assertEquals(22, map.getValue("other"));
    }

    @Test
    void removeIfIntOnValueCollection()
    {
        final int missingValue = -100;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        map.put("zero", 0);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        final IntPredicate filter = (v) -> v > 0 && v < 3;

        assertTrue(map.values().removeIfInt(filter));
        assertEquals(2, map.size());
        assertEquals(0, map.getValue("zero"));
        assertEquals(3, map.getValue("three"));

        assertFalse(map.values().removeIfInt(filter));
        assertEquals(2, map.size());
    }

    @Test
    void removeIfIntOnEntrySet()
    {
        final int missingValue = 999;
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(missingValue);
        map.put("zero", 0);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        final ObjIntPredicate<String> filter = (k, v) -> k.equals("three") || v < 2;

        assertTrue(map.entrySet().removeIfInt(filter));
        assertEquals(1, map.size());
        assertEquals(2, map.getValue("two"));

        assertFalse(map.entrySet().removeIfInt(filter));
        assertEquals(1, map.size());
        assertEquals(2, map.getValue("two"));
    }

    private void assertIterateKeys(final Collection<String> initialSet)
    {
        final Collection<String> copyToSet = new HashSet<>();
        for (final String aInteger : objectToIntMap.keySet())
        {
            //noinspection UseBulkOperation
            copyToSet.add(aInteger);
        }

        assertThat(copyToSet, is(initialSet));
    }

    private void iterateEntries()
    {
        for (final Map.Entry<String, Integer> entry : objectToIntMap.entrySet())
        {
            assertThat(entry.getKey(), equalTo(String.valueOf(entry.getValue())));
        }
    }

    private static class ControlledHash
    {
        private final int value;

        static ControlledHash[] create(final int... values)
        {
            final ControlledHash[] result = new ControlledHash[values.length];
            for (int i = 0; i < values.length; i++)
            {
                result[i] = new ControlledHash(values[i]);
            }

            return result;
        }

        ControlledHash(final int value)
        {
            super();
            this.value = value;
        }

        public String toString()
        {
            return Integer.toString(value);
        }

        public int hashCode()
        {
            return value * 31;
        }

        public boolean equals(final Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if (obj == null)
            {
                return false;
            }

            if (getClass() != obj.getClass())
            {
                return false;
            }

            final ControlledHash other = (ControlledHash)obj;

            return value == other.value;
        }
    }

    private static final class TrickyKey
    {
        private final int hash;
        private final int value;

        private TrickyKey(final int hash, final int value)
        {
            this.hash = hash;
            this.value = value;
        }

        public int hashCode()
        {
            return hash;
        }

        public boolean equals(final Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            return obj instanceof TrickyKey && value == ((TrickyKey)obj).value;
        }

        public String toString()
        {
            return "TrickyKey{" +
                "hash=" + hash +
                ", value=" + value +
                '}';
        }
    }
}
