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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.IntFunction;

/**
 * Supports regular, byte ordered, and atomic (memory ordered) access to an underlying buffer.
 *
 * This buffer wraps multiple underlying {@link UnsafeBuffer} implementations, that are initialised
 * as required.
 */
// TODO: put bytes over a boundary
public class CompositeBuffer implements AtomicBuffer
{
    /** Size must be divisible by two */
    public static final int SIZE = 1024 * 1024 * 1024;

    private static final long MASK = SIZE - 1;
    private static final long SHIFT = (long) Math.sqrt(SIZE);

    private final IntFunction<UnsafeBuffer> factory;

    private UnsafeBuffer[] buffers = new UnsafeBuffer[1];

    public CompositeBuffer(final IntFunction<UnsafeBuffer> factory)
    {
        this.factory = factory;
        buffers[0] = factory.apply(0);
    }

    private long inside(final long offset)
    {
        return offset & MASK;
    }

    private UnsafeBuffer buffer(final long offset)
    {
        if (offset < 0)
        {
            throw new IllegalArgumentException("Cannot have a negative offset: " + offset);
        }

        final int bufferIndex = (int) offset >> SHIFT;
        UnsafeBuffer[] buffers = this.buffers;

        final int oldLength = buffers.length;
        if (bufferIndex > oldLength)
        {
            buffers = Arrays.copyOf(buffers, bufferIndex);
            for (int i = oldLength; i < bufferIndex; i++)
            {
                buffers[i] = factory.apply(i);
            }
            this.buffers = buffers;
        }

        return buffers[bufferIndex];
    }

