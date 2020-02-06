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

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.jupiter.api.Assertions.*;

public class Int2ObjectHashMapTest
{
    final Int2ObjectHashMap<String> intToObjectMap;

    public Int2ObjectHashMapTest()
    {
        intToObjectMap = newMap(Hashing.DEFAULT_LOAD_FACTOR, Int2ObjectHashMap.MIN_CAPACITY);
    }

    Int2ObjectHashMap<String> newMap(final float loadFactor, final int initialCapacity)
    {
        return new Int2ObjectHashMap<>(initialCapacity, loadFactor);
    }

    @Test
    public void shouldDoPutAndThenGet()
    {
        final String value = "Seven";
        intToObjectMap.put(7, value);

        assertThat(intToObjectMap.get(7), is(value));
    }

    @Test
    public void shouldReplaceExistingValueForTheSameKey()
    {
        final int key = 7;
        final String value = "Seven";
        intToObjectMap.put(key, value);

        final String newValue = "New Seven";
        final String oldValue = intToObjectMap.put(key, newValue);

        assertThat(intToObjectMap.get(key), is(newValue));
        assertThat(oldValue, is(value));
        assertThat(intToObjectMap.size(), is(1));
    }

    @Test
    public void shouldGrowWhenThresholdExceeded()
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
    public void shouldHandleCollisionAndThenLinearProbe()
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
    public void shouldClearCollection()
    {
        for (int i = 0; i < 15; i++)
        {
            intToObjectMap.put(i, Integer.toString(i));
        }

        assertThat(intToObjectMap.size(), is(15));
        assertThat(intToObjectMap.get(1), is("1"));

        intToObjectMap.clear();

        assertThat(intToObjectMap.size(), is(0));
        assertNull(intToObjectMap.get(1));
    }

    @Test
    public void shouldCompactCollection()
    {
        final int totalItems = 50;
        for (int i = 0; i < totalItems; i++)
        {
            intToObjectMap.put(i, Integer.toString(i));
        }

        for (int i = 0, limit = totalItems - 4; i < limit; i++)
        {
            intToObjectMap.remove(i);
        }

        final int capacityBeforeCompaction = intToObjectMap.capacity();
        intToObjectMap.compact();

        assertThat(intToObjectMap.capacity(), lessThan(capacityBeforeCompaction));
    }

    @Test
    public void shouldContainValue()
    {
        final int key = 7;
        final String value = "Seven";

        intToObjectMap.put(key, value);

        assertTrue(intToObjectMap.containsValue(value));
        assertFalse(intToObjectMap.containsValue("NoKey"));
    }

    @Test
    public void shouldContainKey()
    {
        final int key = 7;
        final String value = "Seven";

        intToObjectMap.put(key, value);

        assertTrue(intToObjectMap.containsKey(key));
        assertFalse(intToObjectMap.containsKey(0));
    }

    @Test
    public void shouldRemoveEntry()
    {
        final int key = 7;
        final String value = "Seven";

        intToObjectMap.put(key, value);

        assertTrue(intToObjectMap.containsKey(key));

        intToObjectMap.remove(key);

        assertFalse(intToObjectMap.containsKey(key));
    }

    @Test
    public void shouldRemoveEntryAndCompactCollisionChain()
    {
        final int key = 12;
        final String value = "12";

        intToObjectMap.put(key, value);
        intToObjectMap.put(13, "13");

        final int collisionKey = key + intToObjectMap.capacity();
        final String collisionValue = Integer.toString(collisionKey);

        intToObjectMap.put(collisionKey, collisionValue);
        intToObjectMap.put(14, "14");

        assertThat(intToObjectMap.remove(key), is(value));
    }

    @Test
    public void shouldIterateValues()
    {
        final Collection<String> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
            initialSet.add(value);
        }

        final Collection<String> copyToSet = new HashSet<>();

