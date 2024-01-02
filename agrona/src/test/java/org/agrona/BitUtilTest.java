/*
 * Copyright 2014-2024 Real Logic Limited.
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
import org.junit.jupiter.params.provider.CsvSource;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.agrona.BitUtil.CACHE_LINE_LENGTH;
import static org.agrona.BitUtil.align;
import static org.agrona.BitUtil.findNextPositivePowerOfTwo;
import static org.agrona.BitUtil.fromHex;
import static org.agrona.BitUtil.fromHexByteArray;
import static org.agrona.BitUtil.toHex;
import static org.agrona.BitUtil.toHexByteArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitUtilTest
{
    @Test
    void shouldReturnNextPositivePowerOfTwo()
    {
        assertThat(findNextPositivePowerOfTwo(MIN_VALUE), is(MIN_VALUE));
        assertThat(findNextPositivePowerOfTwo(MIN_VALUE + 1), is(1));
        assertThat(findNextPositivePowerOfTwo(-1), is(1));
        assertThat(findNextPositivePowerOfTwo(0), is(1));
        assertThat(findNextPositivePowerOfTwo(1), is(1));
        assertThat(findNextPositivePowerOfTwo(2), is(2));
        assertThat(findNextPositivePowerOfTwo(3), is(4));
        assertThat(findNextPositivePowerOfTwo(4), is(4));
        assertThat(findNextPositivePowerOfTwo(31), is(32));
        assertThat(findNextPositivePowerOfTwo(32), is(32));
        assertThat(findNextPositivePowerOfTwo(1 << 30), is(1 << 30));
        assertThat(findNextPositivePowerOfTwo((1 << 30) + 1), is(MIN_VALUE));
    }

    @Test
    void shouldAlignValueToNextMultipleOfAlignment()
    {
        final int alignment = CACHE_LINE_LENGTH;

        assertThat(align(0, alignment), is(0));
        assertThat(align(1, alignment), is(alignment));
        assertThat(align(alignment, alignment), is(alignment));
        assertThat(align(alignment + 1, alignment), is(alignment * 2));

        final int remainder = MAX_VALUE % alignment;
        final int maxMultiple = MAX_VALUE - remainder;

        assertThat(align(maxMultiple, alignment), is(maxMultiple));
        assertThat(align(MAX_VALUE, alignment), is(MIN_VALUE));
    }

    @Test
    void shouldAlignValueToNextMultipleOfAlignmentLong()
    {
        final long alignment = CACHE_LINE_LENGTH;

        assertThat(align(0L, alignment), is(0L));
        assertThat(align(1L, alignment), is(alignment));
        assertThat(align(alignment, alignment), is(alignment));
        assertThat(align(alignment + 1, alignment), is(alignment * 2));

        final long remainder = Long.MAX_VALUE % alignment;
        final long maxMultiple = Long.MAX_VALUE - remainder;

        assertThat(align(maxMultiple, alignment), is(maxMultiple));
        assertThat(align(Long.MAX_VALUE, alignment), is(Long.MIN_VALUE));
    }

    @ParameterizedTest
    @CsvSource({
        "0,64,0",
        "1,1024,1024",
        "1073741824,4096,1073741824",
        "1073741824,1073741824,1073741824",
        "1073741825,1073741824,2147483648",
        "100,8589934592,8589934592",
        "9999999999,8589934592,17179869184" })
    void alignWithLongValues(final long value, final long alignment, final long expected)
    {
        assertEquals(expected, align(value, alignment));
    }

    @Test
    void shouldConvertToHex()
    {
        final byte[] buffer = { 0x01, 0x23, 0x45, 0x69, 0x78, (byte)0xBC, (byte)0xDA, (byte)0xEF, 0x5F };
        final byte[] converted = toHexByteArray(buffer);
        final String hexString = toHex(buffer);

        assertThat(converted[0], is((byte)'0'));
        assertThat(converted[1], is((byte)'1'));
        assertThat(converted[2], is((byte)'2'));
        assertThat(converted[3], is((byte)'3'));
        assertThat(hexString, is("0123456978bcdaef5f"));
    }

    @Test
    void shouldConvertFromHex()
    {
        final String hexString = "0123456978bcdaef5f";

        final byte[] expectedBuffer = { 0x01, 0x23, 0x45, 0x69, 0x78, (byte)0xBC, (byte)0xDA, (byte)0xEF, 0x5F };
        final byte[] fromHexStringBuffer = fromHex(hexString);
        assertArrayEquals(expectedBuffer, fromHexStringBuffer);

        final byte[] expectedHexBuffer = toHexByteArray(expectedBuffer);
        final byte[] fromHexBuffer = fromHexByteArray(expectedHexBuffer);
        assertArrayEquals(expectedBuffer, fromHexBuffer);
    }

    @Test
    void shouldDetectEvenAndOddNumbers()
    {
        assertTrue(BitUtil.isEven(0));
        assertTrue(BitUtil.isEven(2));
        assertTrue(BitUtil.isEven(MIN_VALUE));

        assertFalse(BitUtil.isEven(1));
        assertFalse(BitUtil.isEven(-1));
        assertFalse(BitUtil.isEven(MAX_VALUE));
    }
}
