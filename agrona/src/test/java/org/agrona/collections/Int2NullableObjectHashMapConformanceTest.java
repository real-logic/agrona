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

import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import junit.framework.TestSuite;

import java.util.List;
import java.util.Map;

public class Int2NullableObjectHashMapConformanceTest
{
    // Generated suite to test conformity to the java.util.Set interface
    public static TestSuite suite()
    {
        return mapTestSuite(new TestMapGenerator<Integer, Integer>()
        {
            public Integer[] createKeyArray(final int length)
            {
                return new Integer[length];
            }

            public Integer[] createValueArray(final int length)
            {
                return new Integer[length];
            }

            public SampleElements<Map.Entry<Integer, Integer>> samples()
            {
                return new SampleElements<>(
                    Helpers.mapEntry(1, 123),
                    Helpers.mapEntry(2, 234),
                    Helpers.mapEntry(3, 345),
                    Helpers.mapEntry(345, 6),
                    Helpers.mapEntry(777, 666));
            }

            public Map<Integer, Integer> create(final Object... entries)
            {
                final Int2NullableObjectHashMap<Integer> map = new Int2NullableObjectHashMap<>(
                    entries.length * 2, Hashing.DEFAULT_LOAD_FACTOR, false);

                for (final Object o : entries)
                {
                    @SuppressWarnings("unchecked")
                    final Map.Entry<Integer, Integer> e = (Map.Entry<Integer, Integer>)o;
                    map.put(e.getKey(), e.getValue());
                }

                return map;
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            public Map.Entry<Integer, Integer>[] createArray(final int length)
            {
                return new Map.Entry[length];
            }

            public Iterable<Map.Entry<Integer, Integer>> order(final List<Map.Entry<Integer, Integer>> insertionOrder)
            {
                return insertionOrder;
            }
        }, Int2NullableObjectHashMap.class.getSimpleName());
    }

    private static <T> TestSuite mapTestSuite(final TestMapGenerator<T, T> testMapGenerator, final String name)
    {
        return new MapTestSuiteBuilder<T, T>()
        {
            {
                usingGenerator(testMapGenerator);
            }
        }.withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_NULL_VALUE_QUERIES,
            MapFeature.ALLOWS_NULL_ENTRY_QUERIES,
            MapFeature.RESTRICTS_KEYS,
            MapFeature.RESTRICTS_VALUES,
            CollectionSize.ANY,
            CollectionFeature.REMOVE_OPERATIONS)
            .named(name)
            .createTestSuite();
    }
}
