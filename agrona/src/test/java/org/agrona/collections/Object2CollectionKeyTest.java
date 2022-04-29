/*
 * Copyright 2014-2022 Real Logic Limited.
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class Object2CollectionKeyTest
{
    private static final CharSequenceKey THREE = new CharSequenceKey("three");

    @ParameterizedTest
    @MethodSource("keys")
    void object2ObjectHashMap(final Object key, final Integer expectedValue)
    {
        final Object2ObjectHashMap<CharSequence, Object> map = new Object2ObjectHashMap<>();
        initialize(map, Integer::valueOf);

        assertEquals(expectedValue, map.get(key));
    }

    @ParameterizedTest
    @MethodSource("keys")
    void object2NullableObjectHashMap(final Object key, final Integer expectedValue)
    {
        final Object2NullableObjectHashMap<CharSequence, Object> map = new Object2NullableObjectHashMap<>();
        initialize(map, Integer::valueOf);

        assertEquals(expectedValue, map.get(key));
    }

    @ParameterizedTest
    @MethodSource("keys")
    void object2IntHashMap(final Object key, final Integer expectedValue)
    {
        final Object2IntHashMap<CharSequence> map = new Object2IntHashMap<>(-1);
        initialize(map, Integer::valueOf);

        assertEquals(expectedValue, map.get(key));
    }

    @ParameterizedTest
    @MethodSource("keys")
    void object2LongHashMap(final Object key, final Integer expectedValue)
    {
        final Object2LongHashMap<CharSequence> map = new Object2LongHashMap<>(-1);
        initialize(map, Long::valueOf);

        if (null == expectedValue)
        {
            assertNull(map.get(key));
        }
        else
        {
            assertEquals(Long.valueOf(expectedValue), map.get(key));
        }
    }

    @ParameterizedTest
    @MethodSource("keys")
    void objectHashSet(final Object key, final Integer expectedValue)
    {
        final Object2ObjectHashMap<CharSequence, Integer> map = new Object2ObjectHashMap<>();
        initialize(map, Integer::valueOf);
        final ObjectHashSet<CharSequence> set = new ObjectHashSet<>();
        set.addAll(map.keySet());

        if (null == expectedValue)
        {
            assertFalse(set.contains(key));
        }
        else
        {
            assertTrue(set.contains(key));
        }
    }

    private static <DATA> void initialize(final Map<CharSequence, DATA> map, final IntFunction<DATA> provider)
    {
        map.put(new CharSequenceKey("one"), provider.apply(1));
        map.put(new CharSequenceKey("two"), provider.apply(2));
        map.put(THREE, provider.apply(3));
        map.put(new CharSequenceKey("four"), provider.apply(4));
        map.put(new CharSequenceKey("五"), provider.apply(5));
    }

    private static List<Arguments> keys()
    {
        return Arrays.asList(
            arguments("one", 1),
            arguments(new CharSequenceKey("two"), 2),
            arguments(THREE, 3),
            arguments(new CharSequenceKey("vier"), null),
            arguments("五", 5));
    }
}
