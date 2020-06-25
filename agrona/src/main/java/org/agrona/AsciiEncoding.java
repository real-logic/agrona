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

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Helper for dealing with ASCII encoding of numbers.
 */
public final class AsciiEncoding
{
    public static final byte ZERO = '0';
    private static final int[] INT_ROUNDS =
    {
        9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE
    };

    private static final long[] LONG_ROUNDS =
    {
        9L, 99L, 999L, 9999L, 99999L, 999999L, 9999999L, 99999999L, 999999999L,
        9_999999999L, 99_999999999L, 999_999999999L, 9999_999999999L,
        99999_999999999L, 999999_999999999L, 9999999_999999999L, 99999999_999999999L,
        999999999_999999999L, Long.MAX_VALUE
    };

    private static final String MIN_INTEGER_AS_STRING = String.valueOf(Integer.MIN_VALUE);
    private static final String MIN_LONG_AS_STRING = String.valueOf(Long.MIN_VALUE);

    // Based on  Integer::parseInt and Long::parseLong
    // If value to be multiplied by 10 is above this threshold, multiplication will overflow;
    // however it may overflow to positive number, so simple check for change of sign does not detect the problem
    private static final int INTEGER_MULT_MAX = Integer.MAX_VALUE / 10;
    private static final long LONG_MULT_MAX = Long.MAX_VALUE / 10;

    public static final byte[] MIN_INTEGER_VALUE = MIN_INTEGER_AS_STRING.getBytes(US_ASCII);
    public static final byte[] MIN_LONG_VALUE = MIN_LONG_AS_STRING.getBytes(US_ASCII);
    public static final byte MINUS_SIGN = (byte)'-';

    private AsciiEncoding()
    {
    }

    /**
     * Get the end offset of an ASCII encoded value.
     *
     * @param value to find the end encoded character offset.
     * @return the offset at which the encoded value will end.
     */
    public static int endOffset(final int value)
    {
        for (int i = 0; true; i++)
        {
            if (value <= INT_ROUNDS[i])
            {
                return i;
            }
        }
    }

    /**
     * Get the end offset of an ASCII encoded value.
     *
     * @param value to find the end encoded character offset.
     * @return the offset at which the encoded value will end.
     */
    public static int endOffset(final long value)
    {
        for (int i = 0; true; i++)
        {
            if (value <= LONG_ROUNDS[i])
            {
                return i;
            }
        }
    }

    /**
     * Get the digit value of an ASCII encoded {@code byte}.
     *
     * @param index within the string the value is encoded.
     * @param value of the encoding in ASCII.
     * @return the digit value of the encoded ASCII.
     * @throws AsciiNumberFormatException if the value is not a digit.
     */
    public static int getDigit(final int index, final byte value)
    {
        if (value < 0x30 || value > 0x39)
        {
            throw new AsciiNumberFormatException("'" + ((char)value) + "' is not a valid digit @ " + index);
        }

        return value - 0x30;
    }

    /**
     * Get the digit value of an ASCII encoded {@code char}.
     *
     * @param index within the string the value is encoded.
     * @param value of the encoding in ASCII.
     * @return the digit value of the encoded ASCII.
     * @throws AsciiNumberFormatException if the value is not a digit.
     */
    public static int getDigit(final int index, final char value)
    {
        if (value < 0x30 || value > 0x39)
        {
            throw new AsciiNumberFormatException("'" + value + "' is not a valid digit @ " + index);
        }

        return value - 0x30;
    }

    /**
     * Parse an ASCII encoded int from a {@link CharSequence}.
     *
     * @param cs     to parse.
     * @param index  at which the number begins.
     * @param length of the encoded number in characters.
     * @throws AsciiNumberFormatException if <code>cs</code> is not an int value, or outside of the int range
     * @throws IndexOutOfBoundsException if <code>cs</code> is empty
     * @return the parsed value.
     */
    public static int parseIntAscii(final CharSequence cs, final int index, final int length)
    {
        final int endExclusive = index + length;
        final int first = cs.charAt(index);
        int i = index;

        if (first == MINUS_SIGN && length > 1)
        {
            i++;
        }

        int tally = 0;
        for (; i < endExclusive; i++)
        {
            if (tally > INTEGER_MULT_MAX)
            {
                overflow(cs, index, i);
            }

            tally = (tally * 10) + AsciiEncoding.getDigit(i, cs.charAt(i));
            if (tally < 0 && !contentEquals(MIN_INTEGER_AS_STRING, cs, index, length))
            {
                overflow(cs, index, i);
            }
        }

        if (first == MINUS_SIGN)
        {
            tally = -tally;
        }

        return tally;
    }

    /**
     * Parse an ASCII encoded long from a {@link CharSequence}.
     *
     * @param cs     to parse.
     * @param index  at which the number begins.
     * @param length of the encoded number in characters.
     * @throws AsciiNumberFormatException if <code>cs</code> is not a long value, or outside of the long range
     * @throws IndexOutOfBoundsException if <code>cs</code> is empty
     * @return the parsed value.
     */
    public static long parseLongAscii(final CharSequence cs, final int index, final int length)
    {
        final int endExclusive = index + length;
        final int first = cs.charAt(index);
        int i = index;

        if (first == MINUS_SIGN && length > 1)
        {
            i++;
        }

        long tally = 0;
        for (; i < endExclusive; i++)
        {
            if (tally > LONG_MULT_MAX)
            {
                overflow(cs, index, i);
            }

            tally = (tally * 10) + AsciiEncoding.getDigit(i, cs.charAt(i));
            if (tally < 0 && !contentEquals(MIN_LONG_AS_STRING, cs, index, length))
            {
                overflow(cs, index, i);
            }
        }

        if (first == MINUS_SIGN)
        {
            tally = -tally;
        }

        return tally;
    }

    private static boolean contentEquals(final String str, final CharSequence cs, final int index, final int length)
    {
        if (str.length() != length)
        {
            return false;
        }

        for (int i = 0; i < length; i++)
        {
            if (str.charAt(i) != cs.charAt(index + i))
            {
                return false;
            }

        }

        return true;
    }

    private static void overflow(final CharSequence cs, final int startIndex, final int overflowIndex)
    {
        throw new NumberFormatException("'" + cs + "' - overflow @ " + overflowIndex +
                                        ": " + cs.subSequence(startIndex, overflowIndex) +
                                        '[' + cs.charAt(overflowIndex) + ']');
    }
}
