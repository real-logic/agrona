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
package org.agrona.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BiBiInt2NullableObjectMapTest
{
    @Test
    void getOrDefaultShouldReturnDefaultValueIfNoMappingExistsForAGivenKey()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = -2;
        final String defaultValue = "fallback";

        assertEquals(defaultValue, map.getOrDefault(key, key, defaultValue));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "abc")
    void getOrDefaultShouldReturnExistingValueForTheGivenKey(final String value)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 121;
        final String defaultValue = "default value";
        map.put(key, key, value);

        assertEquals(value, map.getOrDefault(key, key, defaultValue));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "abc")
    void replaceShouldReturnAnOldValueAfterReplacingAnExistingValue(final String value)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = Integer.MIN_VALUE;
        final String newValue = "new value";
        map.put(key, key, value);

        assertEquals(value, map.replace(key, key, newValue));

        assertEquals(newValue, map.get(key, key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "xyz")
    void replaceShouldReturnTrueAfterReplacingAnExistingValue(final String value)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = Integer.MAX_VALUE;
        final String newValue = "new value";
        map.put(key, key, value);

        assertTrue(map.replace(key, key, value, newValue));
        assertEquals(newValue, map.get(key, key));
    }

    @Test
    void replaceShouldReplaceWithNullValue()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 0;
        final String value = "change me";
        map.put(key, key, value);

        assertTrue(map.replace(key, key, value, null));
        assertEquals(1, map.size());
        assertNull(map.get(key, key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void putIfAbsentShouldReturnAnExistingValueForAnExistingKey(final String value)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        final String newValue = " this is something new";
        map.put(key, key, value);

        assertEquals(value, map.putIfAbsent(key, key, newValue));
    }

    @Test
    void putIfAbsentShouldReturnNullAfterReplacingExistingNullMapping()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        final String newValue = " this is something new";
        map.put(key, key, null);

        assertNull(map.putIfAbsent(key, key, newValue));

        assertEquals(newValue, map.get(key, key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void putIfAbsentShouldReturnNullAfterPuttingANewValue(final String newValue)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        map.put(3, 3, "three");

        assertNull(map.putIfAbsent(key, key, newValue));

        assertEquals(newValue, map.get(key, key));
        assertEquals("three", map.get(3, 3));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void removeReturnsTrueAfterRemovingTheKey(final String value)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        map.put(3, 3, "three");
        map.put(key, key, value);

        assertTrue(map.remove(key, key, value));

        assertEquals(1, map.size());
        assertEquals("three", map.get(3, 3));
    }

    @Test
    void computeIfAbsentThrowsNullPointerExceptionIfMappingFunctionIsNull()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 2;

        assertThrows(NullPointerException.class, () -> map.computeIfAbsent(key, key, null));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "val 1", "你好" })
    @SuppressWarnings("unchecked")
    void computeIfAbsentReturnsAnExistingValueWithoutInvokingTheMappingFunction(final String value)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 2;
        final BiInt2ObjectMap.EntryFunction<String> mappingFunction = mock(BiInt2ObjectMap.EntryFunction.class);
        map.put(key, key, value);

        assertEquals(value, map.computeIfAbsent(key, key, mappingFunction));

        assertEquals(value, map.get(key, key));
        verifyNoInteractions(mappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfAbsentReturnsNullIfMappingFunctionReturnsNull()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final BiInt2ObjectMap.EntryFunction<String> mappingFunction = mock(BiInt2ObjectMap.EntryFunction.class);
        final int key = 2;

        assertNull(map.computeIfAbsent(key, key, mappingFunction));

        assertFalse(map.containsKey(key, key));
        verify(mappingFunction).apply(key, key);
        verifyNoMoreInteractions(mappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfAbsentReturnsNewValueAfterCreatingAMapping()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 2;
        final String value = "new value";
        final BiInt2ObjectMap.EntryFunction<String> mappingFunction = mock(BiInt2ObjectMap.EntryFunction.class);
        when(mappingFunction.apply(key, key)).thenReturn(value);

        assertEquals(value, map.computeIfAbsent(key, key, mappingFunction));

        assertTrue(map.containsKey(key, key));
        assertEquals(value, map.get(key, key));
        verify(mappingFunction).apply(key, key);
        verifyNoMoreInteractions(mappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfAbsentReturnsNewValueAfterReplacingNullMapping()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = -190;
        map.put(key, key, null);
        final String value = "new value";
        final BiInt2ObjectMap.EntryFunction<String> mappingFunction = mock(BiInt2ObjectMap.EntryFunction.class);
        when(mappingFunction.apply(key, key)).thenReturn(value);

        assertEquals(value, map.computeIfAbsent(key, key, mappingFunction));

        assertTrue(map.containsKey(key, key));
        assertEquals(value, map.get(key, key));
        verify(mappingFunction).apply(key, key);
        verifyNoMoreInteractions(mappingFunction);
    }

    @Test
    void computeIfPresentThrowsNullPointerExceptionIfRemappingFunctionIsNull()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 3;

        assertThrowsExactly(NullPointerException.class, () -> map.computeIfPresent(key, key, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfPresentReturnsNullForNonExistingKey()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final BiInt2ObjectMap.EntryRemap<String, String> remappingFunction = mock(BiInt2ObjectMap.EntryRemap.class);
        final int key = 3;

        assertNull(map.computeIfPresent(key, key, remappingFunction));

        assertFalse(map.containsKey(key, key));
        verifyNoInteractions(remappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeIfPresentReturnsNullForIfKeyIsMappedToNull()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final BiInt2ObjectMap.EntryRemap<String, String> remappingFunction = mock(BiInt2ObjectMap.EntryRemap.class);
        final int key = 3;
        map.put(key, key, null);

        assertNull(map.computeIfPresent(key, key, remappingFunction));

        assertTrue(map.containsKey(key, key));
        assertNull(map.get(key, key));
        verifyNoInteractions(remappingFunction);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void computeIfPresentReturnsNewValueAfterAnUpdate(final String oldValue)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        map.put(key, key, oldValue);
        map.put(5, 5, "five");
        final BiInt2ObjectMap.EntryRemap<String, String> remappingFunction = (k1, k2, v) -> k1 + k2 + v;
        final String expectedNewValue = key + key + oldValue;

        assertEquals(expectedNewValue, map.computeIfPresent(key, key, remappingFunction));

        assertEquals(expectedNewValue, map.get(key, key));
        assertEquals("five", map.get(5, 5));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void computeIfPresentReturnsNullAfterRemovingAnExistingValue(final String value)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        map.put(key, key, value);
        map.put(5, 5, "five");
        @SuppressWarnings("unchecked")
        final BiInt2ObjectMap.EntryRemap<String, String> remappingFunction = mock(BiInt2ObjectMap.EntryRemap.class);

        assertNull(map.computeIfPresent(key, key, remappingFunction));

        assertFalse(map.containsKey(key, key));
        assertEquals("five", map.get(5, 5));
    }


    @Test
    void computeThrowsNullPointerExceptionIfRemappingFunctionIsNull()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 3;

        assertThrowsExactly(NullPointerException.class, () -> map.compute(key, key, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeReturnsNullForUnExistingKey()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final BiInt2ObjectMap.EntryRemap<String, String> remappingFunction = mock(BiInt2ObjectMap.EntryRemap.class);
        final int key = 3;
        map.put(5, 5, "five");

        assertNull(map.compute(key, key, remappingFunction));

        assertEquals("five", map.get(5, 5));
        assertFalse(map.containsKey(key, key));
        verify(remappingFunction).apply(key, key, null);
        verifyNoMoreInteractions(remappingFunction);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void computeReturnsNullAfterRemovingAnExistingValue(final String value)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        map.put(key, key, value);
        map.put(5, 5, "five");
        final BiInt2ObjectMap.EntryRemap<String, String> remappingFunction = (k1, k2, v) -> null;

        assertNull(map.compute(key, key, remappingFunction));

        assertFalse(map.containsKey(key, key));
        assertEquals("five", map.get(5, 5));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "val 1", "你好" })
    void computeReturnsANewValueAfterReplacingAnExistingOne(final String oldValue)
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        map.put(key, key, oldValue);
        map.put(5, 5, "five");
        final String newValue = key + key + oldValue;
        final BiInt2ObjectMap.EntryRemap<String, String> remappingFunction = (k1, k2, v) ->
        {
            assertEquals(key, k1);
            assertEquals(key, k2);
            assertEquals(oldValue, v);
            return k1 + k2 + v;
        };

        assertEquals(newValue, map.compute(key, key, remappingFunction));

        assertEquals(newValue, map.get(key, key));
        assertEquals("five", map.get(5, 5));
    }

    @Test
    void computeReturnsANewValueAfterCreatingANewMapping()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        map.put(5, 5, "five");
        final String newValue = String.valueOf(System.currentTimeMillis());
        final BiInt2ObjectMap.EntryRemap<String, String> remappingFunction = (k1, k2, v) -> newValue;

        assertEquals(newValue, map.compute(key, key, remappingFunction));

        assertEquals(newValue, map.get(key, key));
        assertEquals("five", map.get(5, 5));
    }

    @Test
    void mergeThrowsNullPointerExceptionIfValueIsNull()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 42;
        final BiFunction<String, String, String> remappingFunction = (oldValue, newValue) -> null;

        assertThrowsExactly(NullPointerException.class, () -> map.merge(key, key, null, remappingFunction));
    }

    @Test
    void mergeThrowsNullPointerExceptionIfRemappingFunctionIsNull()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 8888;
        final String value = "value";

        assertThrowsExactly(NullPointerException.class, () -> map.merge(key, key, value, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeShouldPutANewMappingForAnUnknownKeyWithoutCallingARemappingFunction()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 8888;
        final String value = "value";
        final BiFunction<String, String, String> remappingFunction = mock(BiFunction.class);

        assertEquals(value, map.merge(key, key, value, remappingFunction));

        assertEquals(value, map.get(key, key));
        verifyNoInteractions(remappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeShouldReplaceNullMappingWithAGivenValue()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        final int key = 8888;
        final String value = "value";
        final BiFunction<String, String, String> remappingFunction = mock(BiFunction.class);
        map.put(key, key, null);

        assertEquals(value, map.merge(key, key, value, remappingFunction));

        assertEquals(value, map.get(key, key));
        verifyNoInteractions(remappingFunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeShouldReplaceExistingValueWithComputedValue()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
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
        map.put(key, key, value);

        assertEquals(computedValue, map.merge(key, key, newValue, remappingFunction));

        assertEquals(computedValue, map.get(key, key));
        verify(remappingFunction).apply(value, newValue);
        verifyNoMoreInteractions(remappingFunction);
    }

    @Test
    void forEachIntShouldUnmapNullValuesBeforeInvokingTheAction()
    {
        final BiInt2NullableObjectMap<String> map = new BiInt2NullableObjectMap<>();
        map.put(1, 1, "one");
        map.put(2, 2, null);
        map.put(3, 3, "three");
        map.put(4, 4, null);
        final MutableInteger count = new MutableInteger();
        final BiInt2ObjectMap.EntryConsumer<String> action = (k1, k2, v) ->
        {
            count.increment();
            if ((k1 & 1) == 0)
            {
                assertNull(v);
            }
            else
            {
                assertNotNull(v);
            }
        };

        map.forEach(action);

        assertEquals(4, count.get());
        assertEquals(4, map.size());
    }
}
