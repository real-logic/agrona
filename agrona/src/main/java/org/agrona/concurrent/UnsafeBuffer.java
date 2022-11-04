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

import org.agrona.AsciiNumberFormatException;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.LangUtil;
import org.agrona.MutableDirectBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.agrona.AsciiEncoding.*;
import static org.agrona.BitUtil.*;
import static org.agrona.BufferUtil.*;
import static org.agrona.UnsafeAccess.*;
import static org.agrona.collections.ArrayUtil.EMPTY_BYTE_ARRAY;

/**
 * Supports regular, byte ordered, and atomic (memory ordered) access to an underlying buffer. The buffer can be a
 * byte[], one of the various {@link ByteBuffer} implementations, or an off Java heap memory address.
 * <p>
 * {@link ByteOrder} of a wrapped buffer is not applied to the {@link UnsafeBuffer}. {@link UnsafeBuffer}s are
 * effectively stateless and can be used concurrently, the wrapping methods are an exception. To control
 * {@link ByteOrder} use the appropriate method with the {@link ByteOrder} overload.
 * <p>
 * <b>Note:</b> This class has a natural ordering that is inconsistent with equals.
 * Types may be different but equal on buffer contents.
 * <p>
 * <b>Note:</b> The wrap methods on this class are not thread safe. Concurrent access should only happen after a
 * successful wrap.
 */
public class UnsafeBuffer implements AtomicBuffer
{
    /**
     * @see AtomicBuffer#ALIGNMENT
     */
    public static final int ALIGNMENT = AtomicBuffer.ALIGNMENT;

    /**
     * @see DirectBuffer#DISABLE_BOUNDS_CHECKS_PROP_NAME
     */
    public static final String DISABLE_BOUNDS_CHECKS_PROP_NAME = DirectBuffer.DISABLE_BOUNDS_CHECKS_PROP_NAME;

    /**
     * @see DirectBuffer#SHOULD_BOUNDS_CHECK
     */
    public static final boolean SHOULD_BOUNDS_CHECK = DirectBuffer.SHOULD_BOUNDS_CHECK;

    private long addressOffset;
    private int capacity;
    private Object array;
    private ByteBuffer byteBuffer;

    /**
     * Empty constructor for a reusable wrapper buffer.
     */
    public UnsafeBuffer()
    {
        wrap(EMPTY_BYTE_ARRAY);
    }

    /**
     * Attach a view to a {@code byte[]} for providing direct access.
     *
     * @param buffer to which the view is attached.
     * @see #wrap(byte[])
     */
    public UnsafeBuffer(final byte[] buffer)
    {
        wrap(buffer);
    }

