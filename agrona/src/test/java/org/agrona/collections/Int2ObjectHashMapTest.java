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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.jupiter.api.Assertions.*;

class Int2ObjectHashMapTest
{
    final Int2ObjectHashMap<String> map;

    Int2ObjectHashMapTest()
    {
        map = newMap(Hashing.DEFAULT_LOAD_FACTOR, Int2ObjectHashMap.MIN_CAPACITY);
    }

    Int2ObjectHashMap<String> newMap(final float loadFactor, final int initialCapacity)
    {
        return new Int2ObjectHashMap<>(initialCapacity, loadFactor);
    }

    @Test
    void shouldDoPutAndThenGet()
    {
        final String value = "Seven";
        map.put(7, value);

        assertThat(map.get(7), is(value));
    }

    @Test
    void shouldReplaceExistingValueForTheSameKey()
    {
        final int key = 7;
        final String value = "Seven";
        map.put(key, value);

        final String newValue = "New Seven";
        final String oldValue = map.put(key, newValue);

        assertThat(map.get(key), is(newValue));
        assertThat(oldValue, is(value));
        assertThat(map.size(), is(1));
    }

    @Test
    void shouldGrowWhenThresholdExceeded()
    {
        final float loadFactor = 0.5f;
        final int initialCapacity = 32;
        final Int2ObjectHashMap<String> map = newMap(loadFactor, initialCapacity);
        for (int i = 0; i < 16; i++)
        {
            map.put(i, Integer.toString(i));
        }

        assertThat(map.resizeThreshold(), is(16));
        assertThat(map.capacity(), is(initialCapacity));
        assertThat(map.size(), is(16));

        map.put(16, "16");

        assertThat(map.resizeThreshold(), is(initialCapacity));
        assertThat(map.capacity(), is(64));
        assertThat(map.size(), is(17));

        assertThat(map.get(16), equalTo("16"));
        assertThat((double)loadFactor, closeTo(map.loadFactor(), 0.0f));
    }

    @Test
    void shouldHandleCollisionAndThenLinearProbe()
    {
        final float loadFactor = 0.5f;
        final int initialCapacity = 32;
        final Int2ObjectHashMap<String> map = newMap(loadFactor, initialCapacity);
        final int key = 7;
        final String value = "Seven";
        map.put(key, value);

        final int collisionKey = key + map.capacity();
        final String collisionValue = Integer.toString(collisionKey);
        map.put(collisionKey, collisionValue);

        assertThat(map.get(key), is(value));
        assertThat(map.get(collisionKey), is(collisionValue));
        assertThat((double)loadFactor, closeTo(map.loadFactor(), 0.0f));
    }

    @Test
    void shouldClearCollection()
    {
        for (int i = 0; i < 15; i++)
        {
            map.put(i, Integer.toString(i));
        }

        assertThat(map.size(), is(15));
        assertThat(map.get(1), is("1"));

        map.clear();

        assertThat(map.size(), is(0));
        assertNull(map.get(1));
    }

    @Test
    void shouldCompactCollection()
    {
        final int totalItems = 50;
        for (int i = 0; i < totalItems; i++)
        {
            map.put(i, Integer.toString(i));
        }

        for (int i = 0, limit = totalItems - 4; i < limit; i++)
        {
            map.remove(i);
        }

        final int capacityBeforeCompaction = map.capacity();
        map.compact();

        assertThat(map.capacity(), lessThan(capacityBeforeCompaction));
    }

