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
package org.agrona;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.agrona.AsciiEncoding.*;
import static org.agrona.BitUtil.*;
import static org.agrona.BufferUtil.*;
import static org.agrona.UnsafeAccess.UNSAFE;

/**
 * Expandable {@link MutableDirectBuffer} that is backed by an array. When values are put into the buffer beyond its
 * current length, then it will be expanded to accommodate the resulting position for the value.
 * <p>
 * Put operations will expand the capacity as necessary up to {@link #MAX_ARRAY_LENGTH}. Get operations will throw
 * a {@link IndexOutOfBoundsException} if past current capacity.
 * <p>
 * <b>Note:</b> this class has a natural ordering that is inconsistent with equals.
 * Types may be different but equal on buffer contents.
 */
public class ExpandableArrayBuffer implements MutableDirectBuffer
{
    /**
     * Maximum length to which the underlying buffer can grow. Some JVMs store state in the last few bytes.
     */
    public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    /**
     * Initial capacity of the buffer from which it will expand as necessary.
     */
    public static final int INITIAL_CAPACITY = 128;

    private byte[] byteArray;

    /**
     * Create an {@link ExpandableArrayBuffer} with an initial length of {@link #INITIAL_CAPACITY}.
     */
    public ExpandableArrayBuffer()
    {
        this(INITIAL_CAPACITY);
    }

