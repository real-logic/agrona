/*
 * Copyright 2014-2023 Real Logic Limited.
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

import java.util.Iterator;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.*;

class Object2IntHashMapNotAvoidingAllocationTest extends Object2IntHashMapTest
{
    <T> Object2IntHashMap<T> newMap(final float loadFactor, final int initialCapacity)
    {
        return new Object2IntHashMap<>(initialCapacity, loadFactor, MISSING_VALUE, false);
    }

    @Test
    void valuesIteratorIsNotCached()
    {
        assertNotSame(objectToIntMap.values().iterator(), objectToIntMap.values().iterator());
    }

    @Test
    void keysIteratorIsNotCached()
    {
        assertNotSame(objectToIntMap.keySet().iterator(), objectToIntMap.keySet().iterator());
    }

    @Test
    void entryIteratorIsNotCached()
    {
        assertNotSame(objectToIntMap.entrySet().iterator(), objectToIntMap.entrySet().iterator());
    }

    @Test
    void entriesAreAllocatedByEntriesIterator()
    {
        objectToIntMap.put("1", 1);
        objectToIntMap.put("2", 2);

        final Iterator<Entry<String, Integer>> entryIterator = objectToIntMap.entrySet().iterator();
        final Entry<String, Integer> entry1 = entryIterator.next();
        final Entry<String, Integer> entry2 = entryIterator.next();

        assertNotEquals(entry1, entry2);
    }
}
