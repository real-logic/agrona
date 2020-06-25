/*
 * Copyright 2014-2020 Real Logic Limited.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AsciiEncodingTest
{
    @Test
    public void shouldParseInt()
    {
        assertThat(AsciiEncoding.parseIntAscii("0", 0, 1), is(0));
        assertThat(AsciiEncoding.parseIntAscii("-0", 0, 2), is(0));
        assertThat(AsciiEncoding.parseIntAscii("7", 0, 1), is(7));
        assertThat(AsciiEncoding.parseIntAscii("-7", 0, 2), is(-7));
        assertThat(AsciiEncoding.parseIntAscii("3333", 1, 2), is(33));
    }

    @Test
    public void shouldParseMaxIntValue()
    {
        final String value = String.valueOf(Integer.MAX_VALUE);
        assertThat(AsciiEncoding.parseIntAscii(value, 0, value.length()), is(Integer.MAX_VALUE));
    }

    @Test
    public void shouldParseMinIntValue()
    {
        final String value = String.valueOf(Integer.MIN_VALUE);
        assertThat(AsciiEncoding.parseIntAscii(value, 0, value.length()), is(Integer.MIN_VALUE));
    }

    @Test
    public void shouldParseLong()
    {
        assertThat(AsciiEncoding.parseLongAscii("0", 0, 1), is(0L));
        assertThat(AsciiEncoding.parseLongAscii("-0", 0, 2), is(0L));
        assertThat(AsciiEncoding.parseLongAscii("7", 0, 1), is(7L));
        assertThat(AsciiEncoding.parseLongAscii("-7", 0, 2), is(-7L));
        assertThat(AsciiEncoding.parseLongAscii("3333", 1, 2), is(33L));
    }

    @Test
    public void shouldParseMaxLongValue()
    {
        final String value = String.valueOf(Long.MAX_VALUE);
        assertThat(AsciiEncoding.parseLongAscii(value, 0, value.length()), is(Long.MAX_VALUE));
    }

    @Test
    public void shouldParseMinLongValue()
    {
        final String value = String.valueOf(Long.MIN_VALUE);
        assertThat(AsciiEncoding.parseLongAscii(value, 0, value.length()), is(Long.MIN_VALUE));
    }

    @Test
    public void shouldThrowExceptionWhenDecodingCharNonNumericValue()
    {
        assertThrows(AsciiNumberFormatException.class, () -> AsciiEncoding.getDigit(0, 'a'));
    }

    @Test
    public void shouldThrowExceptionWhenDecodingByteNonNumericValue()
    {
        assertThrows(AsciiNumberFormatException.class, () -> AsciiEncoding.getDigit(0, (byte)'a'));
    }

    @Test
    public void shouldThrowExceptionWhenParsingLongContainingLoneMinusSign()
    {
        assertThrows(AsciiNumberFormatException.class, () -> AsciiEncoding.parseLongAscii("-", 0, 1));
    }

    @Test
    public void shouldThrowExceptionWhenParsingIntegerContainingLoneMinusSign()
    {
        assertThrows(AsciiNumberFormatException.class, () -> AsciiEncoding.parseIntAscii("-", 0, 1));
    }

    @Test
    public void shouldThrowExceptionWhenParsingLongContainingLonePlusSign()
    {
        assertThrows(AsciiNumberFormatException.class, () -> AsciiEncoding.parseLongAscii("+", 0, 1));
    }

    @Test
    public void shouldThrowExceptionWhenParsingEmptyLong()
    {
        assertThrows(IndexOutOfBoundsException.class, () -> AsciiEncoding.parseLongAscii("", 0, 0));
    }

    @Test
    public void shouldThrowExceptionWhenParsingEmptyInteger()
    {
        assertThrows(IndexOutOfBoundsException.class, () -> AsciiEncoding.parseIntAscii("", 0, 0));
    }

    @Test
    public void shouldThrowNumberFormatExceptionOnIntOverflow()
    {
        final String values[] =
        {
            "2147483648", // Integer.MAX_VALUE + 1
            "10737418230", // (Integer.MAX_VALUE / 2) * 10
            "9999999999",
            "-2147483649", // Integer.MIN_VALUE - 1
            "-10737418240", // (Integer.MIN_VALUE / 2) * 10
            "-9999999999"
        };
        for (final String value : values)
        {
            assertThrows(
                NumberFormatException.class,
                () -> AsciiEncoding.parseIntAscii(value, 0, value.length()),
                value
            );
        }
    }

    @Test
    public void shouldThrowNumberFormatExceptionOnLongOverflow()
    {
        final String values[] =
        {
            "9223372036854775808", // Long.MAX_VALUE + 1
            "46116860184273879030", // (Long.MAX_VALUE / 2) * 10
            "9999999999999999999",
            "-9223372036854775809", // LONG.MIN_VALUE - 1
            "-46116860184273879040", // (Long.MIN_VALUE / 2) * 10
            "-9999999999999999999"
        };
        for (final String value : values)
        {
            assertThrows(
                NumberFormatException.class,
                () -> AsciiEncoding.parseLongAscii(value, 0, value.length()),
                value
            );
        }
    }
}
