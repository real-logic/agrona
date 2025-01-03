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

import java.math.BigInteger;

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
     * An absolute value of the {@link Integer#MIN_VALUE} as long.
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

    /**
     * {@link Long#MAX_VALUE} split into components by 8 digits max.
     */
    public static final int[] LONG_MAX_VALUE_DIGITS = new int[]{ 92233720, 36854775, 807 };

    /**
     * {@link Long#MIN_VALUE} split into components by 8 digits max.
     */
    public static final int[] LONG_MIN_VALUE_DIGITS = new int[]{ 92233720, 36854775, 808 };

    /**
     * Power of ten for int values.
     */
    public static final int[] INT_POW_10 =
    {
        1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000
    };

    /**
     * Power of ten for long values.
     */
    public static final long[] LONG_POW_10 =
    {
        1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L,
        10_000_000_000L, 100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L, 100_000_000_000_000L,
        1_000_000_000_000_000L, 10_000_000_000_000_000L, 100_000_000_000_000_000L, 1_000_000_000_000_000_000L
    };

    private static final long[] INT_DIGITS = new long[32];

    private static final long[] LONG_DIGITS = new long[64];

    static
    {
        for (int i = 1; i < 33; i++)
        {
            final int smallest = 1 << (i - 1);
            final long smallestLog10 = (long)Math.ceil(Math.log10(smallest) / Math.log10(10));
            if (1 == i)
            {
                INT_DIGITS[i - 1] = 1L << 32;
            }
            else if (i < 31)
            {
                INT_DIGITS[i - 1] = (1L << 32) - LONG_POW_10[(int)smallestLog10] + (smallestLog10 << 32);
            }
            else
            {
                INT_DIGITS[i - 1] = smallestLog10 << 32;
            }
        }

        final BigInteger tenToNineteen = BigInteger.TEN.pow(19);
        for (int i = 0; i < 64; i++)
        {
            if (0 == i)
            {
                LONG_DIGITS[i] = 1L << 52;
            }
            else
            {
                final int upper = ((i * 1262611) >> 22) + 1;
                final long correction = upper < LONG_MAX_DIGITS ? LONG_POW_10[upper] >> (i >> 2) :
                    tenToNineteen.shiftRight(i >> 2).longValueExact();
                final long value = ((long)(upper + 1) << 52) - correction;
                LONG_DIGITS[i] = value;
            }
        }
    }

    private AsciiEncoding()
    {
    }

    /**
     * Calling this method is equivalent of doing the following.
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
     * Calling this method is equivalent of doing the following.
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
        final int floorLog2 = 63 ^ Long.numberOfLeadingZeros(value | 1);
        return (int)((LONG_DIGITS[floorLog2] + (value >> (floorLog2 >> 2))) >> 52);
    }

    /**
     * Check if the {@code value} is an ASCII-encoded digit.
     *
     * @param value ti be checked.
     * @return {@code true} if the {@code value} is an ASCII-encoded digit.
     */
    public static boolean isDigit(final byte value)
    {
        return value >= 0x30 && value <= 0x39;
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
     * @throws AsciiNumberFormatException if {@code length <= 0} or {@code cs} is not an int value
     */
    public static int parseIntAscii(final CharSequence cs, final int index, final int length)
    {
        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }

        final boolean negative = MINUS_SIGN == cs.charAt(index);
        int i = index;
        if (negative)
        {
            i++;
            if (1 == length)
            {
                throwParseIntError(cs, index, length);
            }
        }

        final int end = index + length;
        if (end - i < INT_MAX_DIGITS)
        {
            final int tally = parsePositiveIntAscii(cs, index, length, i, end);
            return negative ? -tally : tally;
        }
        else
        {
            final long tally = parsePositiveIntAsciiOverflowCheck(cs, index, length, i, end);
            if (tally > INTEGER_ABSOLUTE_MIN_VALUE || INTEGER_ABSOLUTE_MIN_VALUE == tally && !negative)
            {
                throwParseIntOverflowError(cs, index, length);
            }
            return (int)(negative ? -tally : tally);
        }
    }

    /**
     * Parse an ASCII encoded long from a {@link CharSequence}.
     *
     * @param cs     to parse.
     * @param index  at which the number begins.
     * @param length of the encoded number in characters.
     * @return the parsed value.
     * @throws AsciiNumberFormatException if {@code length <= 0} or {@code cs} is not a long value
     */
    public static long parseLongAscii(final CharSequence cs, final int index, final int length)
    {
        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }

        final boolean negative = MINUS_SIGN == cs.charAt(index);
        int i = index;
        if (negative)
        {
            i++;
            if (1 == length)
            {
                throwParseLongError(cs, index, length);
            }
        }

        final int end = index + length;
        if (end - i < LONG_MAX_DIGITS)
        {
            final long tally = parsePositiveLongAscii(cs, index, length, i, end);
            return negative ? -tally : tally;
        }
        else if (negative)
        {
            return -parseLongAsciiOverflowCheck(cs, index, length, LONG_MIN_VALUE_DIGITS, i, end);
        }
        else
        {
            return parseLongAsciiOverflowCheck(cs, index, length, LONG_MAX_VALUE_DIGITS, i, end);
        }
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

    private static int parsePositiveIntAscii(
        final CharSequence cs, final int index, final int length, final int startIndex, final int end)
    {
        int i = startIndex;
        int tally = 0, quartet;
        while ((end - i) >= 4 && isFourDigitsAsciiEncodedNumber(quartet = readFourBytesLittleEndian(cs, i)))
        {
            tally = (tally * 10_000) + parseFourDigitsLittleEndian(quartet);
            i += 4;
        }

        byte digit;
        while (i < end && isDigit(digit = (byte)cs.charAt(i)))
        {
            tally = (tally * 10) + (digit - 0x30);
            i++;
        }

        if (i != end)
        {
            throwParseIntError(cs, index, length);
        }

        return tally;
    }

    private static long parsePositiveIntAsciiOverflowCheck(
        final CharSequence cs, final int index, final int length, final int startIndex, final int end)
    {
        if ((end - startIndex) > INT_MAX_DIGITS)
        {
            throwParseIntOverflowError(cs, index, length);
        }

        int i = startIndex;
        long tally = 0;
        final long octet = readEightBytesLittleEndian(cs, i);
        if (isEightDigitAsciiEncodedNumber(octet))
        {
            tally = parseEightDigitsLittleEndian(octet);
            i += 8;

            byte digit;
            while (i < end && isDigit(digit = (byte)cs.charAt(i)))
            {
                tally = (tally * 10L) + (digit - 0x30);
                i++;
            }
        }

        if (i != end)
        {
            throwParseIntError(cs, index, length);
        }

        return tally;
    }

    private static void throwParseIntError(final CharSequence cs, final int index, final int length)
    {
        throw new AsciiNumberFormatException("error parsing int: " + cs.subSequence(index, index + length));
    }

    private static void throwParseIntOverflowError(final CharSequence cs, final int index, final int length)
    {
        throw new AsciiNumberFormatException("int overflow parsing: " + cs.subSequence(index, index + length));
    }

    private static long parsePositiveLongAscii(
        final CharSequence cs, final int index, final int length, final int startIndex, final int end)
    {
        int i = startIndex;
        long tally = 0, octet;
        while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = readEightBytesLittleEndian(cs, i)))
        {
            tally = (tally * 100_000_000L) + parseEightDigitsLittleEndian(octet);
            i += 8;
        }

        int quartet;
        while ((end - i) >= 4 && isFourDigitsAsciiEncodedNumber(quartet = readFourBytesLittleEndian(cs, i)))
        {
            tally = (tally * 10_000L) + parseFourDigitsLittleEndian(quartet);
            i += 4;
        }

        byte digit;
        while (i < end && isDigit(digit = (byte)cs.charAt(i)))
        {
            tally = (tally * 10) + (digit - 0x30);
            i++;
        }

        if (i != end)
        {
            throwParseLongError(cs, index, length);
        }

        return tally;
    }

    private static long parseLongAsciiOverflowCheck(
        final CharSequence cs,
        final int index,
        final int length,
        final int[] maxValue,
        final int startIndex,
        final int end)
    {
        if ((end - startIndex) > LONG_MAX_DIGITS)
        {
            throwParseLongOverflowError(cs, index, length);
        }

        int i = startIndex, k = 0;
        boolean checkOverflow = true;
        long tally = 0, octet;
        while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = readEightBytesLittleEndian(cs, i)))
        {
            final int eightDigits = parseEightDigitsLittleEndian(octet);
            if (checkOverflow)
            {
                if (eightDigits > maxValue[k])
                {
                    throwParseLongOverflowError(cs, index, length);
                }
                else if (eightDigits < maxValue[k])
                {
                    checkOverflow = false;
                }
                k++;
            }
            tally = (tally * 100_000_000L) + eightDigits;
            i += 8;
        }

        byte digit;
        int lastDigits = 0;
        while (i < end && isDigit(digit = (byte)cs.charAt(i)))
        {
            lastDigits = (lastDigits * 10) + (digit - 0x30);
            i++;
        }

        if (i != end)
        {
            throwParseLongError(cs, index, length);
        }
        else if (checkOverflow && lastDigits > maxValue[k])
        {
            throwParseLongOverflowError(cs, index, length);
        }

        return (tally * 1000L) + lastDigits;
    }

    private static void throwParseLongError(final CharSequence cs, final int index, final int length)
    {
        throw new AsciiNumberFormatException("error parsing long: " + cs.subSequence(index, index + length));
    }

    private static void throwParseLongOverflowError(final CharSequence cs, final int index, final int length)
    {
        throw new AsciiNumberFormatException("long overflow parsing: " + cs.subSequence(index, index + length));
    }

    private static int readFourBytesLittleEndian(final CharSequence cs, final int index)
    {
        return cs.charAt(index + 3) << 24 |
            cs.charAt(index + 2) << 16 |
            cs.charAt(index + 1) << 8 |
            cs.charAt(index);
    }

    private static long readEightBytesLittleEndian(final CharSequence cs, final int index)
    {
        return (long)cs.charAt(index + 7) << 56 |
            (long)cs.charAt(index + 6) << 48 |
            (long)cs.charAt(index + 5) << 40 |
            (long)cs.charAt(index + 4) << 32 |
            (long)cs.charAt(index + 3) << 24 |
            (long)cs.charAt(index + 2) << 16 |
            cs.charAt(index + 1) << 8 |
            cs.charAt(index);
    }
}
