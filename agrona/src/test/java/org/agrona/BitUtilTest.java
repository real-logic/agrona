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

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.agrona.BitUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitUtilTest
{
    @Test
    public void shouldReturnNextPositivePowerOfTwo()
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
    public void shouldAlignValueToNextMultipleOfAlignment()
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
    public void shouldConvertToHexCorrectly()
    {
        final byte[] buffer = { 0x01, 0x23, 0x45, 0x69, 0x78, (byte)0xBC, (byte)0xDA, (byte)0xEF, 0x5F };
        final byte[] converted = toHexByteArray(buffer);
        final String hexStr = toHex(buffer);

        assertThat(converted[0], is((byte)'0'));
        assertThat(converted[1], is((byte)'1'));
        assertThat(converted[2], is((byte)'2'));
        assertThat(converted[3], is((byte)'3'));
        assertThat(hexStr, is("0123456978bcdaef5f"));
    }

    @Test
    public void shouldDetectEvenAndOddNumbers()
    {
        assertTrue(BitUtil.isEven(0));
        assertTrue(BitUtil.isEven(2));
        assertTrue(BitUtil.isEven(MIN_VALUE));

        assertFalse(BitUtil.isEven(1));
        assertFalse(BitUtil.isEven(-1));
        assertFalse(BitUtil.isEven(MAX_VALUE));
    }
}
