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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UnsafeBufferTest
{
    private static final int ROUND_TRIP_ITERATIONS = 25_000_000;
    private static final byte VALUE = 42;
    private static final int INDEX = 1;
    private static final int ADJUSTMENT_OFFSET = 3;

    private final byte[] wibbleBytes = "Wibble".getBytes(US_ASCII);
    private final byte[] wobbleBytes = "Wobble".getBytes(US_ASCII);
    private final byte[] wibbleBytes2 = "Wibble2".getBytes(US_ASCII);

    @Test
    public void shouldEqualOnInstance()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);

        assertThat(wibbleBuffer, is(wibbleBuffer));
    }

    @Test
    public void shouldEqualOnContent()
    {
        final UnsafeBuffer wibbleBufferOne = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBufferTwo = new UnsafeBuffer(wibbleBytes.clone());

        assertThat(wibbleBufferOne, is(wibbleBufferTwo));
    }

    @Test
    public void shouldNotEqual()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wobbleBuffer = new UnsafeBuffer(wobbleBytes);

        assertThat(wibbleBuffer, is(not(wobbleBuffer)));
    }

    @Test
    public void shouldEqualOnHashCode()
    {
        final UnsafeBuffer wibbleBufferOne = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBufferTwo = new UnsafeBuffer(wibbleBytes.clone());

        assertThat(wibbleBufferOne.hashCode(), is(wibbleBufferTwo.hashCode()));
    }

    @Test
    public void shouldEqualOnCompareContents()
    {
        final UnsafeBuffer wibbleBufferOne = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBufferTwo = new UnsafeBuffer(wibbleBytes.clone());

        assertThat(wibbleBufferOne.compareTo(wibbleBufferTwo), is(0));
    }

    @Test
    public void shouldCompareLessThanOnContents()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wobbleBuffer = new UnsafeBuffer(wobbleBytes);

        assertThat(wibbleBuffer.compareTo(wobbleBuffer), is(lessThan(0)));
    }

    @Test
    public void shouldCompareGreaterThanOnContents()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wobbleBuffer = new UnsafeBuffer(wobbleBytes);

        assertThat(wobbleBuffer.compareTo(wibbleBuffer), is(greaterThan(0)));
    }

    @Test
    public void shouldCompareLessThanOnContentsOfDifferingCapacity()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBuffer2 = new UnsafeBuffer(wibbleBytes2);

        assertThat(wibbleBuffer.compareTo(wibbleBuffer2), is(lessThan(0)));
    }

    @Test
    public void shouldExposePositionAtWhichByteArrayGetsWrapped()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(
            wibbleBytes, ADJUSTMENT_OFFSET, wibbleBytes.length - ADJUSTMENT_OFFSET);

        wibbleBuffer.putByte(0, VALUE);

        assertEquals(VALUE, wibbleBytes[wibbleBuffer.wrapAdjustment()]);
    }

    @Test
    public void shouldExposePositionAtWhichHeapByteBufferGetsWrapped()
    {
        final ByteBuffer wibbleByteBuffer = ByteBuffer.wrap(wibbleBytes);
        shouldExposePositionAtWhichByteBufferGetsWrapped(wibbleByteBuffer);
    }

    @Test
    public void shouldExposePositionAtWhichDirectByteBufferGetsWrapped()
    {
        final ByteBuffer wibbleByteBuffer = ByteBuffer.allocateDirect(wibbleBytes.length);
        shouldExposePositionAtWhichByteBufferGetsWrapped(wibbleByteBuffer);
    }

    private void shouldExposePositionAtWhichByteBufferGetsWrapped(final ByteBuffer byteBuffer)
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(
            byteBuffer, ADJUSTMENT_OFFSET, byteBuffer.capacity() - ADJUSTMENT_OFFSET);

        wibbleBuffer.putByte(0, VALUE);

        assertEquals(VALUE, byteBuffer.get(wibbleBuffer.wrapAdjustment()));
    }

    @Test
    public void shouldGetIntegerValuesAtSpecifiedOffset()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);
        putAscii(buffer, "123");

        final int value = buffer.parseNaturalIntAscii(INDEX, 3);

        assertEquals(123, value);
    }

    @Test
    public void shouldDecodeNegativeIntegers()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        putAscii(buffer, "-1");

        final int value = buffer.parseIntAscii(INDEX, 2);

        assertEquals(-1, value);
    }

    @Test
    public void shouldWriteIntZero()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        final int length = buffer.putIntAscii(INDEX, 0);

        assertEquals(1, length);
        assertContainsString(buffer, "0", 1);
    }

    @Test
    public void shouldWritePositiveIntValues()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        final int length = buffer.putIntAscii(INDEX, 123);

        assertEquals(3, length);
        assertContainsString(buffer, "123", 3);
    }

    @Test
    public void shouldWriteNegativeIntValues()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        final int length = buffer.putIntAscii(INDEX, -123);

        assertEquals(4, length);
        assertContainsString(buffer, "-123", 4);
    }

    @Test
    public void shouldWriteMaxIntValue()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        final int length = buffer.putIntAscii(INDEX, Integer.MAX_VALUE);

        assertContainsString(buffer, String.valueOf(Integer.MAX_VALUE), length);
    }

    @Test
    public void shouldWriteMinIntValue()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        final int length = buffer.putIntAscii(INDEX, Integer.MIN_VALUE);

        assertContainsString(buffer, String.valueOf(Integer.MIN_VALUE), length);
    }

    @ParameterizedTest
    @MethodSource("valuesAndLengths")
    public void shouldPutNaturalFromEnd(final int[] valueAndLength)
    {
        final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[8 * 1024]);
        final int value = valueAndLength[0];
        final int length = valueAndLength[1];

        final int start = buffer.putNaturalIntAsciiFromEnd(value, length);
        final String message = "for " + Arrays.toString(valueAndLength);
        assertEquals(0, start, message);

        assertEquals(
            String.valueOf(value),
            buffer.getStringWithoutLengthAscii(0, length),
            message);
    }

    @Test
    public void shouldWrapValidRange()
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
    public void shouldNotWrapInValidRange()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[8]);
        final UnsafeBuffer slice = new UnsafeBuffer();

        assertThrows(IllegalArgumentException.class, () -> slice.wrap(buffer, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> slice.wrap(buffer, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> slice.wrap(buffer, 8, 1));
        assertThrows(IllegalArgumentException.class, () -> slice.wrap(buffer, 7, 3));
    }

    @Test
    void putIntAsciiRoundTrip()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
        final long seed = ThreadLocalRandom.current().nextLong();
        final Random random = new Random(seed);
        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final int value = random.nextInt();
            final int length = buffer.putIntAscii(0, value);
            final int parsedValue = buffer.parseIntAscii(0, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putLongAsciiRoundTrip()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
        final long seed = ThreadLocalRandom.current().nextLong();
        final Random random = new Random(seed);
        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final long value = random.nextLong();
            final int length = buffer.putLongAscii(0, value);
            final long parsedValue = buffer.parseLongAscii(0, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putNaturalIntAsciiRoundTrip()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
        final long seed = ThreadLocalRandom.current().nextLong();
        final Random random = new Random(seed);
        random.ints(ROUND_TRIP_ITERATIONS, 0, Integer.MAX_VALUE).forEach(
            value ->
            {
                final int length = buffer.putNaturalIntAscii(0, value);
                final int parsedValue = buffer.parseNaturalIntAscii(0, length);
                assertEquals(value, parsedValue);
            });
    }

    @Test
    void putNaturalLongAsciiRoundTrip()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
        final long seed = ThreadLocalRandom.current().nextLong();
        final Random random = new Random(seed);
        random.longs(ROUND_TRIP_ITERATIONS, 0, Long.MAX_VALUE).forEach(
            value ->
            {
                final int length = buffer.putNaturalLongAscii(0, value);
                final long parsedValue = buffer.parseNaturalLongAscii(0, length);
                assertEquals(value, parsedValue);
            }
        );
    }

    @Test
    void foo()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[64]);
        buffer.putLongAscii(0, 123456789);
    }

    private void assertContainsString(final UnsafeBuffer buffer, final String value, final int length)
    {
        assertEquals(value, buffer.getStringWithoutLengthAscii(INDEX, length));
    }

    private void putAscii(final UnsafeBuffer buffer, final String value)
    {
        buffer.putBytes(INDEX, value.getBytes(US_ASCII));
    }

    private static int[][] valuesAndLengths()
    {
        return new int[][]
            {
                { 1, 1 },
                { 10, 2 },
                { 100, 3 },
                { 1000, 4 },
                { 12, 2 },
                { 123, 3 },
                { 2345, 4 },
                { 9, 1 },
                { 99, 2 },
                { 999, 3 },
                { 9999, 4 },
            };
    }
}
