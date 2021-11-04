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
package org.agrona;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class containing a common set of tests for {@link MutableDirectBuffer} implementations.
 */
public abstract class MutableDirectBufferTests
{
    private static final int ROUND_TRIP_ITERATIONS = 10_000_000;

    /**
     * Allocate new buffer with the specified capacity.
     *
     * @param capacity to allocate.
     * @return new buffer.
     */
    protected abstract MutableDirectBuffer newBuffer(int capacity);

    @ParameterizedTest
    @MethodSource("valuesAndLengths")
    void shouldPutNaturalFromEnd(final int[] valueAndLength)
    {
        final MutableDirectBuffer buffer = newBuffer(8 * 1024);
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
    void putIntAsciiRoundTrip()
    {
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final int value = ThreadLocalRandom.current().nextInt();
            final int length = buffer.putIntAscii(0, value);
            final int parsedValue = buffer.parseIntAscii(0, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putLongAsciiRoundTrip()
    {
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final long value = ThreadLocalRandom.current().nextLong();
            final int length = buffer.putLongAscii(0, value);
            final long parsedValue = buffer.parseLongAscii(0, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putNaturalIntAsciiRoundTrip()
    {
        final MutableDirectBuffer buffer = newBuffer(64);
        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final int value = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
            final int length = buffer.putNaturalIntAscii(0, value);
            final int parsedValue = buffer.parseNaturalIntAscii(0, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putNaturalLongAsciiRoundTrip()
    {
        final MutableDirectBuffer buffer = newBuffer(64);
        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final long value = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
            final int length = buffer.putNaturalLongAscii(0, value);
            final long parsedValue = buffer.parseNaturalLongAscii(0, length);
            assertEquals(value, parsedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 11, 64, 1011 })
    void setMemory(final int length)
    {
        final int index = 2;
        final byte value = (byte)11;
        final MutableDirectBuffer buffer = newBuffer(2 * index + length);

        buffer.setMemory(index, length, value);

        assertEquals(0, buffer.getByte(0));
        assertEquals(0, buffer.getByte(1));
        assertEquals(0, buffer.getByte(index + length));
        assertEquals(0, buffer.getByte(index + length + 1));
        for (int i = 0; i < length; i++)
        {
            assertEquals(value, buffer.getByte(index + i));
        }
    }

    @Test
    void putLongAsciiShouldHandleEightDigitNumber()
    {
        final int index = 0;
        final MutableDirectBuffer buffer = newBuffer(16);

        final int length = buffer.putLongAscii(index, 87654321);
        assertEquals(8, length);

        assertEquals("87654321", buffer.getStringWithoutLengthAscii(index, length));
    }

    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, 0, Long.MAX_VALUE })
    void putLongAsciiShouldEncodeBoundaryValues(final long value)
    {
        final String encodedValue = Long.toString(value);
        final int index = 4;
        final MutableDirectBuffer buffer = newBuffer(32);

        final int length = buffer.putLongAscii(index, value);

        assertEquals(encodedValue.length(), length);
        assertEquals(encodedValue, buffer.getStringWithoutLengthAscii(index, length));
        assertEquals(value, buffer.parseLongAscii(index, length));
    }

    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, 0, Integer.MAX_VALUE })
    void putIntAsciiShouldEncodeBoundaryValues(final int value)
    {
        final String encodedValue = Integer.toString(value);
        final int index = 3;
        final MutableDirectBuffer buffer = newBuffer(32);

        final int length = buffer.putIntAscii(index, value);

        assertEquals(encodedValue.length(), length);
        assertEquals(encodedValue, buffer.getStringWithoutLengthAscii(index, length));
        assertEquals(value, buffer.parseIntAscii(index, length));
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
