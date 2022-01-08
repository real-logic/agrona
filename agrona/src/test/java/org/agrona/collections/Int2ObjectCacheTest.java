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

import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;

public class Int2ObjectCacheTest
{
    private static final int NUM_SETS = 16;
    private static final int SET_SIZE = 4;
    private static final int CAPACITY = NUM_SETS * SET_SIZE;
    private static final Consumer<String> EVICTION_CONSUMER = (s) -> {};

    private final Int2ObjectCache<String> cache = new Int2ObjectCache<>(NUM_SETS, SET_SIZE, EVICTION_CONSUMER);

    @Test
    public void shouldDoPutAndThenGet()
    {
        final String value = "Seven";
        cache.put(7, value);

        assertThat(cache.get(7), is(value));
    }

    @Test
    public void shouldReplaceExistingValueForTheSameKey()
    {
        final int key = 7;
        final String value = "Seven";
        cache.put(key, value);

        final String newValue = "New Seven";
        cache.put(key, newValue);

        assertThat(cache.get(key), is(newValue));
        assertThat(cache.size(), is(1));
    }

    @Test
    public void shouldLimitSizeToMaxSize()
    {
        for (int i = 0; i < (CAPACITY * 2); i++)
        {
            cache.put(i, Integer.toString(i));
        }

        assertThat(cache.size(), greaterThan(0));
        assertThat(cache.size(), lessThanOrEqualTo(CAPACITY));
    }

    @Test
    public void shouldClearCollection()
    {
        for (int i = 0; i < CAPACITY; i++)
        {
            cache.put(i, Integer.toString(i));
        }

        assertThat(cache.size(), greaterThan(0));

        cache.clear();

        assertThat(cache.size(), is(0));
    }

    @Test
    public void shouldContainValue()
    {
        final int key = 7;
        final String value = "Seven";

        cache.put(key, value);

        assertTrue(cache.containsValue(value));
        assertFalse(cache.containsValue("NoKey"));
    }

    @Test
    public void shouldContainKey()
    {
        final int key = 7;
        final String value = "Seven";

        cache.put(key, value);

        assertTrue(cache.containsKey(key));
        assertFalse(cache.containsKey(0));
    }

    @Test
    public void shouldRemoveEntry()
    {
        final int key = 7;
        final String value = "Seven";

        cache.put(key, value);

        assertTrue(cache.containsKey(key));

        cache.remove(key);

        assertFalse(cache.containsKey(key));
    }

    @Test
    public void shouldIterateEntries()
    {
        final int count = CAPACITY - 1;
        for (int i = 0; i < count; i++)
        {
            final String value = Integer.toString(i);
            cache.put(i, value);
        }

        for (final Map.Entry<Integer, String> entry : cache.entrySet())
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
            cache.put(testEntry, String.valueOf(testEntry));
        }

        final String mapAsAString = "{12=12, 3=3, 7=7, 11=11, 19=19, 1=1}";
        assertThat(cache.toString(), equalTo(mapAsAString));
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

        assertNull(cache.get(testKey));

        assertThat(cache.computeIfAbsent(testKey, function), is(testValue));
        assertThat(cache.get(testKey), is(testValue));
    }

    @Test
    public void shouldTestStats()
    {
        assertThat(cache.cachePuts(), is(0L));
        assertThat(cache.cacheMisses(), is(0L));
        assertThat(cache.cacheHits(), is(0L));

        cache.get(7);
        assertThat(cache.cacheMisses(), is(1L));
        assertThat(cache.cacheHits(), is(0L));
        assertThat(cache.cachePuts(), is(0L));

        cache.put(7, "Seven");
        assertThat(cache.cacheMisses(), is(1L));
        assertThat(cache.cacheHits(), is(0L));
        assertThat(cache.cachePuts(), is(1L));

        cache.get(7);
        assertThat(cache.cacheMisses(), is(1L));
        assertThat(cache.cacheHits(), is(1L));
        assertThat(cache.cachePuts(), is(1L));

        cache.resetCounters();
        assertThat(cache.cachePuts(), is(0L));
        assertThat(cache.cacheMisses(), is(0L));
        assertThat(cache.cacheHits(), is(0L));
    }
}
