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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.agrona.BufferUtil.NATIVE_BYTE_ORDER;

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
     * Byte value of the minus sign ('{@code +}').
     */
    public static final byte PLUS_SIGN = '+';

    /**
     * Byte value of the zero character ('{@code 0}').
     */
    public static final byte ZERO = '0';

    /**
     * Byte value of the dot character ('{@code .}').
     */
    public static final byte DOT = '.';

    /**
     * Byte value of the lower case letter ('{@code e}').
     */
    public static final byte LOWER_CASE_E = 'e';

    /**
     * Byte value of the upper case letter ('{@code E}').
     */
    public static final byte UPPER_CASE_E = 'E';

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
     * US-ASCII-encoded byte representation of the {@link Double#NaN}.
     */
    public static final byte[] DOUBLE_NAN_VALUE = "NaN".getBytes(US_ASCII);

    /**
     * US-ASCII-encoded byte representation of the {@link Double#POSITIVE_INFINITY}.
     */
    public static final byte[] DOUBLE_INFINITY_VALUE = "Infinity".getBytes(US_ASCII);

    /**
     * US-ASCII-encoded byte representation of the {@link Double#NEGATIVE_INFINITY}.
     */
    public static final byte[] DOUBLE_NEGATIVE_INFINITY_VALUE = "-Infinity".getBytes(US_ASCII);

    /**
     * US-ASCII-encoded byte representation of the {@code 0.0} value.
     */
    public static final byte[] DOUBLE_ZERO_VALUE = "0.0".getBytes(US_ASCII);

    /**
     * US-ASCII-encoded byte representation of the {@code -0.0} value.
     */
    public static final byte[] DOUBLE_NEGATIVE_ZERO_VALUE = "-0.0".getBytes(US_ASCII);

    /**
     * Number of bits in the mantissa of the double value.
     */
    public static final int DOUBLE_MANTISSA_SIZE = 52;

    /**
     * Mask to extract mantissa from a double value.
     */
    public static final long DOUBLE_MANTISSA_MASK = (1L << DOUBLE_MANTISSA_SIZE) - 1;

    /**
     * Number of bits in the exponent of the double value.
     */
    public static final int DOUBLE_EXPONENT_SIZE = 11;

    /**
     * Mask to extract exponent from a double value.
     */
    public static final int DOUBLE_EXPONENT_MASK = (1 << DOUBLE_EXPONENT_SIZE) - 1;

    /**
     * Exponent bias of a double value as per <a href="https://en.wikipedia.org/wiki/IEEE_754">IEEE 754</a> standard.
     */
    public static final int DOUBLE_EXPONENT_BIAS = (1 << DOUBLE_EXPONENT_SIZE - 1) - 1;

    /**
     * Number of bits required to store {@code (2^k/5^q) + 1} in the lookup table.
     *
     * @see <a href="https://dl.acm.org/doi/10.1145/3192366.3192369">ryu algorithm</a>
     */
    public static final int RYU_DOUBLE_B0 = 122;

    /**
     * The lookup table containing pre-computed {@code (2^k/5^q) + 1} values.
     *
     * @see <a href="https://dl.acm.org/doi/10.1145/3192366.3192369">ryu algorithm</a>
     */
    public static final int[][] RYU_DOUBLE_TABLE_GTE = new int[291][4];

    /**
     * Number of bits required to store {@code 5^(−e2 −q)/2^k} in the lookup table.
     *
     * @see <a href="https://dl.acm.org/doi/10.1145/3192366.3192369">ryu algorithm</a>
     */
    public static final int RYU_DOUBLE_B1 = 121;

    /**
     * The lookup table containing pre-computed {@code 5^(−e2 −q)/2^k} values.
     *
     * @see <a href="https://dl.acm.org/doi/10.1145/3192366.3192369">ryu algorithm</a>
     */
    public static final int[][] RYU_DOUBLE_TABLE_LT = new int[326][4];

    /**
     * Powers of ten for {@code int} values.
     */
    public static final int[] INT_POWER_OF_TEN = new int[10];

    /**
     * Powers of ten for {@code long} values.
     */
    public static final long[] LONG_POWER_OF_TEN = new long[19];

    /**
     * Powers of ten for {@code double} values.
     */
    public static final double[] DOUBLE_POWER_OF_TEN = new double[23];

    /**
     * Minimal {@code long} value that contains 19 digits.
     */
    public static final long LONG_MIN_NINETEEN_DIGIT_VALUE = 1_000_000_000_000_000_000L;

    /**
     * Min double exponent for which fast path conversion should be attempted.
     */
    public static final int DOUBLE_MIN_EXPONENT_FAST_PATH = -22;

    /**
     * Max double exponent for which fast path conversion should be attempted.
     */
    public static final int DOUBLE_MAX_EXPONENT_FAST_PATH = 22;

    /**
     * Max double mantissa for which fast path conversion should be attempted.
     */
    public static final long DOUBLE_MAX_MANTISSA_FAST_PATH = 2L << 52;

    /**
     * Smallest decimal exponent for the double exponent.
     */
    public static final int DOUBLE_SMALLEST_DECIMAL_EXPONENT = -342;

    /**
     * Biggest decimal exponent for the double exponent.
     */
    public static final int DOUBLE_BIGGEST_DECIMAL_EXPONENT = 308;

    private static final long[] LONG_POWER_OF_FIVE = new long[28];

    private static final long[] INT_DIGITS = new long[32];
    private static final long[] LONG_DIGITS = new long[64];

    private static final long[] POWER_OF_FIVE_128 =
        new long[2 * (DOUBLE_BIGGEST_DECIMAL_EXPONENT - DOUBLE_SMALLEST_DECIMAL_EXPONENT + 1)];

    private static final long DOUBLE_MULTIPLICATION_PRECISION_MASK = -1L >>> 55;
    private static final int DOUBLE_MIN_EXPONENT_ROUND_TO_EVEN = -4;
    private static final int DOUBLE_MAX_EXPONENT_ROUND_TO_EVEN = 23;
    private static final int DOUBLE_MINIMUM_EXPONENT = -1023;

    private static final MethodHandle UNSIGNED_MULTIPLY_HIGH;
    private static final MethodHandle MULTIPLY_HIGH;

    static
    {
        BigInteger powerOf10 = BigInteger.ONE;
        final BigInteger[] pow10 = new BigInteger[20];
        for (int i = 0; i < DOUBLE_POWER_OF_TEN.length; i++)
        {
            if (i < INT_POWER_OF_TEN.length)
            {
                INT_POWER_OF_TEN[i] = powerOf10.intValueExact();
            }

            if (i < LONG_POWER_OF_TEN.length)
            {
                LONG_POWER_OF_TEN[i] = powerOf10.longValueExact();
            }

            if (i < pow10.length)
            {
                pow10[i] = powerOf10;
            }

            DOUBLE_POWER_OF_TEN[i] = powerOf10.doubleValue();

            powerOf10 = powerOf10.multiply(BigInteger.TEN);
        }

        for (int i = 1; i < 33; i++)
        {
            final double smallest = Math.pow(2, i - 1);
            final long log10 = (long)Math.ceil(Math.log10(smallest));
            final long value = (long)((i < 31 ? (Math.pow(2, 32) - Math.pow(10, log10)) : 0) + (log10 << 32));
            INT_DIGITS[i - 1] = value;
        }

        for (int i = 0; i < 64; i++)
        {
            final int upper = i == 0 ? 0 : ((i * 1262611) >> 22) + 1;
            final long value = ((long)(upper + 1) << 52) - pow10[upper].shiftRight(i / 4).longValue();
            LONG_DIGITS[i] = value;
        }

        final BigInteger five = BigInteger.valueOf(5);
        final BigInteger mask = BigInteger.ONE.shiftLeft(31).subtract(BigInteger.ONE);
        for (int i = 0; i < RYU_DOUBLE_TABLE_LT.length; i++)
        {
            final BigInteger pow = five.pow(i);
            final int log2Power5 = pow.bitLength();

            if (i < LONG_POWER_OF_FIVE.length)
            {
                LONG_POWER_OF_FIVE[i] = pow.longValueExact();
            }

            if (i < RYU_DOUBLE_TABLE_GTE.length)
            {
                final int k = RYU_DOUBLE_B0 + log2Power5 - 1;
                final BigInteger value = BigInteger.ONE.shiftLeft(k).divide(pow).add(BigInteger.ONE);
                for (int j = 0; j < 4; j++)
                {
                    RYU_DOUBLE_TABLE_GTE[i][j] = value.shiftRight((3 - j) * 31).and(mask).intValueExact();
                }
            }

            for (int j = 0; j < 4; j++)
            {
                RYU_DOUBLE_TABLE_LT[i][j] = pow.shiftRight(log2Power5 - RYU_DOUBLE_B1 + (3 - j) * 31).and(mask)
                    .intValueExact();
            }
        }

        int i = 0;
        final BigInteger maxPowerOfFive = BigInteger.ONE.shiftLeft(128);
        final BigInteger oneBelowMaxPowerOfFive = maxPowerOfFive.shiftRight(1);
        final BigInteger mask64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

        for (int q = DOUBLE_SMALLEST_DECIMAL_EXPONENT; q < -27; q++)
        {
            final BigInteger pow5 = five.pow(-q);
            int z = 0;
            while (BigInteger.ONE.shiftLeft(z).compareTo(pow5) < 0)
            {
                z++;
            }
            final int b = 2 * z + 128;
            BigInteger c = BigInteger.ONE.shiftLeft(b).divide(pow5).add(BigInteger.ONE);
            while (c.compareTo(maxPowerOfFive) >= 0)
            {
                c = c.shiftRight(1);
            }

            final BigInteger hi64 = c.shiftRight(64);
            final BigInteger lo64 = c.and(mask64);
            POWER_OF_FIVE_128[i++] = hi64.longValue();
            POWER_OF_FIVE_128[i++] = lo64.longValue();
        }

        for (int q = -27; q < 0; q++)
        {
            final BigInteger pow5 = five.pow(-q);
            int z = 0;
            while (BigInteger.ONE.shiftLeft(z).compareTo(pow5) < 0)
            {
                z++;
            }
            final int b = z + 127;
            final BigInteger c = BigInteger.ONE.shiftLeft(b).divide(pow5).add(BigInteger.ONE);

            final BigInteger hi64 = c.shiftRight(64);
            final BigInteger lo64 = c.and(mask64);
            POWER_OF_FIVE_128[i++] = hi64.longValue();
            POWER_OF_FIVE_128[i++] = lo64.longValue();
        }

        for (int q = 0; q <= DOUBLE_BIGGEST_DECIMAL_EXPONENT; q++)
        {
            BigInteger pow5 = five.pow(q);

            while (pow5.compareTo(oneBelowMaxPowerOfFive) < 0)
            {
                pow5 = pow5.shiftLeft(1);
            }
            while (pow5.compareTo(maxPowerOfFive) >= 0)
            {
                pow5 = pow5.shiftRight(1);
            }

            final BigInteger hi64 = pow5.shiftRight(64);
            final BigInteger lo64 = pow5.and(mask64);
            POWER_OF_FIVE_128[i++] = hi64.longValue();
            POWER_OF_FIVE_128[i++] = lo64.longValue();
        }

        MethodHandle unsignedMultiplyHigh = null;
        MethodHandle multiplyHigh = null;
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final MethodType methodType = MethodType.methodType(long.class, long.class, long.class);
        try
        {
            unsignedMultiplyHigh = lookup.findStatic(Math.class, "unsignedMultiplyHigh", methodType); // JDK 18+
        }
        catch (final NoSuchMethodException | IllegalAccessException ex)
        {
            try
            {
                multiplyHigh = lookup.findStatic(Math.class, "multiplyHigh", methodType); // JDK 9+
            }
            catch (final NoSuchMethodException | IllegalAccessException ex2)
            {
                try
                {
                    multiplyHigh = lookup.findStatic(AsciiEncoding.class, "multiplyHigh0", methodType);
                }
                catch (final NoSuchMethodException | IllegalAccessException ex3)
                {
                    throw new Error(ex3);
                }
            }
        }

        UNSIGNED_MULTIPLY_HIGH = unsignedMultiplyHigh;
        MULTIPLY_HIGH = multiplyHigh;
    }

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
     * @throws AsciiNumberFormatException if {@code length <= 0} or {@code cs} is not an int value.
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
     * @throws AsciiNumberFormatException if {@code length <= 0} or {@code cs} is not a long value.
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
     * Parse an ASCII encoded double from a {@link CharSequence} at a given index. The following formats are supported:
     * <ul>
     *     <li>leading zeroes before the first digit (e.g. {@code 004.5} will yield {@code 4.5})</li>
     *     <li>trailing zeroes after the dot (e.g. {@code 1.230000000} will yield {@code 1.23})</li>
     *     <li>number starting with a dot (e.g. {@code .5} will yield {@code 0.5})</li>
     *     <li>number ending with a dot (e.g. {@code -2.} will yield {@code -2.0})</li>
     *     <li>number in scientific notation (e.g. {@code 1.79e101} will yield {@code 1.79e101})</li>
     * </ul>
     *
     * <p>The implementation is based on the algorithm described in the
     * <a href="https://arxiv.org/pdf/2101.11408.pdf">Daniel Lemire, Number Parsing at a Gigabyte per Second,
     * Software: Practice and Experience 51 (8), 2021. arXiv.2101.11408v3 [cs.DS] 24 Feb 2021</a> paper.
     *
     * @param cs     to parse.
     * @param index  at which the number begins.
     * @param length of the encoded number in characters.
     * @return the parsed value.
     * @throws AsciiNumberFormatException if {@code length <= 0} or {@code cs} is not a double value.
     */
    public static double parseDoubleAscii(final CharSequence cs, final int index, final int length)
    {
        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }

        final byte first = (byte)cs.charAt(index);
        if (MINUS_SIGN == first)
        {
            if (1 == length)
            {
                throwParseDoubleError(cs, index, length);
            }
            if (cs.charAt(index + 1) == DOUBLE_NEGATIVE_INFINITY_VALUE[1]) // -Infinity
            {
                return parseSpecialDoubleValue(
                    cs, index, length, DOUBLE_NEGATIVE_INFINITY_VALUE, Double.NEGATIVE_INFINITY, 2);
            }
            return -parsePositiveDoubleAscii(cs, index, length, index + 1);
        }
        else if (first == DOUBLE_NAN_VALUE[0])
        {
            return parseSpecialDoubleValue(cs, index, length, DOUBLE_NAN_VALUE, Double.NaN, 1);
        }
        else if (first == DOUBLE_INFINITY_VALUE[0])
        {
            return parseSpecialDoubleValue(cs, index, length, DOUBLE_INFINITY_VALUE, Double.POSITIVE_INFINITY, 1);
        }
        else
        {
            return parsePositiveDoubleAscii(cs, index, length, index);
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

    /**
     * Computes number of bits in a power of five number, i.e. {@code 5^power}
     *
     * @param power to which five is raised.
     * @return number of bits in a number that be computed by raising five to the given {@code power}.
     */
    public static int powerOfFiveBitCount(final int power)
    {
        return ((power * 1217359) >>> 19) + 1;
    }

    /**
     * Determines if the given value is multiple to the given power of five, i.e. if the {@code value} is divisible
     * by {@code 5^power}.
     *
     * @param value to check.
     * @param power of five factor.s
     * @return {@code true} if value is divisible by the given power of five.
     */
    public static boolean isMultipleOfPowerOfFive(final long value, final int power)
    {
        // FIXME: Avoid modulo here
        return power < LONG_POWER_OF_FIVE.length && 0L == value % LONG_POWER_OF_FIVE[power];
    }

    /**
     * Performs 128 bit multiplication keeping only the high 64 bits of the result.
     *
     * @param value      to be multiplied.
     * @param multiplier split into four 32 bit values.
     * @param shift      number of bits to shift by.s
     * @return high 64 bits of the multiplications.
     */
    public static long ryuMultiplyHigh128(final long value, final int[] multiplier, final int shift)
    {
        final long mHigh = value >>> 31;
        final long mLow = value & 0x7fffffff;
        final long bits13 = mHigh * multiplier[0];
        final long bits03 = mLow * multiplier[0];
        final long bits12 = mHigh * multiplier[1];
        final long bits02 = mLow * multiplier[1];
        final long bits11 = mHigh * multiplier[2];
        final long bits01 = mLow * multiplier[2];
        final long bits10 = mHigh * multiplier[3];
        final long bits00 = mLow * multiplier[3];

        return ((((((((bits00 >>> 31) + bits01 + bits10) >>> 31) +
            bits02 + bits11) >>> 31) +
            bits03 + bits12) >>> 21) +
            (bits13 << 10)) >>> (shift - 114);
    }

    /**
     * Converts from a decimal form {@code (w * 10^q)} to a binary form of a double value using the algorithm described
     * in the <a href="https://arxiv.org/pdf/2101.11408.pdf">Daniel Lemire, Number Parsing at a Gigabyte per Second,
     * Software: Practice and Experience 51 (8), 2021. arXiv.2101.11408v3 [cs.DS] 24 Feb 2021</a> paper.
     *
     * @param w decimal mantissa.
     * @param q decimal exponent.
     * @return double value or {@link Double#NaN} if conversions fails.
     */
    public static double computeDouble(final long w, final int q)
    {
        if (0 == w || q < DOUBLE_SMALLEST_DECIMAL_EXPONENT)
        {
            return 0;
        }
        else if (q > DOUBLE_BIGGEST_DECIMAL_EXPONENT)
        {
            return Double.POSITIVE_INFINITY;
        }

        final int numLeadingZeros = Long.numberOfLeadingZeros(w);
        final long w1 = w << numLeadingZeros;

        final int powerIndex = (q - DOUBLE_SMALLEST_DECIMAL_EXPONENT) << 1;
        final long pow5 = POWER_OF_FIVE_128[powerIndex];
        long productHi = multiplyHighUnsigned(w1, pow5);
        long productLo = w1 * pow5;
        if (DOUBLE_MULTIPLICATION_PRECISION_MASK == (productHi & DOUBLE_MULTIPLICATION_PRECISION_MASK))
        {
            final long product2Hi = multiplyHighUnsigned(w1, POWER_OF_FIVE_128[powerIndex + 1]);
            productLo += product2Hi;
            if (Long.compareUnsigned(product2Hi, productLo) > 0)
            {
                productHi++;
            }
        }

        if (-1L == productLo && (q < -27 || q > 55))
        {
            return Double.NaN;
        }

        final int power = (((152170 + 65536) * q) >> 16) + 63;
        final int upperBit = (int)(productHi >>> 63);
        long mantissa = productHi >>> (upperBit + 64 - DOUBLE_MANTISSA_SIZE - 3);
        int binaryExponent = power + upperBit - numLeadingZeros - DOUBLE_MINIMUM_EXPONENT;

        if (binaryExponent <= 0) // Subnormal?
        {
            // Here have that answer.power2 <= 0 so -answer.power2 >= 0
            final int numBits = -binaryExponent + 1;
            if (numBits >= 64) // if we have more than 64 bits below the minimum exponent, you have a zero for sure.
            {
                return 0;
            }

            mantissa >>= numBits;
            // Thankfully, we can't have both "round-to-even" and subnormals because
            // "round-to-even" only occurs for powers close to 0.
            mantissa += (mantissa & 1); // round-up
            mantissa >>= 1;
            // There is a weird scenario where we don't have a subnormal but just.
            // Suppose we start with 2.2250738585072013e-308, we end up
            // with 0x3fffffffffffff x 2^-1023-53 which is technically subnormal
            // whereas 0x40000000000000 x 2^-1023-53  is normal. Now, we need to round
            // up 0x3fffffffffffff x 2^-1023-53  and once we do, we are no longer
            // subnormal, but we can only know this after rounding.
            // So we only declare a subnormal if we are smaller than the threshold.
            binaryExponent = mantissa < (1L << DOUBLE_MANTISSA_SIZE) ? 0 : 1;
            return toDouble(mantissa, binaryExponent);
        }

        // usually, we round *up*, but if we fall right in between and and we have an
        // even basis, we need to round down
        // We are only concerned with the cases where 5**q fits in single 64-bit word.
        if (productLo <= 1 && q >= DOUBLE_MIN_EXPONENT_ROUND_TO_EVEN && q <= DOUBLE_MAX_EXPONENT_ROUND_TO_EVEN &&
            (mantissa & 3) == 1)  // we may fall between two floats!
        {
            // To be in-between two floats we need that in doing
            //   answer.mantissa = product.high >> (upperbit + 64 - binary::mantissa_explicit_bits() - 3);
            // ... we dropped out only zeroes. But if this happened, then we can go back!!!
            if ((mantissa << (upperBit + 64 - DOUBLE_MANTISSA_SIZE - 3)) == productHi)
            {
                mantissa &= ~1L; // flip it so that we do not round up
            }
        }

        mantissa += (mantissa & 1); // round-up
        mantissa >>= 1;
        if (mantissa >= DOUBLE_MAX_MANTISSA_FAST_PATH)
        {
            mantissa = 1L << DOUBLE_MANTISSA_SIZE;
            binaryExponent++;
        }

        mantissa &= ~(1L << DOUBLE_MANTISSA_SIZE);
        if (binaryExponent >= DOUBLE_EXPONENT_MASK)
        {
            return Double.POSITIVE_INFINITY;
        }

        return toDouble(mantissa, binaryExponent);
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

    static long multiplyHighUnsigned(final long x, final long y)
    {
        try
        {
            if (null != UNSIGNED_MULTIPLY_HIGH)
            {
                return (long)UNSIGNED_MULTIPLY_HIGH.invokeExact(x, y);
            }
            else
            {
                final long mulHi = (long)MULTIPLY_HIGH.invokeExact(x, y);
                return mulHi + (y & (x >> 63)) + (x & (y >> 63));
            }
        }
        catch (final Throwable ex)
        {
            LangUtil.rethrowUnchecked(ex);
            return 0;
        }
    }

    // Stub for the java.lang.Math.multiplyHigh added in the JDK 9
    private static long multiplyHigh0(final long x, final long y)
    {
        // Use technique from section 8-2 of Henry S. Warren, Jr.,
        // Hacker's Delight (2nd ed.) (Addison Wesley, 2013), 173-174.
        final long x0 = x & 0xFFFFFFFFL;
        final long x1 = x >> 32;
        final long y0 = y & 0xFFFFFFFFL;
        final long y1 = y >> 32;
        final long w0 = x0 * y0;
        final long t = x1 * y0 + (w0 >>> 32);
        long w1 = t & 0xFFFFFFFFL;
        final long w2 = t >> 32;
        w1 = x0 * y1 + w1;
        return x1 * y1 + w2 + (w1 >> 32);
    }

    private static double toDouble(final long mantissa, final int binaryExponent)
    {
        if (binaryExponent < 0)
        {
            return Double.NaN;
        }
        return Double.longBitsToDouble((long)(binaryExponent) << DOUBLE_MANTISSA_SIZE | mantissa);
    }

    private static void throwParseDoubleError(final CharSequence cs, final int index, final int length)
    {
        throw new AsciiNumberFormatException("error parsing double: value=" +
            cs.subSequence(index, length));
    }

    private static double parseSpecialDoubleValue(
        final CharSequence cs,
        final int index,
        final int length,
        final byte[] specialValueBytes,
        final double specialValue,
        final int startCheckAt)
    {
        if (length != specialValueBytes.length)
        {
            throwParseDoubleError(cs, index, length);
        }

        for (int j = startCheckAt; j < specialValueBytes.length; j++)
        {
            if (specialValueBytes[j] != cs.charAt(index + j))
            {
                throwParseDoubleError(cs, index, length);
            }
        }
        return specialValue;
    }

    @SuppressWarnings("MethodLength")
    private static double parsePositiveDoubleAscii(
        final CharSequence cs, final int index, final int length, final int startIndex)
    {
        final int end = index + length;
        int i = startIndex;
        long octet, mantissa = 0;
        while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = readEightBytesLittleEndian(cs, i)))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                octet = Long.reverseBytes(octet);
            }

            mantissa = mantissa * 100_000_000L + parseEightDigitsLittleEndian(octet);
            i += 8;
        }

        byte digit;
        while (i < end && isDigit(digit = (byte)cs.charAt(i)))
        {
            mantissa = (mantissa * 10L) + (digit - 0x30);
            i++;
        }
        final int endOfDigitsIndex = i;
        int digitCount = endOfDigitsIndex - startIndex;
        int exponent = 0;

        if (i < end && DOT == cs.charAt(i))
        {
            i++;
            while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = readEightBytesLittleEndian(cs, i)))
            {
                if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
                {
                    octet = Long.reverseBytes(octet);
                }

                mantissa = mantissa * 100_000_000L + parseEightDigitsLittleEndian(octet);
                i += 8;
            }

            while (i < end && isDigit(digit = (byte)cs.charAt(i)))
            {
                mantissa = (mantissa * 10L) + (digit - 0x30);
                i++;
            }

            exponent = endOfDigitsIndex + 1 - i;
            digitCount -= exponent;
        }

        if (0 == digitCount)
        {
            throwParseDoubleError(cs, index, length);
        }

        int explicitExponent = 0;
        if (i < end)
        {
            byte b = (byte)cs.charAt(i);
            if (LOWER_CASE_E == b || UPPER_CASE_E == b)
            {
                i++;
                boolean negativeExponent = false;
                if (i < end)
                {
                    b = (byte)cs.charAt(i);
                    if (MINUS_SIGN == b)
                    {
                        negativeExponent = true;
                        i++;
                    }
                    else if (PLUS_SIGN == b)
                    {
                        i++;
                    }

                    while (i < end && isDigit(b = (byte)cs.charAt(i)))
                    {
                        if (explicitExponent < 0x10000000)
                        {
                            explicitExponent = 10 * explicitExponent + (b - 0x30);
                        }
                        i++;
                    }

                    if (negativeExponent)
                    {
                        explicitExponent = -explicitExponent;
                    }
                    exponent += explicitExponent;
                }
            }
        }

        if (i != end)
        {
            throwParseDoubleError(cs, index, length);
        }

        boolean tooManyDigits = false;
        if (digitCount > LONG_MAX_DIGITS || LONG_MAX_DIGITS == digitCount && mantissa < 0L)
        {
            i = startIndex;
            while (i < end)
            {
                final byte b = (byte)cs.charAt(i);
                if (ZERO == b)
                {
                    digitCount--;
                }
                else if (DOT != b)
                {
                    break;
                }
                i++;
            }

            if (digitCount > LONG_MAX_DIGITS || LONG_MAX_DIGITS == digitCount && mantissa < 0L)
            {
                tooManyDigits = true;
                mantissa = 0;
                i = startIndex;
                while (i < endOfDigitsIndex && Long.compareUnsigned(mantissa, LONG_MIN_NINETEEN_DIGIT_VALUE) < 0)
                {
                    mantissa = (mantissa * 10L) + (cs.charAt(i) - 0x30);
                    i++;
                }

                if (Long.compareUnsigned(mantissa, LONG_MIN_NINETEEN_DIGIT_VALUE) >= 0)
                {
                    exponent = endOfDigitsIndex - i + explicitExponent;
                }
                else
                {
                    final int exponentStart = endOfDigitsIndex + 1;
                    i = exponentStart;
                    while (i < end && Long.compareUnsigned(mantissa, LONG_MIN_NINETEEN_DIGIT_VALUE) < 0)
                    {
                        mantissa = (mantissa * 10L) + (cs.charAt(i) - 0x30);
                        i++;
                    }
                    exponent = exponentStart - i + explicitExponent;
                }
            }
        }

        if (exponent >= DOUBLE_MIN_EXPONENT_FAST_PATH && exponent <= DOUBLE_MAX_EXPONENT_FAST_PATH &&
            mantissa <= DOUBLE_MAX_MANTISSA_FAST_PATH && !tooManyDigits)
        {
            if (exponent < 0)
            {
                return (double)mantissa / DOUBLE_POWER_OF_TEN[-exponent];
            }
            else
            {
                return (double)mantissa * DOUBLE_POWER_OF_TEN[exponent];
            }
        }

        double result = computeDouble(mantissa, exponent);
        if (tooManyDigits && !Double.isNaN(result))
        {
            final double m1 = computeDouble(mantissa + 1, exponent);
            if (Double.compare(result, m1) == 0)
            {
                return result;
            }
            else
            {
                result = Double.NaN;
            }
        }

        if (Double.isNaN(result)) // Fallback
        {
            return Double.parseDouble(
                cs.subSequence(startIndex, index == startIndex ? length : length - 1).toString());
        }

        return result;
    }
}
