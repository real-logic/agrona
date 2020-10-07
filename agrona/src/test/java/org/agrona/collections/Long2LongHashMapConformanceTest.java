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

public class Long2LongHashMapConformanceTest
{
    // Generated suite to test conformity to the java.util.Set interface
    public static TestSuite suite()
    {
        return mapTestSuite(new TestMapGenerator<Long, Long>()
        {
            public Long[] createKeyArray(final int length)
            {
                return new Long[length];
            }

            public Long[] createValueArray(final int length)
            {
                return new Long[length];
            }

            public SampleElements<Map.Entry<Long, Long>> samples()
            {
                return new SampleElements<>(
                    Helpers.mapEntry(1L, 123L),
                    Helpers.mapEntry(2L, 234L),
                    Helpers.mapEntry(3L, 345L),
                    Helpers.mapEntry(345L, 6L),
                    Helpers.mapEntry(777L, 666L));
            }

            public Map<Long, Long> create(final Object... entries)
            {
                final Long2LongHashMap map = new Long2LongHashMap(
                    entries.length * 2, Hashing.DEFAULT_LOAD_FACTOR, -1L, false);

                for (final Object o : entries)
                {
                    @SuppressWarnings("unchecked")
                    final Map.Entry<Long, Long> e = (Map.Entry<Long, Long>)o;
                    map.put(e.getKey(), e.getValue());
                }

                return map;
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            public Map.Entry<Long, Long>[] createArray(final int length)
            {
                return new Map.Entry[length];
            }

            public Iterable<Map.Entry<Long, Long>> order(final List<Map.Entry<Long, Long>> insertionOrder)
            {
                return insertionOrder;
            }
        }, Long2LongHashMap.class.getSimpleName());
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
            CollectionSize.ANY,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .named(name)
            .createTestSuite();
    }
}
