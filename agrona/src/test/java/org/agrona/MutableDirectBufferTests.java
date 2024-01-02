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

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Base class containing a common set of tests for {@link MutableDirectBuffer} implementations.
 */
public abstract class MutableDirectBufferTests
{
    private static final int ROUND_TRIP_ITERATIONS = 10_000_000;

    /**
     * Allocate new buffer with the specified capacity.
     *
     * @param capacity to allocate.
     * @return new buffer.
     */
    protected abstract MutableDirectBuffer newBuffer(int capacity);

    @Test
    void shouldThrowExceptionWhenOutOfBounds()
    {
        final MutableDirectBuffer buffer = newBuffer(10);
        final int index = buffer.capacity();
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getByte(index));
    }

    @Test
    void shouldPutBytes()
    {
        final int index = 5;
        final String value = "Hello World";
        final MutableDirectBuffer buffer = newBuffer(20);

        buffer.putBytes(index, value.getBytes(US_ASCII));

        assertEquals(value, buffer.getStringWithoutLengthAscii(index, value.length()));
    }

    @Test
    void shouldPutBytesFromByteBuffer()
    {
        final int destIndex = 2;
        final int srcIndex = 5;
        final String value = "Error converting parameter at index 0";
        final int payloadLength = value.length() - srcIndex;
        final ByteBuffer srcBuffer = ByteBuffer.allocate(value.length());
        srcBuffer.put(value.getBytes(US_ASCII), 0, value.length());
        srcBuffer.flip();
        final MutableDirectBuffer buffer = newBuffer(100);

        buffer.putBytes(destIndex, srcBuffer, srcIndex, payloadLength);

        assertEquals(
            value.substring(srcIndex), buffer.getStringWithoutLengthAscii(destIndex, payloadLength));
    }

    @ParameterizedTest
    @MethodSource("valuesAndLengths")
    void shouldPutNaturalFromEnd(final int value, final int length)
    {
        final MutableDirectBuffer buffer = newBuffer(8 * 1024);

        final int start = buffer.putNaturalIntAsciiFromEnd(value, length);
        final Supplier<String> messageSupplier = () -> "value=" + value + " length=" + length;
        assertEquals(0, start, messageSupplier);

        assertEquals(String.valueOf(value), buffer.getStringWithoutLengthAscii(0, length), messageSupplier);
    }

    @Test
    void putIntAsciiRoundTrip()
    {
        final int index = 4;
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final int value = ThreadLocalRandom.current().nextInt();
            final int length = buffer.putIntAscii(index, value);
            final int parsedValue = buffer.parseIntAscii(index, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putLongAsciiRoundTrip()
    {
        final int index = 16;
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final long value = ThreadLocalRandom.current().nextLong();
            final int length = buffer.putLongAscii(index, value);
            final long parsedValue = buffer.parseLongAscii(index, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putNaturalIntAsciiRoundTrip()
    {
        final int index = 8;
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final int value = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
            final int length = buffer.putNaturalIntAscii(index, value);
            final int parsedValue = buffer.parseNaturalIntAscii(index, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putNaturalLongAsciiRoundTrip()
    {
        final int index = 12;
        final MutableDirectBuffer buffer = newBuffer(64);

        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final long value = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
            final int length = buffer.putNaturalLongAscii(index, value);
            final long parsedValue = buffer.parseNaturalLongAscii(index, length);
            assertEquals(value, parsedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 11, 64, 1011 })
    void setMemory(final int length)
    {
        final int index = 2;
        final byte value = (byte)11;
        final MutableDirectBuffer buffer = newBuffer(2 * index + length);

        buffer.setMemory(index, length, value);

        assertEquals(0, buffer.getByte(0));
        assertEquals(0, buffer.getByte(1));
        assertEquals(0, buffer.getByte(index + length));
        assertEquals(0, buffer.getByte(index + length + 1));
        for (int i = 0; i < length; i++)
        {
            assertEquals(value, buffer.getByte(index + i));
        }
    }

    @Test
    void putLongAsciiShouldHandleEightDigitNumber()
    {
        final int index = 0;
        final MutableDirectBuffer buffer = newBuffer(16);

        final int length = buffer.putLongAscii(index, 87654321);
        assertEquals(8, length);

        assertEquals("87654321", buffer.getStringWithoutLengthAscii(index, length));
    }

    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, 0, Long.MAX_VALUE })
    void putLongAsciiShouldEncodeBoundaryValues(final long value)
    {
        final String encodedValue = Long.toString(value);
        final int index = 4;
        final MutableDirectBuffer buffer = newBuffer(32);

        final int length = buffer.putLongAscii(index, value);

        assertEquals(encodedValue.length(), length);
        assertEquals(encodedValue, buffer.getStringWithoutLengthAscii(index, length));
        assertEquals(value, buffer.parseLongAscii(index, length));
    }

    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, 0, Integer.MAX_VALUE })
    void putIntAsciiShouldEncodeBoundaryValues(final int value)
    {
        final String encodedValue = Integer.toString(value);
        final int index = 3;
        final MutableDirectBuffer buffer = newBuffer(32);

        final int length = buffer.putIntAscii(index, value);

        assertEquals(encodedValue.length(), length);
        assertEquals(encodedValue, buffer.getStringWithoutLengthAscii(index, length));
        assertEquals(value, buffer.parseIntAscii(index, length));
    }

    @ParameterizedTest
    @MethodSource("nonParsableIntValues")
    void parseIntAsciiThrowsAsciiNumberFormatExceptionIfValueContainsInvalidCharacters(final String value)
    {
        final int index = 2;
        final MutableDirectBuffer buffer = newBuffer(16);
        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseIntAscii(index, length));
        assertEquals("error parsing int: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("nonParsableLongValues")
    void parseLongAsciiThrowsAsciiNumberFormatExceptionIfValueContainsInvalidCharacters(
        final String value, final int baseIndex)
    {
        final int index = 7;
        final MutableDirectBuffer buffer = newBuffer(32);
        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseLongAscii(index, length));
        assertEquals("error parsing long: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("nonParsableIntValues")
    void parseNaturalIntAsciiThrowsAsciiNumberFormatExceptionIfValueContainsInvalidCharacters(final String value)
    {
        final int index = 1;
        final MutableDirectBuffer buffer = newBuffer(16);
        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseNaturalIntAscii(index, length));
        assertEquals("error parsing int: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("nonParsableLongValues")
    void parseNaturalLongAsciiThrowsAsciiNumberFormatExceptionIfValueContainsInvalidCharacters(
        final String value, final int baseIndex)
    {
        final int index = 8;
        final MutableDirectBuffer buffer = newBuffer(32);
        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseNaturalLongAscii(index, length));
        assertEquals("error parsing long: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "-5547483650",
        "-2147483649",
        "2147483648",
        "2147483649",
        "9999999991",
        "-999999999999999999",
        "12345678901234567890" })
    void parseIntAsciiShouldThrowAsciiNumberFormatExceptionIfValueIsOutOfRange(final String value)
    {
        final int index = 4;
        final MutableDirectBuffer buffer = newBuffer(32);

        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseIntAscii(index, length));
        assertEquals("int overflow parsing: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "-9223372036854775810",
        "-9223372036854775809",
        "9223372036854775808",
        "9223372036854775809",
        "-19191919191919191919",
        "123456789012345678901234567890" })
    void parseLongAsciiShouldThrowAsciiNumberFormatExceptionIfValueIsOutOfRange(final String value)
    {
        final int index = 7;
        final MutableDirectBuffer buffer = newBuffer(64);

        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseLongAscii(index, length));
        assertEquals("long overflow parsing: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2147483648",
        "2147483649",
        "9999999991",
        "12345678901234567890" })
    void parseNaturalIntAsciiShouldThrowAsciiNumberFormatExceptionIfValueIsOutOfRange(final String value)
    {
        final int index = 4;
        final MutableDirectBuffer buffer = newBuffer(32);

        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseNaturalIntAscii(index, length));
        assertEquals("int overflow parsing: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "9223372036854775808",
        "9223372036854775809",
        "123456789012345678901234567890" })
    void parseNaturalLongAsciiShouldThrowAsciiNumberFormatExceptionIfValueIsOutOfRange(final String value)
    {
        final int index = 7;
        final MutableDirectBuffer buffer = newBuffer(64);

        final int length = buffer.putStringWithoutLengthAscii(index, value);

        final AsciiNumberFormatException exception =
            assertThrowsExactly(AsciiNumberFormatException.class, () -> buffer.parseNaturalLongAscii(index, length));
        assertEquals("long overflow parsing: " + value, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("mutableBuffers")
    void getBytesShouldCopyIntoDestinationBuffer(final MutableDirectBuffer dest)
    {
        final int srcIndex = 20;
        final int dstIndex = 13;
        final int length = 49;
        final byte[] data = new byte[64];
        ThreadLocalRandom.current().nextBytes(data);
        final MutableDirectBuffer buffer = newBuffer(100);
        buffer.putBytes(srcIndex, data);

        buffer.getBytes(srcIndex, dest, dstIndex, length);

        for (int i = 0; i < length; i++)
        {
            assertEquals(data[i], dest.getByte(dstIndex + i));
        }
    }

    @ParameterizedTest
    @MethodSource("mutableBuffers")
    void putBytesShouldCopyBytesFromTheSourceBuffer(final MutableDirectBuffer src)
    {
        final int srcIndex = 13;
        final int dstIndex = 6;
        final int length = 28;
        final byte[] data = new byte[32];
        ThreadLocalRandom.current().nextBytes(data);
        src.putBytes(srcIndex, data);

        final MutableDirectBuffer buffer = newBuffer(100);
        buffer.putBytes(dstIndex, src, srcIndex, length);

        for (int i = 0; i < length; i++)
        {
            assertEquals(data[i], buffer.getByte(dstIndex + i));
        }
    }

    @Test
    void equalsReturnsTrueForThis()
    {
        final MutableDirectBuffer buffer = newBuffer(1);
        assertEquals(buffer, buffer);
    }

    @Test
    void equalsReturnsFalseForNull()
    {
        final MutableDirectBuffer buffer = newBuffer(1);
        assertNotEquals(null, buffer);
    }

    @Test
    void equalsReturnsFalseForWrongClass()
    {
        final MutableDirectBuffer buffer = newBuffer(1);
        final AbstractMutableDirectBuffer other = mock(AbstractMutableDirectBuffer.class);

        assertNotEquals(buffer, other);

        verifyNoInteractions(other);
    }

    @Test
    void equalsReturnsFalseWhenCapacityDoesNotMatch()
    {
        final MutableDirectBuffer buffer = newBuffer(1);
        final MutableDirectBuffer other = newBuffer(3);

        assertNotEquals(buffer, other);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 7, 21, 64 })
    void equalsReturnsTrueWhenOtherBufferHasTheSameData(final int capacity)
    {
        final byte[] data = new byte[capacity];
        ThreadLocalRandom.current().nextBytes(data);
        final MutableDirectBuffer buffer = newBuffer(capacity);
        final MutableDirectBuffer other = newBuffer(capacity);
        buffer.putBytes(0, data);
        other.putBytes(0, data);

        assertEquals(buffer, other);
    }

    @Test
    void hashCodeConsidersAnEntireCapacity()
    {
        final MutableDirectBuffer buffer1 = newBuffer(10);
        final MutableDirectBuffer buffer2 = newBuffer(15);
        final MutableDirectBuffer buffer3 = newBuffer(10);
        final int length = 7;
        buffer1.setMemory(0, length, (byte)'f');
        buffer2.setMemory(0, length, (byte)'f');
        buffer3.setMemory(0, length, (byte)'f');

        assertNotEquals(buffer1.hashCode(), buffer2.hashCode());
        assertEquals(buffer1.hashCode(), buffer3.hashCode());
    }

    @ParameterizedTest
    @CsvSource({ "3,687175", "11,17668045", "128,-56919533" })
    void hashCodeDifferentDataSizes(final int capacity, final int expectedHashCode)
    {
        final MutableDirectBuffer buffer = newBuffer(capacity);
        buffer.setMemory(0, capacity, (byte)'z');
        assertEquals(expectedHashCode, buffer.hashCode());
    }

    @Test
    void compareToComparesCapacityIfContentIsEqual()
    {
        final MutableDirectBuffer buffer1 = newBuffer(3);
        final MutableDirectBuffer buffer2 = newBuffer(5);
        final MutableDirectBuffer buffer3 = newBuffer(3);

        assertEquals(-1, buffer1.compareTo(buffer2));
        assertEquals(1, buffer2.compareTo(buffer1));
        assertEquals(0, buffer3.compareTo(buffer1));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 6, 19, 256 })
    void compareToReturnsZeroForEqualContents(final int capacity)
    {
        final byte[] data = new byte[capacity];
        ThreadLocalRandom.current().nextBytes(data);
        final MutableDirectBuffer buffer1 = newBuffer(capacity);
        buffer1.putBytes(0, data);
        final MutableDirectBuffer buffer2 = new UnsafeBuffer(data);

        assertEquals(0, buffer1.compareTo(buffer2));
        assertEquals(0, buffer2.compareTo(buffer1));
    }

    @ParameterizedTest
    @ValueSource(ints = { 5, 33 })
    void compareToStopsComparisonUponTheFirstNonEqualByte(final int capacity)
    {
        final MutableDirectBuffer buffer1 = newBuffer(capacity);
        final MutableDirectBuffer buffer2 = newBuffer(capacity);
        final int index1 = ThreadLocalRandom.current().nextInt(capacity - 2, capacity);
        final int index2 = ThreadLocalRandom.current().nextInt(0, index1);
        buffer1.setMemory(0, index1, (byte)'a');
        buffer2.setMemory(0, index2, (byte)'a');
        buffer2.setMemory(index2, capacity - index2, (byte)'x');

        assertTrue(buffer1.compareTo(buffer2) < 0);
        assertTrue(buffer2.compareTo(buffer1) > 0);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 9, 16, 33, 100, 256 })
    void setMemoryCopiesTheSameByteAcross(final int length)
    {
        final int index = ThreadLocalRandom.current().nextInt(0, 10);
        final MutableDirectBuffer buffer = newBuffer(index + length);
        final byte value = (byte)ThreadLocalRandom.current().nextInt();
        final byte[] expected = new byte[length];
        Arrays.fill(expected, value);

        buffer.setMemory(index, length, value);

        final byte[] actual = new byte[length];
        buffer.getBytes(index, actual);
        assertArrayEquals(expected, actual);
    }

    private static List<Arguments> valuesAndLengths()
    {
        return Arrays.asList(
            Arguments.arguments(1, 1),
            Arguments.arguments(10, 2),
            Arguments.arguments(100, 3),
            Arguments.arguments(1000, 4),
            Arguments.arguments(12, 2),
            Arguments.arguments(123, 3),
            Arguments.arguments(2345, 4),
            Arguments.arguments(9, 1),
            Arguments.arguments(99, 2),
            Arguments.arguments(999, 3),
            Arguments.arguments(9999, 4));
    }

    private static List<String> nonParsableIntValues()
    {
        return Arrays.asList(
            "23.5",
            "+1",
            "a14349",
            "0xFF",
            "999v",
            "-",
            "+",
            "1234%67890");
    }

    private static List<Arguments> nonParsableLongValues()
    {
        return Arrays.asList(
            Arguments.arguments("23.5", 2),
            Arguments.arguments("+1", 0),
            Arguments.arguments("a14349", 0),
            Arguments.arguments("0xFF", 1),
            Arguments.arguments("999v", 3),
            Arguments.arguments("-", 0),
            Arguments.arguments("+", 0),
            Arguments.arguments("123456789^123456789", 9)
        );
    }

    private static List<MutableDirectBuffer> mutableBuffers()
    {
        return Arrays.asList(
            new ExpandableArrayBuffer(64),
            new ExpandableDirectByteBuffer(64),
            new UnsafeBuffer(new byte[64]),
            new UnsafeBuffer(ByteBuffer.allocate(64)),
            new UnsafeBuffer(ByteBuffer.allocateDirect(64)));
    }
}
