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
package org.agrona.nio;

import org.junit.jupiter.api.Test;

import java.nio.channels.SelectionKey;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class NioSelectedKeySetTest
{
    final NioSelectedKeySet set = new NioSelectedKeySet();

    @Test
    void shouldAddToEmptySet()
    {
        final SelectionKey keyOne = mock(SelectionKey.class);

        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        assertTrue(set.add(keyOne));

        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
    }

    @Test
    void shouldNotRemoveFromEmptySet()
    {
        final SelectionKey keyOne = mock(SelectionKey.class);

        assertFalse(set.remove(keyOne));
    }

    @Test
    void shouldRemoveFromSingleElementFromSet()
    {
        final SelectionKey keyOne = mock(SelectionKey.class);

        set.add(keyOne);

        assertTrue(set.remove(keyOne));
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void shouldRemoveSecondElementFromSet()
    {
        final SelectionKey keyOne = mock(SelectionKey.class);
        final SelectionKey keyTwo = mock(SelectionKey.class);

        set.add(keyOne);
        set.add(keyTwo);

        assertTrue(set.remove(keyTwo));
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
    }

    @Test
    void shouldRemoveFirstElementFromSet()
    {
        final SelectionKey keyOne = mock(SelectionKey.class);
        final SelectionKey keyTwo = mock(SelectionKey.class);

        set.add(keyOne);
        set.add(keyTwo);

        assertTrue(set.remove(keyOne));
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
    }

    @Test
    void shouldRemoveMiddleElementFromSet()
    {
        final SelectionKey keyOne = mock(SelectionKey.class);
        final SelectionKey keyTwo = mock(SelectionKey.class);
        final SelectionKey keyThree = mock(SelectionKey.class);

        set.add(keyOne);
        set.add(keyTwo);
        set.add(keyThree);

        assertTrue(set.remove(keyTwo));
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
    }
}
