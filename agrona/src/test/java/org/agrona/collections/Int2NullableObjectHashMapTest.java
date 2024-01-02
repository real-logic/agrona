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
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;

import java.util.function.BiFunction;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Int2NullableObjectHashMapTest
{
    @Test
    void getOrDefaultShouldReturnDefaultValueIfNoMappingExistsForAGivenKey()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = -2;
        final String defaultValue = "fallback";

        assertEquals(defaultValue, map.getOrDefault(key, defaultValue));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "abc")
    void getOrDefaultShouldReturnExistingValueForTheGivenKey(final String value)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 121;
        final String defaultValue = "default value";
        map.put(key, value);

        assertEquals(value, map.getOrDefault(key, defaultValue));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "abc")
    void replaceShouldReturnAnOldValueAfterReplacingAnExisitngValue(final String value)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = Integer.MIN_VALUE;
        final String newValue = "new value";
        map.put(key, value);

        assertEquals(value, map.replace(key, newValue));

        assertEquals(newValue, map.get(key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "xyz")
    void replaceShouldReturnTrueAfterReplacingAnExistingValue(final String value)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = Integer.MAX_VALUE;
        final String newValue = "new value";
        map.put(key, value);

        assertTrue(map.replace(key, value, newValue));
        assertEquals(newValue, map.get(key));
    }

    @Test
    void replaceShouldReplaceWithNullValue()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 0;
        final String value = "change me";
        map.put(key, value);

        assertTrue(map.replace(key, value, null));
        assertEquals(1, map.size());
        assertNull(map.get(key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void putIfAbsentShouldReturnAnExistingValueForAnExistingKey(final String value)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        final String newValue = " this is something new";
        map.put(key, value);

        assertEquals(value, map.putIfAbsent(key, newValue));
    }

    @Test
    void putIfAbsentShouldReturnNullAfterReplacingExistingNullMapping()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        final String newValue = " this is something new";
        map.put(key, null);

        assertNull(map.putIfAbsent(key, newValue));

        assertEquals(newValue, map.get(key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void putIfAbsentShouldReturnNullAfterPuttingANewValue(final String newValue)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        map.put(3, "three");

        assertNull(map.putIfAbsent(key, newValue));

        assertEquals(newValue, map.get(key));
        assertEquals("three", map.get(3));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void removeReturnsTrueAfterRemovingTheKey(final String value)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        map.put(3, "three");
        map.put(key, value);

        assertTrue(map.remove(key, value));

        assertEquals(1, map.size());
        assertEquals("three", map.get(3));
    }

    @Test
    void computeIfAbsentThrowsNullPointerExceptionIfMappingFunctionIsNull()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 2;

        assertThrows(NullPointerException.class, () -> map.computeIfAbsent(key, null));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "val 1", "你好" })
    @SuppressWarnings("unchecked")
    void computeIfAbsentReturnsAnExistingValueWithoutInvokingTheMappingFunction(final String value)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 2;
        final IntFunction<String> mappingFunction = mock(IntFunction.class);
        map.put(key, value);

        assertEquals(value, map.computeIfAbsent(key, mappingFunction));

        assertEquals(value, map.get(key));
        verifyNoInteractions(mappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfAbsentReturnsNullIfMappingFunctionReturnsNull()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final IntFunction<String> mappingFunction = mock(IntFunction.class);
        final int key = 2;

        assertNull(map.computeIfAbsent(key, mappingFunction));

        assertFalse(map.containsKey(key));
        verify(mappingFunction).apply(key);
        verifyNoMoreInteractions(mappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfAbsentReturnsNewValueAfterCreatingAMapping()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 2;
        final String value = "new value";
        final IntFunction<String> mappingFunction = mock(IntFunction.class);
        when(mappingFunction.apply(key)).thenReturn(value);

        assertEquals(value, map.computeIfAbsent(key, mappingFunction));

        assertTrue(map.containsKey(key));
        assertEquals(value, map.get(key));
        verify(mappingFunction).apply(key);
        verifyNoMoreInteractions(mappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfAbsentReturnsNewValueAfterReplacingNullMapping()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = -190;
        map.put(key, null);
        final String value = "new value";
        final IntFunction<String> mappingFunction = mock(IntFunction.class);
        when(mappingFunction.apply(key)).thenReturn(value);

        assertEquals(value, map.computeIfAbsent(key, mappingFunction));

        assertTrue(map.containsKey(key));
        assertEquals(value, map.get(key));
        verify(mappingFunction).apply(key);
        verifyNoMoreInteractions(mappingFunction);
    }

    @Test
    void computeIfPresentThrowsNullPointerExceptionIfRemappingFunctionIsNull()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 3;

        assertThrowsExactly(NullPointerException.class, () -> map.computeIfPresent(key, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfPresentReturnsNullForNonExistingKey()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final IntObjectToObjectFunction<String, String> remappingFunction = mock(IntObjectToObjectFunction.class);
        final int key = 3;

        assertNull(map.computeIfPresent(key, remappingFunction));

        assertFalse(map.containsKey(key));
        verifyNoInteractions(remappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfPresentReturnsNullForIfKeyIsMappedToNull()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final IntObjectToObjectFunction<String, String> remappingFunction = mock(IntObjectToObjectFunction.class);
        final int key = 3;
        map.put(key, null);

        assertNull(map.computeIfPresent(key, remappingFunction));

        assertTrue(map.containsKey(key));
        assertNull(map.get(key));
        verifyNoInteractions(remappingFunction);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void computeIfPresentReturnsNewValueAfterAnUpdate(final String oldValue)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        map.put(key, oldValue);
        map.put(5, "five");
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) -> k + v;
        final String expectedNewValue = key + oldValue;

        assertEquals(expectedNewValue, map.computeIfPresent(key, remappingFunction));

        assertEquals(expectedNewValue, map.get(key));
        assertEquals("five", map.get(5));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void computeIfPresentReturnsNullAfterRemovingAnExistingValue(final String value)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        map.put(key, value);
        map.put(5, "five");
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) -> null;

        assertNull(map.computeIfPresent(key, remappingFunction));

        assertFalse(map.containsKey(key));
        assertEquals("five", map.get(5));
    }


    @Test
    void computeThrowsNullPointerExceptionIfRemappingFunctionIsNull()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 3;

        assertThrowsExactly(NullPointerException.class, () -> map.compute(key, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeReturnsNullForUnExistingKey()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final IntObjectToObjectFunction<String, String> remappingFunction = mock(IntObjectToObjectFunction.class);
        final int key = 3;
        map.put(5, "five");

        assertNull(map.compute(key, remappingFunction));

        assertEquals("five", map.get(5));
        assertFalse(map.containsKey(key));
        verify(remappingFunction).apply(key, null);
        verifyNoMoreInteractions(remappingFunction);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void computeReturnsNullAfterRemovingAnExistingValue(final String value)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        map.put(key, value);
        map.put(5, "five");
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) -> null;

        assertNull(map.compute(key, remappingFunction));

        assertFalse(map.containsKey(key));
        assertEquals("five", map.get(5));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void computeReturnsANewValueAfterReplacingAnExistingOne(final String oldValue)
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        map.put(key, oldValue);
        map.put(5, "five");
        final String newValue = key + oldValue;
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) ->
        {
            assertEquals(key, k);
            assertEquals(oldValue, v);
            return k + v;
        };

        assertEquals(newValue, map.compute(key, remappingFunction));

        assertEquals(newValue, map.get(key));
        assertEquals("five", map.get(5));
    }

    @Test
    void computeReturnsANewValueAfterCreatingANewMapping()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        map.put(5, "five");
        final String newValue = String.valueOf(System.currentTimeMillis());
        final IntObjectToObjectFunction<String, String> remappingFunction = (k, v) -> newValue;

        assertEquals(newValue, map.compute(key, remappingFunction));

        assertEquals(newValue, map.get(key));
        assertEquals("five", map.get(5));
    }

    @Test
    void mergeThrowsNullPointerExceptionIfValueIsNull()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 42;
        final BiFunction<String, String, String> remappingFunction = (oldValue, newValue) -> null;

        assertThrowsExactly(NullPointerException.class, () -> map.merge(key, null, remappingFunction));
    }

    @Test
    void mergeThrowsNullPointerExceptionIfRemappingFunctionIsNull()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 8888;
        final String value = "value";

        assertThrowsExactly(NullPointerException.class, () -> map.merge(key, value, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeShouldPutANewMappingForAnUnknownKeyWithoutCallingARemappingFunction()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 8888;
        final String value = "value";
        final BiFunction<String, String, String> remappingFunction = mock(BiFunction.class);

        assertEquals(value, map.merge(key, value, remappingFunction));

        assertEquals(value, map.get(key));
        verifyNoInteractions(remappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeShouldReplaceNullMappingWithAGivenValue()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 8888;
        final String value = "value";
        final BiFunction<String, String, String> remappingFunction = mock(BiFunction.class);
        map.put(key, null);

        assertEquals(value, map.merge(key, value, remappingFunction));

        assertEquals(value, map.get(key));
        verifyNoInteractions(remappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeShouldReplaceExistingValueWithComputedValue()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final int key = 8888;
        final String value = "value";
        final String newValue = "NEW";
        final String computedValue = "value => NEW";
        final BiFunction<String, String, String> remappingFunction = mock(BiFunction.class);
        when(remappingFunction.apply(any(), any())).thenAnswer((Answer<String>)invocation ->
        {
            final String oldVal = invocation.getArgument(0);
            final String val = invocation.getArgument(1);
            return oldVal + " => " + val;
        });
        map.put(key, value);

        assertEquals(computedValue, map.merge(key, newValue, remappingFunction));

        assertEquals(computedValue, map.get(key));
        verify(remappingFunction).apply(value, newValue);
        verifyNoMoreInteractions(remappingFunction);
    }

    @Test
    void removeIfIntOnEntrySet()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        final IntObjPredicate<String> filter = (key, value) -> (key & 1) == 0 || null == value;
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");
        map.put(5, null);

        assertTrue(map.entrySet().removeIfInt(filter));

        assertEquals(2, map.size());
        assertEquals("one", map.get(1));
        assertEquals("three", map.get(3));

        assertFalse(map.entrySet().removeIfInt(filter));
        assertEquals(2, map.size());
    }

    @Test
    void forEachIntShouldUnmapNullValuesBeforeInvokingTheAction()
    {
        final Int2NullableObjectHashMap<String> map = new Int2NullableObjectHashMap<>();
        map.put(1, "one");
        map.put(2, null);
        map.put(3, "three");
        map.put(4, null);
        final MutableInteger count = new MutableInteger();
        final IntObjConsumer<String> action = (k, v) ->
        {
            count.increment();
            if ((k & 1) == 0)
            {
                assertNull(v);
            }
            else
            {
                assertNotNull(v);
            }
        };

        map.forEachInt(action);

        assertEquals(4, count.get());
        assertEquals(4, map.size());
    }
}
