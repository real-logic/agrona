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
import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.agrona.MutableDirectBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import static uk.co.real_logic.agrona.BitUtil.*;
import static uk.co.real_logic.agrona.concurrent.MemoryAccess.memory;
import static uk.co.real_logic.agrona.concurrent.UnsafeBuffer.*;

/**
 * Supports regular, byte ordered, and atomic (memory ordered) access to an underlying buffer.
 *
 * This buffer is resizable and based upon an underlying
 */
public class MappedResizeableBuffer implements AtomicBuffer, AutoCloseable
{

    private final FileChannel fileChannel;

    private long addressOffset;
    private long capacity;

    /**
     * Attach a view to an off-heap memory region by address.
     *
     * @param fileChannel the file to map
     * @param initialLength  of the buffer from the given address
     */
    public MappedResizeableBuffer(final FileChannel fileChannel, final long initialLength)
    {
        this.fileChannel = fileChannel;
        map(initialLength);
    }

    private void map(final long initialLength)
    {
        capacity = initialLength;
        addressOffset = IoUtil.map(fileChannel, FileChannel.MapMode.READ_WRITE, 0, initialLength);
    }

    private void unmap()
    {
        IoUtil.unmap(fileChannel, addressOffset, capacity);
    }

    public void resize(final long newLength)
    {
        if (newLength <= 0)
        {
            throw new IllegalArgumentException("Length must be a positive long, but is: " + newLength);
        }

        unmap();
        map(newLength);
    }

