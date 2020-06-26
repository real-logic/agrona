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

import static org.agrona.AsciiEncoding.parseIntAscii;
import static org.agrona.AsciiEncoding.parseLongAscii;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AsciiEncodingTest
{
    @Test
    public void shouldParseInt()
    {
        assertEquals(0, parseIntAscii("0", 0, 1));
        assertEquals(0, parseIntAscii("-0", 0, 2));
        assertEquals(7, parseIntAscii("7", 0, 1));
        assertEquals(-7, parseIntAscii("-7", 0, 2));
        assertEquals(33, parseIntAscii("3333", 1, 2));

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
    public void shouldParseLong()
    {
        assertEquals(0L, parseLongAscii("0", 0, 1));
        assertEquals(0L, parseLongAscii("-0", 0, 2));
        assertEquals(7L, parseLongAscii("7", 0, 1));
        assertEquals(-7L, parseLongAscii("-7", 0, 2));
        assertEquals(33L, parseLongAscii("3333", 1, 2));

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
    public void shouldThrowExceptionWhenParsingIntegerWhichCanOverFlow()
    {
        assertThrows(AsciiNumberFormatException.class,
            () ->
            {
                final String value = Integer.MAX_VALUE + "1";
                parseIntAscii(value, 0, value.length());
            });

        assertThrows(AsciiNumberFormatException.class,
            () ->
            {
                final String value = Integer.MIN_VALUE + "1";
                parseIntAscii(value, 0, value.length());
            });
    }

    @Test
    public void shouldThrowExceptionWhenParsingLongWhichCanOverFlow()
    {
        assertThrows(AsciiNumberFormatException.class,
            () ->
            {
                final String value = Long.MAX_VALUE + "1";
                parseLongAscii(value, 0, value.length());
            });

        assertThrows(AsciiNumberFormatException.class,
            () ->
            {
                final String value = Long.MIN_VALUE + "1";
                parseLongAscii(value, 0, value.length());
            });
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
    public void shouldThrowExceptionWhenParsingIntegerContainingLoneMinusSign()
    {
        assertThrows(AsciiNumberFormatException.class, () -> parseIntAscii("-", 0, 1));
    }

    @Test
    public void shouldThrowExceptionWhenParsingLongContainingLoneMinusSign()
    {
        assertThrows(AsciiNumberFormatException.class, () -> parseLongAscii("-", 0, 1));
    }

    @Test
    public void shouldThrowExceptionWhenParsingLongContainingLonePlusSign()
    {
        assertThrows(AsciiNumberFormatException.class, () -> parseLongAscii("+", 0, 1));
    }

    @Test
    public void shouldThrowExceptionWhenParsingEmptyInteger()
    {
        assertThrows(IndexOutOfBoundsException.class, () -> parseIntAscii("", 0, 0));
    }

    @Test
    public void shouldThrowExceptionWhenParsingEmptyLong()
    {
        assertThrows(IndexOutOfBoundsException.class, () -> parseLongAscii("", 0, 0));
    }
}
