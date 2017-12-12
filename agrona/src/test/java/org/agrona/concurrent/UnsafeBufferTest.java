/*
 * Copyright 2014-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.concurrent;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class UnsafeBufferTest
{
    private static final byte VALUE = 42;
    private static final int ADJUSTMENT_OFFSET = 3;

    private byte[] wibbleBytes = "Wibble".getBytes(US_ASCII);
    private byte[] wobbleBytes = "Wobble".getBytes(US_ASCII);
    private byte[] wibbleBytes2 = "Wibble2".getBytes(US_ASCII);

    @Test
    public void shouldEqualOnInstance()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);

        assertThat(wibbleBuffer, is(wibbleBuffer));
    }

    @Test
    public void shouldEqualOnContent()
    {
        final UnsafeBuffer wibbleBufferOne = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBufferTwo = new UnsafeBuffer(wibbleBytes.clone());

        assertThat(wibbleBufferOne, is(wibbleBufferTwo));
    }

    @Test
    public void shouldNotEqual()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wobbleBuffer = new UnsafeBuffer(wobbleBytes);

        assertThat(wibbleBuffer, is(not(wobbleBuffer)));
    }

    @Test
    public void shouldEqualOnHashCode()
    {
        final UnsafeBuffer wibbleBufferOne = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBufferTwo = new UnsafeBuffer(wibbleBytes.clone());

        assertThat(wibbleBufferOne.hashCode(), is(wibbleBufferTwo.hashCode()));
    }

    @Test
    public void shouldEqualOnCompareContents()
    {
        final UnsafeBuffer wibbleBufferOne = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBufferTwo = new UnsafeBuffer(wibbleBytes.clone());

        assertThat(wibbleBufferOne.compareTo(wibbleBufferTwo), is(0));
    }

    @Test
    public void shouldCompareLessThanOnContents()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wobbleBuffer = new UnsafeBuffer(wobbleBytes);

        assertThat(wibbleBuffer.compareTo(wobbleBuffer), is(lessThan(0)));
    }

    @Test
    public void shouldCompareGreaterThanOnContents()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wobbleBuffer = new UnsafeBuffer(wobbleBytes);

        assertThat(wobbleBuffer.compareTo(wibbleBuffer), is(greaterThan(0)));
    }

    @Test
    public void shouldCompareLessThanOnContentsOfDifferingCapacity()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(wibbleBytes);
        final UnsafeBuffer wibbleBuffer2 = new UnsafeBuffer(wibbleBytes2);

        assertThat(wibbleBuffer.compareTo(wibbleBuffer2), is(lessThan(0)));
    }

    @Test
    public void shouldExposePositionAtWhichByteArrayGetsWrapped()
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(
            wibbleBytes, ADJUSTMENT_OFFSET, wibbleBytes.length - ADJUSTMENT_OFFSET);

        wibbleBuffer.putByte(0, VALUE);

        assertEquals(VALUE, wibbleBytes[wibbleBuffer.wrapAdjustment()]);
    }

    @Test
    public void shouldExposePositionAtWhichHeapByteBufferGetsWrapped()
    {
        final ByteBuffer wibbleByteBuffer = ByteBuffer.wrap(wibbleBytes);
        shouldExposePositionAtWhichByteBufferGetsWrapped(wibbleByteBuffer);
    }

    @Test
    public void shouldExposePositionAtWhichDirectByteBufferGetsWrapped()
    {
        final ByteBuffer wibbleByteBuffer = ByteBuffer.allocateDirect(wibbleBytes.length);
        shouldExposePositionAtWhichByteBufferGetsWrapped(wibbleByteBuffer);
    }

    private void shouldExposePositionAtWhichByteBufferGetsWrapped(final ByteBuffer byteBuffer)
    {
        final UnsafeBuffer wibbleBuffer = new UnsafeBuffer(
            byteBuffer, ADJUSTMENT_OFFSET, byteBuffer.capacity() - ADJUSTMENT_OFFSET);

        wibbleBuffer.putByte(0, VALUE);

        assertEquals(VALUE, byteBuffer.get(wibbleBuffer.wrapAdjustment()));
    }

    @Test
    public void shouldGetIntegerValuesAtSpecifiedOffset()
    {
        final UnsafeBuffer buffer = newBuffer();
        putAscii(buffer, "123");

        final int value = buffer.parseNaturalIntAscii(1, 3);

        assertEquals(123, value);
    }

    @Test
    public void shouldDecodeNegativeIntegers()
    {
        final UnsafeBuffer buffer = newBuffer();

        putAscii(buffer, "-1");

        final int value = buffer.parseIntAscii(1, 2);

        assertEquals(-1, value);
    }

    @Test
    public void shouldWriteIntZero()
    {
        final UnsafeBuffer buffer = newBuffer();

        final int length = buffer.putIntAscii(1, 0);

        assertEquals(1, length);
        assertContainsString(buffer, "0", 1, 1);
    }

    @Test
    public void shouldWritePositiveIntValues()
    {
        final UnsafeBuffer buffer = newBuffer();

        final int length = buffer.putIntAscii(1, 123);

        assertEquals(3, length);
        assertContainsString(buffer, "123", 1, 3);
    }

    @Test
    public void shouldWriteNegativeIntValues()
    {
        final UnsafeBuffer buffer = newBuffer();

        final int length = buffer.putIntAscii(1, -123);

        assertEquals(4, length);
        assertContainsString(buffer, "-123", 1, 4);
    }

    @Test
    public void shouldWriteMaxIntValue()
    {
        final UnsafeBuffer buffer = newBuffer();

        final int length = buffer.putIntAscii(1, Integer.MAX_VALUE);

        assertContainsString(buffer, String.valueOf(Integer.MAX_VALUE), 1, length);
    }

    @Test
    public void shouldWriteMinIntValue()
    {
        final UnsafeBuffer buffer = newBuffer();

        final int length = buffer.putIntAscii(1, Integer.MIN_VALUE);

        assertContainsString(buffer, String.valueOf(Integer.MIN_VALUE), 1, length);
    }

    private void assertContainsString(
        final UnsafeBuffer buffer, final String value, final int offset, final int length)
    {
        assertEquals(value, buffer.getStringWithoutLengthAscii(offset, length));
    }

    @DataPoints
    public static int[][] valuesAndLengths()
    {
        return new int[][]
        {
            {1, 1},
            {10, 2},
            {100, 3},
            {1000, 4},
            {12, 2},
            {123, 3},
            {2345, 4},
            {9, 1},
            {99, 2},
            {999, 3},
            {9999, 4},
        };
    }

    @Theory
    public void shouldPutNaturalInt(final int[] valueAndLength)
    {
        final int value = valueAndLength[0];

        final UnsafeBuffer buffer = newBuffer();

        final int length = buffer.putNaturalIntAscii(1, value);
        assertValueAndLengthEquals(valueAndLength, buffer, length);
    }

    @Theory
    public void shouldPutNaturalLong(final int[] valueAndLength)
    {
        final long value = valueAndLength[0];

        final UnsafeBuffer buffer = newBuffer();

        final int length = buffer.putNaturalLongAscii(1, value);
        assertValueAndLengthEquals(valueAndLength, buffer, length);
    }

    @Theory
    public void shouldPutInt(final int[] valueAndLength)
    {
        final int value = valueAndLength[0];

        final UnsafeBuffer buffer = newBuffer();

        final int length = buffer.putIntAscii(1, value);
        assertValueAndLengthEquals(valueAndLength, buffer, length);
    }

    @Theory
    public void shouldPutLong(final int[] valueAndLength)
    {
        final int value = valueAndLength[0];

        final UnsafeBuffer buffer = newBuffer();

        final int length = buffer.putLongAscii(1, value);
        assertValueAndLengthEquals(valueAndLength, buffer, length);
    }

    private void assertValueAndLengthEquals(final int[] valueAndLength, final UnsafeBuffer buffer, final int length)
    {
        final int value = valueAndLength[0];
        final int expectedLength = valueAndLength[1];

        assertEquals("for " + Arrays.toString(valueAndLength), expectedLength, length);

        assertEquals(
            "for " + Arrays.toString(valueAndLength),
            String.valueOf(value),
            buffer.getStringWithoutLengthAscii(1, expectedLength));
    }

    private void putAscii(final UnsafeBuffer buffer, final String value)
    {
        buffer.putBytes(1, value.getBytes(US_ASCII));
    }

    private UnsafeBuffer newBuffer()
    {
        return new UnsafeBuffer(new byte[1024]);
    }
}
