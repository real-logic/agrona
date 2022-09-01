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

import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Object2ObjectNullableHashMapKeyEqualityTests extends Object2ObjectHashMapKeyEqualityTests
{
    Map<Object, Integer> newMap()
    {
        return new Object2NullableObjectHashMap<>();
    }

    @Test
    void entryEqualsAndHashCode()
    {
        final Object2NullableObjectHashMap<CharSequence, CharSequenceKey> map = new Object2NullableObjectHashMap<>();
        map.put(new CharSequenceKey("one"), null);
        map.put(THIS, new CharSequenceKey("test"));

        for (final Map.Entry<CharSequence, CharSequenceKey> e : map.entrySet())
        {
            assertEquals(e.getKey().hashCode() ^ Objects.hashCode(e.getValue()), e.hashCode());
            assertEquals(e, e);
            assertEquals(e, new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
        }
    }

    @Test
    void clonedEntryEqualsAndHashCode()
    {
        final Object2NullableObjectHashMap<CharSequence, CharSequenceKey> map = new Object2NullableObjectHashMap<>();
        map.put(new CharSequenceKey("one"), new CharSequenceKey("one"));
        map.put(THIS, THIS);

        final ArrayList<Map.Entry<CharSequence, CharSequenceKey>> entries = new ArrayList<>(map.entrySet());
        for (final Map.Entry<CharSequence, CharSequenceKey> e : entries)
        {
            assertEquals(e.getKey().hashCode() ^ Objects.hashCode(e.getValue()), e.hashCode());
            assertEquals(e, e);
            assertEquals(e, new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
        }
    }

    @Test
    void entrySetValue()
    {
        final Object2NullableObjectHashMap<CharSequence, Integer> map = new Object2NullableObjectHashMap<>();
        map.put(new CharSequenceKey("test"), -20);

        for (final Map.Entry<CharSequence, Integer> e : map.entrySet())
        {
            assertEquals(-20, e.getValue());
            Integer oldValue = e.setValue(null);
            assertEquals(-20, oldValue);
            assertNull(e.getValue());
            assertNull(map.get(e.getKey()));

            oldValue = e.setValue(-100);
            assertNull(oldValue);
            assertEquals(-100, e.getValue());
            assertEquals(-100, map.get(e.getKey()));
        }
    }

    @Test
    void clonedEntrySetValue()
    {
        final Object2NullableObjectHashMap<CharSequence, Integer> map = new Object2NullableObjectHashMap<>();
        map.put(new CharSequenceKey("one"), 15);

        final ArrayList<Map.Entry<CharSequence, Integer>> entries = new ArrayList<>(map.entrySet());
        for (final Map.Entry<CharSequence, Integer> e : entries)
        {
            assertEquals(15, e.getValue());
            Integer oldValue = e.setValue(null);
            assertEquals(15, oldValue);
            assertNull(e.getValue());
            assertNull(map.get(e.getKey()));

            oldValue = e.setValue(100);
            assertNull(oldValue);
            assertEquals(100, e.getValue());
            assertEquals(100, map.get(e.getKey()));
        }
    }
}
