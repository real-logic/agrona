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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

abstract class MapKeyEqualityTests<T extends Number>
{
    static final CharSequenceKey THIS = new CharSequenceKey("this");
    Map<Object, T> map;

    abstract Map<Object, T> newMap();

    abstract T convert(Integer value);

    @BeforeEach
    void beforeEach()
    {
        map = newMap();
        map.put(new CharSequenceKey("one"), convert(1));
        map.put(new CharSequenceKey("two"), convert(2));
        map.put(THIS, convert(3));
        map.put(new CharSequenceKey("four"), convert(4));
        map.put(new CharSequenceKey("五"), convert(5));
    }

    @ParameterizedTest
    @MethodSource("insertKeys")
    void put(final Object key, final Integer oldValue, final Integer newValue)
    {
        assertEquals(convert(oldValue), map.put(key, convert(newValue)));
        assertEquals(convert(newValue), map.get(key));
    }

    @ParameterizedTest
    @MethodSource("lookupKeys")
    void get(final Object key, final Integer rawValue)
    {
        assertEquals(convert(rawValue), map.get(key));
    }

    @ParameterizedTest
    @MethodSource("lookupKeys")
    void remove(final Object key, final Integer rawValue)
    {
        assertEquals(convert(rawValue), map.remove(key));
    }

    @ParameterizedTest
    @MethodSource("lookupKeys")
    void containsKey(final Object key, final Object rawValue)
    {
        if (null == rawValue)
        {
            assertFalse(map.containsKey(key));
            assertFalse(map.keySet().contains(key));
        }
        else
        {
            assertTrue(map.containsKey(key));
            assertTrue(map.keySet().contains(key));
        }
    }

    @Test
    void entrySetContains()
    {
        final Set<Map.Entry<Object, T>> entrySet = map.entrySet();
        final ArrayList<Map.Entry<Object, T>> entries = new ArrayList<>(entrySet);
        for (final Map.Entry<Object, T> e : entries)
        {
            assertTrue(entrySet.contains(e));
        }
    }

    @Test
    void entryEqualAndHashCode()
    {
        for (final Map.Entry<Object, T> e : map.entrySet())
        {
            assertEquals(e.getKey().hashCode() ^ e.getValue().hashCode(), e.hashCode());
            assertEquals(e, e);
            assertEquals(e, new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
            assertNotEquals(e, new AbstractMap.SimpleImmutableEntry<>(null, e.getValue()));
            assertNotEquals(e, new AbstractMap.SimpleImmutableEntry<>(e.getKey(), null));
        }
    }

    @Test
    void entrySetValue()
    {
        for (final Map.Entry<Object, T> e : map.entrySet())
        {
            final T value = e.getValue();
            assertNotNull(value);
            final T newValue = convert(value.intValue() * 2);
            final T oldValue = e.setValue(newValue);
            assertEquals(value, oldValue);
            assertEquals(newValue, e.getValue());
            assertEquals(newValue, map.get(e.getKey()));
        }
    }

    @Test
    void clonedEntryEqualAndHashCode()
    {
        final ArrayList<Map.Entry<Object, T>> entries = new ArrayList<>(map.entrySet());
        for (final Map.Entry<Object, T> e : entries)
        {
            assertEquals(e.getKey().hashCode() ^ e.getValue().hashCode(), e.hashCode());
            assertEquals(e, e);
            assertEquals(e, new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
            assertNotEquals(e, new AbstractMap.SimpleImmutableEntry<>(null, e.getValue()));
            assertNotEquals(e, new AbstractMap.SimpleImmutableEntry<>(e.getKey(), null));
            assertNotEquals(e, new AbstractMap.SimpleImmutableEntry<>(null, null));
        }
    }

    @Test
    void clonedEntrySetValue()
    {
        final ArrayList<Map.Entry<Object, T>> entries = new ArrayList<>(map.entrySet());
        for (final Map.Entry<Object, T> e : entries)
        {
            final T value = e.getValue();
            assertNotNull(value);
            final T newValue = convert(value.intValue() * 2);
            final T oldValue = e.setValue(newValue);
            assertEquals(value, oldValue);
            assertEquals(newValue, e.getValue());
            assertEquals(newValue, map.get(e.getKey()));
        }
    }

    static List<Arguments> lookupKeys()
    {
        return Arrays.asList(
            arguments("one", 1),
            arguments(new CharSequenceKey("two"), 2),
            arguments(THIS, 3),
            arguments(new CharSequenceKey("vier"), null),
            arguments("五", 5));
    }

    static List<Arguments> insertKeys()
    {
        return Arrays.asList(
            arguments("one", 1, 1_000),
            arguments(new CharSequenceKey("two"), 2, Integer.MAX_VALUE),
            arguments(THIS, 3, -33),
            arguments("vier", null, 4),
            arguments(new CharSequenceKey("new"), null, 0));
    }
}
