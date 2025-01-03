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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Object2ObjectHashMapKeyEqualityTests extends MapKeyEqualityTests<Integer>
{
    Map<Object, Integer> newMap()
    {
        return new Object2ObjectHashMap<>();
    }

    Integer convert(final Integer value)
    {
        return value;
    }

    @ParameterizedTest
    @MethodSource("lookupKeys")
    void containsValue(final CharSequence key, final Object rawValue)
    {
        final Object2ObjectHashMap<CharSequence, CharSequenceKey> map = new Object2ObjectHashMap<>();
        map.put(new CharSequenceKey("one"), new CharSequenceKey("one"));
        map.put(new CharSequenceKey("two"), new CharSequenceKey("two"));
        map.put(THIS, THIS);
        map.put(new CharSequenceKey("four"), new CharSequenceKey("four"));
        map.put(new CharSequenceKey("五"), new CharSequenceKey("五"));


        final Collection<?> values = map.values();
        if (null == rawValue)
        {
            assertFalse(map.containsValue(key));
            assertFalse(values.contains(key));
        }
        else
        {
            assertTrue(map.containsValue(key));
            assertTrue(values.contains(key));
        }
    }

    @Test
    void entrySetContains()
    {
        final Object2ObjectHashMap<CharSequence, CharSequenceKey> map = new Object2ObjectHashMap<>();
        map.put(new CharSequenceKey("one"), new CharSequenceKey("one"));
        map.put(new CharSequenceKey("two"), new CharSequenceKey("two"));
        map.put(THIS, THIS);
        final Set<Map.Entry<CharSequence, CharSequenceKey>> entrySet = map.entrySet();
        final ArrayList<Map.Entry<CharSequence, CharSequenceKey>> entries = new ArrayList<>(entrySet);

        for (final Map.Entry<CharSequence, CharSequenceKey> e : entries)
        {
            assertTrue(entrySet.contains(e));
        }
    }

    @Test
    void entryEqualsAndHashCode()
    {
        final Object2ObjectHashMap<CharSequence, CharSequenceKey> map = new Object2ObjectHashMap<>();
        map.put(new CharSequenceKey("one"), new CharSequenceKey("one"));
        map.put(THIS, THIS);

        for (final Map.Entry<CharSequence, CharSequenceKey> e : map.entrySet())
        {
            assertEquals(e.getKey().hashCode() ^ e.getValue().hashCode(), e.hashCode());
            assertEquals(e, e);
            assertEquals(e, new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
        }
    }

    @Test
    void clonedEntryEqualsAndHashCode()
    {
        final Object2ObjectHashMap<CharSequence, CharSequenceKey> map = new Object2ObjectHashMap<>();
        map.put(new CharSequenceKey("one"), new CharSequenceKey("one"));
        map.put(THIS, THIS);

        final ArrayList<Map.Entry<CharSequence, CharSequenceKey>> entries = new ArrayList<>(map.entrySet());
        for (final Map.Entry<CharSequence, CharSequenceKey> e : entries)
        {
            assertEquals(e.getKey().hashCode() ^ e.getValue().hashCode(), e.hashCode());
            assertEquals(e, e);
            assertEquals(e, new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
        }
    }
}
