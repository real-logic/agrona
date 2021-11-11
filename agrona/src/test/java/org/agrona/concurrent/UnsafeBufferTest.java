/*
 * Copyright 2014-2021 Real Logic Limited.
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
package org.agrona.concurrent;

import org.agrona.MutableDirectBuffer;
import org.agrona.MutableDirectBufferTests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class UnsafeBufferTest extends MutableDirectBufferTests
{
    private static final byte VALUE = 42;
    private static final int ADJUSTMENT_OFFSET = 3;

    private final byte[] wibbleBytes = "Wibble".getBytes(US_ASCII);
    private final byte[] wobbleBytes = "Wobble".getBytes(US_ASCII);
    private final byte[] wibbleBytes2 = "Wibble2".getBytes(US_ASCII);

    protected MutableDirectBuffer newBuffer(final int capacity)
    {
        return new UnsafeBuffer(new byte[capacity]);
    }

    @Test
    void shouldEqualOnInstance()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);

        assertThat(wibbleBuffer, is(wibbleBuffer));
    }

    @Test
    void shouldEqualOnContent()
    {
        final UnsafeBuffer wibbleBufferOne = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBufferTwo = new UnsafeBuffer(wibbleBytes.clone());

        assertThat(wibbleBufferOne, is(wibbleBufferTwo));
    }

    @Test
    void shouldNotEqual()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wobbleBuffer = new UnsafeBuffer(wobbleBytes);

        assertThat(wibbleBuffer, is(not(wobbleBuffer)));
    }

    @Test
    void shouldEqualOnHashCode()
    {
        final UnsafeBuffer wibbleBufferOne = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBufferTwo = new UnsafeBuffer(wibbleBytes.clone());

        assertThat(wibbleBufferOne.hashCode(), is(wibbleBufferTwo.hashCode()));
    }

    @Test
    void shouldEqualOnCompareContents()
    {
        final UnsafeBuffer wibbleBufferOne = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBufferTwo = new UnsafeBuffer(wibbleBytes.clone());

        assertThat(wibbleBufferOne.compareTo(wibbleBufferTwo), is(0));
    }

    @Test
    void shouldCompareLessThanOnContents()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wobbleBuffer = new UnsafeBuffer(wobbleBytes);

        assertThat(wibbleBuffer.compareTo(wobbleBuffer), is(lessThan(0)));
    }

    @Test
    void shouldCompareGreaterThanOnContents()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wobbleBuffer = new UnsafeBuffer(wobbleBytes);

        assertThat(wobbleBuffer.compareTo(wibbleBuffer), is(greaterThan(0)));
    }

    @Test
    void shouldCompareLessThanOnContentsOfDifferingCapacity()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBuffer2 = new UnsafeBuffer(wibbleBytes2);

        assertThat(wibbleBuffer.compareTo(wibbleBuffer2), is(lessThan(0)));
    }

    @Test
    void shouldExposePositionAtWhichByteArrayGetsWrapped()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(
            wibbleBytes, ADJUSTMENT_OFFSET, wibbleBytes.length - ADJUSTMENT_OFFSET);

        wibbleBuffer.putByte(0, VALUE);

        assertEquals(VALUE, wibbleBytes[wibbleBuffer.wrapAdjustment()]);
    }

    @Test
    void shouldExposePositionAtWhichHeapByteBufferGetsWrapped()
    {
        final ByteBuffer wibbleByteBuffer = ByteBuffer.wrap(wibbleBytes);
        shouldExposePositionAtWhichByteBufferGetsWrapped(wibbleByteBuffer);
    }

    @Test
    void shouldExposePositionAtWhichDirectByteBufferGetsWrapped()
    {
        final ByteBuffer wibbleByteBuffer = ByteBuffer.allocateDirect(wibbleBytes.length);
        shouldExposePositionAtWhichByteBufferGetsWrapped(wibbleByteBuffer);
    }

    @Test
    void shouldWrapValidRange()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[8]);
        final UnsafeBuffer slice = new UnsafeBuffer();

        slice.wrap(buffer);
        slice.wrap(buffer, 0, 8);
        slice.wrap(buffer, 1, 7);
        slice.wrap(buffer, 2, 6);
        slice.wrap(buffer, 3, 5);
        slice.wrap(buffer, 4, 4);
        slice.wrap(buffer, 5, 3);
        slice.wrap(buffer, 6, 2);
        slice.wrap(buffer, 7, 1);
        slice.wrap(buffer, 8, 0);
    }

    @Test
    void shouldNotWrapInValidRange()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[8]);
        final UnsafeBuffer slice = new UnsafeBuffer();

        assertThrows(IllegalArgumentException.class, () -> slice.wrap(buffer, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> slice.wrap(buffer, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> slice.wrap(buffer, 8, 1));
        assertThrows(IllegalArgumentException.class, () -> slice.wrap(buffer, 7, 3));
    }

    @ParameterizedTest
    @ValueSource(ints = { 123, -25 })
    void putIntAsciiShouldBoundsCheckBeforeWritingAnyData(final int value)
    {
        assertTrue(UnsafeBuffer.SHOULD_BOUNDS_CHECK, "bounds check disabled!");

        final int index = 6;
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[8]);
        assertEquals(0, buffer.getByte(index));

        final IndexOutOfBoundsException exception =
            assertThrowsExactly(IndexOutOfBoundsException.class, () -> buffer.putIntAscii(index, value));
        assertEquals("index=6 length=3 capacity=8", exception.getMessage());

        assertEquals(0, buffer.getByte(index));
    }

    @ParameterizedTest
    @ValueSource(longs = { -251463777, 1234567890 })
    void putLongAsciiShouldBoundsCheckBeforeWritingAnyData(final long value)
    {
        assertTrue(UnsafeBuffer.SHOULD_BOUNDS_CHECK, "bounds check disabled!");

        final int index = 1;
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[10]);
        assertEquals(0, buffer.getByte(index));

        final IndexOutOfBoundsException exception =
            assertThrowsExactly(IndexOutOfBoundsException.class, () -> buffer.putLongAscii(index, value));
        assertEquals("index=1 length=10 capacity=10", exception.getMessage());

        assertEquals(0, buffer.getByte(index));
    }

    @ParameterizedTest
    @MethodSource("byteOrders")
    void parseIntAsciiIsNotDependentOnTheBufferEndianness(final ByteOrder byteOrder)
    {
        final int index = 13;
        final int value = 1690021;
        final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(32).order(byteOrder));
        final int length = buffer.putIntAscii(index, value);

        assertEquals(value, buffer.parseIntAscii(index, length));
    }

    @ParameterizedTest
    @MethodSource("byteOrders")
    void parseLongAsciiIsNotDependentOnTheBufferEndianness(final ByteOrder byteOrder)
    {
        final int index = 4;
        final long value = 16900210032L;
        final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(64).order(byteOrder));
        final int length = buffer.putLongAscii(index, value);

        assertEquals(value, buffer.parseLongAscii(index, length));
    }

    private static void shouldExposePositionAtWhichByteBufferGetsWrapped(final ByteBuffer byteBuffer)
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(
            byteBuffer, ADJUSTMENT_OFFSET, byteBuffer.capacity() - ADJUSTMENT_OFFSET);

        wibbleBuffer.putByte(0, VALUE);

        assertEquals(VALUE, byteBuffer.get(wibbleBuffer.wrapAdjustment()));
    }

    private static List<ByteOrder> byteOrders()
    {
        return Arrays.asList(ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN);
    }
}
