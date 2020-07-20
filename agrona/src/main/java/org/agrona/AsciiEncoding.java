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
    public static final byte[] MIN_INTEGER_VALUE = String.valueOf(Integer.MIN_VALUE).getBytes(US_ASCII);
    public static final byte[] MIN_LONG_VALUE = String.valueOf(Long.MIN_VALUE).getBytes(US_ASCII);
    public static final byte MINUS_SIGN = '-';
    public static final byte ZERO = '0';

    private static final byte[] MIN_INT_DIGITS = "2147483648".getBytes(US_ASCII);
    private static final byte[] MAX_INT_DIGITS = "2147483647".getBytes(US_ASCII);

    private static final byte[] MIN_LONG_DIGITS = "9223372036854775808".getBytes(US_ASCII);
    private static final byte[] MAX_LONG_DIGITS = "9223372036854775807".getBytes(US_ASCII);

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
     * @throws AsciiNumberFormatException if <code>cs</code> is not an int value
     * @throws IndexOutOfBoundsException if parsing results in access outside string boundaries, or length is negative
     * @return the parsed value.
     */
    public static int parseIntAscii(final CharSequence cs, final int index, final int length)
    {
        if (length <= 1)
        {
            return parseSingleDigit(cs, index, length);
        }

        final boolean hasSign = cs.charAt(index) == MINUS_SIGN;
        final int endExclusive = index + length;
        int i = hasSign ? index + 1 : index;

        if (length >= 10)
        {
            checkIntLimits(cs, index, length, i, hasSign);
        }

        int tally = 0;
        for (; i < endExclusive; i++)
        {
            tally = (tally * 10) + AsciiEncoding.getDigit(i, cs.charAt(i));
        }

        if (hasSign)
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
     * @throws AsciiNumberFormatException if <code>cs</code> is not a long value
     * @throws IndexOutOfBoundsException if parsing results in access outside string boundaries, or length is negative
     * @return the parsed value.
     */
    public static long parseLongAscii(final CharSequence cs, final int index, final int length)
    {
        if (length <= 1)
        {
            return parseSingleDigit(cs, index, length);
        }

        final boolean hasSign = cs.charAt(index) == MINUS_SIGN;
        final int endExclusive = index + length;
        int i = hasSign ? index + 1 : index;

        if (length >= 19)
        {
            checkLongLimits(cs, index, length, i, hasSign);
        }

        long tally = 0;
        for (; i < endExclusive; i++)
        {
            tally = (tally * 10) + AsciiEncoding.getDigit(i, cs.charAt(i));
        }

        if (hasSign)
        {
            tally = -tally;
        }

        return tally;
    }

    private static int parseSingleDigit(final CharSequence cs, final int index, final int length)
    {
        if (1 == length)
        {
            return AsciiEncoding.getDigit(index, cs.charAt(index));
        }
        else if (0 == length)
        {
            throw new AsciiNumberFormatException("'' is not a valid digit @ " + index);
        }
        else
        {
            throw new IndexOutOfBoundsException("length=" + length);
        }
    }

    private static void checkIntLimits(
        final CharSequence cs, final int index, final int length, final int i, final boolean hasSign)
    {
        if (10 == length)
        {
            if (!hasSign && isOverflow(MAX_INT_DIGITS, cs, i))
            {
                throw new AsciiNumberFormatException("int overflow parsing: " + cs.subSequence(index, index + length));
            }
        }
        else if (11 == length && hasSign)
        {
            if (isOverflow(MIN_INT_DIGITS, cs, i))
            {
                throw new AsciiNumberFormatException("int overflow parsing: " + cs.subSequence(index, index + length));
            }
        }
        else
        {
            throw new AsciiNumberFormatException("int overflow parsing: " + cs.subSequence(index, index + length));
        }
    }

    private static void checkLongLimits(
        final CharSequence cs, final int index, final int length, final int i, final boolean hasSign)
    {
        if (19 == length)
        {
            if (!hasSign && isOverflow(MAX_LONG_DIGITS, cs, i))
            {
                throw new AsciiNumberFormatException("long overflow parsing: " + cs.subSequence(index, index + length));
            }
        }
        else if (20 == length && hasSign)
        {
            if (isOverflow(MIN_LONG_DIGITS, cs, i))
            {
                throw new AsciiNumberFormatException("long overflow parsing: " + cs.subSequence(index, index + length));
            }
        }
        else
        {
            throw new AsciiNumberFormatException("long overflow parsing: " + cs.subSequence(index, index + length));
        }
    }

    private static boolean isOverflow(final byte[] limitDigits, final CharSequence cs, final int index)
    {
        for (int i = 0; i < limitDigits.length; i++)
        {
            final int digit = AsciiEncoding.getDigit(i, cs.charAt(index + i));
            final int limitDigit = limitDigits[i] - 0x30;

            if (digit < limitDigit)
            {
                break;
            }

            if (digit > limitDigit)
            {
                return true;
            }
        }

        return false;
    }
}
