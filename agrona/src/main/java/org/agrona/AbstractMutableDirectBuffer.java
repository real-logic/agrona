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

/**
 * Common super class for implementing {@link MutableDirectBuffer} interface.
 */
public abstract class AbstractMutableDirectBuffer implements MutableDirectBuffer
{
    protected byte[] byteArray;
    protected long addressOffset;
    protected int capacity;

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
    public long addressOffset()
    {
        return addressOffset;
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
    public void setMemory(final int index, final int length, final byte value)
    {
        ensureCapacity(index, length);

        final byte[] array = byteArray;
        final long offset = addressOffset + index;

        if (length < 100)
        {
            int i = 0;
            final int end = (length & ~7);
            final long mask = ((((long)value) << 56) |
                (((long)value & 0xff) << 48) |
                (((long)value & 0xff) << 40) |
                (((long)value & 0xff) << 32) |
                (((long)value & 0xff) << 24) |
                (((long)value & 0xff) << 16) |
                (((long)value & 0xff) << 8) |
                (((long)value & 0xff)));

            for (; i < end; i += 8)
            {
                UNSAFE.putLong(array, offset + i, mask);
            }

            for (; i < length; i++)
            {
                UNSAFE.putByte(array, offset + i, value);
            }
        }
        else
        {
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
        ensureCapacity(index, SIZE_OF_LONG);

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
        ensureCapacity(index, SIZE_OF_LONG);

        UNSAFE.putLong(byteArray, addressOffset + index, value);
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
        ensureCapacity(index, SIZE_OF_INT);

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
        ensureCapacity(index, SIZE_OF_INT);

        UNSAFE.putInt(byteArray, addressOffset + index, value);
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
        ensureCapacity(index, SIZE_OF_DOUBLE);

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
        ensureCapacity(index, SIZE_OF_DOUBLE);

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
        ensureCapacity(index, SIZE_OF_FLOAT);

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
        ensureCapacity(index, SIZE_OF_FLOAT);

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
        ensureCapacity(index, SIZE_OF_SHORT);

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
        ensureCapacity(index, SIZE_OF_SHORT);

        UNSAFE.putShort(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public byte getByte(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_BYTE);
        }

        return UNSAFE.getByte(byteArray, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putByte(final int index, final byte value)
    {
        ensureCapacity(index, SIZE_OF_BYTE);

        UNSAFE.putByte(byteArray, addressOffset + index, value);
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
            dstByteArray = BufferUtil.array(dstBuffer);
            dstBaseOffset = ARRAY_BASE_OFFSET + arrayOffset(dstBuffer);
        }

        UNSAFE.copyMemory(byteArray, addressOffset + index, dstByteArray, dstBaseOffset + dstOffset, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final byte[] src)
    {
        ensureCapacity(index, src.length);

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET, byteArray, addressOffset + index, src.length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final byte[] src, final int offset, final int length)
    {
        ensureCapacity(index, length);
        if (SHOULD_BOUNDS_CHECK)
        {
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
        ensureCapacity(index, length);
        if (SHOULD_BOUNDS_CHECK)
        {
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

        UNSAFE.copyMemory(srcByteArray, srcBaseOffset + srcIndex, byteArray, addressOffset + index, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final DirectBuffer srcBuffer, final int srcIndex, final int length)
    {
        ensureCapacity(index, length);
        if (SHOULD_BOUNDS_CHECK)
        {
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
        ensureCapacity(index, SIZE_OF_CHAR);

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
        ensureCapacity(index, SIZE_OF_CHAR);

        UNSAFE.putChar(byteArray, addressOffset + index, value);
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
        if (0 == length)
        {
            return "";
        }

        return getStringWithoutLengthAscii(index + STR_HEADER_LEN, length);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final Appendable appendable)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, STR_HEADER_LEN);
        }

        final int length = UNSAFE.getInt(byteArray, addressOffset + index);
        if (0 == length)
        {
            return 0;
        }

        return getStringWithoutLengthAscii(index + STR_HEADER_LEN, length, appendable);
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
        if (0 == length)
        {
            return "";
        }

        return getStringWithoutLengthAscii(index + STR_HEADER_LEN, length);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final Appendable appendable, final ByteOrder byteOrder)
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
        if (0 == length)
        {
            return 0;
        }

        return getStringWithoutLengthAscii(index + STR_HEADER_LEN, length, appendable);
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

        if (0 == length)
        {
            return "";
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
            final byte[] array = byteArray;
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
        if (null == value)
        {
            ensureCapacity(index, STR_HEADER_LEN);
            UNSAFE.putInt(byteArray, addressOffset + index, 0);
            return STR_HEADER_LEN;
        }
        else
        {
            final int length = value.length();
            ensureCapacity(index, length + STR_HEADER_LEN);

            final byte[] array = byteArray;
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
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final CharSequence value)
    {
        if (null == value)
        {
            ensureCapacity(index, STR_HEADER_LEN);
            UNSAFE.putInt(byteArray, addressOffset + index, 0);
            return STR_HEADER_LEN;
        }
        else
        {
            final int length = value.length();
            ensureCapacity(index, length + STR_HEADER_LEN);

            final byte[] array = byteArray;
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
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final String value, final ByteOrder byteOrder)
    {
        if (null == value)
        {
            ensureCapacity(index, STR_HEADER_LEN);
            UNSAFE.putInt(byteArray, addressOffset + index, 0);
            return STR_HEADER_LEN;
        }
        else
        {
            final int length = value.length();
            ensureCapacity(index, length + STR_HEADER_LEN);

            int lengthBits = length;
            if (NATIVE_BYTE_ORDER != byteOrder)
            {
                lengthBits = Integer.reverseBytes(lengthBits);
            }

            final byte[] array = byteArray;
            final long offset = addressOffset + index;
            UNSAFE.putInt(array, offset, lengthBits);

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
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final CharSequence value, final ByteOrder byteOrder)
    {
        if (null == value)
        {
            ensureCapacity(index, STR_HEADER_LEN);
            UNSAFE.putInt(byteArray, addressOffset + index, 0);
            return STR_HEADER_LEN;
        }
        else
        {
            final int length = value.length();
            ensureCapacity(index, length + STR_HEADER_LEN);

            int lengthBits = length;
            if (NATIVE_BYTE_ORDER != byteOrder)
            {
                lengthBits = Integer.reverseBytes(lengthBits);
            }

            final byte[] array = byteArray;
            final long offset = addressOffset + index;
            UNSAFE.putInt(array, offset, lengthBits);

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

        if (0 == length)
        {
            return "";
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
            final byte[] array = byteArray;
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
        if (null == value)
        {
            return 0;
        }

        final int length = value.length();

        ensureCapacity(index, length);

        final byte[] array = byteArray;
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
        if (null == value)
        {
            return 0;
        }

        final int length = value.length();

        ensureCapacity(index, length);

        final byte[] array = byteArray;
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
        if (null == value)
        {
            return 0;
        }

        final int len = Math.min(value.length() - valueOffset, length);

        ensureCapacity(index, len);

        final byte[] array = byteArray;
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
        if (null == value)
        {
            return 0;
        }

        final int len = Math.min(value.length() - valueOffset, length);

        ensureCapacity(index, len);

        final byte[] array = byteArray;
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

        final int length = UNSAFE.getInt(byteArray, addressOffset + index);
        if (0 == length)
        {
            return "";
        }

        return getStringWithoutLengthUtf8(index + STR_HEADER_LEN, length);
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
        if (0 == length)
        {
            return "";
        }

        return getStringWithoutLengthUtf8(index + STR_HEADER_LEN, length);
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

        ensureCapacity(index, STR_HEADER_LEN + bytes.length);

        final byte[] array = byteArray;
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

        ensureCapacity(index, STR_HEADER_LEN + bytes.length);

        int bits = bytes.length;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        final byte[] array = byteArray;
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

        if (0 == length)
        {
            return "";
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
        ensureCapacity(index, bytes.length);

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

        final boolean negative = MINUS_SIGN == UNSAFE.getByte(byteArray, addressOffset + index);
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

        final boolean negative = MINUS_SIGN == UNSAFE.getByte(byteArray, addressOffset + index);
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

        final byte[] array;
        long offset;
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

            ensureCapacity(index, length);
            array = byteArray;
            offset = addressOffset + index;

            UNSAFE.putByte(array, offset, MINUS_SIGN);
            offset++;
        }
        else
        {
            length = digitCount = digitCount(quotient);

            ensureCapacity(index, length);
            array = byteArray;
            offset = addressOffset + index;
        }

        putPositiveIntAscii(array, offset, quotient, digitCount);

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

        ensureCapacity(index, digitCount);

        putPositiveIntAscii(byteArray, addressOffset + index, value, digitCount);

        return digitCount;
    }

    /**
     * {@inheritDoc}
     */
    public void putNaturalPaddedIntAscii(final int offset, final int length, final int value)
    {
        ensureCapacity(offset, length);

        final byte[] array = byteArray;
        final long addressOffset = this.addressOffset;
        final int end = offset + length;
        int remainder = value;
        for (int index = end - 1; index >= offset; index--)
        {
            final int digit = remainder % 10;
            remainder = remainder / 10;
            UNSAFE.putByte(array, addressOffset + index, (byte)(ZERO + digit));
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
        final int length = digitCount(value);
        ensureCapacity(endExclusive - length, length);

        final byte[] array = byteArray;
        final long addressOffset = this.addressOffset;
        int remainder = value;
        int index = endExclusive;
        while (remainder > 0)
        {
            index--;
            final int digit = remainder % 10;
            remainder = remainder / 10;
            UNSAFE.putByte(array, addressOffset + index, (byte)(ZERO + digit));
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

        ensureCapacity(index, digitCount);

        putPositiveLongAscii(byteArray, addressOffset + index, value, digitCount);

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

        final byte[] array;
        long offset;
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

            ensureCapacity(index, length);
            array = byteArray;
            offset = addressOffset + index;

            UNSAFE.putByte(array, offset, MINUS_SIGN);
            offset++;
        }
        else
        {
            length = digitCount = digitCount(quotient);

            ensureCapacity(index, length);
            array = byteArray;
            offset = addressOffset + index;
        }

        putPositiveLongAscii(array, offset, quotient, digitCount);

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

        final AbstractMutableDirectBuffer that = (AbstractMutableDirectBuffer)obj;

        final int length = capacity;
        if (length != that.capacity)
        {
            return false;
        }

        final byte[] thisArray = byteArray;
        final byte[] thatArray = that.byteArray;
        final long thisOffset = this.addressOffset;
        final long thatOffset = that.addressOffset;

        int i = 0;
        for (int end = length & ~7; i < end; i += 8)
        {
            if (UNSAFE.getLong(thisArray, thisOffset + i) != UNSAFE.getLong(thatArray, thatOffset + i))
            {
                return false;
            }
        }

        for (; i < length; i++)
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
        final byte[] array = byteArray;
        final long addressOffset = this.addressOffset;
        final int length = capacity;
        int i = 0, hashCode = 19;
        for (int end = length & ~7; i < end; i += 8)
        {
            hashCode = 31 * hashCode + Long.hashCode(UNSAFE.getLong(array, addressOffset + i));
        }

        for (; i < length; i++)
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
        if (this == that)
        {
            return 0;
        }

        final int thisCapacity = this.capacity;
        final int thatCapacity = that.capacity();
        final byte[] thisArray = byteArray;
        final byte[] thatArray = that.byteArray();
        final long thisOffset = this.addressOffset;
        final long thatOffset = that.addressOffset();
        final int length = Math.min(thisCapacity, thatCapacity);
        int i = 0;

        for (int end = length & ~7; i < end; i += 8)
        {
            final int cmp = Long.compare(
                UNSAFE.getLong(thisArray, thisOffset + i),
                UNSAFE.getLong(thatArray, thatOffset + i));

            if (0 != cmp)
            {
                return cmp;
            }
        }

        for (; i < length; i++)
        {
            final int cmp = Byte.compare(
                UNSAFE.getByte(thisArray, thisOffset + i),
                UNSAFE.getByte(thatArray, thatOffset + i));

            if (0 != cmp)
            {
                return cmp;
            }
        }

        return Integer.compare(thisCapacity, thatCapacity);
    }

    protected final void boundsCheck0(final int index, final int length)
    {
        final long resultingPosition = index + (long)length;
        if (index < 0 || length < 0 || resultingPosition > capacity)
        {
            throw new IndexOutOfBoundsException("index=" + index + " length=" + length + " capacity=" + capacity);
        }
    }

    protected abstract void ensureCapacity(int index, int length);

    private int parsePositiveIntAscii(final int index, final int length, final int startIndex, final int end)
    {
        final long offset = addressOffset;
        final byte[] array = byteArray;
        int i = startIndex;
        int tally = 0, quartet;
        while ((end - i) >= 4 && isFourDigitsAsciiEncodedNumber(quartet = UNSAFE.getInt(array, offset + i)))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                quartet = Integer.reverseBytes(quartet);
            }

            tally = (tally * 10_000) + parseFourDigitsLittleEndian(quartet);
            i += 4;
        }

        byte digit;
        while (i < end && isDigit(digit = UNSAFE.getByte(array, offset + i)))
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
        final byte[] array = byteArray;
        int i = startIndex;
        long tally = 0;
        long octet = UNSAFE.getLong(array, offset + i);
        if (isEightDigitAsciiEncodedNumber(octet))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                octet = Long.reverseBytes(octet);
            }
            tally = parseEightDigitsLittleEndian(octet);
            i += 8;

            byte digit;
            while (i < end && isDigit(digit = UNSAFE.getByte(array, offset + i)))
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
        final byte[] array = byteArray;
        int i = startIndex;
        long tally = 0, octet;
        while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = UNSAFE.getLong(array, offset + i)))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                octet = Long.reverseBytes(octet);
            }

            tally = (tally * 100_000_000L) + parseEightDigitsLittleEndian(octet);
            i += 8;
        }

        int quartet;
        while ((end - i) >= 4 && isFourDigitsAsciiEncodedNumber(quartet = UNSAFE.getInt(array, offset + i)))
        {
            if (NATIVE_BYTE_ORDER != LITTLE_ENDIAN)
            {
                quartet = Integer.reverseBytes(quartet);
            }

            tally = (tally * 10_000L) + parseFourDigitsLittleEndian(quartet);
            i += 4;
        }

        byte digit;
        while (i < end && isDigit(digit = UNSAFE.getByte(array, offset + i)))
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
        final byte[] array = byteArray;
        int i = startIndex, k = 0;
        boolean checkOverflow = true;
        long tally = 0, octet;
        while ((end - i) >= 8 && isEightDigitAsciiEncodedNumber(octet = UNSAFE.getLong(array, offset + i)))
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
        while (i < end && isDigit(digit = UNSAFE.getByte(array, offset + i)))
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