    /**
     * {@inheritDoc}
     */
    public void verifyAlignment()
    {
        for (UnsafeBuffer buffer : buffers)
        {
            buffer.verifyAlignment();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLongVolatile(final long index)
    {
        return buffer(index).getLongVolatile(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public void putLongVolatile(final long index, final long value)
    {
        buffer(index).putLongVolatile(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void putLongOrdered(final long index, final long value)
    {
        buffer(index).putLongOrdered(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public long addLongOrdered(final long index, final long increment)
    {
        return buffer(index).addLongOrdered(inside(index), increment);
    }

    /**
     * {@inheritDoc}
     */
    public boolean compareAndSetLong(final long index, final long expectedValue, final long updateValue)
    {
        return buffer(index).compareAndSetLong(inside(index), expectedValue, updateValue);
    }

    /**
     * {@inheritDoc}
     */
    public long getAndSetLong(final long index, final long value)
    {
        return buffer(index).getAndSetLong(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public long getAndAddLong(final long index, final long delta)
    {
        return buffer(index).getAndAddLong(inside(index), delta);
    }

    /**
     * {@inheritDoc}
     */
    public int getIntVolatile(final long index)
    {
        return buffer(index).getIntVolatile(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public void putIntVolatile(final long index, final int value)
    {
        buffer(index).putIntVolatile(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void putIntOrdered(final long index, final int value)
    {
        buffer(index).putIntOrdered(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public int addIntOrdered(final long index, final int increment)
    {
        return buffer(index).addIntOrdered(inside(index), increment);
    }

    /**
     * {@inheritDoc}
     */
    public boolean compareAndSetInt(final long index, final int expectedValue, final int updateValue)
    {
        return buffer(index).compareAndSetInt(inside(index), expectedValue, updateValue);
    }

    /**
     * {@inheritDoc}
     */
    public int getAndSetInt(final long index, final int value)
    {
        return buffer(index).getAndSetInt(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public int getAndAddInt(final long index, final int delta)
    {
        return buffer(index).getAndAddInt(inside(index), delta);
    }

    /**
     * {@inheritDoc}
     */
    public short getShortVolatile(final long index)
    {
        return buffer(index).getShortVolatile(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public void putShortVolatile(final long index, final short value)
    {
        buffer(index).putShortVolatile(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public char getCharVolatile(final long index)
    {
        return buffer(index).getCharVolatile(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public void putCharVolatile(final long index, final char value)
    {
        buffer(index).putCharVolatile(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public byte getByteVolatile(final long index)
    {
        return buffer(index).getByteVolatile(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public void putByteVolatile(final long index, final byte value)
    {
        buffer(index).putByteVolatile(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void setMemory(final long index, final int length, final byte value)
    {
        buffer(index).setMemory(inside(index), length, value);
    }

    /**
     * {@inheritDoc}
     */
    public void putLong(final long index, final long value, final ByteOrder byteOrder)
    {
        buffer(index).putLong(inside(index), value, byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public void putLong(final long index, final long value)
    {
        buffer(index).putLong(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void putInt(final long index, final int value, final ByteOrder byteOrder)
    {
        buffer(index).putInt(inside(index), value, byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public void putInt(final long index, final int value)
    {
        buffer(index).putInt(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void putDouble(final long index, final double value, final ByteOrder byteOrder)
    {
        buffer(index).putDouble(inside(index), value, byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public void putDouble(final long index, final double value)
    {
        buffer(index).putDouble(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void putFloat(final long index, final float value, final ByteOrder byteOrder)
    {
        buffer(index).putFloat(inside(index), value, byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public void putFloat(final long index, final float value)
    {
        buffer(index).putFloat(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void putShort(final long index, final short value, final ByteOrder byteOrder)
    {
        buffer(index).putShort(inside(index), value, byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public void putShort(final long index, final short value)
    {
        buffer(index).putShort(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void putChar(final long index, final char value, final ByteOrder byteOrder)
    {
        buffer(index).putChar(inside(index), value, byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public void putChar(final long index, final char value)
    {
        buffer(index).putChar(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void putByte(final long index, final byte value)
    {
        buffer(index).putByte(inside(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final long index, final byte[] src)
    {
        buffer(index).putBytes(inside(index), src);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final long index, final byte[] src, final long offset, final int length)
    {
        buffer(index).putBytes(inside(index), src, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final long index, final ByteBuffer srcBuffer, final int length)
    {
        buffer(index).putBytes(inside(index), srcBuffer, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final long index, final ByteBuffer srcBuffer, final long srcIndex, final int length)
    {
        buffer(index).putBytes(inside(index), srcBuffer, srcIndex, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final long index, final DirectBuffer srcBuffer, final long srcIndex, final int length)
    {
        buffer(index).putBytes(inside(index), srcBuffer, srcIndex, length);
    }

    /**
     * {@inheritDoc}
     */
    public int putStringUtf8(final long offset, final String value, final ByteOrder byteOrder)
    {
        return buffer(offset).putStringUtf8(inside(offset), value, byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public int putStringUtf8(final long offset, final String value, final ByteOrder byteOrder, final int maxEncodedSize)
    {
        return buffer(offset).putStringUtf8(inside(offset), value, byteOrder, maxEncodedSize);
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthUtf8(final long offset, final String value)
    {
        return buffer(offset).putStringWithoutLengthUtf8(inside(offset), value);
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final byte[] buffer)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final byte[] buffer, final long offset, final int length)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final ByteBuffer buffer)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final ByteBuffer buffer, final long offset, final int length)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final DirectBuffer buffer)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final DirectBuffer buffer, final long offset, final int length)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final long address, final int length)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public long addressOffset()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public byte[] byteArray()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer byteBuffer()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int capacity()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void checkLimit(final long limit)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(final long index, final ByteOrder byteOrder)
    {
        return buffer(index).getLong(inside(index), byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(final long index)
    {
        return buffer(index).getLong(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(final long index, final ByteOrder byteOrder)
    {
        return buffer(index).getInt(inside(index), byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(final long index)
    {
        return buffer(index).getInt(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(final long index, final ByteOrder byteOrder)
    {
        return buffer(index).getDouble(inside(index), byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(final long index)
    {
        return buffer(index).getDouble(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(final long index, final ByteOrder byteOrder)
    {
        return buffer(index).getFloat(inside(index), byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(final long index)
    {
        return buffer(index).getFloat(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(final long index, final ByteOrder byteOrder)
    {
        return buffer(index).getShort(inside(index), byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(final long index)
    {
        return buffer(index).getShort(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public char getChar(final long index, final ByteOrder byteOrder)
    {
        return buffer(index).getChar(inside(index), byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public char getChar(final long index)
    {
        return buffer(index).getChar(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public byte getByte(final long index)
    {
        return buffer(index).getByte(inside(index));
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final long index, final byte[] dst)
    {
        buffer(index).getBytes(inside(index), dst);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final long index, final byte[] dst, final long offset, final int length)
    {
        buffer(index).getBytes(inside(index), dst, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final long index, final MutableDirectBuffer dstBuffer, final long dstIndex, final int length)
    {
        buffer(index).getBytes(inside(index), dstBuffer, dstIndex, length);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final long index, final ByteBuffer dstBuffer, final int length)
    {
        buffer(index).getBytes(inside(index), dstBuffer, length);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringUtf8(final long offset, final ByteOrder byteOrder)
    {
        return buffer(offset).getStringUtf8(inside(offset), byteOrder);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringUtf8(final long offset, final int length)
    {
        return buffer(offset).getStringUtf8(inside(offset), length);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringWithoutLengthUtf8(final long offset, final int length)
    {
        return buffer(offset).getStringWithoutLengthUtf8(inside(offset), length);
    }

    /**
     * {@inheritDoc}
     */
    public void boundsCheck(final long index, final int length)
    {
    }
}
