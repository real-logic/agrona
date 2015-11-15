/*
 * Copyright 2014 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.agrona.collections;

import org.junit.Test;

import java.util.*;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class Int2ObjectCacheTest
{
    public static final int MAX_SIZE = 10;

    public static final Consumer<String> EVICTION_HANDLER = (s) -> { };

    private final Int2ObjectCache<String> int2ObjectCache = new Int2ObjectCache<>(MAX_SIZE, EVICTION_HANDLER);

    @Test
    public void shouldDoPutAndThenGet()
    {
        final String value = "Seven";
        int2ObjectCache.put(7, value);

        assertThat(int2ObjectCache.get(7), is(value));
    }

    @Test
    public void shouldReplaceExistingValueForTheSameKey()
    {
        final int key = 7;
        final String value = "Seven";
        int2ObjectCache.put(key, value);

        final String newValue = "New Seven";
        int2ObjectCache.put(key, newValue);

        assertThat(int2ObjectCache.get(key), is(newValue));
        assertThat(int2ObjectCache.size(), is(1));
    }

    @Test
    public void shouldLimitSizeToMaxSize()
    {
        for (int i = 0; i < (MAX_SIZE * 2); i++)
        {
            int2ObjectCache.put(i, Integer.toString(i));
        }

        assertThat(int2ObjectCache.size(), greaterThan(0));
        assertThat(int2ObjectCache.size(), lessThanOrEqualTo(MAX_SIZE));
    }

    @Test
    public void shouldClearCollection()
    {
        for (int i = 0; i < MAX_SIZE; i++)
        {
            int2ObjectCache.put(i, Integer.toString(i));
        }

        assertThat(int2ObjectCache.size(), greaterThan(0));

        int2ObjectCache.clear();

        assertThat(int2ObjectCache.size(), is(0));
    }

    @Test
    public void shouldContainValue()
    {
        final int key = 7;
        final String value = "Seven";

        int2ObjectCache.put(key, value);

        assertTrue(int2ObjectCache.containsValue(value));
        assertFalse(int2ObjectCache.containsValue("NoKey"));
    }

    @Test
    public void shouldContainKey()
    {
        final int key = 7;
        final String value = "Seven";

        int2ObjectCache.put(key, value);

        assertTrue(int2ObjectCache.containsKey(key));
        assertFalse(int2ObjectCache.containsKey(0));
    }

    @Test
    public void shouldRemoveEntry()
    {
        final int key = 7;
        final String value = "Seven";

        int2ObjectCache.put(key, value);

        assertTrue(int2ObjectCache.containsKey(key));

        int2ObjectCache.remove(key);

        assertFalse(int2ObjectCache.containsKey(key));
    }

    @Test
    public void shouldRemoveEntryAndCompactCollisionChain()
    {
        final int key = 12;
        final String value = "12";

        int2ObjectCache.put(key, value);
        int2ObjectCache.put(13, "13");

        final int collisionKey = key + int2ObjectCache.capacity();
        final String collisionValue = Integer.toString(collisionKey);

        int2ObjectCache.put(collisionKey, collisionValue);
        int2ObjectCache.put(14, "14");

        assertThat(int2ObjectCache.remove(key), is(value));
    }

    @Test
    public void shouldIterateValues()
    {
        final Collection<String> initialSet = new HashSet<>();

        for (int i = 0; i < (MAX_SIZE - 1); i++)
        {
            final String value = Integer.toString(i);
            int2ObjectCache.put(i, value);
            initialSet.add(value);
        }

        final Collection<String> copyToSet = new HashSet<>();

        for (final String s : int2ObjectCache.values())
        {
            copyToSet.add(s);
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    public void shouldIterateKeysGettingIntAsPrimitive()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < (MAX_SIZE - 1); i++)
        {
            final String value = Integer.toString(i);
            int2ObjectCache.put(i, value);
            initialSet.add(i);
        }

        final Collection<Integer> copyToSet = new HashSet<>();

        for (final Int2ObjectCache.KeyIterator iter = int2ObjectCache.keySet().iterator(); iter.hasNext();)
        {
            copyToSet.add(iter.nextInt());
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    public void shouldIterateKeys()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < (MAX_SIZE - 1); i++)
        {
            final String value = Integer.toString(i);
            int2ObjectCache.put(i, value);
            initialSet.add(i);
        }

        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
    }

    private void assertIterateKeys(final Collection<Integer> initialSet)
    {
        final Collection<Integer> copyToSet = new HashSet<>();
        for (final Integer aInteger : int2ObjectCache.keySet())
        {
            copyToSet.add(aInteger);
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    public void shouldIterateEntries()
    {
        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            int2ObjectCache.put(i, value);
        }

        iterateEntries();
        iterateEntries();
        iterateEntries();

        for (final Map.Entry<Integer, String> entry : int2ObjectCache.entrySet())
        {
            assertThat(String.valueOf(entry.getKey()), equalTo(entry.getValue()));
        }
    }

    private void iterateEntries()
    {
        for (final Map.Entry<Integer, String> entry : int2ObjectCache.entrySet())
        {
            assertThat(String.valueOf(entry.getKey()), equalTo(entry.getValue()));
        }
    }

    @Test
    public void shouldGenerateStringRepresentation()
    {
        final int[] testEntries = {3, 1, 19, 7, 11, 12, 7};

        for (final int testEntry : testEntries)
        {
            int2ObjectCache.put(testEntry, String.valueOf(testEntry));
        }

        final String mapAsAString = "{12=12, 11=11, 7=7, 19=19, 3=3, 1=1}";
        assertThat(int2ObjectCache.toString(), equalTo(mapAsAString));
    }
}