    /**
     * Attach a view to a {@code byte[]} for providing direct access.
     *
     * @param buffer to which the view is attached.
     * @param offset in bytes within the buffer to begin.
     * @param length in bytes of the buffer included in the view.
     * @see #wrap(byte[], int, int)
     */
    public UnsafeBuffer(final byte[] buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Attach a view to a {@code long[]} for providing direct access.
     *
     * @param buffer to which the view is attached.
     * @see #wrap(long[])
     */
    public UnsafeBuffer(final long[] buffer)
    {
        wrap(buffer);
    }

    /**
     * Attach a view to a {@code long[]} for providing direct access.
     *
     * @param buffer to which the view is attached.
     * @param offset in bytes within the buffer to begin.
     * @param length in bytes of the buffer included in the view.
     * @see #wrap(long[], int, int)
     */
    public UnsafeBuffer(final long[] buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Attach a view to a {@link ByteBuffer} for providing direct access, the {@link ByteBuffer} can be
     * heap based or direct.
     *
     * @param buffer to which the view is attached.
     */
    public UnsafeBuffer(final ByteBuffer buffer)
    {
        wrap(buffer);
    }

    /**
     * Attach a view to a {@link ByteBuffer} for providing direct access, the {@link ByteBuffer} can be
     * heap based or direct.
     *
     * @param buffer to which the view is attached.
     * @param offset in bytes within the buffer to begin.
     * @param length in bytes of the buffer included in the view.
     */
    public UnsafeBuffer(final ByteBuffer buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Attach a view to an existing {@link DirectBuffer}
     *
     * @param buffer to which the view is attached.
     */
    public UnsafeBuffer(final DirectBuffer buffer)
    {
        wrap(buffer);
    }

    /**
     * Attach a view to an existing {@link DirectBuffer}
     *
     * @param buffer to which the view is attached.
     * @param offset in bytes within the buffer to begin.
     * @param length in bytes of the buffer included in the view.
     */
    public UnsafeBuffer(final DirectBuffer buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Attach a view to an off-heap memory region by address. This is useful for interacting with native libraries.
     *
     * @param address where the memory begins off-heap
     * @param length  of the buffer from the given address
     */
    public UnsafeBuffer(final long address, final int length)
    {
        wrap(address, length);
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final byte[] buffer)
    {
        capacity = buffer.length;
        addressOffset = ARRAY_BASE_OFFSET;
        byteBuffer = null;

        if (buffer != array)
        {
            array = buffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final byte[] buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheckWrap(offset, length, buffer.length);
        }

        capacity = length;
        addressOffset = ARRAY_BASE_OFFSET + offset;
        byteBuffer = null;

        if (buffer != array)
        {
            array = buffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final long[] buffer)
    {
        capacity = buffer.length * SIZE_OF_LONG;
        addressOffset = LONG_ARRAY_BASE_OFFSET;
        byteBuffer = null;

        if (buffer != array)
        {
            array = buffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final long[] buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheckWrap(offset, length, buffer.length * SIZE_OF_LONG);
        }

        capacity = length;
        addressOffset = LONG_ARRAY_BASE_OFFSET + offset;
        byteBuffer = null;

        if (buffer != array)
        {
            array = buffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final ByteBuffer buffer)
    {
        capacity = buffer.capacity();

        if (buffer != byteBuffer)
        {
            byteBuffer = buffer;
        }

        if (buffer.isDirect())
        {
            array = null;
            addressOffset = address(buffer);
        }
        else
        {
            array = BufferUtil.array(buffer);
            addressOffset = ARRAY_BASE_OFFSET + arrayOffset(buffer);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final ByteBuffer buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheckWrap(offset, length, buffer.capacity());
        }

        capacity = length;

        if (buffer != byteBuffer)
        {
            byteBuffer = buffer;
        }

        if (buffer.isDirect())
        {
            array = null;
            addressOffset = address(buffer) + offset;
        }
        else
        {
            array = BufferUtil.array(buffer);
            addressOffset = ARRAY_BASE_OFFSET + arrayOffset(buffer) + offset;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final DirectBuffer buffer)
    {
        capacity = buffer.capacity();
        addressOffset = buffer.addressOffset();

        final Object array = buffer.array();
        if (array != this.array)
        {
            this.array = array;
        }

        final ByteBuffer byteBuffer = buffer.byteBuffer();
        if (byteBuffer != this.byteBuffer)
        {
            this.byteBuffer = byteBuffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final DirectBuffer buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheckWrap(offset, length, buffer.capacity());
        }

        capacity = length;
        addressOffset = buffer.addressOffset() + offset;

        final Object array = buffer.array();
        if (array != this.array)
        {
            this.array = array;
        }

        final ByteBuffer byteBuffer = buffer.byteBuffer();
        if (byteBuffer != this.byteBuffer)
        {
            this.byteBuffer = byteBuffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final long address, final int length)
    {
        capacity = length;
        addressOffset = address;
        array = null;
        byteBuffer = null;
    }

    /**
     * {@inheritDoc}
     */
    public long addressOffset()
    {
        return addressOffset;
    }

    /**
     * {@inheritDoc}
     */
    public Object array()
    {
        return array;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] byteArray()
    {
        final Object arr = array;
        return (arr instanceof byte[]) ? (byte[])arr : null;
    }

    /**
     * {@inheritDoc}
     */
    public long[] longArray()
    {
        final Object arr = array;
        return (arr instanceof long[]) ? (long[])arr : null;
    }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer byteBuffer()
    {
        return byteBuffer;
    }

    /**
     * {@inheritDoc}
     */
    public void setMemory(final int index, final int length, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        if (MEMSET_HACK_REQUIRED && length > MEMSET_HACK_THRESHOLD && 0 == (offset & 1))
        {
            // This horrible filth is to encourage the JVM to call memset() when address is even.
            UNSAFE.putByte(array, offset, value);
            UNSAFE.setMemory(array, offset + 1, length - 1, value);
        }
        else
        {
            UNSAFE.setMemory(array, offset, length, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * {@inheritDoc}
     */
    public void checkLimit(final int limit)
    {
        if (limit > capacity)
        {
            throw new IndexOutOfBoundsException("limit=" + limit + " is beyond capacity=" + capacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpandable()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void verifyAlignment()
    {
        if (null != array)
        {
            if (array instanceof byte[])
            {
                throw new IllegalStateException(
                    "AtomicBuffer was created from a byte[] and is not correctly aligned by " + ALIGNMENT);
            }
        }
        else if (0 != (addressOffset & (ALIGNMENT - 1)))
        {
            throw new IllegalStateException(
                "AtomicBuffer is not correctly aligned: addressOffset=" + addressOffset + " is not divisible by " +
                ALIGNMENT);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        long bits = UNSAFE.getLong(array, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Long.reverseBytes(bits);
        }

        return bits;
    }

    /**
     * {@inheritDoc}
     */
    public void putLong(final int index, final long value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        long bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Long.reverseBytes(bits);
        }

        UNSAFE.putLong(array, addressOffset + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getLong(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putLong(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putLong(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public long getLongVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_LONG);
        }

        return UNSAFE.getLongVolatile(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putLongVolatile(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_LONG);
        }

        UNSAFE.putLongVolatile(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public void putLongOrdered(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_LONG);
        }

        UNSAFE.putOrderedLong(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public long addLongOrdered(final int index, final long increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_LONG);
        }

        final long offset = addressOffset + index;
        final Object array = this.array;
        final long value = UNSAFE.getLong(array, offset);
        UNSAFE.putOrderedLong(array, offset, value + increment);

        return value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean compareAndSetLong(final int index, final long expectedValue, final long updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_LONG);
        }

        return UNSAFE.compareAndSwapLong(array, addressOffset + index, expectedValue, updateValue);
    }

    /**
     * {@inheritDoc}
     */
    public long getAndSetLong(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndSetLong(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public long getAndAddLong(final int index, final long delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndAddLong(array, addressOffset + index, delta);
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        int bits = UNSAFE.getInt(array, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        return bits;
    }

    /**
     * {@inheritDoc}
     */
    public void putInt(final int index, final int value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        int bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        UNSAFE.putInt(array, addressOffset + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getInt(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putInt(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putInt(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public int getIntVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_INT);
        }

        return UNSAFE.getIntVolatile(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putIntVolatile(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_INT);
        }

        UNSAFE.putIntVolatile(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public void putIntOrdered(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_INT);
        }

        UNSAFE.putOrderedInt(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public int addIntOrdered(final int index, final int increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_INT);
        }

        final long offset = addressOffset + index;
        final Object byteArray = this.array;
        final int value = UNSAFE.getInt(byteArray, offset);
        UNSAFE.putOrderedInt(byteArray, offset, value + increment);

        return value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean compareAndSetInt(final int index, final int expectedValue, final int updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_INT);
        }

        return UNSAFE.compareAndSwapInt(array, addressOffset + index, expectedValue, updateValue);
    }

    /**
     * {@inheritDoc}
     */
    public int getAndSetInt(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndSetInt(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public int getAndAddInt(final int index, final int delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndAddInt(array, addressOffset + index, delta);
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = UNSAFE.getLong(array, addressOffset + index);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getDouble(array, addressOffset + index);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putDouble(final int index, final double value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = Long.reverseBytes(Double.doubleToRawLongBits(value));
            UNSAFE.putLong(array, addressOffset + index, bits);
        }
        else
        {
            UNSAFE.putDouble(array, addressOffset + index, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        return UNSAFE.getDouble(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putDouble(final int index, final double value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        UNSAFE.putDouble(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = UNSAFE.getInt(array, addressOffset + index);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getFloat(array, addressOffset + index);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putFloat(final int index, final float value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = Integer.reverseBytes(Float.floatToRawIntBits(value));
            UNSAFE.putInt(array, addressOffset + index, bits);
        }
        else
        {
            UNSAFE.putFloat(array, addressOffset + index, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        return UNSAFE.getFloat(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putFloat(final int index, final float value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        UNSAFE.putFloat(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        short bits = UNSAFE.getShort(array, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Short.reverseBytes(bits);
        }

        return bits;
    }

    /**
     * {@inheritDoc}
     */
    public void putShort(final int index, final short value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        short bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Short.reverseBytes(bits);
        }

        UNSAFE.putShort(array, addressOffset + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        return UNSAFE.getShort(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putShort(final int index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        UNSAFE.putShort(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public short getShortVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_SHORT);
        }

        return UNSAFE.getShortVolatile(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putShortVolatile(final int index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_SHORT);
        }

        UNSAFE.putShortVolatile(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public byte getByte(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        return UNSAFE.getByte(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putByte(final int index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        UNSAFE.putByte(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public byte getByteVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        return UNSAFE.getByteVolatile(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putByteVolatile(final int index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        UNSAFE.putByteVolatile(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final int index, final byte[] dst)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, dst.length);
            BufferUtil.boundsCheck(dst, 0, dst.length);
        }

        UNSAFE.copyMemory(array, addressOffset + index, dst, ARRAY_BASE_OFFSET, dst.length);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final int index, final byte[] dst, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(dst, offset, length);
        }

        UNSAFE.copyMemory(array, addressOffset + index, dst, ARRAY_BASE_OFFSET + offset, length);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final int index, final MutableDirectBuffer dstBuffer, final int dstIndex, final int length)
    {
        dstBuffer.putBytes(dstIndex, this, index, length);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final int index, final ByteBuffer dstBuffer, final int length)
    {
        final int dstOffset = dstBuffer.position();
        getBytes(index, dstBuffer, dstOffset, length);
        dstBuffer.position(dstOffset + length);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final int index, final ByteBuffer dstBuffer, final int dstOffset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(dstBuffer, dstOffset, length);
        }

        final byte[] dstByteArray;
        final long dstBaseOffset;
        if (dstBuffer.isDirect())
        {
            dstByteArray = null;
            dstBaseOffset = address(dstBuffer);
        }
        else
        {
            dstByteArray = BufferUtil.array(dstBuffer);
            dstBaseOffset = ARRAY_BASE_OFFSET + arrayOffset(dstBuffer);
        }

        UNSAFE.copyMemory(array, addressOffset + index, dstByteArray, dstBaseOffset + dstOffset, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final byte[] src)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, src.length);
        }

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET, array, addressOffset + index, src.length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final byte[] src, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(src, offset, length);
        }

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET + offset, array, addressOffset + index, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final ByteBuffer srcBuffer, final int length)
    {
        final int srcIndex = srcBuffer.position();
        putBytes(index, srcBuffer, srcIndex, length);
        srcBuffer.position(srcIndex + length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final ByteBuffer srcBuffer, final int srcIndex, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(srcBuffer, srcIndex, length);
        }

        final byte[] srcByteArray;
        final long srcBaseOffset;
        if (srcBuffer.isDirect())
        {
            srcByteArray = null;
            srcBaseOffset = address(srcBuffer);
        }
        else
        {
            srcByteArray = BufferUtil.array(srcBuffer);
            srcBaseOffset = ARRAY_BASE_OFFSET + arrayOffset(srcBuffer);
        }

        UNSAFE.copyMemory(srcByteArray, srcBaseOffset + srcIndex, array, addressOffset + index, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final DirectBuffer srcBuffer, final int srcIndex, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            srcBuffer.boundsCheck(srcIndex, length);
        }

        UNSAFE.copyMemory(
            srcBuffer.array(),
            srcBuffer.addressOffset() + srcIndex,
            array,
            addressOffset + index,
            length);
    }

    /**
     * {@inheritDoc}
     */
    public char getChar(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        char bits = UNSAFE.getChar(array, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = (char)Short.reverseBytes((short)bits);
        }

        return bits;
    }

    /**
     * {@inheritDoc}
     */
    public void putChar(final int index, final char value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        char bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = (char)Short.reverseBytes((short)bits);
        }

        UNSAFE.putChar(array, addressOffset + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public char getChar(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        return UNSAFE.getChar(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putChar(final int index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        UNSAFE.putChar(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public char getCharVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_CHAR);
        }

        return UNSAFE.getCharVolatile(array, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putCharVolatile(final int index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        if (SHOULD_PERFORM_ALIGNMENT_CHECKS)
        {
            checkAlignment(index, SIZE_OF_CHAR);
        }

        UNSAFE.putCharVolatile(array, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringAscii(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, STR_HEADER_LEN);
        }

        final int length = UNSAFE.getInt(array, addressOffset + index);

        return getStringAscii(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final Appendable appendable)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(array, addressOffset + index);

        return getStringAscii(index, length, appendable);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringAscii(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, STR_HEADER_LEN);
        }

        int bits = UNSAFE.getInt(array, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        final int length = bits;

        return getStringAscii(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final Appendable appendable, final ByteOrder byteOrder)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        int bits = UNSAFE.getInt(array, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        final int length = bits;

        return getStringAscii(index, length, appendable);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringAscii(final int index, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index + STR_HEADER_LEN, length);
        }

        final byte[] dst = new byte[length];
        UNSAFE.copyMemory(array, addressOffset + index + STR_HEADER_LEN, dst, ARRAY_BASE_OFFSET, length);

        return new String(dst, US_ASCII);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final int length, final Appendable appendable)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length + STR_HEADER_LEN);
        }

        try
        {
            final Object array = this.array;
            final long offset = addressOffset;
            for (int i = index + STR_HEADER_LEN, limit = index + STR_HEADER_LEN + length; i < limit; i++)
            {
                final char c = (char)UNSAFE.getByte(array, offset + i);
                appendable.append(c > 127 ? '?' : c);
            }
        }
        catch (final IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        return length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final String value)
    {
        final int length = value != null ? value.length() : 0;

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length + STR_HEADER_LEN);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        UNSAFE.putInt(array, offset, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(array, offset + STR_HEADER_LEN + i, (byte)c);
        }

        return STR_HEADER_LEN + length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final CharSequence value)
    {
        final int length = value != null ? value.length() : 0;

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length + STR_HEADER_LEN);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        UNSAFE.putInt(array, offset, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(array, offset + STR_HEADER_LEN + i, (byte)c);
        }

        return STR_HEADER_LEN + length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final String value, final ByteOrder byteOrder)
    {
        final int length = value != null ? value.length() : 0;

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length + STR_HEADER_LEN);
        }

        int bits = length;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        UNSAFE.putInt(array, offset, bits);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(array, offset + STR_HEADER_LEN + i, (byte)c);
        }

        return STR_HEADER_LEN + length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final CharSequence value, final ByteOrder byteOrder)
    {
        final int length = value != null ? value.length() : 0;

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length + STR_HEADER_LEN);
        }

        int bits = length;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        UNSAFE.putInt(array, offset, bits);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(array, offset + STR_HEADER_LEN + i, (byte)c);
        }

        return STR_HEADER_LEN + length;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringWithoutLengthAscii(final int index, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        final byte[] dst = new byte[length];
        UNSAFE.copyMemory(array, addressOffset + index, dst, ARRAY_BASE_OFFSET, length);

        return new String(dst, US_ASCII);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringWithoutLengthAscii(final int index, final int length, final Appendable appendable)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        try
        {
            final Object array = this.array;
            final long offset = addressOffset;
            for (int i = index, limit = index + length; i < limit; i++)
            {
                final char c = (char)UNSAFE.getByte(array, offset + i);
                appendable.append(c > 127 ? '?' : c);
            }
        }
        catch (final IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        return length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthAscii(final int index, final String value)
    {
        final int length = value != null ? value.length() : 0;

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }
            UNSAFE.putByte(array, offset + i, (byte)c);
        }

        return length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthAscii(final int index, final CharSequence value)
    {
        final int length = value != null ? value.length() : 0;

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(array, offset + i, (byte)c);
        }

        return length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthAscii(final int index, final String value, final int valueOffset, final int length)
    {
        final int len = value != null ? Math.min(value.length() - valueOffset, length) : 0;

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, len);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        for (int i = 0; i < len; i++)
        {
            char c = value.charAt(valueOffset + i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(array, offset + i, (byte)c);
        }

        return len;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthAscii(
        final int index, final CharSequence value, final int valueOffset, final int length)
    {
        final int len = value != null ? Math.min(value.length() - valueOffset, length) : 0;

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, len);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        for (int i = 0; i < len; i++)
        {
            char c = value.charAt(valueOffset + i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(array, offset + i, (byte)c);
        }

        return len;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringUtf8(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, STR_HEADER_LEN);
        }

        final int length = UNSAFE.getInt(array, addressOffset + index);

        return getStringUtf8(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringUtf8(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, STR_HEADER_LEN);
        }

        int bits = UNSAFE.getInt(array, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        final int length = bits;

        return getStringUtf8(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringUtf8(final int index, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index + STR_HEADER_LEN, length);
        }

        final byte[] stringInBytes = new byte[length];
        UNSAFE.copyMemory(array, addressOffset + index + STR_HEADER_LEN, stringInBytes, ARRAY_BASE_OFFSET, length);

        return new String(stringInBytes, UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    public int putStringUtf8(final int index, final String value)
    {
        return putStringUtf8(index, value, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    public int putStringUtf8(final int index, final String value, final ByteOrder byteOrder)
    {
        return putStringUtf8(index, value, byteOrder, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    public int putStringUtf8(final int index, final String value, final int maxEncodedLength)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        if (bytes.length > maxEncodedLength)
        {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedLength);
        }

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, STR_HEADER_LEN + bytes.length);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        UNSAFE.putInt(array, offset, bytes.length);
        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, array, offset + STR_HEADER_LEN, bytes.length);

        return STR_HEADER_LEN + bytes.length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringUtf8(final int index, final String value, final ByteOrder byteOrder, final int maxEncodedLength)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        if (bytes.length > maxEncodedLength)
        {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedLength);
        }

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, STR_HEADER_LEN + bytes.length);
        }

        int bits = bytes.length;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        final Object array = this.array;
        final long offset = addressOffset + index;
        UNSAFE.putInt(array, offset, bits);
        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, array, offset + STR_HEADER_LEN, bytes.length);

        return STR_HEADER_LEN + bytes.length;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringWithoutLengthUtf8(final int index, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        final byte[] stringInBytes = new byte[length];
        UNSAFE.copyMemory(array, addressOffset + index, stringInBytes, ARRAY_BASE_OFFSET, length);

        return new String(stringInBytes, UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthUtf8(final int index, final String value)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, bytes.length);
        }

        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, array, addressOffset + index, bytes.length);

        return bytes.length;
    }

    /**
     * {@inheritDoc}
     */
    public int parseNaturalIntAscii(final int index, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }

        if (length < INT_MAX_DIGITS)
        {
            return parsePositiveIntAscii(index, length, index, index + length);
        }
        else
        {
            final long tally = parsePositiveIntAsciiOverflowCheck(index, length, index, index + length);
            if (tally >= INTEGER_ABSOLUTE_MIN_VALUE)
            {
                throwParseIntOverflowError(index, length);
            }
            return (int)tally;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long parseNaturalLongAscii(final int index, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }

        if (length < LONG_MAX_DIGITS)
        {
            return parsePositiveLongAscii(index, length, index, index + length);
        }
        else
        {
            return parseLongAsciiOverflowCheck(index, length, LONG_MAX_VALUE_DIGITS, index, index + length);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int parseIntAscii(final int index, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }

        final boolean negative = MINUS_SIGN == UNSAFE.getByte(array, addressOffset + index);
        int i = index;
        if (negative)
        {
            i++;
            if (1 == length)
            {
                throwParseIntError(index, length);
            }
        }

        final int end = index + length;
        if (end - i < INT_MAX_DIGITS)
        {
            final int tally = parsePositiveIntAscii(index, length, i, end);
            return negative ? -tally : tally;
        }
        else
        {
            final long tally = parsePositiveIntAsciiOverflowCheck(index, length, i, end);
            if (tally > INTEGER_ABSOLUTE_MIN_VALUE || INTEGER_ABSOLUTE_MIN_VALUE == tally && !negative)
            {
                throwParseIntOverflowError(index, length);
            }
            return (int)(negative ? -tally : tally);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long parseLongAscii(final int index, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }

        final boolean negative = MINUS_SIGN == UNSAFE.getByte(array, addressOffset + index);
        int i = index;
        if (negative)
        {
            i++;
            if (1 == length)
            {
                throwParseLongError(index, length);
            }
        }

        final int end = index + length;
        if (end - i < LONG_MAX_DIGITS)
        {
            final long tally = parsePositiveLongAscii(index, length, i, end);
            return negative ? -tally : tally;
        }
        else if (negative)
        {
            return -parseLongAsciiOverflowCheck(index, length, LONG_MIN_VALUE_DIGITS, i, end);
        }
        else
        {
            return parseLongAsciiOverflowCheck(index, length, LONG_MAX_VALUE_DIGITS, i, end);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int putIntAscii(final int index, final int value)
    {
        if (0 == value)
        {
            putByte(index, ZERO);
            return 1;
        }

        final Object dest = array;
        long offset = addressOffset + index;
        int quotient = value;
        final int digitCount, length;
        if (value < 0)
        {
            if (Integer.MIN_VALUE == value)
            {
                putBytes(index, MIN_INTEGER_VALUE);
                return MIN_INTEGER_VALUE.length;
            }

            quotient = -quotient;
            digitCount = digitCount(quotient);
            length = digitCount + 1;

            if (SHOULD_BOUNDS_CHECK)
            {
                boundsCheck0(index, length);
            }

            UNSAFE.putByte(dest, offset, MINUS_SIGN);
            offset++;
        }
        else
        {
            length = digitCount = digitCount(quotient);

            if (SHOULD_BOUNDS_CHECK)
            {
                boundsCheck0(index, length);
            }
        }

        putPositiveIntAscii(dest, offset, quotient, digitCount);

        return length;
    }

    /**
     * {@inheritDoc}
     */
    public int putNaturalIntAscii(final int index, final int value)
    {
        if (0 == value)
        {
            putByte(index, ZERO);
            return 1;
        }

        final int digitCount = digitCount(value);

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, digitCount);
        }

        putPositiveIntAscii(array, addressOffset + index, value, digitCount);

        return digitCount;
    }

    /**
     * {@inheritDoc}
     */
    public void putNaturalPaddedIntAscii(final int offset, final int length, final int value)
    {
        final int end = offset + length;
        int remainder = value;
        for (int index = end - 1; index >= offset; index--)
        {
            final int digit = remainder % 10;
            remainder = remainder / 10;
            putByte(index, (byte)(ZERO + digit));
        }

        if (remainder != 0)
        {
            throw new NumberFormatException("Cannot write " + value + " in " + length + " bytes");
        }
    }

    /**
     * {@inheritDoc}
     */
    public int putNaturalIntAsciiFromEnd(final int value, final int endExclusive)
    {
        int remainder = value;
        int index = endExclusive;
        while (remainder > 0)
        {
            index--;
            final int digit = remainder % 10;
            remainder = remainder / 10;
            putByte(index, (byte)(ZERO + digit));
        }

        return index;
    }

    /**
     * {@inheritDoc}
     */
    public int putNaturalLongAscii(final int index, final long value)
    {
        if (0L == value)
        {
            putByte(index, ZERO);
            return 1;
        }

        final int digitCount = digitCount(value);

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, digitCount);
        }

        putPositiveLongAscii(array, addressOffset + index, value, digitCount);

        return digitCount;
    }

    /**
     * {@inheritDoc}
     */
    public int putLongAscii(final int index, final long value)
    {
        if (0L == value)
        {
            putByte(index, ZERO);
            return 1;
        }

        final Object dest = array;
        long offset = addressOffset + index;
        long quotient = value;
        final int digitCount, length;
        if (value < 0)
        {
            if (Long.MIN_VALUE == value)
            {
                putBytes(index, MIN_LONG_VALUE);
                return MIN_LONG_VALUE.length;
            }

            quotient = -quotient;
            digitCount = digitCount(quotient);
            length = digitCount + 1;

            if (SHOULD_BOUNDS_CHECK)
            {
                boundsCheck0(index, length);
            }

            UNSAFE.putByte(dest, offset, MINUS_SIGN);
            offset++;
        }
        else
        {
            length = digitCount = digitCount(quotient);

            if (SHOULD_BOUNDS_CHECK)
            {
                boundsCheck0(index, length);
            }
        }

        putPositiveLongAscii(dest, offset, quotient, digitCount);

        return length;
    }

    /**
     * {@inheritDoc}
     */
    public void boundsCheck(final int index, final int length)
    {
        boundsCheck0(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public int wrapAdjustment()
    {
        final long offset;
        final Object arr = array;
        if (null != arr)
        {
            offset = (arr instanceof byte[]) ? ARRAY_BASE_OFFSET : LONG_ARRAY_BASE_OFFSET;
        }
        else
        {
            offset = address(byteBuffer);
        }

        return (int)(addressOffset - offset);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }

        final UnsafeBuffer that = (UnsafeBuffer)obj;

        if (capacity != that.capacity)
        {
            return false;
        }

        final Object thisArray = this.array;
        final Object thatArray = that.array;
        final long thisOffset = this.addressOffset;
        final long thatOffset = that.addressOffset;

        // TODO: Compare by 8...

        for (int i = 0, length = capacity; i < length; i++)
        {
            if (UNSAFE.getByte(thisArray, thisOffset + i) != UNSAFE.getByte(thatArray, thatOffset + i))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        int hashCode = 1;

        final Object array = this.array;
        final long addressOffset = this.addressOffset;
        for (int i = 0, length = capacity; i < length; i++)
        {
            hashCode = 31 * hashCode + UNSAFE.getByte(array, addressOffset + i);
        }

        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(final DirectBuffer that)
    {
        final int thisCapacity = this.capacity;
        final int thatCapacity = that.capacity();
        final Object thisArray = this.array;
        final Object thatArray = that.array();
        final long thisOffset = this.addressOffset;
        final long thatOffset = that.addressOffset();

        for (int i = 0, length = Math.min(thisCapacity, thatCapacity); i < length; i++)
        {
            final int cmp = Byte.compare(
                UNSAFE.getByte(thisArray, thisOffset + i),
                UNSAFE.getByte(thatArray, thatOffset + i));

            if (0 != cmp)
            {
                return cmp;
            }
        }

        if (thisCapacity != thatCapacity)
        {
            return thisCapacity - thatCapacity;
        }

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "UnsafeBuffer{" +
            "addressOffset=" + addressOffset +
            ", capacity=" + capacity +
            ", array=" + array +
            ", byteBuffer=" + byteBuffer +
            '}';
    }

    private void boundsCheck(final int index)
    {
        if (index < 0 || index >= capacity)
        {
            throw new IndexOutOfBoundsException("index=" + index + " capacity=" + capacity);
        }
    }

    private void boundsCheck0(final int index, final int length)
    {
        final long resultingPosition = index + (long)length;
        if (index < 0 || length < 0 || resultingPosition > capacity)
        {
            throw new IndexOutOfBoundsException("index=" + index + " length=" + length + " capacity=" + capacity);
        }
    }

    private void checkAlignment(final int index, final int alignment)
    {
        final long offset = addressOffset + index;
        if (0 != (offset & (alignment - 1)) || array instanceof byte[])
        {
            throw new IllegalArgumentException(
                "unaligned atomic operation: (addressOffset + index)=" + offset + " is not divisible by " + alignment);
        }
    }

    private int parsePositiveIntAscii(
        final int index, final int length, final int startIndex, final int end)
    {
        final long offset = addressOffset;
        final Object src = array;
        int i = startIndex;
        int tally = 0, quartet;
        while ((end - i) >= 4 && isFourDigitsAsciiEncodedNumber(quartet = UNSAFE.getInt(src, offset + i)))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                quartet = Integer.reverseBytes(quartet);
            }

            tally = (tally * 10_000) + parseFourDigitsLittleEndian(quartet);
            i += 4;
        }

        byte digit;
        while (i < end && isDigit(digit = UNSAFE.getByte(src, offset + i)))
        {
            tally = (tally * 10) + (digit - 0x30);
            i++;
        }

        if (i != end)
        {
            throwParseIntError(index, length);
        }

        return tally;
    }

    private long parsePositiveIntAsciiOverflowCheck(
        final int index, final int length, final int startIndex, final int end)
    {
        if ((end - startIndex) > INT_MAX_DIGITS)
        {
            throwParseIntOverflowError(index, length);
        }

        final long offset = addressOffset;
        final Object src = array;
        int i = startIndex;
        long tally = 0;
        long octet = UNSAFE.getLong(src, offset + i);
        if (isEightDigitAsciiEncodedNumber(octet))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                octet = Long.reverseBytes(octet);
            }
            tally = parseEightDigitsLittleEndian(octet);
            i += 8;

            byte digit;
            while (i < end && isDigit(digit = UNSAFE.getByte(src, offset + i)))
            {
                tally = (tally * 10L) + (digit - 0x30);
                i++;
            }
        }

        if (i != end)
        {
            throwParseIntError(index, length);
        }

        return tally;
    }

    private void throwParseIntError(final int index, final int length)
    {
        throw new AsciiNumberFormatException("error parsing int: " + getStringWithoutLengthAscii(index, length));
    }

    private void throwParseIntOverflowError(final int index, final int length)
    {
        throw new AsciiNumberFormatException("int overflow parsing: " + getStringWithoutLengthAscii(index, length));
    }

    private long parsePositiveLongAscii(final int index, final int length, final int startIndex, final int end)
    {
        final long offset = addressOffset;
        final Object src = array;
        int i = startIndex;
        long tally = 0, octet;
        while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = UNSAFE.getLong(src, offset + i)))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                octet = Long.reverseBytes(octet);
            }

            tally = (tally * 100_000_000L) + parseEightDigitsLittleEndian(octet);
            i += 8;
        }

        int quartet;
        while ((end - i) >= 4 && isFourDigitsAsciiEncodedNumber(quartet = UNSAFE.getInt(src, offset + i)))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                quartet = Integer.reverseBytes(quartet);
            }

            tally = (tally * 10_000L) + parseFourDigitsLittleEndian(quartet);
            i += 4;
        }

        byte digit;
        while (i < end && isDigit(digit = UNSAFE.getByte(src, offset + i)))
        {
            tally = (tally * 10) + (digit - 0x30);
            i++;
        }

        if (i != end)
        {
            throwParseLongError(index, length);
        }

        return tally;
    }

    private long parseLongAsciiOverflowCheck(
        final int index,
        final int length,
        final int[] maxValue,
        final int startIndex,
        final int end)
    {
        if ((end - startIndex) > LONG_MAX_DIGITS)
        {
            throwParseLongOverflowError(index, length);
        }

        final long offset = addressOffset;
        final Object src = array;
        int i = startIndex, k = 0;
        boolean checkOverflow = true;
        long tally = 0, octet;
        while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = UNSAFE.getLong(src, offset + i)))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                octet = Long.reverseBytes(octet);
            }

            final int eightDigits = parseEightDigitsLittleEndian(octet);
            if (checkOverflow)
            {
                if (eightDigits > maxValue[k])
                {
                    throwParseLongOverflowError(index, length);
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
        while (i < end && isDigit(digit = UNSAFE.getByte(src, offset + i)))
        {
            lastDigits = (lastDigits * 10) + (digit - 0x30);
            i++;
        }

        if (i != end)
        {
            throwParseLongError(index, length);
        }
        else if (checkOverflow && lastDigits > maxValue[k])
        {
            throwParseLongOverflowError(index, length);
        }

        return (tally * 1000L) + lastDigits;
    }

    private void throwParseLongError(final int index, final int length)
    {
        throw new AsciiNumberFormatException("error parsing long: " + getStringWithoutLengthAscii(index, length));
    }

    private void throwParseLongOverflowError(final int index, final int length)
    {
        throw new AsciiNumberFormatException("long overflow parsing: " + getStringWithoutLengthAscii(index, length));
    }

    private static void boundsCheckWrap(final int offset, final int length, final int capacity)
    {
        if (offset < 0)
        {
            throw new IllegalArgumentException("invalid offset: " + offset);
        }

        if (length < 0)
        {
            throw new IllegalArgumentException("invalid length: " + length);
        }

        if ((offset > capacity - length) || (length > capacity - offset))
        {
            throw new IllegalArgumentException(
                "offset=" + offset + " length=" + length + " not valid for capacity=" + capacity);
        }
    }

    private static void putPositiveIntAscii(final Object dest, final long offset, final int value, final int digitCount)
    {
        int i = digitCount;
        int quotient = value;
        while (quotient >= 10_000)
        {
            final int lastFourDigits = quotient % 10_000;
            quotient /= 10_000;

            final int p1 = (lastFourDigits / 100) << 1;
            final int p2 = (lastFourDigits % 100) << 1;

            i -= 4;

            UNSAFE.putByte(dest, offset + i, ASCII_DIGITS[p1]);
            UNSAFE.putByte(dest, offset + i + 1, ASCII_DIGITS[p1 + 1]);
            UNSAFE.putByte(dest, offset + i + 2, ASCII_DIGITS[p2]);
            UNSAFE.putByte(dest, offset + i + 3, ASCII_DIGITS[p2 + 1]);
        }

        if (quotient >= 100)
        {
            final int position = (quotient % 100) << 1;
            quotient /= 100;
            UNSAFE.putByte(dest, offset + i - 1, ASCII_DIGITS[position + 1]);
            UNSAFE.putByte(dest, offset + i - 2, ASCII_DIGITS[position]);
        }

        if (quotient >= 10)
        {
            final int position = quotient << 1;
            UNSAFE.putByte(dest, offset + 1, ASCII_DIGITS[position + 1]);
            UNSAFE.putByte(dest, offset, ASCII_DIGITS[position]);
        }
        else
        {
            UNSAFE.putByte(dest, offset, (byte)(ZERO + quotient));
        }
    }

    private static void putPositiveLongAscii(
        final Object dest, final long offset, final long value, final int digitCount)
    {
        long quotient = value;
        int i = digitCount;
        while (quotient >= 100_000_000)
        {
            final int lastEightDigits = (int)(quotient % 100_000_000);
            quotient /= 100_000_000;

            final int upperPart = lastEightDigits / 10_000;
            final int lowerPart = lastEightDigits % 10_000;

            final int u1 = (upperPart / 100) << 1;
            final int u2 = (upperPart % 100) << 1;
            final int l1 = (lowerPart / 100) << 1;
            final int l2 = (lowerPart % 100) << 1;

            i -= 8;

            UNSAFE.putByte(dest, offset + i, ASCII_DIGITS[u1]);
            UNSAFE.putByte(dest, offset + i + 1, ASCII_DIGITS[u1 + 1]);
            UNSAFE.putByte(dest, offset + i + 2, ASCII_DIGITS[u2]);
            UNSAFE.putByte(dest, offset + i + 3, ASCII_DIGITS[u2 + 1]);
            UNSAFE.putByte(dest, offset + i + 4, ASCII_DIGITS[l1]);
            UNSAFE.putByte(dest, offset + i + 5, ASCII_DIGITS[l1 + 1]);
            UNSAFE.putByte(dest, offset + i + 6, ASCII_DIGITS[l2]);
            UNSAFE.putByte(dest, offset + i + 7, ASCII_DIGITS[l2 + 1]);
        }

        putPositiveIntAscii(dest, offset, (int)quotient, i);
    }
}