    @Test
    void shouldCompute()
    {
        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(map.get(testKey), nullValue());
        assertEquals(0, map.size());

        assertThat(map.compute(testKey, (k, v) -> testValue), is(testValue));
        assertThat(map.get(testKey), is(testValue));
        assertEquals(1, map.size());

        assertThat(map.compute(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(map.get(testKey), is(testValue2));
        assertEquals(1, map.size());
    }

    @Test
    void shouldComputeBoxed()
    {
        final Map<Integer, String> intToObjectMap = this.map;

        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(intToObjectMap.compute(testKey, (k, v) -> testValue), is(testValue));
        assertThat(intToObjectMap.get(testKey), is(testValue));
        assertEquals(1, map.size());

        assertThat(intToObjectMap.compute(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(intToObjectMap.get(testKey), is(testValue2));
        assertEquals(1, map.size());
    }

    @Test
    void computeShouldRemoveExistingMappingIfValueIsNull()
    {
        final int key = 5;
        final String value = "five";
        map.put(key, value);

        assertNull(map.compute(key, (k, v) -> null));

        assertEquals(0, map.size());
    }

    @Test
    void computeIsANoOpIfKeyIsUnknownAndValueIsNull()
    {
        map.put(42, "42");

        assertNull(map.compute(5, (k, v) -> null));

        assertEquals(1, map.size());
        assertEquals("42", map.get(42));
    }

    @Test
    void shouldComputeIfAbsent()
    {
        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(map.get(testKey), nullValue());

        assertThat(map.computeIfAbsent(testKey, (i) -> testValue), is(testValue));
        assertThat(map.get(testKey), is(testValue));

        assertThat(map.computeIfAbsent(testKey, (i) -> testValue2), is(testValue));
        assertThat(map.get(testKey), is(testValue));
    }

    @Test
    void shouldComputeIfAbsentBoxed()
    {
        final Map<Integer, String> intToObjectMap = this.map;

        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(intToObjectMap.computeIfAbsent(testKey, (i) -> testValue), is(testValue));
        assertThat(intToObjectMap.get(testKey), is(testValue));

        assertThat(intToObjectMap.computeIfAbsent(testKey, (i) -> testValue2), is(testValue));
        assertThat(intToObjectMap.get(testKey), is(testValue));
    }

    @Test
    void shouldComputeIfPresent()
    {
        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(map.computeIfPresent(testKey, (k, v) -> testValue), nullValue());
        assertThat(map.get(testKey), nullValue());

        map.put(testKey, testValue);
        assertThat(map.computeIfPresent(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(map.get(testKey), is(testValue2));
    }

    @Test
    void computeIfPresentShouldDeleteExistingEntryIfFunctionReturnsNull()
    {
        map.put(1, "one");
        final int key = 3;
        final String value = "three";
        map.put(key, value);
        final IntObjectToObjectFunction<String, String> function = (k, v) ->
        {
            assertEquals(key, k);
            assertEquals(value, v);
            return null;
        };

        assertNull(map.computeIfPresent(key, function));

        assertEquals(1, map.size());
        assertEquals("one", map.get(1));
        assertFalse(map.containsKey(key));
        assertFalse(map.containsValue(value));
    }

    @Test
    void shouldComputeIfPresentBoxed()
    {
        final Map<Integer, String> intToObjectMap = this.map;

        final int testKey = 7;
        final String testValue = "Seven";
        final String testValue2 = "7";

        assertThat(intToObjectMap.computeIfPresent(testKey, (k, v) -> testValue), nullValue());
        assertThat(intToObjectMap.get(testKey), nullValue());

        intToObjectMap.put(testKey, testValue);
        assertThat(intToObjectMap.computeIfPresent(testKey, (k, v) -> testValue2), is(testValue2));
        assertThat(intToObjectMap.get(testKey), is(testValue2));
    }

    @Test
    void shouldContainValue()
    {
        final int key = 7;
        final String value = "Seven";

        map.put(key, value);

        assertTrue(map.containsValue(value));
        assertFalse(map.containsValue("NoKey"));
    }

    @Test
    void shouldContainKey()
    {
        final int key = 7;
        final String value = "Seven";

        map.put(key, value);

        assertTrue(map.containsKey(key));
        assertFalse(map.containsKey(0));
    }

    @Test
    void shouldRemoveEntry()
    {
        final int key = 7;
        final String value = "Seven";

        map.put(key, value);

        assertTrue(map.containsKey(key));

        map.remove(key);

        assertFalse(map.containsKey(key));
    }

    @Test
    void shouldRemoveEntryAndCompactCollisionChain()
    {
        final int key = 12;
        final String value = "12";

        map.put(key, value);
        map.put(13, "13");

        final int collisionKey = key + map.capacity();
        final String collisionValue = Integer.toString(collisionKey);

        map.put(collisionKey, collisionValue);
        map.put(14, "14");

        assertThat(map.remove(key), is(value));
    }

    @Test
    void shouldIterateValues()
    {
        final Collection<String> initialSet = new ArrayList<>();

        for (int i = 0; i < 11; i++)
        {
            final String value = Integer.toString(i);
            map.put(i, value);
            initialSet.add(value);
        }

        final Collection<String> copyToSetOne = new ArrayList<>();
        for (final String s : map.values())
        {
            //noinspection UseBulkOperation
            copyToSetOne.add(s);
        }

        final Collection<String> copyToSetTwo = new ArrayList<>();
        for (final String s : map.values())
        {
            //noinspection UseBulkOperation
            copyToSetTwo.add(s);
        }

        assertEquals(initialSet.size(), copyToSetOne.size());
        assertTrue(initialSet.containsAll(copyToSetOne));

        assertEquals(initialSet.size(), copyToSetTwo.size());
        assertTrue(initialSet.containsAll(copyToSetTwo));
    }

    @Test
    void shouldForEachValues()
    {
        final Collection<String> expected = new HashSet<>();
        for (int i = 0; i < 11; i++)
        {
            final String value = Integer.toString(i);
            map.put(i, value);
            expected.add(value);
        }

        final Collection<String> copySet = new HashSet<>();
        //noinspection UseBulkOperation
        map.values().forEach(copySet::add);

        assertEquals(expected, copySet);
    }

    @Test
    void shouldIterateKeysGettingIntAsPrimitive()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String value = Integer.toString(i);
            map.put(i, value);
            initialSet.add(i);
        }

        final Collection<Integer> copyToSet = new HashSet<>();

        for (final Int2ObjectHashMap<String>.KeyIterator iter = map.keySet().iterator(); iter.hasNext(); )
        {
            copyToSet.add(iter.nextInt());
        }

        assertEquals(initialSet, copyToSet);
    }

    @Test
    void shouldIterateKeys()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String value = Integer.toString(i);
            map.put(i, value);
            initialSet.add(i);
        }

        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
    }

    private void assertIterateKeys(final Collection<Integer> initialSet)
    {
        final Collection<Integer> copyToSet = new HashSet<>();
        for (final Integer aInteger : map.keySet())
        {
            //noinspection UseBulkOperation
            copyToSet.add(aInteger);
        }

        assertEquals(initialSet, copyToSet);
    }

    @Test
    void shouldIterateAndHandleRemove()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            map.put(i, value);
            initialSet.add(i);
        }

        final Collection<Integer> copyOfSet = new HashSet<>();

        int i = 0;
        for (final Iterator<Integer> iter = map.keySet().iterator(); iter.hasNext(); )
        {
            final Integer item = iter.next();
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
        assertEquals(count, initialSet.size());
        assertEquals(reducedSetSize, map.size());
        assertEquals(reducedSetSize, copyOfSet.size());
    }

    @Test
    void shouldIterateEntries()
    {
        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            map.put(i, value);
        }

        iterateEntries();
        iterateEntries();
        iterateEntries();

        final String testValue = "Wibble";
        for (final Map.Entry<Integer, String> entry : map.entrySet())
        {
            assertThat(String.valueOf(entry.getKey()), equalTo(entry.getValue()));

            if (entry.getKey() == 7)
            {
                entry.setValue(testValue);
            }
        }

        assertEquals(testValue, map.get(7));
    }

