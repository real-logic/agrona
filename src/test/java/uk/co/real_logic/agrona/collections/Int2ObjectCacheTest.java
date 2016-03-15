/*
 * Copyright 2014 - 2016 Real Logic Ltd.
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
import java.util.function.IntFunction;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

public class Int2ObjectCacheTest
{
    public static final int NUM_SETS = 16;
    public static final int SET_SIZE = 4;
    public static final int CAPACITY = NUM_SETS * SET_SIZE;
    public static final Consumer<String> EVICTION_CONSUMER =
        (s) ->
        {
        };

    private final Int2ObjectCache<String> int2ObjectCache = new Int2ObjectCache<>(NUM_SETS, SET_SIZE, EVICTION_CONSUMER);

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
        for (int i = 0; i < (CAPACITY * 2); i++)
        {
            int2ObjectCache.put(i, Integer.toString(i));
        }

        assertThat(int2ObjectCache.size(), greaterThan(0));
        assertThat(int2ObjectCache.size(), lessThanOrEqualTo(CAPACITY));
    }

    @Test
    public void shouldClearCollection()
    {
        for (int i = 0; i < CAPACITY; i++)
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
    public void shouldIterateValues()
    {
        final Collection<String> initialSet = new HashSet<>();

        for (int i = 0; i < (CAPACITY - 1); i++)
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

        for (int i = 0; i < (CAPACITY - 1); i++)
        {
            final String value = Integer.toString(i);
            int2ObjectCache.put(i, value);
            initialSet.add(i);
        }

        final Collection<Integer> copyToSet = new HashSet<>();

        for (final Int2ObjectCache.KeyIterator iter = int2ObjectCache.keySet().iterator(); iter.hasNext(); )
        {
            copyToSet.add(iter.nextInt());
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    public void shouldIterateKeys()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < (CAPACITY - 1); i++)
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
        final int count = CAPACITY - 1;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            int2ObjectCache.put(i, value);
        }

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

        final String mapAsAString = "{1=1, 19=19, 3=3, 7=7, 11=11, 12=12}";
        assertThat(int2ObjectCache.toString(), equalTo(mapAsAString));
    }

    @Test
    public void shouldEvictAsMaxSizeIsExceeded()
    {
        final HashSet<String> evictedItems = new HashSet<>();
        final Consumer<String> evictionConsumer = evictedItems::add;
        final Int2ObjectCache<String> cache = new Int2ObjectCache<>(NUM_SETS, SET_SIZE, evictionConsumer);

        final int count = CAPACITY * 2;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            cache.put(i, value);
        }

        assertThat(cache.size() + evictedItems.size(), is(count));
    }

    @Test
    public void shouldComputeIfAbsent()
    {
        final int testKey = 7;
        final String testValue = "7";

        final IntFunction<String> function = (i) -> testValue;

        assertNull(int2ObjectCache.get(testKey));

        assertThat(int2ObjectCache.computeIfAbsent(testKey, function), is(testValue));
        assertThat(int2ObjectCache.get(testKey), is(testValue));
    }

    @Test
    public void shouldTestStats()
    {
        assertThat(int2ObjectCache.cachePuts(), is(0L));
        assertThat(int2ObjectCache.cacheMisses(), is(0L));
        assertThat(int2ObjectCache.cacheHits(), is(0L));

        int2ObjectCache.get(7);
        assertThat(int2ObjectCache.cacheMisses(), is(1L));
        assertThat(int2ObjectCache.cacheHits(), is(0L));
        assertThat(int2ObjectCache.cachePuts(), is(0L));

        int2ObjectCache.put(7, "Seven");
        assertThat(int2ObjectCache.cacheMisses(), is(1L));
        assertThat(int2ObjectCache.cacheHits(), is(0L));
        assertThat(int2ObjectCache.cachePuts(), is(1L));

        int2ObjectCache.get(7);
        assertThat(int2ObjectCache.cacheMisses(), is(1L));
        assertThat(int2ObjectCache.cacheHits(), is(1L));
        assertThat(int2ObjectCache.cachePuts(), is(1L));

        int2ObjectCache.resetCounters();
        assertThat(int2ObjectCache.cachePuts(), is(0L));
        assertThat(int2ObjectCache.cacheMisses(), is(0L));
        assertThat(int2ObjectCache.cacheHits(), is(0L));
    }
}

