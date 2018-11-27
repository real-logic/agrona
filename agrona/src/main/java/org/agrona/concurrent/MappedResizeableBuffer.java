/*
 * Copyright 2014-2018 Real Logic Ltd.
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

import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.IoUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.agrona.BitUtil.*;
import static org.agrona.BufferUtil.*;
import static org.agrona.UnsafeAccess.UNSAFE;
import static org.agrona.concurrent.UnsafeBuffer.*;

/**
 * Supports regular, byte ordered, and atomic (memory ordered) access to an underlying buffer.
 * <p>
 * This buffer is resizable and based upon an underlying FileChannel.
 * The channel is <b>not</b> closed when the buffer is closed.
 * <p>
 * Note: The resize method is not threadsafe. Concurrent access should only occur after a successful resize.
 */
public class MappedResizeableBuffer implements AutoCloseable
{
    private long addressOffset;
    private long capacity;
    private FileChannel fileChannel;

    /**
     * Attach a view to an off-heap memory region by address.
     *
     * @param fileChannel   the file to map
     * @param offset        the offset of the file to start the mapping
     * @param initialLength of the buffer from the given address
     */
    public MappedResizeableBuffer(final FileChannel fileChannel, final long offset, final long initialLength)
    {
        this.fileChannel = fileChannel;
        map(offset, initialLength);
    }

    public void close()
    {
        unmap();
    }

    public void resize(final long newLength)
    {
        if (newLength <= 0)
        {
            throw new IllegalArgumentException("Length must be a positive long, but is: " + newLength);
        }

        unmap();
        map(0, newLength);
    }

    /**
     * Remap the buffer using the existing file based on a new offset and length
     *
     * @param offset the offset of the file to start the mapping
     * @param length of the buffer from the given address
     */
    public void wrap(final long offset, final long length)
    {
        if (offset == addressOffset && length == capacity)
        {
            return;
        }

        wrap(fileChannel, offset, length);
    }

    /**
     * Remap the buffer based on a new file, offset and a length
     *
     * @param fileChannel the file to map
     * @param offset      the offset of the file to start the mapping
     * @param length      of the buffer from the given address
     */
    public void wrap(final FileChannel fileChannel, final long offset, final long length)
    {
        unmap();
        this.fileChannel = fileChannel;
        map(offset, length);
    }

    /**
     * Address offset in memory at which the mapping begins.
     *
     * @return the address offset in memory at which the mapping begins.
     */
    public long addressOffset()
    {
        return addressOffset;
    }

    /**
     * {@link FileChannel} that this buffer is mapping over.
     *
     * @return the {@link FileChannel} that this buffer is mapping over.
     */
    public FileChannel fileChannel()
    {
        return fileChannel;
    }

    public void setMemory(final long index, final int length, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        UNSAFE.setMemory(null, addressOffset + index, length, value);
    }

    public long capacity()
    {
        return capacity;
    }

    public void checkLimit(final long limit)
    {
        if (limit > capacity)
        {
            throw new IndexOutOfBoundsException("limit=" + limit + " is beyond capacity=" + capacity);
        }
    }

    public void verifyAlignment()
    {
        if (0 != (addressOffset & (ALIGNMENT - 1)))
        {
            throw new IllegalStateException(
                "AtomicBuffer is not correctly aligned: addressOffset=" + addressOffset +
                " is not divisible by " + ALIGNMENT);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public long getLong(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        long bits = UNSAFE.getLong(null, addressOffset + index);
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
            boundsCheck0(index, SIZE_OF_LONG);
        }

        long bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Long.reverseBytes(bits);
        }

        UNSAFE.putLong(null, addressOffset + index, bits);
    }

    public long getLong(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getLong(null, addressOffset + index);
    }

    public void putLong(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putLong(null, addressOffset + index, value);
    }

