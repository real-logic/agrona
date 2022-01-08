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

import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.IoUtil;
import org.agrona.MutableDirectBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.agrona.BitUtil.*;
import static org.agrona.BufferUtil.*;
import static org.agrona.UnsafeAccess.*;
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
    private FileChannel.MapMode mapMode;

    /**
     * Attach a view to an off-heap memory region by address. Defaults to
     * {@link java.nio.channels.FileChannel.MapMode#READ_WRITE}.
     *
     * @param fileChannel   the file to map
     * @param offset        the offset of the file to start the mapping
     * @param initialLength of the buffer from the given address
     */
    public MappedResizeableBuffer(final FileChannel fileChannel, final long offset, final long initialLength)
    {
        this.fileChannel = fileChannel;
        this.mapMode = FileChannel.MapMode.READ_WRITE;
        map(offset, initialLength);
    }

    /**
     * Attach a view to an off-heap memory region by address.
     *
     * @param fileChannel   the file to map
     * @param mapMode       for the mapping
     * @param offset        the offset of the file to start the mapping
     * @param initialLength of the buffer from the given address
     */
    public MappedResizeableBuffer(
        final FileChannel fileChannel, final FileChannel.MapMode mapMode, final long offset, final long initialLength)
    {
        this.fileChannel = fileChannel;
        this.mapMode = mapMode;
        map(offset, initialLength);
    }

    /**
     * {@inheritDoc}
     */
    public void close()
    {
        unmap();
    }

    /**
     * Resize the buffer.
     *
     * @param newLength in bytes.
     */
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
     * Remap the buffer based on a new file, offset, and a length
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
     * Remap the buffer based on a new file, mapping mode, offset, and a length
     *
     * @param fileChannel the file to map
     * @param mapMode     for the file when mapping.
     * @param offset      the offset of the file to start the mapping
     * @param length      of the buffer from the given address
     */
    public void wrap(
        final FileChannel fileChannel, final FileChannel.MapMode mapMode, final long offset, final long length)
    {
        unmap();
        this.fileChannel = fileChannel;
        this.mapMode = mapMode;
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

    /**
     * The {@link java.nio.channels.FileChannel.MapMode} that will be used when mapping the file.
     *
     * @return {@link java.nio.channels.FileChannel.MapMode} that will be used when mapping the file.
     */
    public FileChannel.MapMode mapMode()
    {
        return mapMode;
    }

    /**
     * Set a region of memory to a given byte value.
     *
     * @param index  at which to start.
     * @param length of the run of bytes to set.
     * @param value  the memory will be set to.
     */
    public void setMemory(final long index, final int length, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
        }

        final long offset = addressOffset + index;
        if (MEMSET_HACK_REQUIRED && length > MEMSET_HACK_THRESHOLD && 0 == (offset & 1))
        {
            // This horrible filth is to encourage the JVM to call memset() when address is even.
            UNSAFE.putByte(null, offset, value);
            UNSAFE.setMemory(null, offset + 1, length - 1, value);
        }
        else
        {
            UNSAFE.setMemory(null, offset, length, value);
        }
    }

    /**
     * Capacity of the buffer in bytes.
     *
     * @return capacity of the buffer in bytes.
     */
    public long capacity()
    {
        return capacity;
    }

    /**
     * Check if limit exceeds the capacity.
     *
     * @param limit to check.
     */
    public void checkLimit(final long limit)
    {
        if (limit > capacity)
        {
            throw new IndexOutOfBoundsException("limit=" + limit + " is beyond capacity=" + capacity);
        }
    }

    /**
     * Verify that the underlying buffer is correctly aligned to prevent word tearing and other ordering issues.
     *
     * @throws IllegalStateException if the alignment is not correct.
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

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value for at a given index.
     */
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

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     for at a given index.
     * @param byteOrder of the value when written.
     */
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

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public long getLong(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getLong(null, addressOffset + index);
    }

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putLong(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putLong(null, addressOffset + index, value);
    }

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public long getLongVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getLongVolatile(null, addressOffset + index);
    }

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putLongVolatile(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putLongVolatile(null, addressOffset + index, value);
    }

    /**
     * Put a value to a given index with ordered store semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putLongOrdered(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putOrderedLong(null, addressOffset + index, value);
    }

    /**
     * Add a value to a given index with ordered store semantics. Use a negative increment to decrement.
     *
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     */
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

    /**
     * Atomic compare and set of a long given an expected value.
     *
     * @param index         in bytes for where to put.
     * @param expectedValue at to be compared.
     * @param updateValue   to be exchanged.
     * @return set successful or not.
     */
    public boolean compareAndSetLong(final long index, final long expectedValue, final long updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.compareAndSwapLong(null, addressOffset + index, expectedValue, updateValue);
    }

    /**
     * Atomically exchange a value at a location returning the previous contents.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     * @return previous value at the index.
     */
    public long getAndSetLong(final long index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndSetLong(null, addressOffset + index, value);
    }

    /**
     * Atomically add a delta to a value at a location returning the previous contents.
     * To decrement a negative delta can be provided.
     *
     * @param index in bytes for where to put.
     * @param delta to be added to the value at the index.
     * @return previous value.
     */
    public long getAndAddLong(final long index, final long delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndAddLong(null, addressOffset + index, delta);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value for at a given index.
     */
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

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     for at a given index.
     * @param byteOrder of the value when written.
     */
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

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public int getInt(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getInt(null, addressOffset + index);
    }

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putInt(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putInt(null, addressOffset + index, value);
    }

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public int getIntVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getIntVolatile(null, addressOffset + index);
    }

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putIntVolatile(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putIntVolatile(null, addressOffset + index, value);
    }

    /**
     * Put a value to a given index with ordered store semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putIntOrdered(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putOrderedInt(null, addressOffset + index, value);
    }

    /**
     * Add a value to a given index with ordered store semantics. Use a negative increment to decrement.
     *
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     */
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

    /**
     * Atomic compare and set of a long given an expected value.
     *
     * @param index         in bytes for where to put.
     * @param expectedValue at to be compared.
     * @param updateValue   to be exchanged.
     * @return set successful or not.
     */
    public boolean compareAndSetInt(final long index, final int expectedValue, final int updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.compareAndSwapInt(null, addressOffset + index, expectedValue, updateValue);
    }

    /**
     * Atomically exchange a value at a location returning the previous contents.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     * @return previous value at the index.
     */
    public int getAndSetInt(final long index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndSetInt(null, addressOffset + index, value);
    }

    /**
     * Atomically add a delta to a value at a location returning the previous contents.
     * To decrement a negative delta can be provided.
     *
     * @param index in bytes for where to put.
     * @param delta to be added to the value at the index.
     * @return previous value.
     */
    public int getAndAddInt(final long index, final int delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndAddInt(null, addressOffset + index, delta);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value for at a given index.
     */
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

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     for at a given index.
     * @param byteOrder of the value when written.
     */
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

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public double getDouble(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        return UNSAFE.getDouble(null, addressOffset + index);
    }

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putDouble(final long index, final double value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_DOUBLE);
        }

        UNSAFE.putDouble(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value for at a given index.
     */
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

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     for at a given index.
     * @param byteOrder of the value when written.
     */
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

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public float getFloat(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        return UNSAFE.getFloat(null, addressOffset + index);
    }

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putFloat(final long index, final float value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_FLOAT);
        }

        UNSAFE.putFloat(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value for at a given index.
     */
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

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     for at a given index.
     * @param byteOrder of the value when written.
     */
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

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public short getShort(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        return UNSAFE.getShort(null, addressOffset + index);
    }

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putShort(final long index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        UNSAFE.putShort(null, addressOffset + index, value);
    }

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public short getShortVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        return UNSAFE.getShortVolatile(null, addressOffset + index);
    }

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putShortVolatile(final long index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        UNSAFE.putShortVolatile(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public byte getByte(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        return UNSAFE.getByte(null, addressOffset + index);
    }

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putByte(final long index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        UNSAFE.putByte(null, addressOffset + index, value);
    }

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public byte getByteVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        return UNSAFE.getByteVolatile(null, addressOffset + index);
    }

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putByteVolatile(final long index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck(index);
        }

        UNSAFE.putByteVolatile(null, addressOffset + index, value);
    }

    /**
     * Get from the underlying buffer into a supplied byte array.
     * This method will try to fill the supplied byte array.
     *
     * @param index in the underlying buffer to start from.
     * @param dst   into which the dst will be copied.
     */
    public void getBytes(final long index, final byte[] dst)
    {
        getBytes(index, dst, 0, dst.length);
    }

    /**
     * Get bytes from the underlying buffer into a supplied byte array.
     *
     * @param index  in the underlying buffer to start from.
     * @param dst    into which the bytes will be copied.
     * @param offset in the supplied buffer to start the copy.
     * @param length of the supplied buffer to use.
     */
    public void getBytes(final long index, final byte[] dst, final long offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(dst, offset, length);
        }

        UNSAFE.copyMemory(null, addressOffset + index, dst, ARRAY_BASE_OFFSET + offset, length);
    }

    /**
     * Get from the underlying buffer into a supplied {@link ByteBuffer} current {@link ByteBuffer#position()}.
     * <p>
     * The destination buffer will have its {@link ByteBuffer#position()} advanced as a result.
     *
     * @param index     in the underlying buffer to start from.
     * @param dstBuffer into which the bytes will be copied.
     * @param length    of the supplied buffer to use.
     */
    public void getBytes(final long index, final ByteBuffer dstBuffer, final int length)
    {
        final int dstOffset = dstBuffer.position();
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

        UNSAFE.copyMemory(null, addressOffset + index, dstByteArray, dstBaseOffset + dstOffset, length);
        dstBuffer.position(dstBuffer.position() + length);
    }

    /**
     * Put an array of src into the underlying buffer.
     *
     * @param index in the underlying buffer to start from.
     * @param src   to be copied to the underlying buffer.
     */
    public void putBytes(final long index, final byte[] src)
    {
        putBytes(index, src, 0, src.length);
    }

    /**
     * Put an array into the underlying buffer.
     *
     * @param index  in the underlying buffer to start from.
     * @param src    to be copied to the underlying buffer.
     * @param offset in the supplied buffer to begin the copy.
     * @param length of the supplied buffer to copy.
     */
    public void putBytes(final long index, final byte[] src, final long offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(src, offset, length);
        }

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET + offset, null, addressOffset + index, length);
    }

    /**
     * Put bytes into the underlying buffer for the view.  Bytes will be copied from current
     * {@link ByteBuffer#position()} for a given length.
     * <p>
     * The source buffer will have its {@link ByteBuffer#position()} advanced as a result.
     *
     * @param index     in the underlying buffer to start from.
     * @param srcBuffer to copy the bytes from.
     * @param length    of the supplied buffer to copy.
     */
    public void putBytes(final long index, final ByteBuffer srcBuffer, final int length)
    {
        final int srcIndex = srcBuffer.position();
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
            BufferUtil.boundsCheck(srcBuffer, srcIndex, length);
        }

        putBytes(index, srcBuffer, srcIndex, length);
        srcBuffer.position(srcIndex + length);
    }

    /**
     * Put bytes into the underlying buffer for the view. Bytes will be copied from the buffer index to
     * the buffer index + length.
     * <p>
     * The source buffer will not have its {@link ByteBuffer#position()} advanced as a result.
     *
     * @param index     in the underlying buffer to start from.
     * @param srcBuffer to copy the bytes from (does not change position).
     * @param srcIndex  in the source buffer from which the copy will begin.
     * @param length    of the bytes to be copied.
     */
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

    /**
     * Put bytes from a source {@link DirectBuffer} into this {@link MutableDirectBuffer} at given indices.
     *
     * @param index     in this buffer to begin putting the bytes.
     * @param srcBuffer from which the bytes will be copied.
     * @param srcIndex  in the source buffer from which the byte copy will begin.
     * @param length    of the bytes to be copied.
     */
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

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value for at a given index.
     */
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

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     for at a given index.
     * @param byteOrder of the value when written.
     */
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

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public char getChar(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        return UNSAFE.getChar(null, addressOffset + index);
    }

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putChar(final long index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        UNSAFE.putChar(null, addressOffset + index, value);
    }

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    public char getCharVolatile(final long index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        return UNSAFE.getCharVolatile(null, addressOffset + index);
    }

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    public void putCharVolatile(final long index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        UNSAFE.putCharVolatile(null, addressOffset + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get a String from bytes encoded in UTF-8 format that is length prefixed.
     *
     * @param offset at which the String begins.
     * @return the String as represented by the UTF-8 encoded bytes.
     */
    public String getStringUtf8(final long offset)
    {
        final int length = getInt(offset);

        return getStringUtf8(offset, length);
    }

    /**
     * Get a String from bytes encoded in UTF-8 format that is length prefixed.
     *
     * @param offset    at which the String begins.
     * @param byteOrder for the length at the beginning of the String.
     * @return the String as represented by the UTF-8 encoded bytes.
     */
    public String getStringUtf8(final long offset, final ByteOrder byteOrder)
    {
        final int length = getInt(offset, byteOrder);

        return getStringUtf8(offset, length);
    }

    /**
     * Get part of String from bytes encoded in UTF-8 format that is length prefixed.
     *
     * @param offset at which the String begins.
     * @param length of the String in bytes to decode.
     * @return the String as represented by the UTF-8 encoded bytes.
     */
    public String getStringUtf8(final long offset, final int length)
    {
        final byte[] stringInBytes = new byte[length];
        getBytes(offset + STR_HEADER_LEN, stringInBytes);

        return new String(stringInBytes, UTF_8);
    }

    /**
     * Encode a String as UTF-8 bytes to the buffer with a length prefix.
     *
     * @param offset at which the String should be encoded.
     * @param value  of the String to be encoded.
     * @return the number of bytes put to the buffer.
     */
    public int putStringUtf8(final long offset, final String value)
    {
        return putStringUtf8(offset, value, Integer.MAX_VALUE);
    }

    /**
     * Encode a String as UTF-8 bytes to the buffer with a length prefix.
     *
     * @param offset    at which the String should be encoded.
     * @param value     of the String to be encoded.
     * @param byteOrder for the length prefix.
     * @return the number of bytes put to the buffer.
     */
    public int putStringUtf8(final long offset, final String value, final ByteOrder byteOrder)
    {
        return putStringUtf8(offset, value, byteOrder, Integer.MAX_VALUE);
    }

    /**
     * Encode a String as UTF-8 bytes the buffer with a length prefix with a maximum encoded size check.
     *
     * @param offset         at which the String should be encoded.
     * @param value          of the String to be encoded.
     * @param maxEncodedSize to be checked before writing to the buffer.
     * @return the number of bytes put to the buffer.
     * @throws java.lang.IllegalArgumentException if the encoded bytes are greater than maxEncodedLength.
     */
    public int putStringUtf8(final long offset, final String value, final int maxEncodedSize)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        if (bytes.length > maxEncodedSize)
        {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedSize);
        }

        putInt(offset, bytes.length);
        putBytes(offset + STR_HEADER_LEN, bytes);

        return STR_HEADER_LEN + bytes.length;
    }

    /**
     * Encode a String as UTF-8 bytes the buffer with a length prefix with a maximum encoded size check.
     *
     * @param offset         at which the String should be encoded.
     * @param value          of the String to be encoded.
     * @param byteOrder      for the length prefix.
     * @param maxEncodedSize to be checked before writing to the buffer.
     * @return the number of bytes put to the buffer.
     * @throws java.lang.IllegalArgumentException if the encoded bytes are greater than maxEncodedLength.
     */
    public int putStringUtf8(final long offset, final String value, final ByteOrder byteOrder, final int maxEncodedSize)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        if (bytes.length > maxEncodedSize)
        {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedSize);
        }

        putInt(offset, bytes.length, byteOrder);
        putBytes(offset + STR_HEADER_LEN, bytes);

        return STR_HEADER_LEN + bytes.length;
    }

    /**
     * Get an encoded UTF-8 String from the buffer that does not have a length prefix.
     *
     * @param offset at which the String begins.
     * @param length of the String in bytes to decode.
     * @return the String as represented by the UTF-8 encoded bytes.
     */
    public String getStringWithoutLengthUtf8(final long offset, final int length)
    {
        final byte[] stringInBytes = new byte[length];
        getBytes(offset, stringInBytes);

        return new String(stringInBytes, UTF_8);
    }

    /**
     * Encode a String as UTF-8 bytes in the buffer without a length prefix.
     *
     * @param offset at which the String begins.
     * @param value  of the String to be encoded.
     * @return the number of bytes encoded.
     */
    public int putStringWithoutLengthUtf8(final long offset, final String value)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        putBytes(offset, bytes);

        return bytes.length;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Check that a given length of bytes is within the bounds from a given index.
     *
     * @param index  from which to check.
     * @param length in bytes of the range to check.
     * @throws java.lang.IndexOutOfBoundsException if the length goes outside the capacity range.
     */
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
        addressOffset = IoUtil.map(fileChannel, mapMode, offset, length);
    }

    private void unmap()
    {
        IoUtil.unmap(fileChannel, addressOffset, capacity);
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "MappedResizeableBuffer{" +
            "addressOffset=" + addressOffset +
            ", capacity=" + capacity +
            ", fileChannel=" + fileChannel +
            ", mapMode=" + mapMode +
            '}';
    }
}
