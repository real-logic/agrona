/*
 * Copyright 2014-2025 Real Logic Limited.
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

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringNumbersLengthsTest
{
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

    @ParameterizedTest
    @MethodSource("valuesAndLengths")
    void shouldPutNaturalInt(final int[] valueAndLength)
    {
        final int value = valueAndLength[0];

        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        final int length = buffer.putNaturalIntAscii(1, value);
        assertValueAndLengthEquals(valueAndLength, buffer, length);
    }

    @ParameterizedTest
    @MethodSource("valuesAndLengths")
    void shouldPutNaturalLong(final int[] valueAndLength)
    {
        final long value = valueAndLength[0];

        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        final int length = buffer.putNaturalLongAscii(1, value);
        assertValueAndLengthEquals(valueAndLength, buffer, length);
    }

    @ParameterizedTest
    @MethodSource("valuesAndLengths")
    void shouldPutInt(final int[] valueAndLength)
    {
        final int value = valueAndLength[0];

        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        final int length = buffer.putIntAscii(1, value);
        assertValueAndLengthEquals(valueAndLength, buffer, length);
    }

    @ParameterizedTest
    @MethodSource("valuesAndLengths")
    void shouldPutLong(final int[] valueAndLength)
    {
        final int value = valueAndLength[0];

        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[128]);

        final int length = buffer.putLongAscii(1, value);
        assertValueAndLengthEquals(valueAndLength, buffer, length);
    }

    private void assertValueAndLengthEquals(final int[] valueAndLength, final UnsafeBuffer buffer, final int length)
    {
        final int value = valueAndLength[0];
        final int expectedLength = valueAndLength[1];

        final String message = "for " + Arrays.toString(valueAndLength);

        assertEquals(expectedLength, length, message);

        assertEquals(
            String.valueOf(value),
            buffer.getStringWithoutLengthAscii(1, expectedLength),
            message);
    }
}
