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

import java.util.function.LongPredicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Long2LongHashMapTest
{
    @Test
    void removeIfLongOnKeySet()
    {
        final Long2LongHashMap map = new Long2LongHashMap(-1);
        final LongPredicate filter = (v) -> v < 3;
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        map.put(4, 4);

        assertTrue(map.keySet().removeIfLong(filter));

        assertEquals(2, map.size());
        assertEquals(3, map.get(3));
        assertEquals(4, map.get(4));

        assertFalse(map.keySet().removeIfLong(filter));
        assertEquals(2, map.size());
    }

    @Test
    void removeIfLongOnValuesCollection()
    {
        final Long2LongHashMap map = new Long2LongHashMap(-1);
        final LongPredicate filter = (v) -> v >= 20;
        map.put(1, 10);
        map.put(2, 20);
        map.put(3, 30);
        map.put(4, 40);

        assertTrue(map.values().removeIfLong(filter));

        assertEquals(1, map.size());
        assertEquals(10, map.get(1));

        assertFalse(map.values().removeIfLong(filter));
        assertEquals(1, map.size());
    }

    @Test
    void removeIfLongOnEntrySet()
    {
        final Long2LongHashMap map = new Long2LongHashMap(-1);
        final LongLongPredicate filter = (k, v) -> k >= 2 && v <= 30;
        map.put(1, 10);
        map.put(2, 20);
        map.put(3, 30);
        map.put(4, 40);

        assertTrue(map.entrySet().removeIfLong(filter));

        assertEquals(2, map.size());
        assertEquals(10, map.get(1));
        assertEquals(40, map.get(4));

        assertFalse(map.entrySet().removeIfLong(filter));
        assertEquals(2, map.size());
    }
}
