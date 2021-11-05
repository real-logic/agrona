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
package org.agrona.concurrent;

import org.agrona.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
     * Buffer alignment to ensure atomic word accesses.
     */
    public static final int ALIGNMENT = SIZE_OF_LONG;

    /**
     * Name of the system property that specify if the bounds checks should be disabled.
     * To disable bounds checks set this property to {@code true}.
     */
    public static final String DISABLE_BOUNDS_CHECKS_PROP_NAME = "agrona.disable.bounds.checks";

    /**
     * Should bounds-checks operations be done or not. Controlled by the {@link #DISABLE_BOUNDS_CHECKS_PROP_NAME}
     * system property.
     *
     * @see #DISABLE_BOUNDS_CHECKS_PROP_NAME
     */
    public static final boolean SHOULD_BOUNDS_CHECK =
        !"true".equals(SystemUtil.getProperty(DISABLE_BOUNDS_CHECKS_PROP_NAME));

    private long addressOffset;
    private int capacity;
    private byte[] byteArray;
    private ByteBuffer byteBuffer;

    /**
     * Empty constructor for a reusable wrapper buffer.
     */
    public UnsafeBuffer()
    {
        wrap(EMPTY_BYTE_ARRAY);
    }

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

        if (buffer != byteArray)
        {
            byteArray = buffer;
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

        if (buffer != byteArray)
        {
            byteArray = buffer;
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
            byteArray = null;
            addressOffset = address(buffer);
        }
        else
        {
            byteArray = array(buffer);
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
            byteArray = null;
            addressOffset = address(buffer) + offset;
        }
        else
        {
            byteArray = array(buffer);
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

        final byte[] byteArray = buffer.byteArray();
        if (byteArray != this.byteArray)
        {
            this.byteArray = byteArray;
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

        final byte[] byteArray = buffer.byteArray();
        if (byteArray != this.byteArray)
        {
            this.byteArray = byteArray;
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
        byteArray = null;
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
    public byte[] byteArray()
    {
        return byteArray;
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

        final long offset = addressOffset + index;
        if (MEMSET_HACK_REQUIRED && length > MEMSET_HACK_THRESHOLD && 0 == (offset & 1))
        {
            // This horrible filth is to encourage the JVM to call memset() when address is even.
            UNSAFE.putByte(byteArray, offset, value);
            UNSAFE.setMemory(byteArray, offset + 1, length - 1, value);
        }
        else
        {
            UNSAFE.setMemory(byteArray, offset, length, value);
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
        if (0 != (addressOffset & (ALIGNMENT - 1)))
        {
            throw new IllegalStateException(
                "AtomicBuffer is not correctly aligned: addressOffset=" + addressOffset +
                " is not divisible by " + ALIGNMENT);
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

        long bits = UNSAFE.getLong(byteArray, addressOffset + index);
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

        UNSAFE.putLong(byteArray, addressOffset + index, bits);
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

        return UNSAFE.getLong(byteArray, addressOffset + index);
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

        UNSAFE.putLong(byteArray, addressOffset + index, value);
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

        return UNSAFE.getLongVolatile(byteArray, addressOffset + index);
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

        UNSAFE.putLongVolatile(byteArray, addressOffset + index, value);
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

        UNSAFE.putOrderedLong(byteArray, addressOffset + index, value);
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

        final long offset = addressOffset + index;
        final byte[] byteArray = this.byteArray;
        final long value = UNSAFE.getLong(byteArray, offset);
        UNSAFE.putOrderedLong(byteArray, offset, value + increment);

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

        return UNSAFE.compareAndSwapLong(byteArray, addressOffset + index, expectedValue, updateValue);
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

        return UNSAFE.getAndSetLong(byteArray, addressOffset + index, value);
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

        return UNSAFE.getAndAddLong(byteArray, addressOffset + index, delta);
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

        int bits = UNSAFE.getInt(byteArray, addressOffset + index);
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

        UNSAFE.putInt(byteArray, addressOffset + index, bits);
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

        return UNSAFE.getInt(byteArray, addressOffset + index);
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

        UNSAFE.putInt(byteArray, addressOffset + index, value);
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

        return UNSAFE.getIntVolatile(byteArray, addressOffset + index);
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

        UNSAFE.putIntVolatile(byteArray, addressOffset + index, value);
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

        UNSAFE.putOrderedInt(byteArray, addressOffset + index, value);
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

        final long offset = addressOffset + index;
        final byte[] byteArray = this.byteArray;
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

        return UNSAFE.compareAndSwapInt(byteArray, addressOffset + index, expectedValue, updateValue);
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

        return UNSAFE.getAndSetInt(byteArray, addressOffset + index, value);
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

        return UNSAFE.getAndAddInt(byteArray, addressOffset + index, delta);
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
            final long bits = UNSAFE.getLong(byteArray, addressOffset + index);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getDouble(byteArray, addressOffset + index);
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
            UNSAFE.putLong(byteArray, addressOffset + index, bits);
        }
        else
        {
            UNSAFE.putDouble(byteArray, addressOffset + index, value);
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

        return UNSAFE.getDouble(byteArray, addressOffset + index);
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

        UNSAFE.putDouble(byteArray, addressOffset + index, value);
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
            final int bits = UNSAFE.getInt(byteArray, addressOffset + index);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getFloat(byteArray, addressOffset + index);
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
            UNSAFE.putInt(byteArray, addressOffset + index, bits);
        }
        else
        {
            UNSAFE.putFloat(byteArray, addressOffset + index, value);
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

        return UNSAFE.getFloat(byteArray, addressOffset + index);
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

        UNSAFE.putFloat(byteArray, addressOffset + index, value);
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

        short bits = UNSAFE.getShort(byteArray, addressOffset + index);
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

        UNSAFE.putShort(byteArray, addressOffset + index, bits);
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

        return UNSAFE.getShort(byteArray, addressOffset + index);
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

        UNSAFE.putShort(byteArray, addressOffset + index, value);
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

        return UNSAFE.getShortVolatile(byteArray, addressOffset + index);
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

        UNSAFE.putShortVolatile(byteArray, addressOffset + index, value);
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

        return UNSAFE.getByte(byteArray, addressOffset + index);
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

        UNSAFE.putByte(byteArray, addressOffset + index, value);
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

        return UNSAFE.getByteVolatile(byteArray, addressOffset + index);
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

        UNSAFE.putByteVolatile(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final int index, final byte[] dst)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, dst.length);
        }

        UNSAFE.copyMemory(byteArray, addressOffset + index, dst, ARRAY_BASE_OFFSET, dst.length);
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

        UNSAFE.copyMemory(byteArray, addressOffset + index, dst, ARRAY_BASE_OFFSET + offset, length);
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
            dstByteArray = array(dstBuffer);
            dstBaseOffset = ARRAY_BASE_OFFSET + arrayOffset(dstBuffer);
        }

        UNSAFE.copyMemory(byteArray, addressOffset + index, dstByteArray, dstBaseOffset + dstOffset, length);
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

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET, byteArray, addressOffset + index, src.length);
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

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET + offset, byteArray, addressOffset + index, length);
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
            srcByteArray = array(srcBuffer);
            srcBaseOffset = ARRAY_BASE_OFFSET + arrayOffset(srcBuffer);
        }

        UNSAFE.copyMemory(srcByteArray, srcBaseOffset + srcIndex, byteArray, addressOffset + index, length);
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
            srcBuffer.byteArray(),
            srcBuffer.addressOffset() + srcIndex,
            byteArray,
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

        char bits = UNSAFE.getChar(byteArray, addressOffset + index);
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

        UNSAFE.putChar(byteArray, addressOffset + index, bits);
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

        return UNSAFE.getChar(byteArray, addressOffset + index);
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

        UNSAFE.putChar(byteArray, addressOffset + index, value);
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

        return UNSAFE.getCharVolatile(byteArray, addressOffset + index);
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

        UNSAFE.putCharVolatile(byteArray, addressOffset + index, value);
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

        final int length = UNSAFE.getInt(byteArray, addressOffset + index);

        return getStringAscii(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final Appendable appendable)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(byteArray, addressOffset + index);

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

        int bits = UNSAFE.getInt(byteArray, addressOffset + index);
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

        int bits = UNSAFE.getInt(byteArray, addressOffset + index);
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
        UNSAFE.copyMemory(byteArray, addressOffset + index + STR_HEADER_LEN, dst, ARRAY_BASE_OFFSET, length);

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
            for (int i = index + STR_HEADER_LEN, limit = index + STR_HEADER_LEN + length; i < limit; i++)
            {
                final char c = (char)UNSAFE.getByte(byteArray, addressOffset + i);
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

        UNSAFE.putInt(byteArray, addressOffset + index, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, addressOffset + STR_HEADER_LEN + index + i, (byte)c);
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

        UNSAFE.putInt(byteArray, addressOffset + index, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, addressOffset + STR_HEADER_LEN + index + i, (byte)c);
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

        UNSAFE.putInt(byteArray, addressOffset + index, bits);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, addressOffset + STR_HEADER_LEN + index + i, (byte)c);
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

        UNSAFE.putInt(byteArray, addressOffset + index, bits);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, addressOffset + STR_HEADER_LEN + index + i, (byte)c);
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
        UNSAFE.copyMemory(byteArray, addressOffset + index, dst, ARRAY_BASE_OFFSET, length);

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
            for (int i = index, limit = index + length; i < limit; i++)
            {
                final char c = (char)UNSAFE.getByte(byteArray, addressOffset + i);
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

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, addressOffset + index + i, (byte)c);
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

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, addressOffset + index + i, (byte)c);
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

        for (int i = 0; i < len; i++)
        {
            char c = value.charAt(valueOffset + i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, addressOffset + index + i, (byte)c);
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

        for (int i = 0; i < len; i++)
        {
            char c = value.charAt(valueOffset + i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, addressOffset + index + i, (byte)c);
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

        final int length = UNSAFE.getInt(byteArray, addressOffset + index);

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

        int bits = UNSAFE.getInt(byteArray, addressOffset + index);
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
        UNSAFE.copyMemory(byteArray, addressOffset + index + STR_HEADER_LEN, stringInBytes, ARRAY_BASE_OFFSET, length);

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

        UNSAFE.putInt(byteArray, addressOffset + index, bytes.length);
        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, byteArray, addressOffset + index + STR_HEADER_LEN, bytes.length);

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

        UNSAFE.putInt(byteArray, addressOffset + index, bits);
        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, byteArray, addressOffset + index + STR_HEADER_LEN, bytes.length);

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
        UNSAFE.copyMemory(byteArray, addressOffset + index, stringInBytes, ARRAY_BASE_OFFSET, length);

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

        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, byteArray, addressOffset + index, bytes.length);

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
        else if (length > INT_MAX_DIGITS)
        {
            throw new AsciiNumberFormatException("int overflow parsing: " + getStringWithoutLengthAscii(index, length));
        }

        final long offset = addressOffset;
        final byte[] src = byteArray;
        final int firstDigit = AsciiEncoding.getDigit(index, UNSAFE.getByte(src, offset + index));

        if (length < INT_MAX_DIGITS || firstDigit < 2)
        {
            int tally = firstDigit;
            for (int i = index + 1, end = index + length; i < end; i++)
            {
                tally = (tally * 10) + AsciiEncoding.getDigit(i, UNSAFE.getByte(src, offset + i));
            }
            return tally;
        }
        else
        {
            long tally = firstDigit;
            for (int i = index + 1, end = index + length; i < end; i++)
            {
                tally = (tally * 10L) + AsciiEncoding.getDigit(i, UNSAFE.getByte(src, offset + i));
            }

            if (tally >= INTEGER_ABSOLUTE_MIN_VALUE)
            {
                throw new AsciiNumberFormatException("int overflow parsing: " +
                    getStringWithoutLengthAscii(index, length));
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
        else if (length > LONG_MAX_DIGITS)
        {
            throw new AsciiNumberFormatException("long overflow parsing: " +
                getStringWithoutLengthAscii(index, length));
        }

        final long offset = addressOffset;
        final byte[] src = byteArray;
        final int firstDigit = AsciiEncoding.getDigit(index, UNSAFE.getByte(src, offset + index));

        if (length < LONG_MAX_DIGITS || firstDigit < 9)
        {
            long tally = firstDigit;
            for (int i = index + 1, end = index + length; i < end; i++)
            {
                tally = (tally * 10L) + AsciiEncoding.getDigit(i, UNSAFE.getByte(src, offset + i));
            }
            return tally;
        }
        else
        {
            return parseLongAsciiOverflowCheck(index, length, src, offset, MAX_LONG_VALUE, 1, firstDigit);
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
        else if (1 == length)
        {
            return AsciiEncoding.getDigit(index, UNSAFE.getByte(byteArray, addressOffset + index));
        }

        final long offset = addressOffset;
        final byte[] src = byteArray;
        final byte first = UNSAFE.getByte(src, offset + index);
        final boolean negativeValue = MINUS_SIGN == first;
        final int digitCount;
        final int firstDigit;
        int i = index + 1;
        if (negativeValue)
        {
            digitCount = length - 1;
            firstDigit = getDigit(i, UNSAFE.getByte(src, offset + i));
            i++;
        }
        else
        {
            digitCount = length;
            firstDigit = getDigit(index, first);
        }

        if (digitCount < INT_MAX_DIGITS || INT_MAX_DIGITS == digitCount && firstDigit < 2)
        {
            int tally = firstDigit;
            for (int end = index + length; i < end; i++)
            {
                tally = (tally * 10) + AsciiEncoding.getDigit(i, UNSAFE.getByte(src, offset + i));
            }
            return negativeValue ? -tally : tally;
        }
        else if (INT_MAX_DIGITS == digitCount)
        {
            long tally = firstDigit;
            for (int end = index + length; i < end; i++)
            {
                tally = (tally * 10L) + AsciiEncoding.getDigit(i, UNSAFE.getByte(src, offset + i));
            }

            if (tally < INTEGER_ABSOLUTE_MIN_VALUE || tally == INTEGER_ABSOLUTE_MIN_VALUE && negativeValue)
            {
                return (int)(negativeValue ? -tally : tally);
            }
        }

        throw new AsciiNumberFormatException("int overflow parsing: " + getStringWithoutLengthAscii(index, length));
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
        else if (1 == length)
        {
            return AsciiEncoding.getDigit(index, UNSAFE.getByte(byteArray, addressOffset + index));
        }

        final long offset = addressOffset;
        final byte[] src = byteArray;
        final byte first = UNSAFE.getByte(src, offset + index);
        final boolean negativeValue = MINUS_SIGN == first;
        final int digitCount;
        final int firstDigit;
        int i = index + 1;
        if (negativeValue)
        {
            digitCount = length - 1;
            firstDigit = getDigit(i, UNSAFE.getByte(src, offset + i));
            i++;
        }
        else
        {
            digitCount = length;
            firstDigit = getDigit(index, first);
        }

        if (digitCount < LONG_MAX_DIGITS || LONG_MAX_DIGITS == digitCount && firstDigit < 9)
        {
            long tally = firstDigit;
            for (int end = index + length; i < end; i++)
            {
                tally = (tally * 10L) + AsciiEncoding.getDigit(i, UNSAFE.getByte(src, offset + i));
            }
            return negativeValue ? -tally : tally;
        }
        else if (LONG_MAX_DIGITS == digitCount)
        {
            if (negativeValue)
            {
                return -parseLongAsciiOverflowCheck(index, length, src, offset, MIN_LONG_VALUE, 2, firstDigit);
            }
            else
            {
                return parseLongAsciiOverflowCheck(index, length, src, offset, MAX_LONG_VALUE, 1, firstDigit);
            }
        }

        throw new AsciiNumberFormatException("long overflow parsing: " + getStringWithoutLengthAscii(index, length));
    }

    /**
     * {@inheritDoc}
     */
    public int putIntAscii(final int index, final int value)
    {
        if (value == 0)
        {
            putByte(index, ZERO);
            return 1;
        }

        if (value == Integer.MIN_VALUE)
        {
            putBytes(index, MIN_INTEGER_VALUE);
            return MIN_INTEGER_VALUE.length;
        }

        final byte[] dest = byteArray;
        long offset = addressOffset + index;
        int quotient = value;
        final int digitCount, length;
        if (value < 0)
        {
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
        if (value == 0)
        {
            putByte(index, ZERO);
            return 1;
        }

        final int digitCount = digitCount(value);

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, digitCount);
        }

        putPositiveIntAscii(byteArray, addressOffset + index, value, digitCount);

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
        if (value == 0)
        {
            putByte(index, ZERO);
            return 1;
        }

        final int digitCount = digitCount(value);

        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, digitCount);
        }

        putPositiveLongAscii(byteArray, addressOffset + index, value, digitCount);

        return digitCount;
    }

    /**
     * {@inheritDoc}
     */
    public int putLongAscii(final int index, final long value)
    {
        if (value == 0)
        {
            putByte(index, ZERO);
            return 1;
        }

        if (value == Long.MIN_VALUE)
        {
            putBytes(index, MIN_LONG_VALUE);
            return MIN_LONG_VALUE.length;
        }

        final byte[] dest = byteArray;
        long offset = addressOffset + index;
        long quotient = value;
        final int digitCount, length;
        if (value < 0)
        {
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
        final long offset = byteArray != null ? ARRAY_BASE_OFFSET : BufferUtil.address(byteBuffer);

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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public int compareTo(final DirectBuffer that)
    {
        final int thisCapacity = this.capacity;
        final int thatCapacity = that.capacity();
        final byte[] thisByteArray = this.byteArray;
        final byte[] thatByteArray = that.byteArray();
        final long thisOffset = this.addressOffset;
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

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "UnsafeBuffer{" +
            "addressOffset=" + addressOffset +
            ", capacity=" + capacity +
            ", byteArray=" + byteArray + // lgtm [java/print-array]
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

    private long parseLongAsciiOverflowCheck(
        final int index,
        final int length,
        final byte[] src,
        final long offset,
        final byte[] maxValue,
        final int position,
        final int first)
    {
        long tally = first;
        boolean checkOverflow = true;
        for (int i = index + position, end = index + length; i < end; i++)
        {
            final byte b = UNSAFE.getByte(src, offset + i);
            if (checkOverflow)
            {
                if (b > maxValue[i - index])
                {
                    throw new AsciiNumberFormatException("long overflow parsing: " +
                        getStringWithoutLengthAscii(index, length));
                }
                else if (b < maxValue[i - index])
                {
                    checkOverflow = false;
                }
            }
            tally = (tally * 10L) + AsciiEncoding.getDigit(i, b);
        }
        return tally;
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

    private static void putPositiveIntAscii(final byte[] dest, final long offset, final int value, final int digitCount)
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
        final byte[] dest, final long offset, final long value, final int digitCount)
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
