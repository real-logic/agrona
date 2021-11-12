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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
    void shouldPutNaturalFromEnd(final int value, final int length)
    {
        final MutableDirectBuffer buffer = newBuffer(8 * 1024);

        final int start = buffer.putNaturalIntAsciiFromEnd(value, length);
        final Supplier<String> messageSupplier = () -> "value=" + value + " length=" + length;
        assertEquals(0, start, messageSupplier);

        assertEquals(String.valueOf(value), buffer.getStringWithoutLengthAscii(0, length), messageSupplier);
    }

    @Test
    void putIntAsciiRoundTrip()
    {
        final int index = 4;
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final int value = ThreadLocalRandom.current().nextInt();
            final int length = buffer.putIntAscii(index, value);
            final int parsedValue = buffer.parseIntAscii(index, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putLongAsciiRoundTrip()
    {
        final int index = 16;
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final long value = ThreadLocalRandom.current().nextLong();
            final int length = buffer.putLongAscii(index, value);
            final long parsedValue = buffer.parseLongAscii(index, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putNaturalIntAsciiRoundTrip()
    {
        final int index = 8;
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final int value = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
            final int length = buffer.putNaturalIntAscii(index, value);
            final int parsedValue = buffer.parseNaturalIntAscii(index, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putNaturalLongAsciiRoundTrip()
    {
        final int index = 12;
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final long value = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
            final int length = buffer.putNaturalLongAscii(index, value);
            final long parsedValue = buffer.parseNaturalLongAscii(index, length);
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

    @ParameterizedTest
    @MethodSource("nonParsableIntValues")
    void parseIntAsciiThrowsAsciiNumberFormatExceptionIfValueContainsInvalidCharacters(final String value)
    {
        final int index = 2;
        final MutableDirectBuffer buffer = newBuffer(16);
        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseIntAscii(index, length));
        assertEquals("error parsing int: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("nonParsableLongValues")
    void parseLongAsciiThrowsAsciiNumberFormatExceptionIfValueContainsInvalidCharacters(
        final String value, final int baseIndex)
    {
        final int index = 7;
        final MutableDirectBuffer buffer = newBuffer(32);
        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseLongAscii(index, length));
        assertEquals("error parsing long: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("nonParsableIntValues")
    void parseNaturalIntAsciiThrowsAsciiNumberFormatExceptionIfValueContainsInvalidCharacters(final String value)
    {
        final int index = 1;
        final MutableDirectBuffer buffer = newBuffer(16);
        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseNaturalIntAscii(index, length));
        assertEquals("error parsing int: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("nonParsableLongValues")
    void parseNaturalLongAsciiThrowsAsciiNumberFormatExceptionIfValueContainsInvalidCharacters(
        final String value, final int baseIndex)
    {
        final int index = 8;
        final MutableDirectBuffer buffer = newBuffer(32);
        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseNaturalLongAscii(index, length));
        assertEquals("error parsing long: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "-5547483650",
        "-2147483649",
        "2147483648",
        "2147483649",
        "9999999991",
        "-999999999999999999",
        "12345678901234567890" })
    void parseIntAsciiShouldThrowAsciiNumberFormatExceptionIfValueIsOutOfRange(final String value)
    {
        final int index = 4;
        final MutableDirectBuffer buffer = newBuffer(32);

        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseIntAscii(index, length));
        assertEquals("int overflow parsing: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "-9223372036854775810",
        "-9223372036854775809",
        "9223372036854775808",
        "9223372036854775809",
        "-19191919191919191919",
        "123456789012345678901234567890" })
    void parseLongAsciiShouldThrowAsciiNumberFormatExceptionIfValueIsOutOfRange(final String value)
    {
        final int index = 7;
        final MutableDirectBuffer buffer = newBuffer(64);

        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseLongAscii(index, length));
        assertEquals("long overflow parsing: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2147483648",
        "2147483649",
        "9999999991",
        "12345678901234567890" })
    void parseNaturalIntAsciiShouldThrowAsciiNumberFormatExceptionIfValueIsOutOfRange(final String value)
    {
        final int index = 4;
        final MutableDirectBuffer buffer = newBuffer(32);

        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseNaturalIntAscii(index, length));
        assertEquals("int overflow parsing: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "9223372036854775808",
        "9223372036854775809",
        "123456789012345678901234567890" })
    void parseNaturalLongAsciiShouldThrowAsciiNumberFormatExceptionIfValueIsOutOfRange(final String value)
    {
        final int index = 7;
        final MutableDirectBuffer buffer = newBuffer(64);

        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseNaturalLongAscii(index, length));
        assertEquals("long overflow parsing: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("doubleSpecialValues")
    void putDoubleAsciiShouldHandleSpecialValues(final double value, final String expected)
    {
        final MutableDirectBuffer buffer = newBuffer(64);

        final int index = 4;
        final int length = buffer.putDoubleAscii(index, value);

        assertEquals(expected.length(), length);
        assertEquals(expected, buffer.getStringWithoutLengthAscii(index, length));
    }

    @Test
    void putDoubleAsciiShouldEncodeNumberThatCanBeExactlyRepresentedInBinary()
    {
        final MutableDirectBuffer buffer = newBuffer(64);

        final int index = 8;
        final int length = buffer.putDoubleAscii(index, 3.875);

        assertEquals(5, length);
        assertEquals("3.875", buffer.getStringWithoutLengthAscii(index, length));
    }

    @Test
    void putDoubleAsciiShouldEncodeNumberThatCannotBeRepresentedExactlyInBinary()
    {
        final MutableDirectBuffer buffer = newBuffer(64);

        final int index = 1;
        final int length = buffer.putDoubleAscii(index, 0.100000000000000005551115123125787021181583404541015625);

        assertEquals(3, length);
        assertEquals("0.1", buffer.getStringWithoutLengthAscii(index, length));
    }

    @Test
    void putDoubleAsciiShouldEncodeNumberWithLeadingZerosAfterTheDot()
    {
        final MutableDirectBuffer buffer = newBuffer(64);

        final int index = 6;
        final int length = buffer.putDoubleAscii(index, -0.0012534);

        assertEquals(10, length);
        assertEquals("-0.0012534", buffer.getStringWithoutLengthAscii(index, length));
    }

    @Test
    void putDoubleAsciiShouldEncodeNumberWithTrailingZerosBeforeTheDot()
    {
        final MutableDirectBuffer buffer = newBuffer(64);

        final int index = 0;
        final int length = buffer.putDoubleAscii(index, 198400000000000d);

        assertEquals(17, length);
        assertEquals("198400000000000.0", buffer.getStringWithoutLengthAscii(index, length));
    }

    @ParameterizedTest
    @MethodSource("doubleBoundaryValuesFixedFormat")
    void putDoubleAsciiShouldEncodeBoundaryValues(final double value, final String encodedValue)
    {
        final MutableDirectBuffer buffer = newBuffer(384);

        final int index = 10;
        final int length = buffer.putDoubleAscii(index, value);

        assertEquals(encodedValue.length(), length);
        assertEquals(encodedValue, buffer.getStringWithoutLengthAscii(index, length));
    }

    @Test
    void putDoubleAsciiNegativeNumberWithDotInTheMiddle()
    {
        final MutableDirectBuffer buffer = newBuffer(64);

        final int index = 4;
        final int length = buffer.putDoubleAscii(index, -12.667);

        assertEquals(7, length);
        assertEquals("-12.667", buffer.getStringWithoutLengthAscii(index, length));
    }

    @ParameterizedTest
    @MethodSource("doubleValuesWithMixedOutput")
    void putDoubleAsciiShouldHandleAllOutputCombinations(final double value, final String expected)
    {
        final MutableDirectBuffer buffer = newBuffer(384);

        final int index = 4;
        final int length = buffer.putDoubleAscii(index, value);

        assertEquals(expected.length(), length);
        assertEquals(expected, buffer.getStringWithoutLengthAscii(index, length));
    }

    @Test
    void putDoubleAsciiRoundTrip()
    {
        final int index = 22;
        final MutableDirectBuffer buffer = newBuffer(384);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final long rawBits = ThreadLocalRandom.current().nextLong();
            final double value = Double.longBitsToDouble(rawBits);
            final int length = buffer.putDoubleAscii(index, value);
            final String strValue = buffer.getStringWithoutLengthAscii(index, length);
            final double parsedValue = Double.parseDouble(strValue);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void parseDoubleAsciiShouldRejectZeroLength()
    {
        final MutableDirectBuffer buffer = newBuffer(5);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseDoubleAscii(3, 0));
        assertEquals("empty string: index=3 length=0", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("doubleSpecialValues")
    void parseDoubleAsciiShouldParseSpecialValues(final double expectedValue, final String encodedValue)
    {
        final MutableDirectBuffer buffer = newBuffer(24);

        final int index = 4;
        final int length = buffer.putStringWithoutLengthAscii(index, encodedValue);

        final double value = buffer.parseDoubleAscii(index, length);
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
        final int index = 6;
        final MutableDirectBuffer buffer = newBuffer(index + value.length());

        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseDoubleAscii(index, length));
        assertEquals("error parsing double: value=" + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(longs = { -100010001000L, 0, 3, 14567 })
    void parseDoubleAsciiShouldParseIntegralValues(final long value)
    {
        final MutableDirectBuffer buffer = newBuffer(32);

        final int index = 6;
        final int length = buffer.putLongAscii(index, value);

        assertEquals((double)value, buffer.parseDoubleAscii(index, length));
    }

    @ParameterizedTest
    @MethodSource("doubleValidValues")
    void parseDoubleAsciiShouldParseValidValues(final String value, final double expectedValue)
    {
        final int index = 14;
        final MutableDirectBuffer buffer = newBuffer(index + value.length());

        final int length = buffer.putStringWithoutLengthAscii(index, value);
        assertEquals(value.length(), length);

        assertEquals(expectedValue, buffer.parseDoubleAscii(index, length));
    }

    @Test
    void parseDoubleAsciiShouldHandleAllPowersOfTen()
    {
        final int index = 2;
        final MutableDirectBuffer buffer = newBuffer(32);

        for (int i = -1000; i <= 308; i++)
        {
            final String value = "1e" + i;
            final int length = buffer.putStringWithoutLengthAscii(index, value);
            final double expectedValues = i >= -307 ? EXACT_POWERS_OF_TEN[i + 307] : Math.pow(10, i);
            final double actualValue = buffer.parseDoubleAscii(index, length);
            assertEquals(expectedValues, actualValue, value);
        }
    }

    @Test
    void parseDoubleAsciiShouldHandleTrickyPowersOfFive()
    {
        final int index = 4;
        final MutableDirectBuffer buffer = newBuffer(64);

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

                final int length = buffer.putStringWithoutLengthAscii(index, value);

                final double parsedValue = buffer.parseDoubleAscii(index, length);
                assertEquals(Double.parseDouble(value), parsedValue);
            }
        }
    }

    private static List<Arguments> valuesAndLengths()
    {
        return Arrays.asList(
            Arguments.arguments(1, 1),
            Arguments.arguments(10, 2),
            Arguments.arguments(100, 3),
            Arguments.arguments(1000, 4),
            Arguments.arguments(12, 2),
            Arguments.arguments(123, 3),
            Arguments.arguments(2345, 4),
            Arguments.arguments(9, 1),
            Arguments.arguments(99, 2),
            Arguments.arguments(999, 3),
            Arguments.arguments(9999, 4));
    }

    private static List<String> nonParsableIntValues()
    {
        return Arrays.asList(
            "23.5",
            "+1",
            "a14349",
            "0xFF",
            "999v",
            "-",
            "+",
            "1234%67890");
    }

    private static List<Arguments> nonParsableLongValues()
    {
        return Arrays.asList(
            Arguments.arguments("23.5", 2),
            Arguments.arguments("+1", 0),
            Arguments.arguments("a14349", 0),
            Arguments.arguments("0xFF", 1),
            Arguments.arguments("999v", 3),
            Arguments.arguments("-", 0),
            Arguments.arguments("+", 0),
            Arguments.arguments("123456789^123456789", 9)
        );
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

    private static List<Arguments> doubleValuesWithMixedOutput()
    {
        return Arrays.asList(
            arguments(-5.212680729440889e17, "-521268072944088900.0"),
            arguments(6.157934106540014e15, "6157934106540014.0"),
            arguments(1.5501284640872244e-24, "0.0000000000000000000000015501284640872244"),
            arguments(-4.7477675821412186e-9, "-0.000000004747767582141219"),
            arguments(10096.357534152876, "10096.357534152876"),
            arguments(-1348197.6786845152, "-1348197.6786845152"),
            arguments(3.997640515505592e22, "39976405155055920000000.0"),
            arguments(-2.9928867511316713e24, "-2992886751131671300000000.0"),
            arguments(2.4647073177022235e15, "2464707317702223.5"),
            arguments(7.7723803003247168e16, "77723803003247170.0"),
            arguments(-1.1379008962552722e15, "-1137900896255272.2"),
            arguments(876000.00021654, "876000.00021654"),
            arguments(1.62393824548399514e18, "1623938245483995100.0")
        );
    }

    private static List<Arguments> doubleBoundaryValuesFixedFormat()
    {
        return Arrays.asList(
            arguments(Double.MAX_VALUE, addTrailingZeroes("17976931348623157", 292) + ".0"),
            arguments(Double.MIN_VALUE, addTrailingZeroes("0.", 323) + "5"),
            arguments(Double.MIN_NORMAL, addTrailingZeroes("0.", 307) + "22250738585072014")
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
            arguments("7.2057594037927933e+16", 7.2057594037927933e+16));
    }

    private static String addTrailingZeroes(final String value, final int numberOfZeroes)
    {
        final char[] result = new char[value.length() + numberOfZeroes];
        System.arraycopy(value.toCharArray(), 0, result, 0, value.length());
        Arrays.fill(result, value.length(), result.length, '0');
        return new String(result);
    }

    private static final double[] EXACT_POWERS_OF_TEN =
    {
        1e-307, 1e-306, 1e-305, 1e-304, 1e-303, 1e-302, 1e-301, 1e-300, 1e-299,
        1e-298, 1e-297, 1e-296, 1e-295, 1e-294, 1e-293, 1e-292, 1e-291, 1e-290,
        1e-289, 1e-288, 1e-287, 1e-286, 1e-285, 1e-284, 1e-283, 1e-282, 1e-281,
        1e-280, 1e-279, 1e-278, 1e-277, 1e-276, 1e-275, 1e-274, 1e-273, 1e-272,
        1e-271, 1e-270, 1e-269, 1e-268, 1e-267, 1e-266, 1e-265, 1e-264, 1e-263,
        1e-262, 1e-261, 1e-260, 1e-259, 1e-258, 1e-257, 1e-256, 1e-255, 1e-254,
        1e-253, 1e-252, 1e-251, 1e-250, 1e-249, 1e-248, 1e-247, 1e-246, 1e-245,
        1e-244, 1e-243, 1e-242, 1e-241, 1e-240, 1e-239, 1e-238, 1e-237, 1e-236,
        1e-235, 1e-234, 1e-233, 1e-232, 1e-231, 1e-230, 1e-229, 1e-228, 1e-227,
        1e-226, 1e-225, 1e-224, 1e-223, 1e-222, 1e-221, 1e-220, 1e-219, 1e-218,
        1e-217, 1e-216, 1e-215, 1e-214, 1e-213, 1e-212, 1e-211, 1e-210, 1e-209,
        1e-208, 1e-207, 1e-206, 1e-205, 1e-204, 1e-203, 1e-202, 1e-201, 1e-200,
        1e-199, 1e-198, 1e-197, 1e-196, 1e-195, 1e-194, 1e-193, 1e-192, 1e-191,
        1e-190, 1e-189, 1e-188, 1e-187, 1e-186, 1e-185, 1e-184, 1e-183, 1e-182,
        1e-181, 1e-180, 1e-179, 1e-178, 1e-177, 1e-176, 1e-175, 1e-174, 1e-173,
        1e-172, 1e-171, 1e-170, 1e-169, 1e-168, 1e-167, 1e-166, 1e-165, 1e-164,
        1e-163, 1e-162, 1e-161, 1e-160, 1e-159, 1e-158, 1e-157, 1e-156, 1e-155,
        1e-154, 1e-153, 1e-152, 1e-151, 1e-150, 1e-149, 1e-148, 1e-147, 1e-146,
        1e-145, 1e-144, 1e-143, 1e-142, 1e-141, 1e-140, 1e-139, 1e-138, 1e-137,
        1e-136, 1e-135, 1e-134, 1e-133, 1e-132, 1e-131, 1e-130, 1e-129, 1e-128,
        1e-127, 1e-126, 1e-125, 1e-124, 1e-123, 1e-122, 1e-121, 1e-120, 1e-119,
        1e-118, 1e-117, 1e-116, 1e-115, 1e-114, 1e-113, 1e-112, 1e-111, 1e-110,
        1e-109, 1e-108, 1e-107, 1e-106, 1e-105, 1e-104, 1e-103, 1e-102, 1e-101,
        1e-100, 1e-99, 1e-98, 1e-97, 1e-96, 1e-95, 1e-94, 1e-93, 1e-92,
        1e-91, 1e-90, 1e-89, 1e-88, 1e-87, 1e-86, 1e-85, 1e-84, 1e-83,
        1e-82, 1e-81, 1e-80, 1e-79, 1e-78, 1e-77, 1e-76, 1e-75, 1e-74,
        1e-73, 1e-72, 1e-71, 1e-70, 1e-69, 1e-68, 1e-67, 1e-66, 1e-65,
        1e-64, 1e-63, 1e-62, 1e-61, 1e-60, 1e-59, 1e-58, 1e-57, 1e-56,
        1e-55, 1e-54, 1e-53, 1e-52, 1e-51, 1e-50, 1e-49, 1e-48, 1e-47,
        1e-46, 1e-45, 1e-44, 1e-43, 1e-42, 1e-41, 1e-40, 1e-39, 1e-38,
        1e-37, 1e-36, 1e-35, 1e-34, 1e-33, 1e-32, 1e-31, 1e-30, 1e-29,
        1e-28, 1e-27, 1e-26, 1e-25, 1e-24, 1e-23, 1e-22, 1e-21, 1e-20,
        1e-19, 1e-18, 1e-17, 1e-16, 1e-15, 1e-14, 1e-13, 1e-12, 1e-11,
        1e-10, 1e-9, 1e-8, 1e-7, 1e-6, 1e-5, 1e-4, 1e-3, 1e-2,
        1e-1, 1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7,
        1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16,
        1e17, 1e18, 1e19, 1e20, 1e21, 1e22, 1e23, 1e24, 1e25,
        1e26, 1e27, 1e28, 1e29, 1e30, 1e31, 1e32, 1e33, 1e34,
        1e35, 1e36, 1e37, 1e38, 1e39, 1e40, 1e41, 1e42, 1e43,
        1e44, 1e45, 1e46, 1e47, 1e48, 1e49, 1e50, 1e51, 1e52,
        1e53, 1e54, 1e55, 1e56, 1e57, 1e58, 1e59, 1e60, 1e61,
        1e62, 1e63, 1e64, 1e65, 1e66, 1e67, 1e68, 1e69, 1e70,
        1e71, 1e72, 1e73, 1e74, 1e75, 1e76, 1e77, 1e78, 1e79,
        1e80, 1e81, 1e82, 1e83, 1e84, 1e85, 1e86, 1e87, 1e88,
        1e89, 1e90, 1e91, 1e92, 1e93, 1e94, 1e95, 1e96, 1e97,
        1e98, 1e99, 1e100, 1e101, 1e102, 1e103, 1e104, 1e105, 1e106,
        1e107, 1e108, 1e109, 1e110, 1e111, 1e112, 1e113, 1e114, 1e115,
        1e116, 1e117, 1e118, 1e119, 1e120, 1e121, 1e122, 1e123, 1e124,
        1e125, 1e126, 1e127, 1e128, 1e129, 1e130, 1e131, 1e132, 1e133,
        1e134, 1e135, 1e136, 1e137, 1e138, 1e139, 1e140, 1e141, 1e142,
        1e143, 1e144, 1e145, 1e146, 1e147, 1e148, 1e149, 1e150, 1e151,
        1e152, 1e153, 1e154, 1e155, 1e156, 1e157, 1e158, 1e159, 1e160,
        1e161, 1e162, 1e163, 1e164, 1e165, 1e166, 1e167, 1e168, 1e169,
        1e170, 1e171, 1e172, 1e173, 1e174, 1e175, 1e176, 1e177, 1e178,
        1e179, 1e180, 1e181, 1e182, 1e183, 1e184, 1e185, 1e186, 1e187,
        1e188, 1e189, 1e190, 1e191, 1e192, 1e193, 1e194, 1e195, 1e196,
        1e197, 1e198, 1e199, 1e200, 1e201, 1e202, 1e203, 1e204, 1e205,
        1e206, 1e207, 1e208, 1e209, 1e210, 1e211, 1e212, 1e213, 1e214,
        1e215, 1e216, 1e217, 1e218, 1e219, 1e220, 1e221, 1e222, 1e223,
        1e224, 1e225, 1e226, 1e227, 1e228, 1e229, 1e230, 1e231, 1e232,
        1e233, 1e234, 1e235, 1e236, 1e237, 1e238, 1e239, 1e240, 1e241,
        1e242, 1e243, 1e244, 1e245, 1e246, 1e247, 1e248, 1e249, 1e250,
        1e251, 1e252, 1e253, 1e254, 1e255, 1e256, 1e257, 1e258, 1e259,
        1e260, 1e261, 1e262, 1e263, 1e264, 1e265, 1e266, 1e267, 1e268,
        1e269, 1e270, 1e271, 1e272, 1e273, 1e274, 1e275, 1e276, 1e277,
        1e278, 1e279, 1e280, 1e281, 1e282, 1e283, 1e284, 1e285, 1e286,
        1e287, 1e288, 1e289, 1e290, 1e291, 1e292, 1e293, 1e294, 1e295,
        1e296, 1e297, 1e298, 1e299, 1e300, 1e301, 1e302, 1e303, 1e304,
        1e305, 1e306, 1e307, 1e308
    };
}
