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
package org.agrona;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpandableArrayBufferTest extends MutableDirectBufferTests
{
    protected MutableDirectBuffer newBuffer(final int capacity)
    {
        return new ExpandableArrayBuffer(capacity);
    }

    @ParameterizedTest
    @ValueSource(ints = { -123, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 77777 })
    void putIntAsciiShouldExpandCapacity(final int value)
    {
        final int initialCapacity = 2;
        final int index = 5;
        final MutableDirectBuffer buffer = newBuffer(initialCapacity);

        final int length = buffer.putIntAscii(index, value);
        assertEquals(value, buffer.parseIntAscii(index, length));
    }

    @ParameterizedTest
    @ValueSource(ints = { Integer.MAX_VALUE, 0, 77777, 1 })
    void putNaturalIntAsciiShouldExpandCapacity(final int value)
    {
        final int initialCapacity = 2;
        final int index = initialCapacity;
        final MutableDirectBuffer buffer = newBuffer(initialCapacity);

        final int length = buffer.putNaturalIntAscii(index, value);
        assertEquals(value, buffer.parseIntAscii(index, length));
    }

    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE + 1, Long.MIN_VALUE, Long.MAX_VALUE, 0, 1754837658435L })
    void putLongAsciiShouldExpandCapacity(final long value)
    {
        final int initialCapacity = 12;
        final int index = 50;
        final MutableDirectBuffer buffer = newBuffer(initialCapacity);

        final int length = buffer.putLongAscii(index, value);
        assertEquals(value, buffer.parseLongAscii(index, length));
    }

    @ParameterizedTest
    @ValueSource(longs = { Long.MAX_VALUE, 0, 1754837658435L, Long.MAX_VALUE - 111 })
    void putNaturalLongAsciiShouldExpandCapacity(final long value)
    {
        final int initialCapacity = 2;
        final int index = initialCapacity * 2;
        final MutableDirectBuffer buffer = newBuffer(initialCapacity);

        final int length = buffer.putNaturalLongAscii(index, value);
        assertEquals(value, buffer.parseNaturalLongAscii(index, length));
    }
}