    public long getLongVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getLongVolatile(null, addressOffset + index);
    }

    public void putLongVolatile(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putLongVolatile(null, addressOffset + index, value);
    }

    public void putLongOrdered(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putOrderedLong(null, addressOffset + index, value);
    }

    public long addLongOrdered(final long index, final long increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        final long offset = addressOffset + index;
        final long value = UNSAFE.getLong(null, offset);
        UNSAFE.putOrderedLong(null, offset, value + increment);

        return value;
    }

    public boolean compareAndSetLong(final long index, final long expectedValue, final long updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.compareAndSwapLong(null, addressOffset + index, expectedValue, updateValue);
    }

    public long getAndSetLong(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndSetLong(null, addressOffset + index, value);
    }

    public long getAndAddLong(final long index, final long delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndAddLong(null, addressOffset + index, delta);
    }

    ///////////////////////////////////////////////////////////////////////////

    public int getInt(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        int bits = UNSAFE.getInt(null, addressOffset + index);
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
            boundsCheck0(index, SIZE_OF_INT);
        }

        int bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        UNSAFE.putInt(null, addressOffset + index, bits);
    }

    public int getInt(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getInt(null, addressOffset + index);
    }

    public void putInt(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putInt(null, addressOffset + index, value);
    }

    public int getIntVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getIntVolatile(null, addressOffset + index);
    }

    public void putIntVolatile(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putIntVolatile(null, addressOffset + index, value);
    }

    public void putIntOrdered(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putOrderedInt(null, addressOffset + index, value);
    }

    public int addIntOrdered(final long index, final int increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        final long offset = addressOffset + index;
        final int value = UNSAFE.getInt(null, offset);
        UNSAFE.putOrderedInt(null, offset, value + increment);

        return value;
    }

    public boolean compareAndSetInt(final long index, final int expectedValue, final int updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.compareAndSwapInt(null, addressOffset + index, expectedValue, updateValue);
    }

    public int getAndSetInt(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndSetInt(null, addressOffset + index, value);
    }

    public int getAndAddInt(final long index, final int delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndAddInt(null, addressOffset + index, delta);
    }

    ///////////////////////////////////////////////////////////////////////////

    public double getDouble(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = UNSAFE.getLong(null, addressOffset + index);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getDouble(null, addressOffset + index);
        }
    }

    public void putDouble(final long index, final double value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = Long.reverseBytes(Double.doubleToRawLongBits(value));
            UNSAFE.putLong(null, addressOffset + index, bits);
        }
        else
        {
            UNSAFE.putDouble(null, addressOffset + index, value);
        }
    }

    public double getDouble(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        return UNSAFE.getDouble(null, addressOffset + index);
    }

    public void putDouble(final long index, final double value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        UNSAFE.putDouble(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public float getFloat(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = UNSAFE.getInt(null, addressOffset + index);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getFloat(null, addressOffset + index);
        }
    }

    public void putFloat(final long index, final float value, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = Integer.reverseBytes(Float.floatToRawIntBits(value));
            UNSAFE.putInt(null, addressOffset + index, bits);
        }
        else
        {
            UNSAFE.putFloat(null, addressOffset + index, value);
        }
    }

    public float getFloat(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        return UNSAFE.getFloat(null, addressOffset + index);
    }

    public void putFloat(final long index, final float value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        UNSAFE.putFloat(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public short getShort(final long index, final ByteOrder byteOrder)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        short bits = UNSAFE.getShort(null, addressOffset + index);
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
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        short bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Short.reverseBytes(bits);
        }

        UNSAFE.putShort(null, addressOffset + index, bits);
    }

    public short getShort(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        return UNSAFE.getShort(null, addressOffset + index);
    }

    public void putShort(final long index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        UNSAFE.putShort(null, addressOffset + index, value);
    }

    public short getShortVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        return UNSAFE.getShortVolatile(null, addressOffset + index);
    }

    public void putShortVolatile(final long index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        UNSAFE.putShortVolatile(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public byte getByte(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        return UNSAFE.getByte(null, addressOffset + index);
    }

    public void putByte(final long index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        UNSAFE.putByte(null, addressOffset + index, value);
    }

    public byte getByteVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        return UNSAFE.getByteVolatile(null, addressOffset + index);
    }

    public void putByteVolatile(final long index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        UNSAFE.putByteVolatile(null, addressOffset + index, value);
    }

    public void getBytes(final long index, final byte[] dst)
    {
        getBytes(index, dst, 0, dst.length);
    }

    public void getBytes(final long index, final byte[] dst, final long offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(dst, offset, length);
        }

        UNSAFE.copyMemory(null, addressOffset + index, dst, ARRAY_BASE_OFFSET + offset, length);
    }

    public void getBytes(final long index, final ByteBuffer dstBuffer, final int length)
    {
        final int dstOffset = dstBuffer.position();
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(dstBuffer, (long)dstOffset, length);
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

        UNSAFE.copyMemory(null, addressOffset + index, dstByteArray, dstBaseOffset + dstOffset, length);
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
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(src, offset, length);
        }

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET + offset, null, addressOffset + index, length);
    }

    public void putBytes(final long index, final ByteBuffer srcBuffer, final int length)
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

    public void putBytes(final long index, final ByteBuffer srcBuffer, final long srcIndex, final int length)
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

        UNSAFE.copyMemory(srcByteArray, srcBaseOffset + srcIndex, null, addressOffset + index, length);
    }

    public void putBytes(final long index, final DirectBuffer srcBuffer, final int srcIndex, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            srcBuffer.boundsCheck(srcIndex, length);
        }

        UNSAFE.copyMemory(
            srcBuffer.byteArray(),
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
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        char bits = UNSAFE.getChar(null, addressOffset + index);
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
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        char bits = value;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = (char)Short.reverseBytes((short)bits);
        }

        UNSAFE.putChar(null, addressOffset + index, bits);
    }

    public char getChar(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        return UNSAFE.getChar(null, addressOffset + index);
    }

    public void putChar(final long index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        UNSAFE.putChar(null, addressOffset + index, value);
    }

    public char getCharVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        return UNSAFE.getCharVolatile(null, addressOffset + index);
    }

    public void putCharVolatile(final long index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        UNSAFE.putCharVolatile(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public String getStringUtf8(final long offset)
    {
        final int length = getInt(offset);

        return getStringUtf8(offset, length);
    }

    public String getStringUtf8(final long offset, final ByteOrder byteOrder)
    {
        final int length = getInt(offset, byteOrder);

        return getStringUtf8(offset, length);
    }

    public String getStringUtf8(final long offset, final int length)
    {
        final byte[] stringInBytes = new byte[length];
        getBytes(offset + SIZE_OF_INT, stringInBytes);

        return new String(stringInBytes, UTF_8);
    }

    public int putStringUtf8(final long offset, final String value)
    {
        return putStringUtf8(offset, value, Integer.MAX_VALUE);
    }

    public int putStringUtf8(final long offset, final String value, final ByteOrder byteOrder)
    {
        return putStringUtf8(offset, value, byteOrder, Integer.MAX_VALUE);
    }

    public int putStringUtf8(final long offset, final String value, final int maxEncodedSize)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        if (bytes.length > maxEncodedSize)
        {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedSize);
        }

        putInt(offset, bytes.length);
        putBytes(offset + SIZE_OF_INT, bytes);

        return SIZE_OF_INT + bytes.length;
    }

    public int putStringUtf8(final long offset, final String value, final ByteOrder byteOrder, final int maxEncodedSize)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
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

        return new String(stringInBytes, UTF_8);
    }

    public int putStringWithoutLengthUtf8(final long offset, final String value)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        putBytes(offset, bytes);

        return bytes.length;
    }

    ///////////////////////////////////////////////////////////////////////////

    public void boundsCheck(final long index, final int length)
    {
        boundsCheck0(index, length);
    }

    private void boundsCheck(final long index)
    {
        if (index < 0 || index >= capacity)
        {
            throw new IndexOutOfBoundsException("index=" + index + " capacity=" + capacity);
        }
    }

    private void boundsCheck0(final long index, final int length)
    {
        final long resultingPosition = index + (long)length;
        if (index < 0 || resultingPosition > capacity || resultingPosition < index)
        {
            throw new IndexOutOfBoundsException(
                "index=" + index + " length=" + length + " capacity=" + capacity);
        }
    }

    private void map(final long offset, final long length)
    {
        capacity = length;
        addressOffset = IoUtil.map(fileChannel, FileChannel.MapMode.READ_WRITE, offset, length);
    }

    private void unmap()
    {
        IoUtil.unmap(fileChannel, addressOffset, capacity);
    }
}
