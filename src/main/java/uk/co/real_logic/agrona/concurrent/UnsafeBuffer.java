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
package uk.co.real_logic.agrona.concurrent;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.UnsafeAccess;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static uk.co.real_logic.agrona.BitUtil.*;
import static uk.co.real_logic.agrona.concurrent.MemoryAccess.memory;

/**
 * Supports regular, byte ordered, and atomic (memory ordered) access to an underlying buffer.
 * The buffer can be a byte[] or one of the various {@link ByteBuffer} implementations.
 *
 * {@link ByteOrder} of a wrapped buffer is not applied to the {@link UnsafeBuffer}; {@link UnsafeBuffer}s are
 * stateless and can be used concurrently. To control {@link ByteOrder} use the appropriate accessor method
 * with the {@link ByteOrder} overload.
 */
public class UnsafeBuffer implements AtomicBuffer
{
    /**
     * Buffer alignment to ensure atomic word accesses.
     */
    public static final int ALIGNMENT = SIZE_OF_LONG;

    public static final String DISABLE_BOUNDS_CHECKS_PROP_NAME = "agrona.disable.bounds.checks";
    public static final boolean SHOULD_BOUNDS_CHECK = !Boolean.getBoolean(DISABLE_BOUNDS_CHECKS_PROP_NAME);

    static final byte[] NULL_BYTES = "null".getBytes(StandardCharsets.UTF_8);
    static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();
    static final long ARRAY_BASE_OFFSET = UnsafeAccess.UNSAFE.arrayBaseOffset(byte[].class);

    private byte[] byteArray;
    private ByteBuffer byteBuffer;
    private long addressOffset;

    private int capacity;

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
    public UnsafeBuffer(final DirectBuffer buffer, final long offset, final int length)
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
            addressOffset = ((sun.nio.ch.DirectBuffer)buffer).address();
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
            addressOffset = ((sun.nio.ch.DirectBuffer)buffer).address() + offset;
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

    public void wrap(final DirectBuffer buffer, final long offset, final int length)
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

