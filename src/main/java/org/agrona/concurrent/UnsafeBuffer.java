/*
 * Copyright 2014-2015 Real Logic Ltd.
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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.agrona.BitUtil.*;
import static org.agrona.UnsafeAccess.UNSAFE;
import static org.agrona.concurrent.BufferUtil.ARRAY_BASE_OFFSET;
import static org.agrona.concurrent.BufferUtil.NATIVE_BYTE_ORDER;
import static org.agrona.concurrent.BufferUtil.NULL_BYTES;

/**
 * Supports regular, byte ordered, and atomic (memory ordered) access to an underlying buffer.
 * The buffer can be a byte[] or one of the various {@link ByteBuffer} implementations.
 *
 * {@link ByteOrder} of a wrapped buffer is not applied to the {@link UnsafeBuffer}; {@link UnsafeBuffer}s are
 * stateless and can be used concurrently. To control {@link ByteOrder} use the appropriate accessor method
 * with the {@link ByteOrder} overload.
 *
 * Note: this class has a natural ordering that is inconsistent with equals.
 * Types my be different but equal on buffer contents.
 */
public class UnsafeBuffer implements AtomicBuffer, Comparable<DirectBuffer>
{
    /**
     * Buffer alignment to ensure atomic word accesses.
     */
    public static final int ALIGNMENT = SIZE_OF_LONG;

    public static final String DISABLE_BOUNDS_CHECKS_PROP_NAME = "agrona.disable.bounds.checks";
    public static final boolean SHOULD_BOUNDS_CHECK = !Boolean.getBoolean(DISABLE_BOUNDS_CHECKS_PROP_NAME);

    private long addressOffset;
    private int capacity;
    private byte[] byteArray;
    private ByteBuffer byteBuffer;

    /**
     * Attach a view to a byte[] for providing direct access.
     *
     * @param buffer to which the view is attached.
     */
    public UnsafeBuffer(final byte[] buffer)
    {
        wrap(buffer);
    }

    /**
     * Attach a view to a byte[] for providing direct access.
     *
     * @param buffer to which the view is attached.
     * @param offset within the buffer to begin.
     * @param length of the buffer to be included.
     */
    public UnsafeBuffer(final byte[] buffer, final int offset, final int length)
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
     * @param offset within the buffer to begin.
     * @param length of the buffer to be included.
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
     * @param offset within the buffer to begin.
     * @param length of the buffer to be included.
     */
    public UnsafeBuffer(final DirectBuffer buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Attach a view to an off-heap memory region by address.
     *
     * @param address where the memory begins off-heap
     * @param length  of the buffer from the given address
     */
    public UnsafeBuffer(final long address, final int length)
    {
        wrap(address, length);
    }

    public void wrap(final byte[] buffer)
    {
        addressOffset = ARRAY_BASE_OFFSET;
        capacity = buffer.length;
        byteArray = buffer;
        byteBuffer = null;
    }

    public void wrap(final byte[] buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            final int bufferLength = buffer.length;
            if (offset != 0 && (offset < 0 || offset > bufferLength - 1))
            {
                throw new IllegalArgumentException("offset=" + offset + " not valid for buffer.length=" + bufferLength);
            }

            if (length < 0 || length > bufferLength - offset)
            {
                throw new IllegalArgumentException(
                    "offset=" + offset + " length=" + length + " not valid for buffer.length=" + bufferLength);
            }
        }

        addressOffset = ARRAY_BASE_OFFSET + offset;
        capacity = length;
        byteArray = buffer;
        byteBuffer = null;
    }

    public void wrap(final ByteBuffer buffer)
    {
        byteBuffer = buffer;

        if (buffer.hasArray())
        {
            byteArray = buffer.array();
            addressOffset = ARRAY_BASE_OFFSET + buffer.arrayOffset();
        }
        else
        {
            byteArray = null;
            addressOffset = BufferUtil.address(buffer);
        }

        capacity = buffer.capacity();
    }

