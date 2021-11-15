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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.agrona.AsciiEncoding.*;
import static org.agrona.MutableDirectBufferTests.EXACT_POWERS_OF_TEN;
import static org.agrona.MutableDirectBufferTests.addTrailingZeroes;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AsciiEncodingTest
{
    private static final int ITERATIONS = 10_000_000;

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

    @Test
    void multiplyHighUnsignedRandomTest()
    {
        for (int i = 0; i < ITERATIONS; i++)
        {
            final long x = ThreadLocalRandom.current().nextLong();
            final long y = ThreadLocalRandom.current().nextLong();
            assertEquals(multiplyHighUnsignedManual(x, y), multiplyHighUnsigned(x, y), () -> x + " * " + y);
        }
    }

    @Test
    void parseDoubleAsciiRoundTrip()
    {
        final String prefix = "parse_double_test";
        final StringBuilder buffer = new StringBuilder(1024);
        buffer.append(prefix);

        for (int i = 0; i < ITERATIONS; i++)
        {
            final double value = Double.longBitsToDouble(ThreadLocalRandom.current().nextLong());
            buffer.append(value);

            final double parsedValue = parseDoubleAscii(buffer, prefix.length(), buffer.length() - prefix.length());

            assertEquals(parsedValue, value, () -> buffer.substring(prefix.length()));
            buffer.delete(prefix.length(), 1024);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { -8, 0 })
    void parseDoubleAsciiShouldRejectInvalidLength(final int length)
    {
        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> parseDoubleAscii("abc", 3, length));
        assertEquals("empty string: index=3 length=" + length, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("doubleSpecialValues")
    void parseDoubleAsciiShouldParseSpecialValues(final double expectedValue, final String encodedValue)
    {
        final double value = parseDoubleAscii(encodedValue, 0, encodedValue.length());
        if (Double.isNaN(expectedValue))
        {
            assertTrue(Double.isNaN(value));
        }
        else
        {
            assertEquals(expectedValue, value);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "NaM", "INFINITY", "-INFINITY", "Inf", "NaNa", "-0.5x", "-", "+", "." })
    void parseDoubleAsciiShouldThrowIfNotANumberOrASpecialValue(final String value)
    {
        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> parseDoubleAscii(value, 0, value.length()));
        assertEquals("error parsing double: value=" + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(longs = { -100010001000L, 0, 3, 14567 })
    void parseDoubleAsciiShouldParseIntegralValues(final long value)
    {
        final int index = 6;
        final String encodedValue = " four " + value;
        assertEquals((double)value, parseDoubleAscii(encodedValue, index, encodedValue.length() - index));
    }

    @ParameterizedTest
    @MethodSource("doubleValidValues")
    void parseDoubleAsciiShouldParseValidValues(final String value, final double expectedValue)
    {
        assertEquals(expectedValue, parseDoubleAscii(value, 0, value.length()));
    }

    @Test
    void parseDoubleAsciiShouldHandleAllPowersOfTen()
    {
        final int index = 2;
        for (int i = -1000; i <= 308; i++)
        {
            final String value = "  1e" + i;
            final double expectedValues = i >= -307 ? EXACT_POWERS_OF_TEN[i + 307] : Math.pow(10, i);
            final double actualValue = parseDoubleAscii(value, index, value.length() - index);
            assertEquals(expectedValues, actualValue, value);
        }
    }

    @Test
    void parseDoubleAsciiShouldHandleTrickyPowersOfFive()
    {
        for (int q = 18; q <= 27; q++)
        {
            long pow5 = 1;
            for (int i = 0; i < q; i++)
            {
                pow5 *= 5;
            }

            final long from = (0x20000000000000L / pow5) + 1;
            final long to = Long.MAX_VALUE / pow5 + 1;
            for (int i = 0; i < 10_000; i++)
            {
                final long mantissa = ThreadLocalRandom.current().nextLong(from, to) * pow5;
                final String value = mantissa + "e" + (-q);

                final double parsedValue = parseDoubleAscii(value, 0, value.length());
                assertEquals(Double.parseDouble(value), parsedValue);
            }
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

    private static long multiplyHighUnsignedManual(final long x, final long y)
    {
        final long x0 = x & 0xFFFFFFFFL;
        final long x1 = x >> 32;
        final long y0 = y & 0xFFFFFFFFL;
        final long y1 = y >> 32;
        final long w0 = x0 * y0;
        final long t = x1 * y0 + (w0 >>> 32);
        long w1 = t & 0xFFFFFFFFL;
        final long w2 = t >> 32;
        w1 = x0 * y1 + w1;
        final long mulHi = x1 * y1 + w2 + (w1 >> 32);

        return mulHi + (y & (x >> 63)) + (x & (y >> 63));
    }

    private static List<Arguments> doubleSpecialValues()
    {
        return Arrays.asList(
            arguments(Double.NaN, "NaN"),
            arguments(Double.NEGATIVE_INFINITY, "-Infinity"),
            arguments(Double.POSITIVE_INFINITY, "Infinity"),
            arguments(0.0d, "0.0"),
            arguments(-0.0d, "-0.0")
        );
    }

    @SuppressWarnings("LineLength")
    private static List<Arguments> doubleValidValues()
    {
        return Arrays.asList(
            arguments(".75", 0.75),
            arguments("-.5", -0.5),
            arguments("3.", 3.0),
            arguments("-554449.", -554449.0),
            arguments("1e10", 1e10),
            arguments("-9e-3", -0.009),
            arguments(addTrailingZeroes("17976931348623157", 292) + ".0", Double.MAX_VALUE),
            arguments(addTrailingZeroes("0.", 323) + "5", Double.MIN_VALUE),
            arguments(addTrailingZeroes("0.", 307) + "22250738585072014", Double.MIN_NORMAL),
            arguments("0000000000000000000123.45", 123.45),
            arguments("-123.45000000000000000000000000000000000000000000", -123.45),
            arguments("1230000000000000000000.0000045", 1230000000000000000000.0000045),
            arguments("-2.2222222222223e-322", -0x1.68p-1069),
            arguments("9007199254740993.0", 0x1p+53),
            arguments("860228122.6654514319E+90", 0x1.92bb20990715fp+328),
            arguments(addTrailingZeroes("9007199254740993.0", 1000), 0x1p+53),
            arguments("10000000000000000000", 0x1.158e460913dp+63),
            arguments("10000000000000000000000000000001000000000000", 0x1.cb2d6f618c879p+142),
            arguments("10000000000000000000000000000000000000000001", 0x1.cb2d6f618c879p+142),
            arguments("1.1920928955078125e-07", 1.1920928955078125e-07),
            arguments("0001.1920928955078125e-07", 1.1920928955078125e-07),
            arguments("9355950000000000000.00000000000000000000000000000000001844674407370955161600000184467440737095516161844674407370955161407370955161618446744073709551616000184467440737095516166000001844674407370955161618446744073709551614073709551616184467440737095516160001844674407370955161601844674407370955674451616184467440737095516140737095516161844674407370955161600018446744073709551616018446744073709551611616000184467440737095001844674407370955161600184467440737095516160018446744073709551168164467440737095516160001844073709551616018446744073709551616184467440737095516160001844674407536910751601611616000184467440737095001844674407370955161600184467440737095516160018446744073709551616184467440737095516160001844955161618446744073709551616000184467440753691075160018446744073709", 0x1.03ae05e8fca1cp+63),
            arguments("-0", -0.0),
            arguments("-0.0", -0.0),
            arguments("0.0", 0.0),
            arguments("00.000", 0.0),
            arguments("-2.9", -2.9),
            arguments("1.5", 1.5),
            arguments("2.22507385850720212418870147920222032907240528279439037814303133837435107319244194686754406432563881851382188218502438069999947733013005649884107791928741341929297200970481951993067993290969042784064731682041565926728632933630474670123316852983422152744517260835859654566319282835244787787799894310779783833699159288594555213714181128458251145584319223079897504395086859412457230891738946169368372321191373658977977723286698840356390251044443035457396733706583981055420456693824658413747607155981176573877626747665912387199931904006317334709003012790188175203447190250028061277777916798391090578584006464715943810511489154282775041174682194133952466682503431306181587829379004205392375072083366693241580002758391118854188641513168478436313080237596295773983001708984375e-308", 0x1.0000000000002p-1022),
            arguments("1.0000000000000006661338147750939242541790008544921875", 1.0000000000000007),
            arguments("1090544144181609348835077142190", 0x1.b8779f2474dfbp+99),
            arguments("2.2250738585072013e-308", 2.2250738585072013e-308),
            arguments("-92666518056446206563E3", -92666518056446206563E3),
            arguments("-92666518056446206563E3", -92666518056446206563E3),
            arguments("-42823146028335318693e-128", -42823146028335318693e-128),
            arguments("90054602635948575728E72", 90054602635948575728E72),
            arguments("1.00000000000000188558920870223463870174566020691753515394643550663070558368373221972569761144603605635692374830246134201063722058e-309", 1.00000000000000188558920870223463870174566020691753515394643550663070558368373221972569761144603605635692374830246134201063722058e-309),
            arguments("0e9999999999999999999999999999", 0.0),
            arguments("-2402844368454405395.2", -2402844368454405395.2),
            arguments("2402844368454405395.2", 2402844368454405395.2),
            arguments("7.0420557077594588669468784357561207962098443483187940792729600000e+59", 7.0420557077594588669468784357561207962098443483187940792729600000e+59),
            arguments("7.0420557077594588669468784357561207962098443483187940792729600000e+59", 7.0420557077594588669468784357561207962098443483187940792729600000e+59),
            arguments("-1.7339253062092163730578609458683877051596800000000000000000000000e+42", -1.7339253062092163730578609458683877051596800000000000000000000000e+42),
            arguments("-2.0972622234386619214559824785284023792871122537545728000000000000e+52", -2.0972622234386619214559824785284023792871122537545728000000000000e+52),
            arguments("-1.0001803374372191849407179462120053338028379051879898808320000000e+57", -1.0001803374372191849407179462120053338028379051879898808320000000e+57),
            arguments("-1.8607245283054342363818436991534856973992070520151142825984000000e+58", -1.8607245283054342363818436991534856973992070520151142825984000000e+58),
            arguments("-1.9189205311132686907264385602245237137907390376574976000000000000e+52", -1.9189205311132686907264385602245237137907390376574976000000000000e+52),
            arguments("-2.8184483231688951563253238886553506793085187889855201280000000000e+54", -2.8184483231688951563253238886553506793085187889855201280000000000e+54),
            arguments("-1.7664960224650106892054063261344555646357024359107788800000000000e+53", -1.7664960224650106892054063261344555646357024359107788800000000000e+53),
            arguments("-2.1470977154320536489471030463761883783915110400000000000000000000e+45", -2.1470977154320536489471030463761883783915110400000000000000000000e+45),
            arguments("-4.4900312744003159009338275160799498340862630046359789166919680000e+61", -4.4900312744003159009338275160799498340862630046359789166919680000e+61),
            arguments("1", 1.0),
            arguments("1.797693134862315700000000000000001e308", 1.7976931348623157e308),
            arguments("3e-324", 0x0.0000000000001p-1022),
            arguments("1.00000006e+09", 0x1.dcd651ep+29),
            arguments("4.9406564584124653e-324", 0x0.0000000000001p-1022),
            arguments("4.9406564584124654e-324", 0x0.0000000000001p-1022),
            arguments("2.2250738585072009e-308", 0x0.fffffffffffffp-1022),
            arguments("2.2250738585072014e-308", 0x1p-1022),
            arguments("1.7976931348623157e308", 0x1.fffffffffffffp+1023),
            arguments("1.7976931348623158e308", 0x1.fffffffffffffp+1023),
            arguments("4503599627370496.5", 4503599627370496.5),
            arguments("4503599627475352.5", 4503599627475352.5),
            arguments("4503599627475353.5", 4503599627475353.5),
            arguments("2251799813685248.25", 2251799813685248.25),
            arguments("1125899906842624.125", 1125899906842624.125),
            arguments("1125899906842901.875", 1125899906842901.875),
            arguments("2251799813685803.75", 2251799813685803.75),
            arguments("4503599627370497.5", 4503599627370497.5),
            arguments("45035996.273704995", 45035996.273704995),
            arguments("45035996.273704985", 45035996.273704985),
            arguments("0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000044501477170144022721148195934182639518696390927032912960468522194496444440421538910330590478162701758282983178260792422137401728773891892910553144148156412434867599762821265346585071045737627442980259622449029037796981144446145705102663115100318287949527959668236039986479250965780342141637013812613333119898765515451440315261253813266652951306000184917766328660755595837392240989947807556594098101021612198814605258742579179000071675999344145086087205681577915435923018910334964869420614052182892431445797605163650903606514140377217442262561590244668525767372446430075513332450079650686719491377688478005309963967709758965844137894433796621993967316936280457084866613206797017728916080020698679408551343728867675409720757232455434770912461317493580281734466552734375", 0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000044501477170144022721148195934182639518696390927032912960468522194496444440421538910330590478162701758282983178260792422137401728773891892910553144148156412434867599762821265346585071045737627442980259622449029037796981144446145705102663115100318287949527959668236039986479250965780342141637013812613333119898765515451440315261253813266652951306000184917766328660755595837392240989947807556594098101021612198814605258742579179000071675999344145086087205681577915435923018910334964869420614052182892431445797605163650903606514140377217442262561590244668525767372446430075513332450079650686719491377688478005309963967709758965844137894433796621993967316936280457084866613206797017728916080020698679408551343728867675409720757232455434770912461317493580281734466552734375),
            arguments("0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000022250738585072008890245868760858598876504231122409594654935248025624400092282356951787758888037591552642309780950434312085877387158357291821993020294379224223559819827501242041788969571311791082261043971979604000454897391938079198936081525613113376149842043271751033627391549782731594143828136275113838604094249464942286316695429105080201815926642134996606517803095075913058719846423906068637102005108723282784678843631944515866135041223479014792369585208321597621066375401613736583044193603714778355306682834535634005074073040135602968046375918583163124224521599262546494300836851861719422417646455137135420132217031370496583210154654068035397417906022589503023501937519773030945763173210852507299305089761582519159720757232455434770912461317493580281734466552734375", 0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000022250738585072008890245868760858598876504231122409594654935248025624400092282356951787758888037591552642309780950434312085877387158357291821993020294379224223559819827501242041788969571311791082261043971979604000454897391938079198936081525613113376149842043271751033627391549782731594143828136275113838604094249464942286316695429105080201815926642134996606517803095075913058719846423906068637102005108723282784678843631944515866135041223479014792369585208321597621066375401613736583044193603714778355306682834535634005074073040135602968046375918583163124224521599262546494300836851861719422417646455137135420132217031370496583210154654068035397417906022589503023501937519773030945763173210852507299305089761582519159720757232455434770912461317493580281734466552734375),
            arguments("1438456663141390273526118207642235581183227845246331231162636653790368152091394196930365828634687637948157940776599182791387527135353034738357134110310609455693900824193549772792016543182680519740580354365467985440183598701312257624545562331397018329928613196125590274187720073914818062530830316533158098624984118889298281371812288789537310599037529113415438738954894752124724983067241108764488346454376699018673078404751121414804937224240805993123816932326223683090770561597570457793932985826162604255884529134126396282202126526253389383421806727954588525596114379801269094096329805054803089299736996870951258573010877404407451953846698609198213926882692078557033228265259305481198526059813164469187586693257335779522020407645498684263339921905227556616698129967412891282231685504660671277927198290009824680186319750978665734576683784255802269708917361719466043175201158849097881370477111850171579869056016061666173029059588433776015644439705050377554277696143928278093453792803846252715966016733222646442382892123940052441346822429721593884378212558701004356924243030059517489346646577724622498919752597382095222500311124181823512251071356181769376577651390028297796156208815375089159128394945710515861334486267101797497111125909272505194792870889617179758703442608016143343262159998149700606597792535574457560429226974273443630323818747730771316763398572110874959981923732463076884528677392654150010269822239401993427482376513231389212353583573566376915572650916866553612366187378959554983566712767093372906030188976220169058025354973622211666504549316958271880975697143546564469806791358707318873075708383345004090151974068325838177531266954177406661392229801349994695941509935655355652985723782153570084089560139142231.738475042362596875449154552392299548947138162081694168675340677843807613129780449323363759027012972466987370921816813162658754726545121090545507240267000456594786540949605260722461937870630634874991729398208026467698131898691830012167897399682179601734569071423681e-733", Double.POSITIVE_INFINITY),
            arguments("-2240084132271013504.131248280843119943687942846658579428", -0x1.f1660a65b00bfp+60),
            arguments("1234456789012345678901234567890e9999999999999999999999999999", Double.POSITIVE_INFINITY),
            arguments("-2139879401095466344511101915470454744.9813888656856943E+272", Double.NEGATIVE_INFINITY),
            arguments("1.8e308", Double.POSITIVE_INFINITY),
            arguments("1.832312213213213232132132143451234453123412321321312e308", Double.POSITIVE_INFINITY),
            arguments("2e30000000000000000", Double.POSITIVE_INFINITY),
            arguments("2e3000", Double.POSITIVE_INFINITY),
            arguments("-2e3000", Double.NEGATIVE_INFINITY),
            arguments("1.9e308", Double.POSITIVE_INFINITY),
            arguments("0.156250000000000000000000000000000000000000", 0.15625),
            arguments("3.14159265358979323846264338327950288419716939937510", 3.141592653589793),
            arguments("2.71828182845904523536028747135266249775724709369995", 2.718281828459045),
            arguments("7.3177701707893310e+15", 7317770170789331.0),
            arguments("7.2057594037927933e+16", 7.2057594037927933e+16),
            arguments("971778443352269300.0", 9.7177844335226931E17),
            arguments("-4.2336496951542226E38", -0x1.3e81209173ecep+128),
            arguments("-4.233649695154223E38", -0x1.3e81209173ecfp+128),
            arguments("-5.8081984879362226E38", -5.8081984879362226E38),
            arguments("-5.808198487936223E38", -5.808198487936223E38),
            arguments("-2.9619366646299568E38", -2.9619366646299568E38),
            arguments("-2.961936664629957E38", -2.961936664629957E38));
    }
}
