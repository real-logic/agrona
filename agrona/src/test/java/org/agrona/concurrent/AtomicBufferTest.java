/*
 * Copyright 2014-2022 Real Logic Limited.
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
package org.agrona.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.*;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.agrona.BitUtil.*;
import static org.agrona.BufferUtil.allocateDirectAligned;
import static org.agrona.concurrent.UnsafeBuffer.SHOULD_PERFORM_ALIGNMENT_CHECKS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class AtomicBufferTest
{
    private static final int BUFFER_CAPACITY = 64;
    private static final int INDEX = 8;

    private static final byte BYTE_VALUE = 1;
    private static final short SHORT_VALUE = Byte.MAX_VALUE + 2;
    private static final char CHAR_VALUE = '8';
    private static final int INT_VALUE = Short.MAX_VALUE + 3;
    private static final float FLOAT_VALUE = Short.MAX_VALUE + 4.0f;
    private static final long LONG_VALUE = Integer.MAX_VALUE + 5L;
    private static final double DOUBLE_VALUE = Integer.MAX_VALUE + 7.0d;

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldCopyMemory(final ByteBuffer buffer)
    {
        final byte[] expected = "xxxxxxxxxxx".getBytes(US_ASCII);

        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);
        unsafeBuffer.setMemory(0, expected.length, (byte)'x');

        final byte[] buff = new byte[expected.length];
        buffer.get(buff);
        assertThat(buff, is(expected));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetLongFromBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putLong(INDEX, LONG_VALUE);

        assertThat(unsafeBuffer.getLong(INDEX, buffer.order()), is(LONG_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutLongToBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putLong(INDEX, LONG_VALUE, buffer.order());

        assertThat(buffer.getLong(INDEX), is(LONG_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldPutLongVolatileToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putLongVolatile(INDEX, LONG_VALUE);

        assertThat(buffer.getLong(INDEX), is(LONG_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldPutLongOrderedToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putLongOrdered(INDEX, LONG_VALUE);

        assertThat(buffer.getLong(INDEX), is(LONG_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldAddLongOrderedToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        final long initialValue = Integer.MAX_VALUE + 7L;
        final long increment = 9L;
        unsafeBuffer.putLongOrdered(INDEX, initialValue);
        unsafeBuffer.addLongOrdered(INDEX, increment);

        assertThat(buffer.getLong(INDEX), is(initialValue + increment));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldCompareAndSetLongToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putLong(INDEX, LONG_VALUE);

        assertTrue(unsafeBuffer.compareAndSetLong(INDEX, LONG_VALUE, LONG_VALUE + 1));

        assertThat(buffer.getLong(INDEX), is(LONG_VALUE + 1));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldGetAndSetLongToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putLong(INDEX, LONG_VALUE);

        final long afterValue = 1;
        final long beforeValue = unsafeBuffer.getAndSetLong(INDEX, afterValue);

        assertThat(beforeValue, is(LONG_VALUE));
        assertThat(buffer.getLong(INDEX), is(afterValue));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldGetAndAddLongToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putLong(INDEX, LONG_VALUE);

        final long delta = 1;
        final long beforeValue = unsafeBuffer.getAndAddLong(INDEX, delta);

        assertThat(beforeValue, is(LONG_VALUE));
        assertThat(buffer.getLong(INDEX), is(LONG_VALUE + delta));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetIntFromBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putInt(INDEX, INT_VALUE);

        assertThat(unsafeBuffer.getInt(INDEX, buffer.order()), is(INT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutIntToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putInt(INDEX, INT_VALUE, buffer.order());

        assertThat(buffer.getInt(INDEX), is(INT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldGetIntVolatileFromNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putInt(INDEX, INT_VALUE);

        assertThat(unsafeBuffer.getIntVolatile(INDEX), is(INT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldPutIntVolatileToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putIntVolatile(INDEX, INT_VALUE);

        assertThat(buffer.getInt(INDEX), is(INT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldPutIntOrderedToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putIntOrdered(INDEX, INT_VALUE);

        assertThat(buffer.getInt(INDEX), is(INT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldAddIntOrderedToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        final int initialValue = 7;
        final int increment = 9;
        unsafeBuffer.putIntOrdered(INDEX, initialValue);
        unsafeBuffer.addIntOrdered(INDEX, increment);

        assertThat(buffer.getInt(INDEX), is(initialValue + increment));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldCompareAndSetIntToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putInt(INDEX, INT_VALUE);

        assertTrue(unsafeBuffer.compareAndSetInt(INDEX, INT_VALUE, INT_VALUE + 1));

        assertThat(buffer.getInt(INDEX), is(INT_VALUE + 1));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldGetAndSetIntToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putInt(INDEX, INT_VALUE);

        final int afterValue = 1;
        final int beforeValue = unsafeBuffer.getAndSetInt(INDEX, afterValue);

        assertThat(beforeValue, is(INT_VALUE));
        assertThat(buffer.getInt(INDEX), is(afterValue));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldGetAndAddIntToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putInt(INDEX, INT_VALUE);

        final int delta = 1;
        final int beforeValue = unsafeBuffer.getAndAddInt(INDEX, delta);

        assertThat(beforeValue, is(INT_VALUE));
        assertThat(buffer.getInt(INDEX), is(INT_VALUE + delta));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetShortFromBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putShort(INDEX, SHORT_VALUE);

        assertThat(unsafeBuffer.getShort(INDEX, buffer.order()), is(SHORT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutShortToBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putShort(INDEX, SHORT_VALUE, buffer.order());

        assertThat(buffer.getShort(INDEX), is(SHORT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetShortFromNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putShort(INDEX, SHORT_VALUE);

        assertThat(unsafeBuffer.getShort(INDEX, buffer.order()), is(SHORT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutShortToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putShort(INDEX, SHORT_VALUE, buffer.order());

        assertThat(buffer.getShort(INDEX), is(SHORT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldGetShortVolatileFromNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putShort(INDEX, SHORT_VALUE);

        assertThat(unsafeBuffer.getShortVolatile(INDEX), is(SHORT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldPutShortVolatileToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putShortVolatile(INDEX, SHORT_VALUE);

        assertThat(buffer.getShort(INDEX), is(SHORT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetCharFromBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putChar(INDEX, CHAR_VALUE);

        assertThat(unsafeBuffer.getChar(INDEX, buffer.order()), is(CHAR_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutCharToBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putChar(INDEX, CHAR_VALUE, buffer.order());

        assertThat(buffer.getChar(INDEX), is(CHAR_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetCharFromNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putChar(INDEX, CHAR_VALUE);

        assertThat(unsafeBuffer.getChar(INDEX, buffer.order()), is(CHAR_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutCharToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putChar(INDEX, CHAR_VALUE, buffer.order());

        assertThat(buffer.getChar(INDEX), is(CHAR_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldGetCharVolatileFromNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putChar(INDEX, CHAR_VALUE);

        assertThat(unsafeBuffer.getCharVolatile(INDEX), is(CHAR_VALUE));
    }

    @ParameterizedTest
    @MethodSource("atomicBuffers")
    void shouldPutCharVolatileToNativeBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putCharVolatile(INDEX, CHAR_VALUE);

        assertThat(buffer.getChar(INDEX), is(CHAR_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetDoubleFromBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putDouble(INDEX, DOUBLE_VALUE);

        assertThat(unsafeBuffer.getDouble(INDEX, buffer.order()), is(DOUBLE_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutDoubleToBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putDouble(INDEX, DOUBLE_VALUE, buffer.order());

        assertThat(buffer.getDouble(INDEX), is(DOUBLE_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetFloatFromBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.putFloat(INDEX, FLOAT_VALUE);

        assertThat(unsafeBuffer.getFloat(INDEX, buffer.order()), is(FLOAT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutFloatToBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putFloat(INDEX, FLOAT_VALUE, buffer.order());

        assertThat(buffer.getFloat(INDEX), is(FLOAT_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetByteFromBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.put(INDEX, BYTE_VALUE);

        assertThat(unsafeBuffer.getByte(INDEX), is(BYTE_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutByteToBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putByte(INDEX, BYTE_VALUE);

        assertThat(buffer.get(INDEX), is(BYTE_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetByteVolatileFromBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        buffer.put(INDEX, BYTE_VALUE);

        assertThat(unsafeBuffer.getByteVolatile(INDEX), is(BYTE_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldPutByteVolatileToBuffer(final ByteBuffer buffer)
    {
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);

        unsafeBuffer.putByteVolatile(INDEX, BYTE_VALUE);

        assertThat(buffer.get(INDEX), is(BYTE_VALUE));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetBytesFromBuffer(final ByteBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);
        buffer.position(INDEX);
        buffer.put(testBytes);

        final byte[] buff = new byte[testBytes.length];
        unsafeBuffer.getBytes(INDEX, buff);

        assertThat(buff, is(testBytes));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetBytesFromBufferToBuffer(final ByteBuffer buffer)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);
        buffer.position(INDEX);
        buffer.put(testBytes);

        final ByteBuffer dstBuffer = allocate(testBytes.length);
        unsafeBuffer.getBytes(INDEX, dstBuffer, testBytes.length);

        assertThat(dstBuffer.array(), is(testBytes));
    }

    @ParameterizedTest
    @MethodSource("nativeBuffers")
    void shouldGetBytesFromBufferToSlice(final ByteBuffer buffer)
    {
        final byte[] testBytes = "shouldGetBytesFromBufferToSlice".getBytes(US_ASCII);

        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(buffer);
        buffer.position(INDEX);
        buffer.put(testBytes);

        final ByteBuffer dstBuffer =
            sliceBuffer(allocate(testBytes.length * 2).position(testBytes.length));

        unsafeBuffer.getBytes(INDEX, dstBuffer, testBytes.length);

        dstBuffer.flip();
        final byte[] result = new byte[testBytes.length];
        dstBuffer.get(result);

        assertThat(result, is(testBytes));
    }

    @Test
    void shouldCheckAlignmentForCharOperationsUnalignedIndex()
    {
        assertTrue(SHOULD_PERFORM_ALIGNMENT_CHECKS);

        final UnsafeBuffer buffer = new UnsafeBuffer(new long[1]);

        assertAlignmentCheck(buffer, 1, SIZE_OF_CHAR, i -> buffer.putCharVolatile(i, '?'));
        assertAlignmentCheck(buffer, 3, SIZE_OF_CHAR, buffer::getCharVolatile);
    }

    @ParameterizedTest
    @MethodSource("unalignedBuffers")
    void shouldCheckAlignmentForCharOperationsUnalignedBuffer(final UnsafeBuffer buffer)
    {
        assertTrue(SHOULD_PERFORM_ALIGNMENT_CHECKS);

        assertAlignmentCheck(buffer, 0, SIZE_OF_CHAR, i -> buffer.putCharVolatile(i, '?'));
        assertAlignmentCheck(buffer, 2, SIZE_OF_CHAR, buffer::getCharVolatile);
    }

    @Test
    void shouldCheckAlignmentForShortOperationsUnalignedIndex()
    {
        assertTrue(SHOULD_PERFORM_ALIGNMENT_CHECKS);

        final UnsafeBuffer buffer = new UnsafeBuffer(new long[1]);

        assertAlignmentCheck(buffer, 5, SIZE_OF_SHORT, i -> buffer.putShortVolatile(i, Short.MAX_VALUE));
        assertAlignmentCheck(buffer, 1, SIZE_OF_SHORT, buffer::getShortVolatile);
    }

    @ParameterizedTest
    @MethodSource("unalignedBuffers")
    void shouldCheckAlignmentForShortOperationsUnalignedBuffer(final UnsafeBuffer buffer)
    {
        assertTrue(SHOULD_PERFORM_ALIGNMENT_CHECKS);

        assertAlignmentCheck(buffer, 2, SIZE_OF_SHORT, i -> buffer.putShortVolatile(i, Short.MAX_VALUE));
        assertAlignmentCheck(buffer, 6, SIZE_OF_SHORT, buffer::getShortVolatile);
    }

    @Test
    void shouldCheckAlignmentForIntOperationsUnalignedIndex()
    {
        assertTrue(SHOULD_PERFORM_ALIGNMENT_CHECKS);

        final UnsafeBuffer buffer = new UnsafeBuffer(new long[2]);

        assertAlignmentCheck(buffer, 1, SIZE_OF_INT, i -> buffer.putIntVolatile(i, Integer.MIN_VALUE));
        assertAlignmentCheck(buffer, 2, SIZE_OF_INT, buffer::getIntVolatile);
        assertAlignmentCheck(buffer, 3, SIZE_OF_INT, i -> buffer.putIntOrdered(i, Integer.MAX_VALUE));
        assertAlignmentCheck(buffer, 5, SIZE_OF_INT, i -> buffer.addIntOrdered(i, 111));
        assertAlignmentCheck(buffer, 6, SIZE_OF_INT, i -> buffer.compareAndSetInt(i, 0, 2));
        assertAlignmentCheck(buffer, 7, SIZE_OF_INT, i -> buffer.getAndSetInt(i, 42));
        assertAlignmentCheck(buffer, 9, SIZE_OF_INT, i -> buffer.getAndAddInt(i, 9));
    }

    @ParameterizedTest
    @MethodSource("unalignedBuffers")
    void shouldCheckAlignmentForIntOperationsUnalignedBuffer(final UnsafeBuffer buffer)
    {
        assertTrue(SHOULD_PERFORM_ALIGNMENT_CHECKS);

        assertAlignmentCheck(buffer, 0, SIZE_OF_INT, i -> buffer.putIntVolatile(i, Integer.MIN_VALUE));
        assertAlignmentCheck(buffer, 4, SIZE_OF_INT, buffer::getIntVolatile);
        assertAlignmentCheck(buffer, 8, SIZE_OF_INT, i -> buffer.putIntOrdered(i, Integer.MAX_VALUE));
        assertAlignmentCheck(buffer, 12, SIZE_OF_INT, i -> buffer.addIntOrdered(i, 111));
        assertAlignmentCheck(buffer, 0, SIZE_OF_INT, i -> buffer.compareAndSetInt(i, 0, 2));
        assertAlignmentCheck(buffer, 4, SIZE_OF_INT, i -> buffer.getAndSetInt(i, 42));
        assertAlignmentCheck(buffer, 8, SIZE_OF_INT, i -> buffer.getAndAddInt(i, 9));
    }

    @Test
    void shouldCheckAlignmentForLongOperationsUnalignedIndex()
    {
        assertTrue(SHOULD_PERFORM_ALIGNMENT_CHECKS);

        final UnsafeBuffer buffer = new UnsafeBuffer(new long[2]);

        assertAlignmentCheck(buffer, 1, SIZE_OF_LONG, i -> buffer.putLongVolatile(i, Long.MIN_VALUE));
        assertAlignmentCheck(buffer, 2, SIZE_OF_LONG, buffer::getLongVolatile);
        assertAlignmentCheck(buffer, 3, SIZE_OF_LONG, i -> buffer.putLongOrdered(i, Long.MAX_VALUE));
        assertAlignmentCheck(buffer, 4, SIZE_OF_LONG, i -> buffer.addLongOrdered(i, 111));
        assertAlignmentCheck(buffer, 5, SIZE_OF_LONG, i -> buffer.compareAndSetLong(i, 2734683567834L, 2));
        assertAlignmentCheck(buffer, 6, SIZE_OF_LONG, i -> buffer.getAndSetLong(i, -422374823L));
        assertAlignmentCheck(buffer, 7, SIZE_OF_LONG, i -> buffer.getAndAddLong(i, 9248937239L));
    }

    @ParameterizedTest
    @MethodSource("unalignedBuffers")
    void shouldCheckAlignmentForLongOperationsUnalignedBuffer(final UnsafeBuffer buffer)
    {
        assertTrue(SHOULD_PERFORM_ALIGNMENT_CHECKS);

        assertAlignmentCheck(buffer, 0, SIZE_OF_LONG, i -> buffer.putLongVolatile(i, Long.MIN_VALUE));
        assertAlignmentCheck(buffer, 8, SIZE_OF_LONG, buffer::getLongVolatile);
        assertAlignmentCheck(buffer, 0, SIZE_OF_LONG, i -> buffer.putLongOrdered(i, Long.MAX_VALUE));
        assertAlignmentCheck(buffer, 8, SIZE_OF_LONG, i -> buffer.addLongOrdered(i, 111));
        assertAlignmentCheck(buffer, 0, SIZE_OF_LONG, i -> buffer.compareAndSetLong(i, 2734683567834L, 2));
        assertAlignmentCheck(buffer, 8, SIZE_OF_LONG, i -> buffer.getAndSetLong(i, -422374823L));
        assertAlignmentCheck(buffer, 0, SIZE_OF_LONG, i -> buffer.getAndAddLong(i, 9248937239L));
    }

    private static ByteBuffer sliceBuffer(final Buffer buffer)
    {
        return ((ByteBuffer)buffer).slice();
    }

    private static List<ByteBuffer> nativeBuffers()
    {
        return Arrays.asList(
            allocate(BUFFER_CAPACITY).order(LITTLE_ENDIAN),
            allocateDirect(BUFFER_CAPACITY).order(BIG_ENDIAN),
            sliceBuffer(allocate(BUFFER_CAPACITY * 2).position(BUFFER_CAPACITY)));
    }

    private static List<ByteBuffer> atomicBuffers()
    {
        return Collections.singletonList(
            allocateDirectAligned(BUFFER_CAPACITY, SIZE_OF_LONG).order(nativeOrder()));
    }

    private static List<UnsafeBuffer> unalignedBuffers()
    {
        return Arrays.asList(
            new UnsafeBuffer(new byte[16]),
            new UnsafeBuffer(allocate(16)),
            new UnsafeBuffer(sliceBuffer(allocateDirectAligned(24, SIZE_OF_LONG).position(1))));
    }

    private static void assertAlignmentCheck(
        final UnsafeBuffer buffer, final int index, final int alignment, final IntConsumer operation)
    {
        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> operation.accept(index));
        assertEquals(
            "unaligned atomic operation: (addressOffset + index)=" + (buffer.addressOffset() + index) +
            " is not divisible by " + alignment,
            exception.getMessage());
    }
}
