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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Int2ObjectCacheTest
{
    private static final int NUM_SETS = 16;
    private static final int SET_SIZE = 4;
    private static final int CAPACITY = NUM_SETS * SET_SIZE;
    private static final Consumer<String> EVICTION_CONSUMER = (s) -> {};

    private final Int2ObjectCache<String> cache = new Int2ObjectCache<>(NUM_SETS, SET_SIZE, EVICTION_CONSUMER);

    @Test
    void shouldDoPutAndThenGet()
    {
        final String value = "Seven";
        cache.put(7, value);

        assertThat(cache.get(7), is(value));
    }

    @Test
    void shouldReplaceExistingValueForTheSameKey()
    {
        final int key = 7;
        final String value = "Seven";
        cache.put(key, value);

        final String newValue = "New Seven";
        cache.put(key, newValue);

        assertThat(cache.get(key), is(newValue));
        assertThat(cache.size(), is(1));
    }

    @Test
    void shouldReturnNullForAnUnknownKeyWhenAnEntireSetIsUnmatched()
    {
        final Int2ObjectCache<String> cache = new Int2ObjectCache<>(1, 1, EVICTION_CONSUMER);
        final int key = 42;
        final String value = "value";
        cache.put(key, value);
        assertSame(value, cache.get(key));

        final int unknownKey = -1;
        assertNull(cache.get(unknownKey));
        assertNull(cache.get((Object)unknownKey));

        assertEquals(1, cache.cacheHits());
        assertEquals(2, cache.cacheMisses());
    }

    @Test
    void shouldLimitSizeToMaxSize()
    {
        for (int i = 0; i < (CAPACITY * 2); i++)
        {
            cache.put(i, Integer.toString(i));
        }

        assertThat(cache.size(), greaterThan(0));
        assertThat(cache.size(), lessThanOrEqualTo(CAPACITY));
    }

    @Test
    void shouldClearCollection()
    {
        for (int i = 0; i < CAPACITY; i++)
        {
            cache.put(i, Integer.toString(i));
        }

        assertThat(cache.size(), greaterThan(0));

        cache.clear();

        assertThat(cache.size(), is(0));
        assertNull(cache.get(0));
        assertFalse(cache.containsKey(CAPACITY - 1));
        assertFalse(cache.containsValue("1"));
    }

    @Test
    void shouldContainValue()
    {
        final int key = 7;
        final String value = "Seven";

        cache.put(key, value);

        assertTrue(cache.containsValue(value));
        assertFalse(cache.containsValue("NoKey"));
    }

    @Test
    void shouldContainKey()
    {
        final int key = 7;
        final String value = "Seven";

        cache.put(key, value);

        assertTrue(cache.containsKey(key));
        assertFalse(cache.containsKey(0));
    }

    @Test
    void shouldRemoveEntry()
    {
        final int key = 7;
        final String value = "Seven";

        cache.put(key, value);

        assertTrue(cache.containsKey(key));

        cache.remove(key);

        assertFalse(cache.containsKey(key));
    }

    @Test
    void shouldIterateEntries()
    {
        final int count = CAPACITY - 1;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            cache.put(i, value);
        }

        for (final Map.Entry<Integer, String> entry : cache.entrySet())
        {
            assertThat(String.valueOf(entry.getKey()), equalTo(entry.getValue()));
        }
    }

    @Test
    void shouldGenerateStringRepresentation()
    {
        final int[] testEntries = { 3, 1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            cache.put(testEntry, String.valueOf(testEntry));
        }

        final String mapAsAString = "{12=12, 3=3, 7=7, 11=11, 19=19, 1=1}";
        assertThat(cache.toString(), equalTo(mapAsAString));
    }

    @Test
    void shouldEvictAsMaxSizeIsExceeded()
    {
        final HashSet<String> evictedItems = new HashSet<>();
        final Consumer<String> evictionConsumer = evictedItems::add;
        final Int2ObjectCache<String> cache = new Int2ObjectCache<>(NUM_SETS, SET_SIZE, evictionConsumer);

        final int count = CAPACITY * 2;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            cache.put(i, value);
        }

        assertThat(cache.size() + evictedItems.size(), is(count));
    }

    @Test
    void shouldComputeIfAbsent()
    {
        final int testKey = 7;
        final String testValue = "7";

        final IntFunction<String> function = (i) -> testValue;

        assertNull(cache.get(testKey));

        assertThat(cache.computeIfAbsent(testKey, function), is(testValue));
        assertThat(cache.get(testKey), is(testValue));
    }

    @Test
    void computeIfAbsentIsANoOpIfNewValueIsNull()
    {
        final int key = 9;
        final IntFunction<String> function = (i) -> null;

        assertNull(cache.computeIfAbsent(key, function));

        assertFalse(cache.containsKey(key));
    }

    @Test
    void shouldTestStats()
    {
        assertThat(cache.cachePuts(), is(0L));
        assertThat(cache.cacheMisses(), is(0L));
        assertThat(cache.cacheHits(), is(0L));

        cache.get(7);
        assertThat(cache.cacheMisses(), is(1L));
        assertThat(cache.cacheHits(), is(0L));
        assertThat(cache.cachePuts(), is(0L));

        cache.put(7, "Seven");
        assertThat(cache.cacheMisses(), is(1L));
        assertThat(cache.cacheHits(), is(0L));
        assertThat(cache.cachePuts(), is(1L));

        cache.get(7);
        assertThat(cache.cacheMisses(), is(1L));
        assertThat(cache.cacheHits(), is(1L));
        assertThat(cache.cachePuts(), is(1L));

        cache.resetCounters();
        assertThat(cache.cachePuts(), is(0L));
        assertThat(cache.cacheMisses(), is(0L));
        assertThat(cache.cacheHits(), is(0L));
    }

    @Test
    void containsValueShouldUseValueStoredInTheCacheToPerformAnEqualityCheck()
    {
        final Int2ObjectCache<CharSequence> cache = new Int2ObjectCache<>(4, 2, (key) -> {});
        final CharSequenceKey key = new CharSequenceKey("one");
        cache.put(1, key);

        assertTrue(cache.containsValue("one"));
        assertTrue(cache.containsValue(new CharSequenceKey("one")));
        assertTrue(cache.containsValue(key));

        final Int2ObjectCache<CharSequence>.ValueCollection values = cache.values();
        assertTrue(values.contains("one"));
        assertTrue(values.contains(new CharSequenceKey("one")));
        assertTrue(values.contains(key));

        assertFalse(cache.containsValue(null));
        assertFalse(cache.containsValue("two"));
        assertFalse(cache.containsValue(new CharSequenceKey("two")));
    }

    @Test
    void getOrDefaultShouldReturnDefaultValueIfNoMappingExistsForAGivenKey()
    {
        final int key = 121;
        final String defaultValue = "fallback";

        assertEquals(defaultValue, cache.getOrDefault(key, defaultValue));
        assertEquals(0, cache.cacheHits());
        assertEquals(1, cache.cacheMisses());
    }

    @Test
    void getOrDefaultShouldReturnValueForAnExistingKey()
    {
        final int key = 121;
        final String value = "found";
        final String defaultValue = "fallback";
        cache.put(key, value);

        assertEquals(value, cache.getOrDefault(key, defaultValue));
        assertEquals(1, cache.cacheHits());
        assertEquals(0, cache.cacheMisses());
    }

    @Test
    void forEachIntShouldIterateOverAllStoredValues()
    {
        @SuppressWarnings("unchecked") final IntObjConsumer<String> consumer = mock(IntObjConsumer.class);
        cache.put(1, "one");
        cache.put(-3, "three");
        cache.put(10, "ten");
        cache.put(13, "thirteen");
        cache.put(-11, "eleven");
        cache.put(12, "twelve");
        cache.put(42, "forty two");

        cache.forEachInt(consumer);

        verify(consumer).accept(1, "one");
        verify(consumer).accept(-3, "three");
        verify(consumer).accept(10, "ten");
        verify(consumer).accept(13, "thirteen");
        verify(consumer).accept(-11, "eleven");
        verify(consumer).accept(12, "twelve");
        verify(consumer).accept(42, "forty two");
        verifyNoMoreInteractions(consumer);
    }

    @Test
    void forEachShouldIterateOverAllStoredValues()
    {
        @SuppressWarnings("unchecked") final BiConsumer<Integer, String> consumer = mock(BiConsumer.class);
        cache.put(1, "one");
        cache.put(-3, "three");
        cache.put(10, "ten");
        cache.put(12, "twelve");
        cache.put(42, "forty two");

        cache.forEach(consumer);

        verify(consumer).accept(1, "one");
        verify(consumer).accept(-3, "three");
        verify(consumer).accept(10, "ten");
        verify(consumer).accept(12, "twelve");
        verify(consumer).accept(42, "forty two");
        verifyNoMoreInteractions(consumer);
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfPresentReturnsNullForAnUnknownKey()
    {
        cache.put(1, "one");
        final IntObjectToObjectFunction<String, String> remappingFunction = mock(IntObjectToObjectFunction.class);

        assertNull(cache.computeIfPresent(555, remappingFunction));

        verifyNoInteractions(remappingFunction);
    }

    @Test
    void computeIfPresentReturnsNewValueAfterUpdatingTheMapping()
    {
        final int key = 1;
        final String oldValue = "one";
        final String newValue = "1oneNEW";
        cache.put(key, oldValue);
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) -> k + v + "NEW";

        assertEquals(newValue, cache.computeIfPresent(key, remappingFunction));

        assertEquals(newValue, cache.get(key));
    }

    @Test
    void computeIfPresentReturnsNullAfterRemovingTheExistingMapping()
    {
        final int key = -1111;
        final String value = "one";
        cache.put(key, value);
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) -> null;

        assertNull(cache.computeIfPresent(key, remappingFunction));

        assertNull(cache.get(key));
        assertFalse(cache.containsKey(key));
    }

    @Test
    void computeReturnsNewValueAfterInsertingANewMapping()
    {
        final int key = 42;
        final String newValue = "new";
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) ->
        {
            assertEquals(key, k);
            assertNull(v);
            return newValue;
        };

        assertEquals(newValue, cache.compute(key, remappingFunction));

        assertEquals(newValue, cache.get(key));
    }

    @Test
    void computeReturnsNewValueAfterUpdatingAnExistingValue()
    {
        final int key = 42;
        final String oldValue = "old";
        cache.put(key, oldValue);
        final String newValue = "new";
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) ->
        {
            assertEquals(key, k);
            assertEquals(oldValue, v);
            return newValue;
        };

        assertEquals(newValue, cache.compute(key, remappingFunction));

        assertEquals(newValue, cache.get(key));
    }

    @Test
    void computeReturnsNullAfterRemovingAnExistingMapping()
    {
        final int key = 42;
        final String oldValue = "old";
        cache.put(key, oldValue);
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) -> null;

        assertNull(cache.compute(key, remappingFunction));

        assertFalse(cache.containsKey(key));
    }

    @Test
    void computeReturnsNullForAnUnknownKeyIfTheFunctionReturnsNull()
    {
        cache.put(11, "11");
        final int key = 42;
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) ->
        {
            assertEquals(key, k);
            assertNull(v);
            return null;
        };

        assertNull(cache.compute(key, remappingFunction));

        assertFalse(cache.containsKey(key));
        assertEquals("11", cache.get(11));
        assertEquals(1, cache.size());
    }

    @Test
    void mergeThrowsNullPointerExceptionIfValueIsNull()
    {
        final int key = -9;
        final BiFunction<String, String, String> remappingFunction = (v1, v2) -> "NEW";

        assertThrowsExactly(NullPointerException.class, () -> cache.merge(key, null, remappingFunction));
    }

    @Test
    void mergeThrowsNullPointerExceptionIfRemappingFunctionIsNull()
    {
        final int key = -9;
        final String value = "abc";

        assertThrowsExactly(NullPointerException.class, () -> cache.merge(key, value, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeInsertsNewValueForAnUnknownKey()
    {
        final int key = -9;
        final String value = "abc";
        final BiFunction<String, String, String> remappingFunction = mock(BiFunction.class);

        assertEquals(value, cache.merge(key, value, remappingFunction));

        assertEquals(value, cache.get(key));
        verifyNoInteractions(remappingFunction);
    }

    @Test
    void mergeReplacesAnExistingValue()
    {
        final int key = -9;
        final String oldValue = "abc";
        final String newValue = "abcXYZ";
        cache.put(key, oldValue);
        final BiFunction<String, String, String> remappingFunction = (oldVal, newVal) -> oldVal + newVal;

        assertEquals(newValue, cache.merge(key, "XYZ", remappingFunction));

        assertEquals(newValue, cache.get(key));
    }

    @Test
    void mergeReturnsNullAfterRemovingExistingMapping()
    {
        final int key = -9;
        final String oldValue = "abc";
        cache.put(key, oldValue);
        final BiFunction<String, String, String> remappingFunction = (oldVal, newVal) -> null;

        assertNull(cache.merge(key, "XYZ", remappingFunction));

        assertFalse(cache.containsKey(key));
    }

    @Test
    void putIfAbsentReturnsAnExistingValue()
    {
        final int key = 8;
        final String value = "abc";
        final String replaceValue = "something else";
        cache.put(key, value);

        assertEquals(value, cache.putIfAbsent(key, replaceValue));

        assertEquals(value, cache.get(key));
    }

    @Test
    void putIfAbsentInsertsANewValueIfNoMappingExists()
    {
        final int key = 8;
        final String replaceValue = "something else";

        assertNull(cache.putIfAbsent(key, replaceValue));

        assertEquals(replaceValue, cache.get(key));
    }

    @Test
    void removeIsANoOpIfTheKeyDoesNotExist()
    {
        final int key = 5;
        final String value = "abc";
        assertFalse(cache.remove(key, value));

        assertFalse(cache.containsKey(key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "xyz", "ਯੋਧਾ" })
    void removeIsANoOpIfTheValueDoesNotMatch(final String otherValue)
    {
        final int key = 5;
        final String value = "abc";
        cache.put(key, value);

        assertFalse(cache.remove(key, otherValue));

        assertEquals(value, cache.get(key));
    }

    @Test
    void removeDeletesAKeyUponValueMatch()
    {
        final Int2ObjectCache<CharSequence> cache = new Int2ObjectCache<>(2, 4, (v) -> {});
        final int key = 42;
        cache.put(key, new CharSequenceKey("1,2,3"));

        assertTrue(cache.remove(key, "1,2,3"));

        assertFalse(cache.containsKey(key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "xyz", "ਯੋਧਾ" })
    void replaceReturnsFalseIfOldValueDoesNotMatch(final String oldValue)
    {
        final int key = -5;
        final String value = "abc";
        final String newValue = "NEW";
        cache.put(key, value);

        assertFalse(cache.replace(key, oldValue, newValue));

        assertEquals(value, cache.get(key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "xyz", "ਯੋਧਾ" })
    void replaceIsANoOpIfTheKeyDoesNotExist(final String oldValue)
    {
        final int key = 0;

        assertFalse(cache.replace(key, oldValue, "NEW"));

        assertFalse(cache.containsKey(key));
    }

    @Test
    void replaceReturnsTrueAfterChangingTheMapping()
    {
        final Int2ObjectCache<CharSequence> cache = new Int2ObjectCache<>(2, 4, (v) -> {});
        final int key = -990;
        cache.put(key, new CharSequenceKey("old"));

        assertTrue(cache.replace(key, "old", "new"));

        assertEquals("new", cache.get(key));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "hello", "добрий вечір" })
    void replaceReturnsOldValueAfterReplacingItWithANewOne(final String value)
    {
        final int key = 2;
        final String newValue = "new";
        cache.put(key, value);

        assertEquals(value, cache.replace(key, newValue));

        assertEquals(newValue, cache.get(key));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "hello", "добрий вечір" })
    void replaceReturnsNullWithoutAddingMappingForAnUnknownKey(final String newValue)
    {
        final int key = 2;

        assertNull(cache.replace(key, newValue));

        assertFalse(cache.containsKey(key));
    }

    @Test
    void replaceThrowsNullPointerExceptionIfNewValueIsNull()
    {
        final int key = 2;
        final String value = "abc";
        cache.put(key, value);

        assertThrowsExactly(NullPointerException.class, () -> cache.replace(key, null));
    }

    @Test
    void replaceAllIntThrowsNullPointerExceptionIfFunctionIsNull()
    {
        assertThrowsExactly(NullPointerException.class, () -> cache.replaceAllInt(null));
    }

    @Test
    void replaceAllIntThrowsNullPointerExceptionIfFunctionReturnsNullForAnyValue()
    {
        cache.put(4, "four");
        final IntObjectToObjectFunction<String, String> function = (k, v) -> null;

        final NullPointerException exception =
            assertThrowsExactly(NullPointerException.class, () -> cache.replaceAllInt(function));
        assertEquals("null values are not supported", exception.getMessage());
    }

    @Test
    void replaceAllIntUpdatesEveryValueMapping()
    {
        cache.put(4, "four");
        cache.put(-1, "minus one");
        final IntObjectToObjectFunction<String, String> function = (k, v) -> v + k;

        cache.replaceAllInt(function);

        assertEquals(2, cache.size());
        assertEquals("minus one-1", cache.get(-1));
        assertEquals("four4", cache.get(4));
    }

    @Test
    void putAllCopiesAllOfTheValuesFromTheSourceMap()
    {
        cache.put(42, "forty two");
        cache.put(0, "zero");
        final Int2ObjectCache<String> otherMap = new Int2ObjectCache<>(8, 32, EVICTION_CONSUMER);
        otherMap.put(1, "1");
        otherMap.put(2, "2");
        otherMap.put(3, "3");
        otherMap.put(42, "42");

        cache.putAll(otherMap);

        assertEquals(5, cache.size());
        assertEquals("zero", cache.get(0));
        assertEquals("1", cache.get(1));
        assertEquals("2", cache.get(2));
        assertEquals("3", cache.get(3));
        assertEquals("42", cache.get(42));
    }

    @Test
    void removeIfOnKeySet()
    {
        final Predicate<Integer> filter = (key) -> true;

        final UnsupportedOperationException exception =
            assertThrowsExactly(UnsupportedOperationException.class, () -> cache.keySet().removeIf(filter));
        assertEquals("Cannot remove from KeySet", exception.getMessage());
    }

    @Test
    void removeIfOnValuesCollection()
    {
        final Predicate<String> filter = (value) -> value.contains("e");

        final UnsupportedOperationException exception =
            assertThrowsExactly(UnsupportedOperationException.class, () -> cache.values().removeIf(filter));
        assertEquals("Cannot remove from ValueCollection", exception.getMessage());
    }

    @Test
    void removeIfOnEntrySetThrowsUnsupportedOperationException()
    {
        final Predicate<Map.Entry<Integer, String>> filter = (entry) -> true;

        final UnsupportedOperationException exception =
            assertThrowsExactly(UnsupportedOperationException.class, () -> cache.entrySet().removeIf(filter));
        assertEquals("Cannot remove from EntrySet", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("setIndexes")
    void testFullSet(final int removeOffset)
    {
        final int[] sameSetKeys = generateSameSetKeys(SET_SIZE + 1);
        final List<String> expectedValues = new ArrayList<>();

        for (final int key : sameSetKeys)
        {
            final String value = Integer.toString(key);
            cache.put(key, value);

            if (expectedValues.size() == SET_SIZE)
            {
                expectedValues.remove(expectedValues.size() - 1);
            }
            expectedValues.add(0, value);

            assertEquals(expectedValues, new ArrayList<>(cache.values()));
        }

        final int keyToRemove = sameSetKeys[sameSetKeys.length - 1 - removeOffset];
        final String removedValue = cache.remove(keyToRemove);
        expectedValues.remove(removedValue);
        assertEquals(expectedValues, new ArrayList<>(cache.values()));
    }

    private int[] generateSameSetKeys(final int count)
    {
        final int[] keys = new int[count];

        int index = 0;
        int candidate = 1;
        while (index < count)
        {
            if (Hashing.hash(candidate, NUM_SETS - 1) == 0)
            {
                keys[index++] = candidate;
            }
            candidate++;
        }

        return keys;
    }

    static IntStream setIndexes()
    {
        return IntStream.range(0, SET_SIZE);
    }
}