    public void setMemory(final long index, final int length, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, length);
        }

        memory().setMemory(byteArray, addressOffset + index, length, value);
    }

    public int capacity()
    {
        return capacity;
    }

    public void checkLimit(final long limit)
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

    public long getLong(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        long bits = memory().getLong(byteArray, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Long.reverseBytes(bits);
        }

        return bits;
    }

    public void putLong(final long index, final long value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        long bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Long.reverseBytes(bits);
        }

        memory().putLong(byteArray, addressOffset + index, bits);
    }

    public long getLong(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().getLong(byteArray, addressOffset + index);
    }

    public void putLong(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        memory().putLong(byteArray, addressOffset + index, value);
    }

    public long getLongVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().getLongVolatile(byteArray, addressOffset + index);
    }

    public void putLongVolatile(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        memory().putLongVolatile(byteArray, addressOffset + index, value);
    }

    public void putLongOrdered(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        memory().putOrderedLong(byteArray, addressOffset + index, value);
    }

    public long addLongOrdered(final long index, final long increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        final long offset = addressOffset + index;
        final byte[] byteArray = this.byteArray;
        final long value = memory().getLong(byteArray, offset);
        memory().putOrderedLong(byteArray, offset, value + increment);

        return value;
    }

    public boolean compareAndSetLong(final long index, final long expectedValue, final long updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().compareAndSwapLong(byteArray, addressOffset + index, expectedValue, updateValue);
    }

    public long getAndSetLong(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().getAndSetLong(byteArray, addressOffset + index, value);
    }

    public long getAndAddLong(final long index, final long delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().getAndAddLong(byteArray, addressOffset + index, delta);
    }

    ///////////////////////////////////////////////////////////////////////////

    public int getInt(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        int bits = memory().getInt(byteArray, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        return bits;
    }

    public void putInt(final long index, final int value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        int bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        memory().putInt(byteArray, addressOffset + index, bits);
    }

    public int getInt(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().getInt(byteArray, addressOffset + index);
    }

    public void putInt(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        memory().putInt(byteArray, addressOffset + index, value);
    }

    public int getIntVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().getIntVolatile(byteArray, addressOffset + index);
    }

    public void putIntVolatile(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        memory().putIntVolatile(byteArray, addressOffset + index, value);
    }

    public void putIntOrdered(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        memory().putOrderedInt(byteArray, addressOffset + index, value);
    }

    public int addIntOrdered(final long index, final int increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        final long offset = addressOffset + index;
        final byte[] byteArray = this.byteArray;
        final int value = memory().getInt(byteArray, offset);
        memory().putOrderedInt(byteArray, offset, value + increment);

        return value;
    }

    public boolean compareAndSetInt(final long index, final int expectedValue, final int updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().compareAndSwapInt(byteArray, addressOffset + index, expectedValue, updateValue);
    }

    public int getAndSetInt(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().getAndSetInt(byteArray, addressOffset + index, value);
    }

    public int getAndAddInt(final long index, final int delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().getAndAddInt(byteArray, addressOffset + index, delta);
    }

    ///////////////////////////////////////////////////////////////////////////

    public double getDouble(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_DOUBLE);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = memory().getLong(byteArray, addressOffset + index);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
        else
        {
            return memory().getDouble(byteArray, addressOffset + index);
        }
    }

    public void putDouble(final long index, final double value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_DOUBLE);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = Long.reverseBytes(Double.doubleToRawLongBits(value));
            memory().putLong(byteArray, addressOffset + index, bits);
        }
        else
        {
            memory().putDouble(byteArray, addressOffset + index, value);
        }
    }

    public double getDouble(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_DOUBLE);
        }

        return memory().getDouble(byteArray, addressOffset + index);
    }

    public void putDouble(final long index, final double value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_DOUBLE);
        }

        memory().putDouble(byteArray, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public float getFloat(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_FLOAT);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = memory().getInt(byteArray, addressOffset + index);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
        else
        {
            return memory().getFloat(byteArray, addressOffset + index);
        }
    }

    public void putFloat(final long index, final float value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_FLOAT);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = Integer.reverseBytes(Float.floatToRawIntBits(value));
            memory().putInt(byteArray, addressOffset + index, bits);
        }
        else
        {
            memory().putFloat(byteArray, addressOffset + index, value);
        }
    }

    public float getFloat(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_FLOAT);
        }

        return memory().getFloat(byteArray, addressOffset + index);
    }

    public void putFloat(final long index, final float value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_FLOAT);
        }

        memory().putFloat(byteArray, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public short getShort(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        short bits = memory().getShort(byteArray, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Short.reverseBytes(bits);
        }

        return bits;
    }

    public void putShort(final long index, final short value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        short bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Short.reverseBytes(bits);
        }

        memory().putShort(byteArray, addressOffset + index, bits);
    }

    public short getShort(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        return memory().getShort(byteArray, addressOffset + index);
    }

    public void putShort(final long index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        memory().putShort(byteArray, addressOffset + index, value);
    }

    public short getShortVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        return memory().getShortVolatile(byteArray, addressOffset + index);
    }

    public void putShortVolatile(final long index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        memory().putShortVolatile(byteArray, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public byte getByte(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_BYTE);
        }

        return memory().getByte(byteArray, addressOffset + index);
    }

    public void putByte(final long index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_BYTE);
        }

        memory().putByte(byteArray, addressOffset + index, value);
    }

    public byte getByteVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_BYTE);
        }

        return memory().getByteVolatile(byteArray, addressOffset + index);
    }

    public void putByteVolatile(final long index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_BYTE);
        }

        memory().putByteVolatile(byteArray, addressOffset + index, value);
    }

    public void getBytes(final long index, final byte[] dst)
    {
        getBytes(index, dst, 0, dst.length);
    }

    public void getBytes(final long index, final byte[] dst, final long offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, length);
            BufferUtil.boundsCheck(dst, offset, length);
        }

        memory().copyMemory(byteArray, addressOffset + index, dst, ARRAY_BASE_OFFSET + offset, length);
    }

    public void getBytes(final long index, final MutableDirectBuffer dstBuffer, final long dstIndex, final int length)
    {
        dstBuffer.putBytes(dstIndex, this, index, length);
    }

    public void getBytes(final long index, final ByteBuffer dstBuffer, final int length)
    {
        final int dstOffset = dstBuffer.position();
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, length);
            BufferUtil.boundsCheck(dstBuffer, (long) dstOffset, length);
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
            dstBaseOffset = ((sun.nio.ch.DirectBuffer)dstBuffer).address();
        }

        memory().copyMemory(byteArray, addressOffset + index, dstByteArray, dstBaseOffset + dstOffset, length);
        dstBuffer.position(dstBuffer.position() + length);
    }

    public void putBytes(final long index, final byte[] src)
    {
        putBytes(index, src, 0, src.length);
    }

    public void putBytes(final long index, final byte[] src, final long offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, length);
            BufferUtil.boundsCheck(src, offset, length);
        }

        memory().copyMemory(src, ARRAY_BASE_OFFSET + offset, byteArray, addressOffset + index, length);
    }

    public void putBytes(final long index, final ByteBuffer srcBuffer, final int length)
    {
        final int srcIndex = srcBuffer.position();
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, length);
            BufferUtil.boundsCheck(srcBuffer, (long) srcIndex, length);
        }

        putBytes(index, srcBuffer, srcIndex, length);
        srcBuffer.position(srcIndex + length);
    }

    public void putBytes(final long index, final ByteBuffer srcBuffer, final long srcIndex, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, length);
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
            srcBaseOffset = ((sun.nio.ch.DirectBuffer)srcBuffer).address();
        }

        memory().copyMemory(srcByteArray, srcBaseOffset + srcIndex, byteArray, addressOffset + index, length);
    }

    public void putBytes(final long index, final DirectBuffer srcBuffer, final long srcIndex, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, length);
            srcBuffer.boundsCheck(srcIndex, length);
        }

        memory().copyMemory(
            srcBuffer.byteArray(),
            srcBuffer.addressOffset() + srcIndex,
            byteArray,
            addressOffset + index,
            length);
    }

    ///////////////////////////////////////////////////////////////////////////

    public char getChar(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        char bits = memory().getChar(byteArray, addressOffset + index);
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = (char)Short.reverseBytes((short)bits);
        }

        return bits;
    }

    public void putChar(final long index, final char value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        char bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = (char)Short.reverseBytes((short)bits);
        }

        memory().putChar(byteArray, addressOffset + index, bits);
    }

    public char getChar(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_CHAR);
        }

        return memory().getChar(byteArray, addressOffset + index);
    }

    public void putChar(final long index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_CHAR);
        }

        memory().putChar(byteArray, addressOffset + index, value);
    }

    public char getCharVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_CHAR);
        }

        return memory().getCharVolatile(byteArray, addressOffset + index);
    }

    public void putCharVolatile(final long index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_CHAR);
        }

        memory().putCharVolatile(byteArray, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public String getStringUtf8(final long offset, final ByteOrder byteOrder)
    {
        final int length = getInt(offset, byteOrder);

        return getStringUtf8(offset, length);
    }

    public String getStringUtf8(final long offset, final int length)
    {
        final byte[] stringInBytes = new byte[length];
        getBytes(offset + SIZE_OF_INT, stringInBytes);

        return new String(stringInBytes, StandardCharsets.UTF_8);
    }

    public int putStringUtf8(final long offset, final String value, final ByteOrder byteOrder)
    {
        return putStringUtf8(offset, value, byteOrder, Integer.MAX_VALUE);
    }

    public int putStringUtf8(final long offset, final String value, final ByteOrder byteOrder, final int maxEncodedSize)
    {
        final byte[] bytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : NULL_BYTES;
        if (bytes.length > maxEncodedSize)
        {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedSize);
        }

        putInt(offset, bytes.length, byteOrder);
        putBytes(offset + SIZE_OF_INT, bytes);

        return SIZE_OF_INT + bytes.length;
    }

    public String getStringWithoutLengthUtf8(final long offset, final int length)
    {
        final byte[] stringInBytes = new byte[length];
        getBytes(offset, stringInBytes);

        return new String(stringInBytes, StandardCharsets.UTF_8);
    }

    public int putStringWithoutLengthUtf8(final long offset, final String value)
    {
        final byte[] bytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : NULL_BYTES;
        putBytes(offset, bytes);

        return bytes.length;
    }

    ///////////////////////////////////////////////////////////////////////////

    public void boundsCheck(final long index, final int length)
    {
        final long resultingPosition = index + (long)length;
        if (index < 0 || resultingPosition > capacity)
        {
            throw new IndexOutOfBoundsException(String.format("index=%d, length=%d, capacity=%d", index, length, capacity));
        }
    }

}
