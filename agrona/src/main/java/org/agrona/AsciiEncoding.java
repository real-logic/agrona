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

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Helper for dealing with ASCII encoding of numbers.
 */
public final class AsciiEncoding
{
    /**
     * Maximum number of digits in a US-ASCII-encoded int.
     */
    public static final int INT_MAX_DIGITS = 10;

    /**
     * Maximum number of digits in a US-ASCII-encoded long.
     */
    public static final int LONG_MAX_DIGITS = 19;

    /**
     * A absolute value of the {@link Integer#MIN_VALUE} as long.
     */
    public static final long INTEGER_ABSOLUTE_MIN_VALUE = Math.abs((long)Integer.MIN_VALUE);

    /**
     * US-ASCII-encoded byte representation of the {@link Integer#MIN_VALUE}.
     */
    public static final byte[] MIN_INTEGER_VALUE = String.valueOf(Integer.MIN_VALUE).getBytes(US_ASCII);

    /**
     * US-ASCII-encoded byte representation of the {@link Integer#MAX_VALUE}.
     */
    public static final byte[] MAX_INTEGER_VALUE = String.valueOf(Integer.MAX_VALUE).getBytes(US_ASCII);

    /**
     * US-ASCII-encoded byte representation of the {@link Long#MIN_VALUE}.
     */
    public static final byte[] MIN_LONG_VALUE = String.valueOf(Long.MIN_VALUE).getBytes(US_ASCII);

    /**
     * US-ASCII-encoded byte representation of the {@link Long#MAX_VALUE}.
     */
    public static final byte[] MAX_LONG_VALUE = String.valueOf(Long.MAX_VALUE).getBytes(US_ASCII);

    /**
     * Byte value of the minus sign ('{@code -}').
     */
    public static final byte MINUS_SIGN = '-';

    /**
     * Byte value of zero character ('{@code 0}').
     */
    public static final byte ZERO = '0';

    /**
     * Lookup table used for encoding ints/longs as ASCII characters.
     */
    public static final byte[] ASCII_DIGITS = new byte[]
    {
        '0', '0', '0', '1', '0', '2', '0', '3', '0', '4', '0', '5', '0', '6', '0', '7', '0', '8', '0', '9',
        '1', '0', '1', '1', '1', '2', '1', '3', '1', '4', '1', '5', '1', '6', '1', '7', '1', '8', '1', '9',
        '2', '0', '2', '1', '2', '2', '2', '3', '2', '4', '2', '5', '2', '6', '2', '7', '2', '8', '2', '9',
        '3', '0', '3', '1', '3', '2', '3', '3', '3', '4', '3', '5', '3', '6', '3', '7', '3', '8', '3', '9',
        '4', '0', '4', '1', '4', '2', '4', '3', '4', '4', '4', '5', '4', '6', '4', '7', '4', '8', '4', '9',
        '5', '0', '5', '1', '5', '2', '5', '3', '5', '4', '5', '5', '5', '6', '5', '7', '5', '8', '5', '9',
        '6', '0', '6', '1', '6', '2', '6', '3', '6', '4', '6', '5', '6', '6', '6', '7', '6', '8', '6', '9',
        '7', '0', '7', '1', '7', '2', '7', '3', '7', '4', '7', '5', '7', '6', '7', '7', '7', '8', '7', '9',
        '8', '0', '8', '1', '8', '2', '8', '3', '8', '4', '8', '5', '8', '6', '8', '7', '8', '8', '8', '9',
        '9', '0', '9', '1', '9', '2', '9', '3', '9', '4', '9', '5', '9', '6', '9', '7', '9', '8', '9', '9'
    };

    private static final byte[] MIN_INT_DIGITS = "2147483648".getBytes(US_ASCII);
    private static final byte[] MAX_INT_DIGITS = "2147483647".getBytes(US_ASCII);

    private static final byte[] MIN_LONG_DIGITS = "9223372036854775808".getBytes(US_ASCII);
    private static final byte[] MAX_LONG_DIGITS = "9223372036854775807".getBytes(US_ASCII);

    private static final long[] INT_DIGITS =
    {
        4294967295L, 8589934582L, 8589934582L, 8589934582L, 12884901788L, 12884901788L, 12884901788L, 17179868184L,
        17179868184L, 17179868184L, 21474826480L, 21474826480L, 21474826480L, 21474826480L, 25769703776L, 25769703776L,
        25769703776L, 30063771072L, 30063771072L, 30063771072L, 34349738368L, 34349738368L, 34349738368L, 34349738368L,
        38554705664L, 38554705664L, 38554705664L, 41949672960L, 41949672960L, 41949672960L, 42949672960L, 42949672960L
    };

    private static final long[] LONG_DIGITS =
    {
        4503599627370495L, 9007199254740982L, 9007199254740982L, 9007199254740982L, 13510798882111438L,
        13510798882111438L, 13510798882111438L, 18014398509481484L, 18014398509481734L, 18014398509481734L,
        22517998136849980L, 22517998136849980L, 22517998136851230L, 22517998136851230L, 27021597764210476L,
        27021597764210476L, 27021597764216726L, 31525197391530972L, 31525197391530972L, 31525197391530972L,
        36028797018651468L, 36028797018651468L, 36028797018651468L, 36028797018651468L, 40532396644771964L,
        40532396644771964L, 40532396644771964L, 45035996258079960L, 45035996265892460L, 45035996265892460L,
        49539595822950456L, 49539595822950456L, 49539595862012956L, 49539595862012956L, 54043195137820952L,
        54043195137820952L, 54043195333133452L, 58546793202691448L, 58546793202691448L, 58546793202691448L,
        63050385017561944L, 63050385017561944L, 63050385017561944L, 63050385017561944L, 67553945582432440L,
        67553945582432440L, 67553945582432440L, 72057105756677936L, 72057349897302936L, 72057349897302936L,
        76558752259048432L, 76558752259048432L, 76559972962173432L, 76559972962173432L, 81052586261418928L,
        81052586261418928L, 81058689777043928L, 85507357763789424L, 85507357763789424L, 85507357763789424L,
        89766816766159920L, 89766816766159920L, 89766816766159920L, 89766816766159920L
    };

