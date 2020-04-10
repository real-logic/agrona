/*
 * Copyright 2014-2020 Real Logic Limited.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class Object2ObjectHashMapTest
{
    @Test
    public void testToArray()
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
    public void testToArrayTyped()
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
    public void testToArrayWithArrayListConstructor()
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
    public void shouldForEachValues()
    {
        final Object2ObjectHashMap<String, String> map = new Object2ObjectHashMap<>();
        for (int i = 0; i < 11; i++)
        {
            final String val = Integer.toString(i);
            map.put(val, val);
        }

        final Collection<String> copyToSetOne = new ArrayList<>();
        for (final String s : map.values())
        {
            //noinspection UseBulkOperation
            copyToSetOne.add(s);
        }

        final Collection<String> copyToSetTwo = new ArrayList<>();
        map.values().forEach(copyToSetTwo::add);

        assertEquals(copyToSetTwo, copyToSetOne);
    }

    @Test
    public void shouldForEachKeys()
    {
        final Object2ObjectHashMap<String, String> map = new Object2ObjectHashMap<>();
        for (int i = 0; i < 11; i++)
        {
            final String val = Integer.toString(i);
            map.put(val, val);
        }

        final Collection<String> copyToSetOne = new ArrayList<>();
        for (final String s : map.keySet())
        {
            //noinspection UseBulkOperation
            copyToSetOne.add(s);
        }

        final Collection<String> copyToSetTwo = new ArrayList<>();
        map.keySet().forEach(copyToSetTwo::add);

        assertEquals(copyToSetTwo, copyToSetOne);
    }
}
