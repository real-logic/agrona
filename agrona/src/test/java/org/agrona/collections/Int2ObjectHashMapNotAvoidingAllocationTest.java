/*
 * Copyright 2014-2024 Real Logic Limited.
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

class Int2ObjectHashMapNotAvoidingAllocationTest extends Int2ObjectHashMapTest
{
    Int2ObjectHashMap<String> newMap(final float loadFactor, final int initialCapacity)
    {
        return new Int2ObjectHashMap<>(initialCapacity, loadFactor, false);
    }

    @Test
    void valuesIteratorIsNotCached()
    {
        assertNotSame(intToObjectMap.values().iterator(), intToObjectMap.values().iterator());
    }

    @Test
    void keysIteratorIsNotCached()
    {
        assertNotSame(intToObjectMap.keySet().iterator(), intToObjectMap.keySet().iterator());
    }

    @Test
    void entryIteratorIsNotCached()
    {
        assertNotSame(intToObjectMap.entrySet().iterator(), intToObjectMap.entrySet().iterator());
    }

    @Test
    void entriesAreAllocatedByEntriesIterator()
    {
        intToObjectMap.put(1, "1");
        intToObjectMap.put(2, "2");

        final Iterator<Entry<Integer, String>> entryIterator = intToObjectMap.entrySet().iterator();
        final Entry<Integer, String> entry1 = entryIterator.next();
        final Entry<Integer, String> entry2 = entryIterator.next();

        assertNotEquals(entry1, entry2);
    }
}