    private void iterateEntries()
    {
        for (final Map.Entry<Integer, String> entry : map.entrySet())
        {
            assertThat(String.valueOf(entry.getKey()), equalTo(entry.getValue()));
        }
    }

    @Test
    void shouldIterateForEach()
    {
        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            map.put(i, value);
        }

        final Collection<Integer> copyToSet = new HashSet<>();
        map.forEachInt(
            (key, value) ->
            {
                assertEquals(value, String.valueOf(key));

                // not copying values, because they match keys
                copyToSet.add(key);
            });
        assertEquals(copyToSet, map.keySet());
    }

    @Test
    void shouldGenerateStringRepresentation()
    {
        final int[] testEntries = { 3, 1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            map.put(testEntry, String.valueOf(testEntry));
        }

        final String mapAsAString = "{19=19, 1=1, 11=11, 7=7, 3=3, 12=12}";
        assertThat(map.toString(), equalTo(mapAsAString));
    }

    @Test
    void shouldCopyConstructAndBeEqual()
    {
        final int[] testEntries = { 3, 1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            map.put(testEntry, String.valueOf(testEntry));
        }

        final Int2ObjectHashMap<String> mapCopy = new Int2ObjectHashMap<>(map);
        assertThat(mapCopy, is(map));
    }

    @Test
    void shouldAllowNullValuesWithNullMapping()
    {
        final Int2ObjectHashMap<String> map = new Int2ObjectHashMap<String>()
        {
            private final Object nullRef = new Object();

            protected Object mapNullValue(final Object value)
            {
                return value == null ? nullRef : value;
            }

            protected String unmapNullValue(final Object value)
            {
                return value == nullRef ? null : (String)value;
            }
        };

        map.put(0, null);
        map.put(1, "one");

        assertThat(map.get(0), nullValue());
        assertThat(map.get(1), is("one"));
        assertThat(map.get(-1), nullValue());

        assertThat(map.containsKey(0), is(true));
        assertThat(map.containsKey(1), is(true));
        assertThat(map.containsKey(-1), is(false));

        assertThat(map.values(), containsInAnyOrder(null, "one"));
        assertThat(map.keySet(), containsInAnyOrder(0, 1));

        assertThat(map.size(), is(2));

        map.remove(0);

        assertThat(map.get(0), nullValue());
        assertThat(map.get(1), is("one"));
        assertThat(map.get(-1), nullValue());

        assertThat(map.containsKey(0), is(false));
        assertThat(map.containsKey(1), is(true));
        assertThat(map.containsKey(-1), is(false));

        assertThat(map.size(), is(1));
    }

    @Test
    void shouldToArray()
    {
        final Int2ObjectHashMap<String> map = new Int2ObjectHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final Object[] array = map.entrySet().toArray();
        for (final Object entry : array)
        {
            map.remove(((Map.Entry<?, ?>)entry).getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void shouldToArrayTyped()
    {
        final Int2ObjectHashMap<String> map = new Int2ObjectHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final Map.Entry<?, ?>[] type = new Map.Entry[1];
        final Map.Entry<?, ?>[] array = map.entrySet().toArray(type);
        for (final Map.Entry<?, ?> entry : array)
        {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void shouldToArrayWithArrayListConstructor()
    {
        final Int2ObjectHashMap<String> map = new Int2ObjectHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final List<Map.Entry<Integer, String>> list = new ArrayList<>(map.entrySet());
        for (final Map.Entry<Integer, String> entry : list)
        {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void containsValueShouldPerformEqualityCheckBasedOnTheValueStoredInTheMap()
    {
        final Int2ObjectHashMap<CharSequence> map = new Int2ObjectHashMap<>();
        map.put(11, new CharSequenceKey("abc"));
        final CharSequenceKey xyzKey = new CharSequenceKey("xyz");
        map.put(42, xyzKey);

        assertTrue(map.containsValue("abc"));
        assertTrue(map.containsValue(new CharSequenceKey("abc")));
        assertTrue(map.containsValue(xyzKey));
        assertTrue(map.containsValue(new CharSequenceKey("xyz")));

        final Int2ObjectHashMap<CharSequence>.ValueCollection values = map.values();
        assertTrue(values.contains("abc"));
        assertTrue(values.contains(new CharSequenceKey("abc")));
        assertTrue(values.contains(xyzKey));

        assertFalse(map.containsValue(null));
        assertFalse(map.containsValue("null"));
        assertFalse(map.containsValue(new CharSequenceKey("test")));
    }

    @Test
    void getOrDefaultShouldReturnDefaultValueIfNoMappingExistsForAGivenKey()
    {
        final int key = 121;
        final String defaultValue = "fallback";

        assertEquals(defaultValue, map.getOrDefault(key, defaultValue));
    }

    @Test
    void getOrDefaultShouldReturnValueForAnExistingKey()
    {
        final int key = 121;
        final String value = "found";
        final String defaultValue = "fallback";
        map.put(key, value);

        assertEquals(value, map.getOrDefault(key, defaultValue));
    }

    @Test
    void removeIsANoOpIfValueIsNull()
    {
        final int key = 42;
        final String value = "nine";
        map.put(key, value);

        assertFalse(map.remove(key, null));

        assertEquals(value, map.get(key));
    }

    @Test
    void removeIsANoOpIfValueDoesNotMatch()
    {
        final int key = 42;
        final String value = "nine";
        map.put(key, value);

        assertFalse(map.remove(key, "ten"));

        assertEquals(value, map.get(key));
    }

    @Test
    void removeShouldDeleteKeyMappingIfValueMatches()
    {
        final int key = -100;
        final Int2ObjectHashMap<CharSequence> map = new Int2ObjectHashMap<>();
        map.put(key, new CharSequenceKey("abc"));
        map.put(2, "two");

        assertTrue(map.remove(key, "abc"));

        assertEquals(1, map.size());
        assertEquals("two", map.get(2));
    }

    @Test
    void replaceThrowsNullPointerExceptionIfValueIsNull()
    {
        final NullPointerException exception =
            assertThrowsExactly(NullPointerException.class, () -> map.replace(42, null));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void replaceReturnsNullForAnUnknownKey()
    {
        map.put(1, "one");

        assertNull(map.replace(2, "three"));

        assertEquals("one", map.get(1));
    }

    @Test
    void replaceReturnsPreviousValueAfterSettingTheNewOne()
    {
        final int key = 1;
        final String oldValue = "one";
        final String newValue = "three";
        map.put(key, oldValue);

        assertEquals(oldValue, map.replace(key, newValue));

        assertEquals(newValue, map.get(key));
    }

    @Test
    void replaceThrowsNullPointerExceptionIfNewValueIsNull()
    {
        final NullPointerException exception =
            assertThrowsExactly(NullPointerException.class, () -> map.replace(42, "abc", null));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void replaceReturnsFalseForAnUnknownKey()
    {
        map.put(1, "one");

        assertFalse(map.replace(2, "a", "b"));

        assertEquals("one", map.get(1));
    }

    @Test
    void replaceReturnsFalseIfTheOldValueDoesNotMatch()
    {
        final int key = 1;
        final String value = "one";
        map.put(key, value);

        assertFalse(map.replace(key, "wrong!", "new one"));

        assertEquals(value, map.get(key));
    }

    @Test
    void replaceReturnsTrueAfterUpdatingTheNewValue()
    {
        final Int2ObjectHashMap<CharSequence> map = new Int2ObjectHashMap<>();
        final int key = 1;
        final String newValue = "two";
        map.put(key, new CharSequenceKey("one"));

        assertTrue(map.replace(key, "one", newValue));

        assertEquals(newValue, map.get(key));
    }

    @Test
    void replaceAllIntThrowsNullPointerExceptionIfTheNewValueIsNull()
    {
        final IntObjectToObjectFunction<String, String> nullFunction = (key, value) -> null;
        map.put(1, "one");

        final NullPointerException exception =
            assertThrowsExactly(NullPointerException.class, () -> map.replaceAllInt(nullFunction));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void replaceAllIntUpdatesEveryExistingValue()
    {
        final IntObjectToObjectFunction<String, String> updateFunction = (key, value) -> key + "_" + value;
        map.put(1, "one");
        map.put(2, "two");
        map.put(-100, "null");

        map.replaceAllInt(updateFunction);

        assertEquals(3, map.size());
        assertEquals("1_one", map.get(1));
        assertEquals("2_two", map.get(2));
        assertEquals("-100_null", map.get(-100));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "val 1", "你好" })
    void putIfAbsentShouldReturnAnExistingValueForAnExistingKey(final String value)
    {
        final int key = 42;
        final String newValue = " this is something new";
        map.put(key, value);

        assertEquals(value, map.putIfAbsent(key, newValue));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "val 1", "你好" })
    void putIfAbsentShouldReturnNullAfterPuttingANewValue(final String newValue)
    {
        final int key = 42;
        map.put(3, "three");

        assertNull(map.putIfAbsent(key, newValue));

        assertEquals(newValue, map.get(key));
        assertEquals("three", map.get(3));
    }

    @Test
    void putIfAbsentThrowsNullPointerExceptionIfValueIsNull()
    {
        final NullPointerException exception =
            assertThrowsExactly(NullPointerException.class, () -> map.putIfAbsent(42, null));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void putAllCopiesAllOfTheValuesFromTheSourceMap()
    {
        map.put(42, "forty two");
        map.put(0, "zero");
        final Int2ObjectHashMap<String> otherMap = new Int2ObjectHashMap<>();
        otherMap.put(1, "1");
        otherMap.put(2, "2");
        otherMap.put(3, "3");
        otherMap.put(42, "42");

        map.putAll(otherMap);

        assertEquals(5, map.size());
        assertEquals("zero", map.get(0));
        assertEquals("1", map.get(1));
        assertEquals("2", map.get(2));
        assertEquals("3", map.get(3));
        assertEquals("42", map.get(42));
    }

    @Test
    void putAllThrowsNullPointerExceptionIfOtherMapContainsNull()
    {
        map.put(0, "zero");
        final Int2NullableObjectHashMap<String> otherMap = new Int2NullableObjectHashMap<>();
        otherMap.put(1, null);

        final NullPointerException exception =
            assertThrowsExactly(NullPointerException.class, () -> map.putAll(otherMap));
        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void removeIfIntOnKeySet()
    {
        final IntPredicate filter = (key) -> (key & 1) == 0;
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertTrue(map.keySet().removeIfInt(filter));

        assertEquals(2, map.size());
        assertEquals("one", map.get(1));
        assertEquals("three", map.get(3));

        assertFalse(map.keySet().removeIfInt(filter));
        assertEquals(2, map.size());
    }

    @Test
    void removeIfOnValuesCollection()
    {
        final Predicate<String> filter = (value) -> value.contains("e");
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertTrue(map.values().removeIf(filter));

        assertEquals(1, map.size());
        assertEquals("two", map.get(2));

        assertFalse(map.values().removeIf(filter));
        assertEquals(1, map.size());
    }

    @Test
    void removeIfIntOnEntrySet()
    {
        final IntObjPredicate<String> filter = (key, value) -> (key & 1) == 0 && value.startsWith("t");
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");

        assertTrue(map.entrySet().removeIfInt(filter));

        assertEquals(3, map.size());
        assertEquals("one", map.get(1));
        assertEquals("three", map.get(3));
        assertEquals("four", map.get(4));

        assertFalse(map.entrySet().removeIfInt(filter));
        assertEquals(3, map.size());
    }

    @Test
    void mergeShouldInsertNewMapping()
    {
        map.put(1, "one");
        final int key = 42;
        final String value = "forty two";

        assertEquals(value, map.merge(key, value, (oldValue, newValue) -> fail("not expected")));

        assertEquals(2, map.size());
        assertEquals("one", map.get(1));
        assertEquals(value, map.get(key));
    }

    @Test
    void mergeShouldUpdateExistingMapping()
    {
        final int key = 42;
        final String value = "forty two";
        final String expectedValue = value + "-" + "eleven";
        map.put(key, value);

        assertEquals(expectedValue, map.merge(key, "eleven", (oldValue, newValue) -> expectedValue));

        assertEquals(1, map.size());
        assertEquals(expectedValue, map.get(key));
    }

    @Test
    void mergeShouldDeleteExistingMapping()
    {
        final int key = 42;
        final String value = "forty two";
        map.put(key, value);

        assertNull(map.merge(key, "eleven", (oldValue, newValue) -> null));

        assertEquals(0, map.size());
    }
}
