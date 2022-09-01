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

import java.util.*;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Object2ObjectHashMapTest
{
    @Test
    void testToArray()
    {
        final Object2ObjectHashMap<String, String> cut = new Object2ObjectHashMap<>();
        cut.put("a", "valA");
        cut.put("b", "valA");
        cut.put("c", "valA");

        final Object[] array = cut.entrySet().toArray();
        for (final Object entry : array)
        {
            cut.remove(((Entry<?, ?>)entry).getKey());
        }

        assertTrue(cut.isEmpty());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testToArrayTyped()
    {
        final Object2ObjectHashMap<String, String> map = new Object2ObjectHashMap<>();
        map.put("a", "valA");
        map.put("b", "valA");
        map.put("c", "valA");

        final Entry<?, ?>[] type = new Entry[1];
        final Entry<?, ?>[] array = map.entrySet().toArray(type);
        for (final Entry<?, ?> entry : array)
        {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void testToArrayWithArrayListConstructor()
    {
        final Object2ObjectHashMap<String, String> map = new Object2ObjectHashMap<>();
        map.put("a", "valA");
        map.put("b", "valA");
        map.put("c", "valA");

        final List<Entry<String, String>> list = new ArrayList<>(map.entrySet());
        for (final Map.Entry<String, String> entry : list)
        {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void shouldForEachEntries()
    {
        class Entry
        {
            final String key;
            final String value;

            Entry(final String key, final String value)
            {
                this.key = key;
                this.value = value;
            }

            public boolean equals(final Object o)
            {
                if (this == o)
                {
                    return true;
                }

                if (o == null || getClass() != o.getClass())
                {
                    return false;
                }

                final Entry entry = (Entry)o;
                return Objects.equals(key, entry.key) && Objects.equals(value, entry.value);
            }

            public int hashCode()
            {
                return Objects.hash(key, value);
            }

            public String toString()
            {
                return "Entry{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    '}';
            }
        }

        final Object2ObjectHashMap<String, String> map = new Object2ObjectHashMap<>();

        for (int i = 0; i < 11; i++)
        {
            final String val = Integer.toString(i);
            final String key = "key-" + val;
            final String value = "value-" + val;

            map.put(key, value);
        }

        final HashSet<Entry> copyOne = new HashSet<>();
        for (final Map.Entry<String, String> entry : map.entrySet())
        {
            copyOne.add(new Entry(entry.getKey(), entry.getValue()));
        }

        final HashSet<Entry> copyTwo = new HashSet<>();
        map.forEach((key, value) -> copyTwo.add(new Entry(key, value)));

        assertEquals(copyOne, copyTwo);
    }

    @Test
    void shouldForEachValues()
    {
        final Object2ObjectHashMap<String, String> map = new Object2ObjectHashMap<>();
        for (int i = 0; i < 11; i++)
        {
            final String val = Integer.toString(i);
            map.put("key-" + val, "value-" + val);
        }

        final Collection<String> copyOne = new HashSet<>();
        for (final String s : map.values())
        {
            //noinspection UseBulkOperation
            copyOne.add(s);
        }

        final Collection<String> copyTwo = new HashSet<>();
        map.values().forEach(copyTwo::add);

        assertEquals(copyTwo, copyOne);
    }

    @Test
    void shouldForEachKeys()
    {
        final Object2ObjectHashMap<String, String> map = new Object2ObjectHashMap<>();
        for (int i = 0; i < 11; i++)
        {
            final String val = Integer.toString(i);
            map.put("key-" + val, "value-" + val);
        }

        final Collection<String> copyOne = new HashSet<>();
        for (final String s : map.keySet())
        {
            //noinspection UseBulkOperation
            copyOne.add(s);
        }

        final Collection<String> copyTwo = new HashSet<>();
        map.keySet().forEach(copyTwo::add);

        assertEquals(copyTwo, copyOne);
    }
}
