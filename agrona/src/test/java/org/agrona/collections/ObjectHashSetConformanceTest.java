/*
 * Copyright 2014-2020 Real Logic Ltd.
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

import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import junit.framework.TestSuite;

import java.util.List;
import java.util.Set;

public class ObjectHashSetConformanceTest
{
    // Generated suite to test conformity to the java.util.Set interface
    public static TestSuite suite()
    {
        return SetTestSuiteBuilder.using(new Generator())
            .named("ObjectHashSet Tests")
            .withFeatures(CollectionSize.ANY,
                CollectionFeature.NON_STANDARD_TOSTRING,
                CollectionFeature.SUPPORTS_ADD,
                CollectionFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.REMOVE_OPERATIONS)
            .createTestSuite();
    }

    private static class Generator implements TestSetGenerator<String>
    {
        public Set<String> create(final Object... elements)
        {
            final ObjectHashSet<String> set = new ObjectHashSet<>(
                elements.length * 2, Hashing.DEFAULT_LOAD_FACTOR, false);

            for (final Object o : elements)
            {
                set.add((String)o);
            }

            return set;
        }

        public SampleElements<String> samples()
        {
            return new SampleElements<>("Elani", "von", "der", "Schavener", "Heide");
        }

        public String[] createArray(final int length)
        {
            return new String[length];
        }

        public Iterable<String> order(final List<String> insertionOrder)
        {
            return insertionOrder;
        }
    }
}