    /**
     * Create an {@link ExpandableArrayBuffer} with a provided initial length.
     *
     * @param initialCapacity of the buffer.
     */
    public ExpandableArrayBuffer(final int initialCapacity)
    {
        byteArray = new byte[initialCapacity];
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
    public void wrap(final byte[] buffer, final int offset, final int length)
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
    public void wrap(final ByteBuffer buffer, final int offset, final int length)
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
    public void wrap(final DirectBuffer buffer, final int offset, final int length)
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
        return ARRAY_BASE_OFFSET;
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setMemory(final int index, final int length, final byte value)
    {
        ensureCapacity(index, length);
        Arrays.fill(byteArray, index, index + length, value);
    }

    /**
     * {@inheritDoc}
     */
    public int capacity()
    {
        return byteArray.length;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpandable()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void checkLimit(final int limit)
    {
        ensureCapacity(limit, SIZE_OF_BYTE);
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_LONG);

        long bits = UNSAFE.getLong(byteArray, ARRAY_BASE_OFFSET + index);
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

        UNSAFE.putLong(byteArray, ARRAY_BASE_OFFSET + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(final int index)
    {
        boundsCheck0(index, SIZE_OF_LONG);

        return UNSAFE.getLong(byteArray, ARRAY_BASE_OFFSET + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putLong(final int index, final long value)
    {
        ensureCapacity(index, SIZE_OF_LONG);

        UNSAFE.putLong(byteArray, ARRAY_BASE_OFFSET + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_INT);

        int bits = UNSAFE.getInt(byteArray, ARRAY_BASE_OFFSET + index);
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

        UNSAFE.putInt(byteArray, ARRAY_BASE_OFFSET + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(final int index)
    {
        boundsCheck0(index, SIZE_OF_INT);

        return UNSAFE.getInt(byteArray, ARRAY_BASE_OFFSET + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putInt(final int index, final int value)
    {
        ensureCapacity(index, SIZE_OF_INT);

        UNSAFE.putInt(byteArray, ARRAY_BASE_OFFSET + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_DOUBLE);

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = UNSAFE.getLong(byteArray, ARRAY_BASE_OFFSET + index);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getDouble(byteArray, ARRAY_BASE_OFFSET + index);
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
            UNSAFE.putLong(byteArray, ARRAY_BASE_OFFSET + index, bits);
        }
        else
        {
            UNSAFE.putDouble(byteArray, ARRAY_BASE_OFFSET + index, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(final int index)
    {
        boundsCheck0(index, SIZE_OF_DOUBLE);

        return UNSAFE.getDouble(byteArray, ARRAY_BASE_OFFSET + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putDouble(final int index, final double value)
    {
        ensureCapacity(index, SIZE_OF_DOUBLE);

        UNSAFE.putDouble(byteArray, ARRAY_BASE_OFFSET + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_FLOAT);

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = UNSAFE.getInt(byteArray, ARRAY_BASE_OFFSET + index);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getFloat(byteArray, ARRAY_BASE_OFFSET + index);
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
            UNSAFE.putInt(byteArray, ARRAY_BASE_OFFSET + index, bits);
        }
        else
        {
            UNSAFE.putFloat(byteArray, ARRAY_BASE_OFFSET + index, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(final int index)
    {
        boundsCheck0(index, SIZE_OF_FLOAT);

        return UNSAFE.getFloat(byteArray, ARRAY_BASE_OFFSET + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putFloat(final int index, final float value)
    {
        ensureCapacity(index, SIZE_OF_FLOAT);

        UNSAFE.putFloat(byteArray, ARRAY_BASE_OFFSET + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_SHORT);

        short bits = UNSAFE.getShort(byteArray, ARRAY_BASE_OFFSET + index);
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

        UNSAFE.putShort(byteArray, ARRAY_BASE_OFFSET + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(final int index)
    {
        boundsCheck0(index, SIZE_OF_SHORT);

        return UNSAFE.getShort(byteArray, ARRAY_BASE_OFFSET + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putShort(final int index, final short value)
    {
        ensureCapacity(index, SIZE_OF_SHORT);

        UNSAFE.putShort(byteArray, ARRAY_BASE_OFFSET + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public byte getByte(final int index)
    {
        return byteArray[index];
    }

    /**
     * {@inheritDoc}
     */
    public void putByte(final int index, final byte value)
    {
        ensureCapacity(index, SIZE_OF_BYTE);
        byteArray[index] = value;
    }

    private void putByte0(final int index, final byte value)
    {
        ensureCapacity(index, SIZE_OF_BYTE);
        byteArray[index] = value;
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final int index, final byte[] dst)
    {
        getBytes(index, dst, 0, dst.length);
    }

    /**
     * {@inheritDoc}
     */
    public void getBytes(final int index, final byte[] dst, final int offset, final int length)
    {
        System.arraycopy(byteArray, index, dst, offset, length);
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
        boundsCheck0(index, length);
        BufferUtil.boundsCheck(dstBuffer, dstOffset, length);

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


        UNSAFE.copyMemory(byteArray, ARRAY_BASE_OFFSET + index, dstByteArray, dstBaseOffset + dstOffset, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final byte[] src)
    {
        putBytes(index, src, 0, src.length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final byte[] src, final int offset, final int length)
    {
        ensureCapacity(index, length);
        System.arraycopy(src, offset, byteArray, index, length);
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
        BufferUtil.boundsCheck(srcBuffer, srcIndex, length);

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

        UNSAFE.copyMemory(srcByteArray, srcBaseOffset + srcIndex, byteArray, ARRAY_BASE_OFFSET + index, length);
    }

    /**
     * {@inheritDoc}
     */
    public void putBytes(final int index, final DirectBuffer srcBuffer, final int srcIndex, final int length)
    {
        ensureCapacity(index, length);
        srcBuffer.boundsCheck(srcIndex, length);

        UNSAFE.copyMemory(
            srcBuffer.byteArray(),
            srcBuffer.addressOffset() + srcIndex,
            byteArray,
            ARRAY_BASE_OFFSET + index,
            length);
    }

    /**
     * {@inheritDoc}
     */
    public char getChar(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_SHORT);

        char bits = UNSAFE.getChar(byteArray, ARRAY_BASE_OFFSET + index);
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

        UNSAFE.putChar(byteArray, ARRAY_BASE_OFFSET + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public char getChar(final int index)
    {
        boundsCheck0(index, SIZE_OF_CHAR);

        return UNSAFE.getChar(byteArray, ARRAY_BASE_OFFSET + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putChar(final int index, final char value)
    {
        ensureCapacity(index, SIZE_OF_CHAR);

        UNSAFE.putChar(byteArray, ARRAY_BASE_OFFSET + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringAscii(final int index)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(byteArray, ARRAY_BASE_OFFSET + index);

        return getStringAscii(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final Appendable appendable)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(byteArray, ARRAY_BASE_OFFSET + index);

        return getStringAscii(index, length, appendable);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringAscii(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        int bits = UNSAFE.getInt(byteArray, ARRAY_BASE_OFFSET + index);
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

        int bits = UNSAFE.getInt(byteArray, ARRAY_BASE_OFFSET + index);
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
        final byte[] stringInBytes = new byte[length];
        System.arraycopy(byteArray, index + STR_HEADER_LEN, stringInBytes, 0, length);

        return new String(stringInBytes, US_ASCII);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final int length, final Appendable appendable)
    {
        try
        {
            for (int i = index + STR_HEADER_LEN, limit = index + STR_HEADER_LEN + length; i < limit; i++)
            {
                final char c = (char)(byteArray[i]);
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

        ensureCapacity(index, length + STR_HEADER_LEN);

        UNSAFE.putInt(byteArray, ARRAY_BASE_OFFSET + index, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, ARRAY_BASE_OFFSET + STR_HEADER_LEN + index + i, (byte)c);
        }

        return STR_HEADER_LEN + length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final CharSequence value)
    {
        final int length = value != null ? value.length() : 0;

        ensureCapacity(index, length + STR_HEADER_LEN);

        UNSAFE.putInt(byteArray, ARRAY_BASE_OFFSET + index, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, ARRAY_BASE_OFFSET + STR_HEADER_LEN + index + i, (byte)c);
        }

        return STR_HEADER_LEN + length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final String value, final ByteOrder byteOrder)
    {
        final int length = value != null ? value.length() : 0;

        ensureCapacity(index, length + STR_HEADER_LEN);

        int bits = length;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        UNSAFE.putInt(byteArray, ARRAY_BASE_OFFSET + index, bits);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, ARRAY_BASE_OFFSET + STR_HEADER_LEN + index + i, (byte)c);
        }

        return STR_HEADER_LEN + length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringAscii(final int index, final CharSequence value, final ByteOrder byteOrder)
    {
        final int length = value != null ? value.length() : 0;

        ensureCapacity(index, length + STR_HEADER_LEN);

        int bits = length;
        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            bits = Integer.reverseBytes(bits);
        }

        UNSAFE.putInt(byteArray, ARRAY_BASE_OFFSET + index, bits);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, ARRAY_BASE_OFFSET + STR_HEADER_LEN + index + i, (byte)c);
        }

        return STR_HEADER_LEN + length;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringWithoutLengthAscii(final int index, final int length)
    {
        final byte[] stringInBytes = new byte[length];
        System.arraycopy(byteArray, index, stringInBytes, 0, length);

        return new String(stringInBytes, US_ASCII);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringWithoutLengthAscii(final int index, final int length, final Appendable appendable)
    {
        try
        {
            for (int i = index, limit = index + length; i < limit; i++)
            {
                final char c = (char)(byteArray[i]);
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

        ensureCapacity(index, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, ARRAY_BASE_OFFSET + index + i, (byte)c);
        }

        return length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthAscii(final int index, final CharSequence value)
    {
        final int length = value != null ? value.length() : 0;

        ensureCapacity(index, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, ARRAY_BASE_OFFSET + index + i, (byte)c);
        }

        return length;
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthAscii(final int index, final String value, final int valueOffset, final int length)
    {
        final int len = value != null ? Math.min(value.length() - valueOffset, length) : 0;

        ensureCapacity(index, len);

        for (int i = 0; i < len; i++)
        {
            char c = value.charAt(valueOffset + i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, ARRAY_BASE_OFFSET + index + i, (byte)c);
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

        ensureCapacity(index, len);

        for (int i = 0; i < len; i++)
        {
            char c = value.charAt(valueOffset + i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(byteArray, ARRAY_BASE_OFFSET + index + i, (byte)c);
        }

        return len;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringUtf8(final int index)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(byteArray, ARRAY_BASE_OFFSET + index);

        return getStringUtf8(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringUtf8(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        int bits = UNSAFE.getInt(byteArray, ARRAY_BASE_OFFSET + index);
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
        final byte[] stringInBytes = new byte[length];
        System.arraycopy(byteArray, index + STR_HEADER_LEN, stringInBytes, 0, length);

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

        UNSAFE.putInt(byteArray, ARRAY_BASE_OFFSET + index, bytes.length);
        System.arraycopy(bytes, 0, byteArray, index + STR_HEADER_LEN, bytes.length);

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

        UNSAFE.putInt(byteArray, ARRAY_BASE_OFFSET + index, bits);
        System.arraycopy(bytes, 0, byteArray, index + STR_HEADER_LEN, bytes.length);

        return STR_HEADER_LEN + bytes.length;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringWithoutLengthUtf8(final int index, final int length)
    {
        final byte[] stringInBytes = new byte[length];
        System.arraycopy(byteArray, index, stringInBytes, 0, length);

        return new String(stringInBytes, UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthUtf8(final int index, final String value)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        ensureCapacity(index, bytes.length);
        System.arraycopy(bytes, 0, byteArray, index, bytes.length);

        return bytes.length;
    }

    /**
     * {@inheritDoc}
     */
    public int parseNaturalIntAscii(final int index, final int length)
    {
        boundsCheck0(index, length);

        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }
        else if (length > INT_MAX_DIGITS)
        {
            throw new AsciiNumberFormatException("int overflow parsing: " + getStringWithoutLengthAscii(index, length));
        }

        final byte[] src = byteArray;
        final int firstDigit = AsciiEncoding.getDigit(index, src[index]);

        if (length < INT_MAX_DIGITS || firstDigit < 2)
        {
            int tally = firstDigit;
            for (int i = index + 1, end = index + length; i < end; i++)
            {
                tally = (tally * 10) + AsciiEncoding.getDigit(i, src[i]);
            }
            return tally;
        }
        else
        {
            long tally = firstDigit;
            for (int i = index + 1, end = index + length; i < end; i++)
            {
                tally = (tally * 10L) + AsciiEncoding.getDigit(i, src[i]);
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
        boundsCheck0(index, length);

        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }
        else if (length > LONG_MAX_DIGITS)
        {
            throw new AsciiNumberFormatException("long overflow parsing: " +
                getStringWithoutLengthAscii(index, length));
        }

        final byte[] src = byteArray;
        final int firstDigit = AsciiEncoding.getDigit(index, src[index]);

        if (length < LONG_MAX_DIGITS || firstDigit < 9)
        {
            long tally = firstDigit;
            for (int i = index + 1, end = index + length; i < end; i++)
            {
                tally = (tally * 10L) + AsciiEncoding.getDigit(i, src[i]);
            }
            return tally;
        }
        else
        {
            return parseLongAsciiOverflowCheck(index, length, src, MAX_LONG_VALUE, 1, firstDigit);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int parseIntAscii(final int index, final int length)
    {
        boundsCheck0(index, length);

        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }
        else if (1 == length)
        {
            return AsciiEncoding.getDigit(index, byteArray[index]);
        }

        final byte[] src = byteArray;
        final byte first = src[index];
        final boolean negativeValue = MINUS_SIGN == first;
        final int digitCount;
        final int firstDigit;
        int i = index + 1;
        if (negativeValue)
        {
            digitCount = length - 1;
            firstDigit = getDigit(i, src[i]);
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
                tally = (tally * 10) + AsciiEncoding.getDigit(i, src[i]);
            }
            return negativeValue ? -tally : tally;
        }
        else if (INT_MAX_DIGITS == digitCount)
        {
            long tally = firstDigit;
            for (int end = index + length; i < end; i++)
            {
                tally = (tally * 10L) + AsciiEncoding.getDigit(i, src[i]);
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
        boundsCheck0(index, length);

        if (length <= 0)
        {
            throw new AsciiNumberFormatException("empty string: index=" + index + " length=" + length);
        }
        else if (1 == length)
        {
            return AsciiEncoding.getDigit(index, byteArray[index]);
        }

        final byte[] src = byteArray;
        final byte first = src[index];
        final boolean negativeValue = MINUS_SIGN == first;
        final int digitCount;
        final int firstDigit;
        int i = index + 1;
        if (negativeValue)
        {
            digitCount = length - 1;
            firstDigit = getDigit(i, src[i]);
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
                tally = (tally * 10L) + AsciiEncoding.getDigit(i, src[i]);
            }
            return negativeValue ? -tally : tally;
        }
        else if (LONG_MAX_DIGITS == digitCount)
        {
            if (negativeValue)
            {
                return -parseLongAsciiOverflowCheck(index, length, src, MIN_LONG_VALUE, 2, firstDigit);
            }
            else
            {
                return parseLongAsciiOverflowCheck(index, length, src, MAX_LONG_VALUE, 1, firstDigit);
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
            putByte0(index, ZERO);
            return 1;
        }

        if (value == Integer.MIN_VALUE)
        {
            putBytes(index, MIN_INTEGER_VALUE);
            return MIN_INTEGER_VALUE.length;
        }

        final byte[] dest = byteArray;
        int offset = index;
        int quotient = value;
        final int digitCount, length;
        if (value < 0)
        {
            quotient = -quotient;
            digitCount = digitCount(quotient);
            length = digitCount + 1;

            ensureCapacity(index, length);

            dest[offset] = MINUS_SIGN;
            offset++;
        }
        else
        {
            length = digitCount = digitCount(quotient);

            ensureCapacity(index, length);
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
            putByte0(index, ZERO);
            return 1;
        }

        final int digitCount = digitCount(value);

        ensureCapacity(index, digitCount);

        putPositiveIntAscii(byteArray, index, value, digitCount);

        return digitCount;
    }

    /**
     * {@inheritDoc}
     */
    public void putNaturalPaddedIntAscii(final int offset, final int length, final int value)
    {
        ensureCapacity(offset, length);

        final byte[] dest = byteArray;
        final int end = offset + length;
        int remainder = value;
        for (int index = end - 1; index >= offset; index--)
        {
            final int digit = remainder % 10;
            remainder = remainder / 10;
            dest[index] = (byte)(ZERO + digit);
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
            putByte0(index, (byte)(ZERO + digit));
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
            putByte0(index, ZERO);
            return 1;
        }

        final int digitCount = digitCount(value);

        ensureCapacity(index, digitCount);

        putPositiveLongAscii(byteArray, index, value, digitCount);

        return digitCount;
    }

    /**
     * {@inheritDoc}
     */
    public int putLongAscii(final int index, final long value)
    {
        if (value == 0)
        {
            putByte0(index, ZERO);
            return 1;
        }

        if (value == Long.MIN_VALUE)
        {
            putBytes(index, MIN_LONG_VALUE);
            return MIN_LONG_VALUE.length;
        }

        final byte[] dest = byteArray;
        int offset = index;
        long quotient = value;
        final int digitCount, length;
        if (value < 0)
        {
            quotient = -quotient;
            digitCount = digitCount(quotient);
            length = digitCount + 1;

            ensureCapacity(index, length);

            dest[offset] = MINUS_SIGN;
            offset++;
        }
        else
        {
            length = digitCount = digitCount(quotient);

            ensureCapacity(index, length);
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
        return 0;
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

        final ExpandableArrayBuffer that = (ExpandableArrayBuffer)obj;

        return Arrays.equals(this.byteArray, that.byteArray);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return Arrays.hashCode(byteArray);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "ExpandableArrayBuffer{" +
            "byteArray=" + byteArray + // lgtm [java/print-array]
            " byteArray.length" + (null == byteArray ? 0 : byteArray.length) +
            '}';
    }

    private void ensureCapacity(final int index, final int length)
    {
        if (index < 0 || length < 0)
        {
            throw new IndexOutOfBoundsException("negative value: index=" + index + " length=" + length);
        }

        final long resultingPosition = index + (long)length;
        final int currentArrayLength = byteArray.length;
        if (resultingPosition > currentArrayLength)
        {
            if (resultingPosition > MAX_ARRAY_LENGTH)
            {
                throw new IndexOutOfBoundsException(
                    "index=" + index + " length=" + length + " maxCapacity=" + MAX_ARRAY_LENGTH);
            }

            byteArray = Arrays.copyOf(byteArray, calculateExpansion(currentArrayLength, resultingPosition));
        }
    }

    private int calculateExpansion(final int currentLength, final long requiredLength)
    {
        long value = Math.max(currentLength, INITIAL_CAPACITY);

        while (value < requiredLength)
        {
            value = value + (value >> 1);

            if (value > MAX_ARRAY_LENGTH)
            {
                value = MAX_ARRAY_LENGTH;
            }
        }

        return (int)value;
    }

    private void boundsCheck0(final int index, final int length)
    {
        final int currentArrayLength = byteArray.length;
        final long resultingPosition = index + (long)length;
        if (index < 0 || length < 0 || resultingPosition > currentArrayLength)
        {
            throw new IndexOutOfBoundsException(
                "index=" + index + " length=" + length + " capacity=" + currentArrayLength);
        }
    }

    private long parseLongAsciiOverflowCheck(
        final int index,
        final int length,
        final byte[] src,
        final byte[] maxValue,
        final int position,
        final int first)
    {
        long tally = first;
        boolean checkOverflow = true;
        for (int i = index + position, end = index + length; i < end; i++)
        {
            final byte b = src[i];
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

    private static void putPositiveIntAscii(final byte[] dest, final int offset, final int value, final int digitCount)
    {
        int quotient = value;
        int i = digitCount;
        while (quotient >= 10_000)
        {
            final int lastFourDigits = quotient % 10_000;
            quotient /= 10_000;

            final int p1 = (lastFourDigits / 100) << 1;
            final int p2 = (lastFourDigits % 100) << 1;

            i -= 4;

            dest[offset + i] = ASCII_DIGITS[p1];
            dest[offset + i + 1] = ASCII_DIGITS[p1 + 1];
            dest[offset + i + 2] = ASCII_DIGITS[p2];
            dest[offset + i + 3] = ASCII_DIGITS[p2 + 1];
        }

        if (quotient >= 100)
        {
            final int position = (quotient % 100) << 1;
            quotient /= 100;
            dest[offset + i - 1] = ASCII_DIGITS[position + 1];
            dest[offset + i - 2] = ASCII_DIGITS[position];
        }

        if (quotient >= 10)
        {
            final int position = quotient << 1;
            dest[offset + 1] = ASCII_DIGITS[position + 1];
            dest[offset] = ASCII_DIGITS[position];
        }
        else
        {
            dest[offset] = (byte)(ZERO + quotient);
        }
    }

    private static void putPositiveLongAscii(
        final byte[] dest, final int offset, final long value, final int digitCount)
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

            dest[offset + i] = ASCII_DIGITS[u1];
            dest[offset + i + 1] = ASCII_DIGITS[u1 + 1];
            dest[offset + i + 2] = ASCII_DIGITS[u2];
            dest[offset + i + 3] = ASCII_DIGITS[u2 + 1];
            dest[offset + i + 4] = ASCII_DIGITS[l1];
            dest[offset + i + 5] = ASCII_DIGITS[l1 + 1];
            dest[offset + i + 6] = ASCII_DIGITS[l2];
            dest[offset + i + 7] = ASCII_DIGITS[l2 + 1];
        }

        putPositiveIntAscii(dest, offset, (int)quotient, i);
    }
}
