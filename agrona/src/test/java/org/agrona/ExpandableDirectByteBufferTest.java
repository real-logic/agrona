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
package org.agrona;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;

import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.agrona.ExpandableDirectByteBuffer.MAX_BUFFER_LENGTH;
import static org.junit.jupiter.api.Assertions.*;

class ExpandableDirectByteBufferTest extends MutableDirectBufferTests
{
    protected MutableDirectBuffer newBuffer(final int capacity)
    {
        return new ExpandableDirectByteBuffer(capacity);
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

    @Test
    void ensureCapacityThrowsIndexOutOfBoundsExceptionIfIndexIsNegative()
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(1);
        final IndexOutOfBoundsException exception =
            assertThrowsExactly(IndexOutOfBoundsException.class, () -> buffer.ensureCapacity(-3, 4));
        assertEquals("negative value: index=-3 length=4", exception.getMessage());
    }

    @Test
    void ensureCapacityThrowsIndexOutOfBoundsExceptionIfLengthIsNegative()
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(1);
        final IndexOutOfBoundsException exception =
            assertThrowsExactly(IndexOutOfBoundsException.class, () -> buffer.ensureCapacity(8, -100));
        assertEquals("negative value: index=8 length=-100", exception.getMessage());
    }

    @Test
    void ensureCapacityIsANoOpIfExistingCapacityIsEnough()
    {
        final int index = 1;
        final int capacity = 5;
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(capacity);

        buffer.ensureCapacity(index, capacity - index);

        assertEquals(capacity, buffer.capacity());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 6, 7",
        "99, 38, 163",
    })
    void ensureCapacityCreatesANewDirectByteBufferWithBiggerCapacity(
        final int index, final int length, final int expectedCapacity)
    {
        final int initialCapacity = 5;
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(initialCapacity);
        assertEquals(initialCapacity, buffer.capacity());
        final ByteBuffer byteBuffer = buffer.byteBuffer();

        buffer.ensureCapacity(index, length);

        assertEquals(expectedCapacity, buffer.capacity());
        assertNotSame(byteBuffer, buffer.byteBuffer());
    }

    @Test
    void wrapAdjustmentIsAlwaysZero()
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(1);
        final long originalAddress = BufferUtil.address(buffer.byteBuffer());
        assertEquals(originalAddress, buffer.addressOffset());
        assertEquals(buffer.addressOffset() - originalAddress, buffer.wrapAdjustment());
        assertEquals(1, buffer.capacity());
        assertEquals(0, buffer.wrapAdjustment());

        buffer.ensureCapacity(5, 12);

        final long newOriginalAddress = BufferUtil.address(buffer.byteBuffer());
        assertNotEquals(originalAddress, newOriginalAddress);
        assertEquals(buffer.addressOffset() - newOriginalAddress, buffer.wrapAdjustment());
        assertEquals(19, buffer.capacity());
        assertEquals(0, buffer.wrapAdjustment());
    }

    @Test
    void dataIsCopiedAfterTheCapacityAdjustment()
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(16);
        buffer.putLong(0, Long.MAX_VALUE);
        buffer.putLong(SIZE_OF_LONG, Long.MIN_VALUE);
        assertEquals(2 * SIZE_OF_LONG, buffer.capacity());

        buffer.setMemory(2 * SIZE_OF_LONG, 100, (byte)'x');
        assertEquals(121, buffer.capacity());
        assertEquals(Long.MAX_VALUE, buffer.getLong(0));
        assertEquals(Long.MIN_VALUE, buffer.getLong(SIZE_OF_LONG));
    }

    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -1234556, -1, Integer.MAX_VALUE, Integer.MAX_VALUE - 7 })
    void checkLimitInvalidValue(final int limit)
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(16);
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.checkLimit(limit));
    }

    @ParameterizedTest
    @ValueSource(ints = { 10, 200 })
    void checkLimitIncreasesCapacityWhenLimitExceedsCurrentCapacity(final int limit)
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(1);
        assertTrue(limit > buffer.capacity());
        final ByteBuffer originalBuffer = buffer.byteBuffer();

        buffer.checkLimit(limit);

        assertTrue(limit < buffer.capacity());
        assertNotSame(originalBuffer, buffer.byteBuffer());
    }

    @Test
    void maxBufferLength()
    {
        assertEquals(Integer.MAX_VALUE - 8, MAX_BUFFER_LENGTH);
    }
}