    private AsciiEncoding()
    {
    }

    /**
     * Calling this method is equivalent of doing:
     * <pre>
     * {@code digitCount(value) - 1}
     * </pre>
     *
     * @param value to find the end encoded character offset.
     * @return the offset at which the encoded value will end.
     * @see #digitCount(int)
     * @deprecated Use {@link #digitCount(int)} instead.
     */
    @Deprecated
    public static int endOffset(final int value)
    {
        return digitCount(value) - 1;
    }

    /**
     * Calling this method is equivalent of doing:
     * <pre>
     * {@code digitCount(value) - 1}
     * </pre>
     *
     * @param value to find the end encoded character offset.
     * @return the offset at which the encoded value will end.
     * @see #digitCount(long)
     * @deprecated Use {@link #digitCount(long)} instead.
     */
    @Deprecated
    public static int endOffset(final long value)
    {
        return digitCount(value) - 1;
    }

    /**
     * Count number of digits in a positive {@code int} value.
     *
     * <p>Implementation is based on the Kendall Willets' idea as presented in the
     * <a href="https://lemire.me/blog/2021/06/03/computing-the-number-of-digits-of-an-integer-even-faster/"
     * target="_blank">Computing the number of digits of an integer even faster</a> blog post.
     *
     * <p>
     * Use {@code org.agrona.AsciiEncodingTest#printDigitCountIntTable()} to regenerate lookup table.
     *
     * @param value to count number of digits int.
     * @return number of digits in a number, e.g. if input value is {@code 123} then the result will be {@code 3}.
     */
    public static int digitCount(final int value)
    {
        return (int)((value + INT_DIGITS[31 - Integer.numberOfLeadingZeros(value | 1)]) >> 32);
    }

    /**
     * Count number of digits in a positive {@code long} value.
     *
     * <p>Implementation is based on the Kendall Willets' idea as presented in the
     * <a href="https://lemire.me/blog/2021/06/03/computing-the-number-of-digits-of-an-integer-even-faster/"
     * target="_blank">Computing the number of digits of an integer even faster</a> blog post.
     *
     * <p>
     * Use {@code org.agrona.AsciiEncodingTest#printDigitCountLongTable()} to regenerate lookup table.
     *
     * @param value to count number of digits int.
     * @return number of digits in a number, e.g. if input value is {@code 12345678909876} then the result will be
     * {@code 14}.
     */
    public static int digitCount(final long value)
    {
        final int floorLog2 = 63 ^ Long.numberOfLeadingZeros(value);
        return (int)((LONG_DIGITS[floorLog2] + (value >> (floorLog2 >> 2))) >> 52);
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
     * @return the parsed value.
     * @throws AsciiNumberFormatException if <code>cs</code> is not an int value
     * @throws IndexOutOfBoundsException  if parsing results in access outside string boundaries, or length is negative
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
     * @return the parsed value.
     * @throws AsciiNumberFormatException if <code>cs</code> is not a long value
     * @throws IndexOutOfBoundsException  if parsing results in access outside string boundaries, or length is negative
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

    /**
     * Checks if the provided {@code value} represents an ASCII-encoded number which contains exactly four digits.
     *
     * @param value four ASCII-encoded bytes to check.
     * @return {@code true} if the {@code value} is an ASCII-encoded number with four digits in it.
     */
    public static boolean isFourDigitsAsciiEncodedNumber(final int value)
    {
        return 0 == ((((value + 0x46464646) | (value - 0x30303030)) & 0x80808080));
    }

    /**
     * Parses a four-digit number out of an ASCII-encoded value assuming little-endian byte order.
     *
     * @param bytes ASCII-encoded value in little-endian byte order.
     * @return {@code int} value with four digits.
     */
    public static int parseFourDigitsLittleEndian(final int bytes)
    {
        int val = bytes & 0x0F0F0F0F;
        val = (val * 10) + (val >> 8);
        return ((val & 0x00FF00FF) * 6553601) >> 16;
    }

    /**
     * Checks if the provided {@code value} represents an ASCII-encoded number which contains exactly eight digits.
     *
     * @param value eoght ASCII-encoded bytes to check.
     * @return {@code true} if the {@code value} is an ASCII-encoded number with eight digits in it.
     */
    public static boolean isEightDigitAsciiEncodedNumber(final long value)
    {
        return 0L == ((((value + 0x4646464646464646L) | (value - 0x3030303030303030L)) & 0x8080808080808080L));
    }

    /**
     * Parses an eight-digit number out of an ASCII-encoded value assuming little-endian byte order.
     *
     * @param bytes ASCII-encoded value in little-endian byte order.
     * @return {@code int} value with eight digits.
     */
    public static int parseEightDigitsLittleEndian(final long bytes)
    {
        long val = bytes - 0x3030303030303030L;
        val = (val * 10) + (val >> 8);
        val = (((val & 0x000000FF000000FFL) * 0x000F424000000064L) +
            (((val >> 16) & 0x000000FF000000FFL) * 0x0000271000000001L)) >> 32;
        return (int)val;
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
