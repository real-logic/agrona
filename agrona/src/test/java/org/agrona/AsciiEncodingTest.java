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

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.agrona.AsciiEncoding.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.*;

class AsciiEncodingTest
{
    private static final int ITERATIONS = 1_000_000;

    @Test
    void shouldParseInt()
    {
        assertEquals(0, parseIntAscii("0", 0, 1));
        assertEquals(0, parseIntAscii("-0", 0, 2));
        assertEquals(7, parseIntAscii("7", 0, 1));
        assertEquals(-7, parseIntAscii("-7", 0, 2));
        assertEquals(33, parseIntAscii("3333", 1, 2));
        assertEquals(-123456789, parseIntAscii("-123456789", 0, 10));

        final String maxValueMinusOne = String.valueOf(Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE - 1, parseIntAscii(maxValueMinusOne, 0, maxValueMinusOne.length()));

        final String maxValue = String.valueOf(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, parseIntAscii(maxValue, 0, maxValue.length()));

        final String minValuePlusOne = String.valueOf(Integer.MIN_VALUE + 1);
        assertEquals(Integer.MIN_VALUE + 1, parseIntAscii(minValuePlusOne, 0, minValuePlusOne.length()));

        final String minValue = String.valueOf(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, parseIntAscii(minValue, 0, minValue.length()));
    }

    @Test
    void shouldParseLong()
    {
        assertEquals(0L, parseLongAscii("0", 0, 1));
        assertEquals(0L, parseLongAscii("-0", 0, 2));
        assertEquals(7L, parseLongAscii("7", 0, 1));
        assertEquals(-7L, parseLongAscii("-7", 0, 2));
        assertEquals(33L, parseLongAscii("3333", 1, 2));
        assertEquals(-123456789876543210L, parseLongAscii("-123456789876543210", 0, 19));

        final String maxValueMinusOne = String.valueOf(Long.MAX_VALUE - 1);
        assertEquals(Long.MAX_VALUE - 1, parseLongAscii(maxValueMinusOne, 0, maxValueMinusOne.length()));

        final String maxValue = String.valueOf(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, parseLongAscii(maxValue, 0, maxValue.length()));

        final String minValuePlusOne = String.valueOf(Long.MIN_VALUE + 1);
        assertEquals(Long.MIN_VALUE + 1, parseLongAscii(minValuePlusOne, 0, minValuePlusOne.length()));

        final String minValue = String.valueOf(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, parseLongAscii(minValue, 0, minValue.length()));
    }

    @Test
    void shouldThrowExceptionWhenParsingIntegerWhichCanOverFlow()
    {
        final String maxValuePlusOneDigit = Integer.MAX_VALUE + "1";
        assertThrows(AsciiNumberFormatException.class,
            () -> parseIntAscii(maxValuePlusOneDigit, 0, maxValuePlusOneDigit.length()),
            maxValuePlusOneDigit);

        final String maxValuePlusOne = "2147483648";
        assertThrows(AsciiNumberFormatException.class,
            () -> parseIntAscii(maxValuePlusOne, 0, maxValuePlusOne.length()),
            maxValuePlusOne);

        final String minValuePlusOneDigit = Integer.MIN_VALUE + "1";
        assertThrows(AsciiNumberFormatException.class,
            () -> parseIntAscii(minValuePlusOneDigit, 0, minValuePlusOneDigit.length()),
            minValuePlusOneDigit);

        final String minValueMinusOne = "-2147483649";
        assertThrows(AsciiNumberFormatException.class,
            () -> parseIntAscii(minValueMinusOne, 0, minValueMinusOne.length()),
            minValueMinusOne);
    }

    @Test
    void shouldThrowExceptionWhenParsingLongWhichCanOverFlow()
    {
        final String maxValuePlusOneDigit = Long.MAX_VALUE + "1";
        assertThrows(AsciiNumberFormatException.class,
            () -> parseLongAscii(maxValuePlusOneDigit, 0, maxValuePlusOneDigit.length()),
            maxValuePlusOneDigit);

        final String maxValuePlusOne = "9223372036854775808";
        assertThrows(AsciiNumberFormatException.class,
            () -> parseLongAscii(maxValuePlusOne, 0, maxValuePlusOne.length()),
            maxValuePlusOne);

        final String minValuePlusOneDigit = Long.MIN_VALUE + "1";
        assertThrows(AsciiNumberFormatException.class,
            () -> parseLongAscii(minValuePlusOneDigit, 0, minValuePlusOneDigit.length()),
            minValuePlusOneDigit);

        final String minValueMinusOne = "-9223372036854775809";
        assertThrows(AsciiNumberFormatException.class,
            () -> parseLongAscii(minValueMinusOne, 0, minValueMinusOne.length()),
            minValueMinusOne);
    }