    public void wrap(final ByteBuffer buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            final int bufferCapacity = buffer.capacity();
            if (offset != 0 && (offset < 0 || offset > bufferCapacity - 1))
            {
                throw new IllegalArgumentException("offset=" + offset + " not valid for buffer.capacity()=" + bufferCapacity);
            }

            if (length < 0 || length > bufferCapacity - offset)
            {
                throw new IllegalArgumentException(
                    "offset=" + offset + " length=" + length + " not valid for buffer.capacity()=" + bufferCapacity);
            }
        }

        byteBuffer = buffer;

        if (buffer.hasArray())
        {
            byteArray = buffer.array();
            addressOffset = ARRAY_BASE_OFFSET + buffer.arrayOffset() + offset;
        }
        else
        {
            byteArray = null;
            addressOffset = BufferUtil.address(buffer);
        }

        capacity = length;
    }

    public void wrap(final DirectBuffer buffer)
    {
        addressOffset = buffer.addressOffset();
        capacity = buffer.capacity();
        byteArray = buffer.byteArray();
        byteBuffer = buffer.byteBuffer();
    }

    public void wrap(final DirectBuffer buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            final int bufferCapacity = buffer.capacity();
            if (offset != 0 && (offset < 0 || offset > bufferCapacity - 1))
            {
                throw new IllegalArgumentException("offset=" + offset + " not valid for buffer.capacity()=" + bufferCapacity);
            }

            if (length < 0 || length > bufferCapacity - offset)
            {
                throw new IllegalArgumentException(
                    "offset=" + offset + " length=" + length + " not valid for buffer.capacity()=" + bufferCapacity);
            }
        }

        addressOffset = buffer.addressOffset() + offset;
        capacity = length;
        byteArray = buffer.byteArray();
        byteBuffer = buffer.byteBuffer();
    }

    public void wrap(final long address, final int length)
    {
        addressOffset = address;
        capacity = length;
        byteArray = null;
        byteBuffer = null;
    }

    public long addressOffset()
    {
        return addressOffset;
    }

    public byte[] byteArray()
    {
        return byteArray;
    }

    public ByteBuffer byteBuffer()
    {
        return byteBuffer;
    }

    public void setMemory(final int index, final int length, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        UNSAFE.setMemory(byteArray, addressOffset + index, length, value);
    }

    public int capacity()
    {
        return capacity;
    }

    public void checkLimit(final int limit)
    {
        if (limit > capacity)
        {
            final String msg = String.format("limit=%d is beyond capacity=%d", limit, capacity);
            throw new IndexOutOfBoundsException(msg);
        }
    }

    public void verifyAlignment()
    {
        if (0 != (addressOffset & (ALIGNMENT - 1)))
        {
            throw new IllegalStateException(String.format(
                "AtomicBuffer is not correctly aligned: addressOffset=%d in not divisible by %d",
                addressOffset,
                ALIGNMENT));
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public long getLong(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        long bits = UNSAFE.getLong(byteArray, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Long.reverseBytes(bits);
        }

        return bits;
    }

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

        UNSAFE.putLong(byteArray, addressOffset + index, bits);
    }

    public long getLong(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getLong(byteArray, addressOffset + index);
    }

    public void putLong(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putLong(byteArray, addressOffset + index, value);
    }

    public long getLongVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getLongVolatile(byteArray, addressOffset + index);
    }

    public void putLongVolatile(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putLongVolatile(byteArray, addressOffset + index, value);
    }

    public void putLongOrdered(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putOrderedLong(byteArray, addressOffset + index, value);
    }

    public long addLongOrdered(final int index, final long increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        final long offset = addressOffset + index;
        final byte[] byteArray = this.byteArray;
        final long value = UNSAFE.getLong(byteArray, offset);
        UNSAFE.putOrderedLong(byteArray, offset, value + increment);

        return value;
    }

    public boolean compareAndSetLong(final int index, final long expectedValue, final long updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.compareAndSwapLong(byteArray, addressOffset + index, expectedValue, updateValue);
    }

    public long getAndSetLong(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndSetLong(byteArray, addressOffset + index, value);
    }

    public long getAndAddLong(final int index, final long delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndAddLong(byteArray, addressOffset + index, delta);
    }

    ///////////////////////////////////////////////////////////////////////////

    public int getInt(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        int bits = UNSAFE.getInt(byteArray, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        return bits;
    }

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

        UNSAFE.putInt(byteArray, addressOffset + index, bits);
    }

    public int getInt(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getInt(byteArray, addressOffset + index);
    }

    public void putInt(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putInt(byteArray, addressOffset + index, value);
    }

    public int getIntVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getIntVolatile(byteArray, addressOffset + index);
    }

    public void putIntVolatile(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putIntVolatile(byteArray, addressOffset + index, value);
    }

    public void putIntOrdered(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putOrderedInt(byteArray, addressOffset + index, value);
    }

    public int addIntOrdered(final int index, final int increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        final long offset = addressOffset + index;
        final byte[] byteArray = this.byteArray;
        final int value = UNSAFE.getInt(byteArray, offset);
        UNSAFE.putOrderedInt(byteArray, offset, value + increment);

        return value;
    }

    public boolean compareAndSetInt(final int index, final int expectedValue, final int updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.compareAndSwapInt(byteArray, addressOffset + index, expectedValue, updateValue);
    }

    public int getAndSetInt(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndSetInt(byteArray, addressOffset + index, value);
    }

    public int getAndAddInt(final int index, final int delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndAddInt(byteArray, addressOffset + index, delta);
    }

    ///////////////////////////////////////////////////////////////////////////

    public double getDouble(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = UNSAFE.getLong(byteArray, addressOffset + index);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getDouble(byteArray, addressOffset + index);
        }
    }

    public void putDouble(final int index, final double value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = Long.reverseBytes(Double.doubleToRawLongBits(value));
            UNSAFE.putLong(byteArray, addressOffset + index, bits);
        }
        else
        {
            UNSAFE.putDouble(byteArray, addressOffset + index, value);
        }
    }

    public double getDouble(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        return UNSAFE.getDouble(byteArray, addressOffset + index);
    }

    public void putDouble(final int index, final double value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        UNSAFE.putDouble(byteArray, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public float getFloat(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = UNSAFE.getInt(byteArray, addressOffset + index);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getFloat(byteArray, addressOffset + index);
        }
    }

    public void putFloat(final int index, final float value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = Integer.reverseBytes(Float.floatToRawIntBits(value));
            UNSAFE.putInt(byteArray, addressOffset + index, bits);
        }
        else
        {
            UNSAFE.putFloat(byteArray, addressOffset + index, value);
        }
    }

    public float getFloat(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        return UNSAFE.getFloat(byteArray, addressOffset + index);
    }

    public void putFloat(final int index, final float value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        UNSAFE.putFloat(byteArray, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public short getShort(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        short bits = UNSAFE.getShort(byteArray, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Short.reverseBytes(bits);
        }

        return bits;
    }

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

        UNSAFE.putShort(byteArray, addressOffset + index, bits);
    }

    public short getShort(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        return UNSAFE.getShort(byteArray, addressOffset + index);
    }

    public void putShort(final int index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        UNSAFE.putShort(byteArray, addressOffset + index, value);
    }

    public short getShortVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        return UNSAFE.getShortVolatile(byteArray, addressOffset + index);
    }

    public void putShortVolatile(final int index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        UNSAFE.putShortVolatile(byteArray, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public byte getByte(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        return UNSAFE.getByte(byteArray, addressOffset + index);
    }

    public void putByte(final int index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        UNSAFE.putByte(byteArray, addressOffset + index, value);
    }

    public byte getByteVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        return UNSAFE.getByteVolatile(byteArray, addressOffset + index);
    }

    public void putByteVolatile(final int index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        UNSAFE.putByteVolatile(byteArray, addressOffset + index, value);
    }

    public void getBytes(final int index, final byte[] dst)
    {
        getBytes(index, dst, 0, dst.length);
    }

    public void getBytes(final int index, final byte[] dst, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(dst, offset, length);
        }

        UNSAFE.copyMemory(byteArray, addressOffset + index, dst, ARRAY_BASE_OFFSET + offset, length);
    }

    public void getBytes(final int index, final MutableDirectBuffer dstBuffer, final int dstIndex, final int length)
    {
        dstBuffer.putBytes(dstIndex, this, index, length);
    }

    public void getBytes(final int index, final ByteBuffer dstBuffer, final int length)
    {
        final int dstOffset = dstBuffer.position();
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(dstBuffer, (long)dstOffset, length);
        }

        final byte[] dstByteArray;
        final long dstBaseOffset;
        if (dstBuffer.hasArray())
        {
            dstByteArray = dstBuffer.array();
            dstBaseOffset = ARRAY_BASE_OFFSET + dstBuffer.arrayOffset();
        }
        else
        {
            dstByteArray = null;
            dstBaseOffset = BufferUtil.address(dstBuffer);
        }

        UNSAFE.copyMemory(byteArray, addressOffset + index, dstByteArray, dstBaseOffset + dstOffset, length);
        dstBuffer.position(dstBuffer.position() + length);
    }

    public void putBytes(final int index, final byte[] src)
    {
        putBytes(index, src, 0, src.length);
    }

    public void putBytes(final int index, final byte[] src, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(src, offset, length);
        }

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET + offset, byteArray, addressOffset + index, length);
    }

    public void putBytes(final int index, final ByteBuffer srcBuffer, final int length)
    {
        final int srcIndex = srcBuffer.position();
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(srcBuffer, (long)srcIndex, length);
        }

        putBytes(index, srcBuffer, srcIndex, length);
        srcBuffer.position(srcIndex + length);
    }

    public void putBytes(final int index, final ByteBuffer srcBuffer, final int srcIndex, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(srcBuffer, srcIndex, length);
        }

        final byte[] srcByteArray;
        final long srcBaseOffset;
        if (srcBuffer.hasArray())
        {
            srcByteArray = srcBuffer.array();
            srcBaseOffset = ARRAY_BASE_OFFSET + srcBuffer.arrayOffset();
        }
        else
        {
            srcByteArray = null;
            srcBaseOffset = BufferUtil.address(srcBuffer);
        }

        UNSAFE.copyMemory(srcByteArray, srcBaseOffset + srcIndex, byteArray, addressOffset + index, length);
    }

    public void putBytes(final int index, final DirectBuffer srcBuffer, final int srcIndex, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            srcBuffer.boundsCheck(srcIndex, length);
        }

        UNSAFE.copyMemory(
            srcBuffer.byteArray(),
            srcBuffer.addressOffset() + srcIndex,
            byteArray,
            addressOffset + index,
            length);
    }

    ///////////////////////////////////////////////////////////////////////////

    public char getChar(final int index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        char bits = UNSAFE.getChar(byteArray, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = (char)Short.reverseBytes((short)bits);
        }

        return bits;
    }

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

        UNSAFE.putChar(byteArray, addressOffset + index, bits);
    }

    public char getChar(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        return UNSAFE.getChar(byteArray, addressOffset + index);
    }

    public void putChar(final int index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        UNSAFE.putChar(byteArray, addressOffset + index, value);
    }

    public char getCharVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        return UNSAFE.getCharVolatile(byteArray, addressOffset + index);
    }

    public void putCharVolatile(final int index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        UNSAFE.putCharVolatile(byteArray, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public String getStringUtf8(final int index)
    {
        final int length = getInt(index);

        return getStringUtf8(index, length);
    }

    public String getStringUtf8(final int index, final ByteOrder byteOrder)
    {
        final int length = getInt(index, byteOrder);

        return getStringUtf8(index, length);
    }

    public String getStringUtf8(final int index, final int length)
    {
        final byte[] stringInBytes = new byte[length];
        getBytes(index + SIZE_OF_INT, stringInBytes);

        return new String(stringInBytes, UTF_8);
    }

    public int putStringUtf8(final int index, final String value)
    {
        return putStringUtf8(index, value, Integer.MAX_VALUE);
    }

    public int putStringUtf8(final int index, final String value, final ByteOrder byteOrder)
    {
        return putStringUtf8(index, value, byteOrder, Integer.MAX_VALUE);
    }

    public int putStringUtf8(final int index, final String value, final int maxEncodedSize)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        if (bytes.length > maxEncodedSize)
        {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedSize);
        }

        putInt(index, bytes.length);
        putBytes(index + SIZE_OF_INT, bytes);

        return SIZE_OF_INT + bytes.length;
    }

    public int putStringUtf8(final int index, final String value, final ByteOrder byteOrder, final int maxEncodedSize)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        if (bytes.length > maxEncodedSize)
        {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedSize);
        }

        putInt(index, bytes.length, byteOrder);
        putBytes(index + SIZE_OF_INT, bytes);

        return SIZE_OF_INT + bytes.length;
    }

    public String getStringWithoutLengthUtf8(final int index, final int length)
    {
        final byte[] stringInBytes = new byte[length];
        getBytes(index, stringInBytes);

        return new String(stringInBytes, UTF_8);
    }

    public int putStringWithoutLengthUtf8(final int index, final String value)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        putBytes(index, bytes);

        return bytes.length;
    }

    ///////////////////////////////////////////////////////////////////////////

    private void boundsCheck(final int index)
    {
        if (index < 0 || index >= capacity)
        {
            throw new IndexOutOfBoundsException(String.format("index=%d, capacity=%d", index, capacity));
        }
    }

    private void boundsCheck0(final int index, final int length)
    {
        final long resultingPosition = index + (long)length;
        if (index < 0 || resultingPosition > capacity)
        {
            throw new IndexOutOfBoundsException(String.format("index=%d, length=%d, capacity=%d", index, length, capacity));
        }
    }

    public void boundsCheck(final int index, final int length)
    {
        boundsCheck0(index, length);
    }

    ///////////////////////////////////////////////////////////////////////////

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

        final byte[] thisByteArray = this.byteArray;
        final byte[] thatByteArray = that.byteArray;
        final long thisOffset = this.addressOffset;
        final long thatOffset = that.addressOffset;

        for (int i = 0, length = capacity; i < length; i++)
        {
            if (UNSAFE.getByte(thisByteArray, thisOffset + i) != UNSAFE.getByte(thatByteArray, thatOffset + i))
            {
                return false;
            }
        }

        return true;
    }

    public int hashCode()
    {
        int hashCode = 1;

        final byte[] byteArray = this.byteArray;
        final long addressOffset = this.addressOffset;
        for (int i = 0, length = capacity; i < length; i++)
        {
            hashCode = 31 * hashCode + UNSAFE.getByte(byteArray, addressOffset + i);
        }

        return hashCode;
    }

    public int compareTo(final DirectBuffer that)
    {
        final int thisCapacity = this.capacity();
        final int thatCapacity = that.capacity();
        final byte[] thisByteArray = this.byteArray;
        final byte[] thatByteArray = that.byteArray();
        final long thisOffset = this.addressOffset();
        final long thatOffset = that.addressOffset();

        for (int i = 0, length = Math.min(thisCapacity, thatCapacity); i < length; i++)
        {
            final int cmp = Byte.compare(
                UNSAFE.getByte(thisByteArray, thisOffset + i),
                UNSAFE.getByte(thatByteArray, thatOffset + i));

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
}
