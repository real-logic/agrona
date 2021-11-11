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
            arguments("1.9e308", Double.POSITIVE_INFINITY));
    }

    private static String addTrailingZeroes(final String value, final int numberOfZeroes)
    {
        final char[] result = new char[value.length() + numberOfZeroes];
        System.arraycopy(value.toCharArray(), 0, result, 0, value.length());
        Arrays.fill(result, value.length(), result.length, '0');
        return new String(result);
    }
}