    public void wrap(final byte[] buffer)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final byte[] buffer, final long offset, final int length)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final ByteBuffer buffer)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final ByteBuffer buffer, final long offset, final int length)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final DirectBuffer buffer)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final DirectBuffer buffer, final long offset, final int length)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final long address, final int length)
    {
        throw new UnsupportedOperationException();
    }

    public long addressOffset()
    {
        return addressOffset;
    }

    public byte[] byteArray()
    {
        throw new UnsupportedOperationException();
    }

    public ByteBuffer byteBuffer()
    {
        throw new UnsupportedOperationException();
    }

    public void setMemory(final long index, final int length, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, length);
        }

        memory().setMemory(null, addressOffset + index, length, value);
    }

    public int capacity()
    {
        return (int) capacity;
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

        long bits = memory().getLong(null, addressOffset + index);
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

        memory().putLong(null, addressOffset + index, bits);
    }

    public long getLong(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().getLong(null, addressOffset + index);
    }

    public void putLong(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        memory().putLong(null, addressOffset + index, value);
    }

    public long getLongVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().getLongVolatile(null, addressOffset + index);
    }

    public void putLongVolatile(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        memory().putLongVolatile(null, addressOffset + index, value);
    }

    public void putLongOrdered(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        memory().putOrderedLong(null, addressOffset + index, value);
    }

    public long addLongOrdered(final long index, final long increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        final long offset = addressOffset + index;
        final long value = memory().getLong(null, offset);
        memory().putOrderedLong(null, offset, value + increment);

        return value;
    }

    public boolean compareAndSetLong(final long index, final long expectedValue, final long updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().compareAndSwapLong(null, addressOffset + index, expectedValue, updateValue);
    }

    public long getAndSetLong(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().getAndSetLong(null, addressOffset + index, value);
    }

    public long getAndAddLong(final long index, final long delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_LONG);
        }

        return memory().getAndAddLong(null, addressOffset + index, delta);
    }

    ///////////////////////////////////////////////////////////////////////////

    public int getInt(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        int bits = memory().getInt(null, addressOffset + index);
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

        memory().putInt(null, addressOffset + index, bits);
    }

    public int getInt(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().getInt(null, addressOffset + index);
    }

    public void putInt(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        memory().putInt(null, addressOffset + index, value);
    }

    public int getIntVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().getIntVolatile(null, addressOffset + index);
    }

    public void putIntVolatile(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        memory().putIntVolatile(null, addressOffset + index, value);
    }

    public void putIntOrdered(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        memory().putOrderedInt(null, addressOffset + index, value);
    }

    public int addIntOrdered(final long index, final int increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        final long offset = addressOffset + index;
        final int value = memory().getInt(null, offset);
        memory().putOrderedInt(null, offset, value + increment);

        return value;
    }

    public boolean compareAndSetInt(final long index, final int expectedValue, final int updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().compareAndSwapInt(null, addressOffset + index, expectedValue, updateValue);
    }

    public int getAndSetInt(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().getAndSetInt(null, addressOffset + index, value);
    }

    public int getAndAddInt(final long index, final int delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_INT);
        }

        return memory().getAndAddInt(null, addressOffset + index, delta);
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
            final long bits = memory().getLong(null, addressOffset + index);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
        else
        {
            return memory().getDouble(null, addressOffset + index);
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
            memory().putLong(null, addressOffset + index, bits);
        }
        else
        {
            memory().putDouble(null, addressOffset + index, value);
        }
    }

    public double getDouble(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_DOUBLE);
        }

        return memory().getDouble(null, addressOffset + index);
    }

    public void putDouble(final long index, final double value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_DOUBLE);
        }

        memory().putDouble(null, addressOffset + index, value);
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
            final int bits = memory().getInt(null, addressOffset + index);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
        else
        {
            return memory().getFloat(null, addressOffset + index);
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
            memory().putInt(null, addressOffset + index, bits);
        }
        else
        {
            memory().putFloat(null, addressOffset + index, value);
        }
    }

    public float getFloat(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_FLOAT);
        }

        return memory().getFloat(null, addressOffset + index);
    }

    public void putFloat(final long index, final float value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_FLOAT);
        }

        memory().putFloat(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public short getShort(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        short bits = memory().getShort(null, addressOffset + index);
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

        memory().putShort(null, addressOffset + index, bits);
    }

    public short getShort(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        return memory().getShort(null, addressOffset + index);
    }

    public void putShort(final long index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        memory().putShort(null, addressOffset + index, value);
    }

    public short getShortVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        return memory().getShortVolatile(null, addressOffset + index);
    }

    public void putShortVolatile(final long index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_SHORT);
        }

        memory().putShortVolatile(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public byte getByte(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_BYTE);
        }

        return memory().getByte(null, addressOffset + index);
    }

    public void putByte(final long index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_BYTE);
        }

        memory().putByte(null, addressOffset + index, value);
    }

    public byte getByteVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_BYTE);
        }

        return memory().getByteVolatile(null, addressOffset + index);
    }

    public void putByteVolatile(final long index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_BYTE);
        }

        memory().putByteVolatile(null, addressOffset + index, value);
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

        memory().copyMemory(null, addressOffset + index, dst, ARRAY_BASE_OFFSET + offset, length);
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

        final byte[] dstnull;
        final long dstBaseOffset;
        if (dstBuffer.hasArray())
        {
            dstnull = dstBuffer.array();
            dstBaseOffset = ARRAY_BASE_OFFSET + dstBuffer.arrayOffset();
        }
        else
        {
            dstnull = null;
            dstBaseOffset = ((sun.nio.ch.DirectBuffer)dstBuffer).address();
        }

        memory().copyMemory(null, addressOffset + index, dstnull, dstBaseOffset + dstOffset, length);
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

        memory().copyMemory(src, ARRAY_BASE_OFFSET + offset, null, addressOffset + index, length);
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

        final byte[] srcnull;
        final long srcBaseOffset;
        if (srcBuffer.hasArray())
        {
            srcnull = srcBuffer.array();
            srcBaseOffset = ARRAY_BASE_OFFSET + srcBuffer.arrayOffset();
        }
        else
        {
            srcnull = null;
            srcBaseOffset = ((sun.nio.ch.DirectBuffer)srcBuffer).address();
        }

        memory().copyMemory(srcnull, srcBaseOffset + srcIndex, null, addressOffset + index, length);
    }

    public void putBytes(final long index, final DirectBuffer srcBuffer, final long srcIndex, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, length);
            srcBuffer.boundsCheck(srcIndex, length);
        }

        memory().copyMemory(
            null,
            srcBuffer.addressOffset() + srcIndex,
            null,
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

        char bits = memory().getChar(null, addressOffset + index);
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

        memory().putChar(null, addressOffset + index, bits);
    }

    public char getChar(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_CHAR);
        }

        return memory().getChar(null, addressOffset + index);
    }

    public void putChar(final long index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_CHAR);
        }

        memory().putChar(null, addressOffset + index, value);
    }

    public char getCharVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_CHAR);
        }

        return memory().getCharVolatile(null, addressOffset + index);
    }

    public void putCharVolatile(final long index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index, SIZE_OF_CHAR);
        }

        memory().putCharVolatile(null, addressOffset + index, value);
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

    public void close()
    {
        unmap();
    }
}