    @Test
    void shouldThrowExceptionWhenDecodingCharNonNumericValue()
    {
        assertThrows(AsciiNumberFormatException.class, () -> AsciiEncoding.getDigit(0, 'a'));
    }

    @Test
    void shouldThrowExceptionWhenDecodingByteNonNumericValue()
    {
        assertThrows(AsciiNumberFormatException.class, () -> AsciiEncoding.getDigit(0, (byte)'a'));
    }

    @Test
    void shouldThrowExceptionWhenParsingIntegerContainingLoneMinusSign()
    {
        assertThrows(AsciiNumberFormatException.class, () -> parseIntAscii("-", 0, 1));
    }

    @Test
    void shouldThrowExceptionWhenParsingLongContainingLoneMinusSign()
    {
        assertThrows(AsciiNumberFormatException.class, () -> parseLongAscii("-", 0, 1));
    }

    @Test
    void shouldThrowExceptionWhenParsingLongContainingLonePlusSign()
    {
        assertThrows(AsciiNumberFormatException.class, () -> parseLongAscii("+", 0, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = { -3, 0 })
    void shouldThrowExceptionWhenParsingEmptyInteger(final int length)
    {
        assertThrows(AsciiNumberFormatException.class, () -> parseIntAscii("123", 0, length));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0 })
    void shouldThrowExceptionWhenParsingEmptyLong(final int length)
    {
        assertThrows(AsciiNumberFormatException.class, () -> parseLongAscii("900000", 0, length));
    }

    @Test
    void digitCountIntValue()
    {
        final int[] values =
        {
            9, 99, 999, 9_999, 99_999, 999_999, 9_999_999, 99_999_999, 999_999_999, Integer.MAX_VALUE
        };

        for (int i = 0; i < values.length; i++)
        {
            assertEquals(i + 1, digitCount(values[i]));
        }
    }

    @Test
    void digitCountLongValue()
    {
        final long[] values =
        {
            9, 99, 999, 9_999, 99_999, 999_999, 9_999_999, 99_999_999, 999_999_999, 9_999_999_999L,
            99_999_999_999L, 999_999_999_999L, 9_999_999_999_999L, 99_999_999_999_999L, 999999_999999999L,
            9_999_999_999_999_999L, 99_999_999_999_999_999L, 999_999_999_999_999_999L, Long.MAX_VALUE
        };

        for (int i = 0; i < values.length; i++)
        {
            final int iter = i;
            assertEquals(i + 1, digitCount(values[i]), () -> iter + " -> " + values[iter]);
        }
    }

    // prints a lookup table for org.agrona.AsciiEncoding.digitCount(int)
    @Test
    @Disabled
    void printDigitCountIntTable()
    {
        for (int i = 1; i < 33; i++)
        {
            final double smallest = Math.pow(2, i - 1);
            final long log10 = (long)Math.ceil(Math.log10(smallest));
            final long value = (long)((i < 31 ? (Math.pow(2, 32) - Math.pow(10, log10)) : 0) + (log10 << 32));
            if (i != 1)
            {
                System.out.println(",");
            }
            System.out.print(value);
            System.out.print("L");
        }
        System.out.println();
    }

    // prints a lookup table for org.agrona.AsciiEncoding.digitCount(long)
    @Test
    @Disabled
    void printDigitCountLongTable()
    {
        final BigInteger[] pow10 = IntStream.rangeClosed(0, 19)
            .mapToObj(BigInteger.TEN::pow)
            .toArray(BigInteger[]::new);

        for (int i = 0; i < 64; i++)
        {
            final int upper = i == 0 ? 0 : ((i * 1262611) >> 22) + 1;
            final long value = ((long)(upper + 1) << 52) - pow10[upper].shiftRight(i / 4).longValue();
            if (i != 0)
            {
                System.out.println(",");
            }
            System.out.print(value);
            System.out.print("L");
        }
        System.out.println();
    }

    @Test
    void shouldDetectFourDigitsAsciiEncodedNumbers()
    {
        final int index = 2;
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[8]);

        for (int i = 0; i < 1000; i++)
        {
            buffer.putIntAscii(index, i);
            assertFalse(isFourDigitsAsciiEncodedNumber(buffer.getInt(index, LITTLE_ENDIAN)));
            assertFalse(isFourDigitsAsciiEncodedNumber(buffer.getInt(index, BIG_ENDIAN)));
        }

        for (int i = 1000; i < 10000; i++)
        {
            buffer.putIntAscii(index, i);
            assertTrue(isFourDigitsAsciiEncodedNumber(buffer.getInt(index, LITTLE_ENDIAN)));
            assertTrue(isFourDigitsAsciiEncodedNumber(buffer.getInt(index, BIG_ENDIAN)));
        }

        buffer.putIntAscii(index, 1234);
        buffer.putByte(index, (byte)'a');
        assertFalse(isFourDigitsAsciiEncodedNumber(buffer.getInt(index, LITTLE_ENDIAN)));
        assertFalse(isFourDigitsAsciiEncodedNumber(buffer.getInt(index, BIG_ENDIAN)));
    }

