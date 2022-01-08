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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ArrayUtilTest
{
    // Reference Equality
    private static final Integer ONE = 1;
    private static final Integer TWO = 2;
    private static final Integer THREE = 3;

    private final Integer[] values = { ONE, TWO };

    @Test
    public void shouldNotRemoveMissingElement()
    {
        final Integer[] result = ArrayUtil.remove(values, THREE);

        assertArrayEquals(values, result);
    }

    @Test
    public void shouldRemovePresentElementAtEnd()
    {
        final Integer[] result = ArrayUtil.remove(values, TWO);

        assertArrayEquals(new Integer[]{ ONE }, result);
    }

    @Test
    public void shouldRemovePresentElementAtStart()
    {
        final Integer[] result = ArrayUtil.remove(values, ONE);

        assertArrayEquals(new Integer[]{ TWO }, result);
    }

    @Test
    public void shouldRemoveByIndex()
    {
        final Integer[] result = ArrayUtil.remove(values, 0);

        assertArrayEquals(new Integer[]{ TWO }, result);
    }
}
