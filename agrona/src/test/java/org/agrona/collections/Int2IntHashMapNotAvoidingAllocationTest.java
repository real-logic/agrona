/*
 * Copyright 2014-2018 Real Logic Ltd.
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
package org.agrona.collections;

import org.junit.Test;

import java.util.Iterator;
import java.util.Map.Entry;

import static org.agrona.collections.Int2IntHashMap.MIN_CAPACITY;
import static org.junit.Assert.*;

public class Int2IntHashMapNotAvoidingAllocationTest extends Int2IntHashMapTest
{
    public Int2IntHashMapNotAvoidingAllocationTest()
    {
        super(new Int2IntHashMap(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, MISSING_VALUE, false));
    }

    @Test
    public void valuesIteratorIsNotCached()
    {
        assertNotSame(map.values().iterator(), map.values().iterator());
    }

    @Test
    public void keysIteratorIsNotCached()
    {
        assertNotSame(map.keySet().iterator(), map.keySet().iterator());
    }

    @Test
    public void entryIteratorIsNotCached()
    {
        assertNotSame(map.entrySet().iterator(), map.entrySet().iterator());
    }

    @Test
    public void entriesAreAllocatedByEntriesIterator()
    {
        map.put(1, 1);
        map.put(2, 2);

        final Iterator<Entry<Integer, Integer>> entryIterator = map.entrySet().iterator();
        final Entry<Integer, Integer> entry1 = entryIterator.next();
        final Entry<Integer, Integer> entry2 = entryIterator.next();

        assertNotEquals(entry1, entry2);
    }
}