    @Test
    void shouldParseFourDigitsFromAnAsciiEncodedNumberInLittleEndianByteOrder()
    {
        final int index = 2;
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[8]);

        for (int i = 1000; i < 10000; i++)
        {
            buffer.putIntAscii(index, i);
            final int bytes = buffer.getInt(index, LITTLE_ENDIAN);
            assertEquals(i, parseFourDigitsLittleEndian(bytes));
        }
    }

    @Test
    void shouldDetectEightDigitsAsciiEncodedNumbers()
    {
        final int index = 4;
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);

        buffer.putIntAscii(index, 1234);
        assertFalse(isEightDigitAsciiEncodedNumber(buffer.getLong(index, LITTLE_ENDIAN)));
        assertFalse(isEightDigitAsciiEncodedNumber(buffer.getLong(index, BIG_ENDIAN)));

        for (int i = 10_000_000; i < 100_000_000; i += 111)
        {
            buffer.putLongAscii(index, i);
            assertTrue(isEightDigitAsciiEncodedNumber(buffer.getLong(index, LITTLE_ENDIAN)));
            assertTrue(isEightDigitAsciiEncodedNumber(buffer.getLong(index, BIG_ENDIAN)));
        }

        buffer.putByte(index, (byte)'a');
        assertFalse(isEightDigitAsciiEncodedNumber(buffer.getLong(index, LITTLE_ENDIAN)));
        assertFalse(isEightDigitAsciiEncodedNumber(buffer.getLong(index, BIG_ENDIAN)));
    }

    @Test
    void shouldParseEightDigitsFromAnAsciiEncodedNumberInLittleEndianByteOrder()
    {
        final int index = 3;
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[16]);

        for (int i = 10_000_000; i < 100_000_000; i += 111)
        {
            buffer.putIntAscii(index, i);
            final long bytes = buffer.getLong(index, LITTLE_ENDIAN);
            assertEquals(i, parseEightDigitsLittleEndian(bytes));
        }
    }

    @ParameterizedTest
    @ValueSource(bytes = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' })
    void isDigitReturnsTrueForAsciiEncodedDigits(final byte value)
    {
        assertTrue(isDigit(value));
    }

    @Test
    void isDigitReturnsFalseForAnyOtherByte()
    {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++)
        {
            final byte value = (byte)i;
            if (value >= 0x30 && value <= 0x39)
            {
                continue;
            }
            assertFalse(isDigit(value), () -> Integer.toHexString(value));
        }
    }

    @Test
    void parseIntAsciiRoundTrip()
    {
        final String prefix = "testInt";
        final StringBuilder buffer = new StringBuilder(24);
        buffer.append(prefix);

        for (int i = 0; i < ITERATIONS; i++)
        {
            final int value = ThreadLocalRandom.current().nextInt();
            buffer.append(value);

            final int parsedValue = parseIntAscii(buffer, prefix.length(), buffer.length() - prefix.length());

            assertEquals(parsedValue, value);
            buffer.delete(prefix.length(), 24);
        }
    }

    @Test
    void parseLongAsciiRoundTrip()
    {
        final String prefix = "long to test";
        final StringBuilder buffer = new StringBuilder(64);
        buffer.append(prefix);

        for (int i = 0; i < ITERATIONS; i++)
        {
            final long value = ThreadLocalRandom.current().nextLong();
            buffer.append(value);

            final long parsedValue = parseLongAscii(buffer, prefix.length(), buffer.length() - prefix.length());

            assertEquals(parsedValue, value);
            buffer.delete(prefix.length(), 64);
        }
    }

    @ParameterizedTest
    @MethodSource("longPowerOfFive")
    void isMultipleOfPowerOfFiveShouldHandlePositiveValuesUntilMaxLongPower(final int power, final long value)
    {
        assertTrue(isMultipleOfPowerOfFive(value, power));
        if (0 != power)
        {
            assertFalse(isMultipleOfPowerOfFive(value - 1, power));
            assertFalse(isMultipleOfPowerOfFive(value + 1, power));
        }
    }

    private static List<Arguments> longPowerOfFive()
    {
        final BigInteger five = BigInteger.valueOf(5);
        final List<Arguments> result = new ArrayList<>();
        for (int i = 0; i < 28; i++)
        {
            final BigInteger pow = five.pow(i);
            result.add(Arguments.arguments(i, pow.longValueExact()));
        }
        return result;
    }
}