        for (final String s : intToObjectMap.values())
        {
            //noinspection UseBulkOperation
            copyToSet.add(s);
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    public void shouldIterateKeysGettingIntAsPrimitive()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
            initialSet.add(i);
        }

        final Collection<Integer> copyToSet = new HashSet<>();

        for (final Int2ObjectHashMap<String>.KeyIterator iter = intToObjectMap.keySet().iterator(); iter.hasNext(); )
        {
            copyToSet.add(iter.nextInt());
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    public void shouldIterateKeys()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
            initialSet.add(i);
        }

        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
    }

    private void assertIterateKeys(final Collection<Integer> initialSet)
    {
        final Collection<Integer> copyToSet = new HashSet<>();
        for (final Integer aInteger : intToObjectMap.keySet())
        {
            //noinspection UseBulkOperation
            copyToSet.add(aInteger);
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    public void shouldIterateAndHandleRemove()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
            initialSet.add(i);
        }

        final Collection<Integer> copyOfSet = new HashSet<>();

        int i = 0;
        for (final Iterator<Integer> iter = intToObjectMap.keySet().iterator(); iter.hasNext(); )
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
        assertThat(initialSet.size(), is(count));
        assertThat(intToObjectMap.size(), is(reducedSetSize));
        assertThat(copyOfSet.size(), is(reducedSetSize));
    }

    @Test
    public void shouldIterateEntries()
    {
        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            intToObjectMap.put(i, value);
        }

        iterateEntries();
        iterateEntries();
        iterateEntries();

        final String testValue = "Wibble";
        for (final Map.Entry<Integer, String> entry : intToObjectMap.entrySet())
        {
            assertThat(String.valueOf(entry.getKey()), equalTo(entry.getValue()));

            if (entry.getKey() == 7)
            {
                entry.setValue(testValue);
            }
        }

        assertThat(intToObjectMap.get(7), equalTo(testValue));
    }

    private void iterateEntries()
    {
        for (final Map.Entry<Integer, String> entry : intToObjectMap.entrySet())
        {
            assertThat(String.valueOf(entry.getKey()), equalTo(entry.getValue()));
        }
    }

    @Test
    public void shouldGenerateStringRepresentation()
    {
        final int[] testEntries = { 3, 1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            intToObjectMap.put(testEntry, String.valueOf(testEntry));
        }

        final String mapAsAString = "{1=1, 19=19, 3=3, 7=7, 11=11, 12=12}";
        assertThat(intToObjectMap.toString(), equalTo(mapAsAString));
    }

    @Test
    public void shouldCopyConstructAndBeEqual()
    {
        final int[] testEntries = { 3, 1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            intToObjectMap.put(testEntry, String.valueOf(testEntry));
        }

        final Int2ObjectHashMap<String> mapCopy = new Int2ObjectHashMap<>(intToObjectMap);
        assertThat(mapCopy, is(intToObjectMap));
    }

    @Test
    public void shouldAllowNullValuesWithNullMapping()
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
    public void testToArray()
    {
        final Int2ObjectHashMap<String> cut = new Int2ObjectHashMap<>();
        cut.put(1, "a");
        cut.put(2, "b");
        cut.put(3, "c");

        final Object[] array = cut.entrySet().toArray();
        for (final Object entry : array)
        {
            cut.remove(((Map.Entry<Integer, String>) entry).getKey());
        }
        assertTrue(cut.isEmpty());
    }

    @Test
    public void testToArrayWithArrayListConstructor()
    {
        final Int2ObjectHashMap<String> cut = new Int2ObjectHashMap<>();
        cut.put(1, "a");
        cut.put(2, "b");
        cut.put(3, "c");

        final List<Map.Entry<Integer, String>> list = new ArrayList<>(cut.entrySet());
        for (final Map.Entry<Integer, String> entry : list)
        {
            cut.remove(entry.getKey());
        }
        assertTrue(cut.isEmpty());
    }
}
