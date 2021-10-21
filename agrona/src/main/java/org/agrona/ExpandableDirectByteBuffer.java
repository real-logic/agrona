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

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.agrona.AsciiEncoding.*;
import static org.agrona.BitUtil.*;
import static org.agrona.BufferUtil.*;
import static org.agrona.UnsafeAccess.*;

/**
 * Expandable {@link MutableDirectBuffer} that is backed by a direct {@link ByteBuffer}. When values are put into the
 * buffer beyond its current length, then it will be expanded to accommodate the resulting position for the value.
 * <p>
 * Put operations will expand the capacity as necessary up to {@link #MAX_BUFFER_LENGTH}. Get operations will throw
 * a {@link IndexOutOfBoundsException} if past current capacity.
 * <p>
 * {@link ByteOrder} of a wrapped buffer is not applied to the {@link ExpandableDirectByteBuffer};
 * To control {@link ByteOrder} use the appropriate method with the {@link ByteOrder} overload.
 * <p>
 * <b>Note:</b> this class has a natural ordering that is inconsistent with equals.
 * Types may be different but equal on buffer contents.
 */
public class ExpandableDirectByteBuffer implements MutableDirectBuffer
{
    /**
     * Maximum length to which the underlying buffer can grow.
     */
    public static final int MAX_BUFFER_LENGTH = 1024 * 1024 * 1024;

    /**
     * Initial capacity of the buffer from which it will expand.
     */
    public static final int INITIAL_CAPACITY = 128;

    private long address;
    private int capacity;
    private ByteBuffer byteBuffer;

    /**
     * Create an {@link ExpandableDirectByteBuffer} with an initial length of {@link #INITIAL_CAPACITY}.
     */
    public ExpandableDirectByteBuffer()
    {
        this(INITIAL_CAPACITY);
    }

    /**
     * Create an {@link ExpandableDirectByteBuffer} with a provided initial capacity.
     *
     * @param initialCapacity of the backing array.
     */
    public ExpandableDirectByteBuffer(final int initialCapacity)
    {
        byteBuffer = ByteBuffer.allocateDirect(initialCapacity);
        capacity = initialCapacity;
        address = address(byteBuffer);
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
        return address;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] byteArray()
    {
        return null;
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
        ensureCapacity(index, length);

        final long offset = address + index;
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
     * {@inheritDoc}
     */
    public int capacity()
    {
        return capacity;
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
        if (limit < 0)
        {
            throw new IndexOutOfBoundsException("limit cannot be negative: limit=" + limit);
        }

        ensureCapacity(limit, SIZE_OF_BYTE);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public long getLong(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_LONG);

        long bits = UNSAFE.getLong(null, address + index);
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

        UNSAFE.putLong(null, address + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(final int index)
    {
        boundsCheck0(index, SIZE_OF_LONG);

        return UNSAFE.getLong(null, address + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putLong(final int index, final long value)
    {
        ensureCapacity(index, SIZE_OF_LONG);

        UNSAFE.putLong(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public int getInt(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_INT);

        int bits = UNSAFE.getInt(null, address + index);
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

        UNSAFE.putInt(null, address + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(final int index)
    {
        boundsCheck0(index, SIZE_OF_INT);

        return UNSAFE.getInt(null, address + index);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public double getDouble(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_DOUBLE);

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final long bits = UNSAFE.getLong(null, address + index);
            return Double.longBitsToDouble(Long.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getDouble(null, address + index);
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
            UNSAFE.putLong(null, address + index, bits);
        }
        else
        {
            UNSAFE.putDouble(null, address + index, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(final int index)
    {
        boundsCheck0(index, SIZE_OF_DOUBLE);

        return UNSAFE.getDouble(null, address + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putDouble(final int index, final double value)
    {
        ensureCapacity(index, SIZE_OF_DOUBLE);

        UNSAFE.putDouble(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public float getFloat(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_FLOAT);

        if (NATIVE_BYTE_ORDER != byteOrder)
        {
            final int bits = UNSAFE.getInt(null, address + index);
            return Float.intBitsToFloat(Integer.reverseBytes(bits));
        }
        else
        {
            return UNSAFE.getFloat(null, address + index);
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
            UNSAFE.putInt(null, address + index, bits);
        }
        else
        {
            UNSAFE.putFloat(null, address + index, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(final int index)
    {
        boundsCheck0(index, SIZE_OF_FLOAT);

        return UNSAFE.getFloat(null, address + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putFloat(final int index, final float value)
    {
        ensureCapacity(index, SIZE_OF_FLOAT);

        UNSAFE.putFloat(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public short getShort(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_SHORT);

        short bits = UNSAFE.getShort(null, address + index);
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

        UNSAFE.putShort(null, address + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(final int index)
    {
        boundsCheck0(index, SIZE_OF_SHORT);

        return UNSAFE.getShort(null, address + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putShort(final int index, final short value)
    {
        ensureCapacity(index, SIZE_OF_SHORT);

        UNSAFE.putShort(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public byte getByte(final int index)
    {
        boundsCheck0(index, SIZE_OF_BYTE);
        return UNSAFE.getByte(null, address + index);
    }

    private byte getByte0(final int index)
    {
        boundsCheck0(index, SIZE_OF_BYTE);
        return UNSAFE.getByte(null, address + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putByte(final int index, final byte value)
    {
        ensureCapacity(index, SIZE_OF_BYTE);
        UNSAFE.putByte(null, address + index, value);
    }

    private void putByte0(final int index, final byte value)
    {
        ensureCapacity(index, SIZE_OF_BYTE);
        UNSAFE.putByte(null, address + index, value);
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
        boundsCheck0(index, length);
        BufferUtil.boundsCheck(dst, offset, length);

        UNSAFE.copyMemory(null, address + index, dst, ARRAY_BASE_OFFSET + offset, length);
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

        UNSAFE.copyMemory(null, address + index, dstByteArray, dstBaseOffset + dstOffset, length);
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

        BufferUtil.boundsCheck(src, offset, length);

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET + offset, null, address + index, length);
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

        UNSAFE.copyMemory(srcByteArray, srcBaseOffset + srcIndex, null, address + index, length);
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
            null,
            address + index,
            length);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public char getChar(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, SIZE_OF_SHORT);

        char bits = UNSAFE.getChar(null, address + index);
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

        UNSAFE.putChar(null, address + index, bits);
    }

    /**
     * {@inheritDoc}
     */
    public char getChar(final int index)
    {
        boundsCheck0(index, SIZE_OF_CHAR);

        return UNSAFE.getChar(null, address + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putChar(final int index, final char value)
    {
        ensureCapacity(index, SIZE_OF_CHAR);

        UNSAFE.putChar(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public String getStringAscii(final int index)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(null, address + index);

        return getStringAscii(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final Appendable appendable)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(null, address + index);

        return getStringAscii(index, length, appendable);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringAscii(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        int bits = UNSAFE.getInt(null, address + index);
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

        int bits = UNSAFE.getInt(null, address + index);
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
        boundsCheck0(index + STR_HEADER_LEN, length);

        final byte[] dst = new byte[length];
        UNSAFE.copyMemory(null, address + index + STR_HEADER_LEN, dst, ARRAY_BASE_OFFSET, length);

        return new String(dst, US_ASCII);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringAscii(final int index, final int length, final Appendable appendable)
    {
        boundsCheck0(index, length + STR_HEADER_LEN);

        try
        {
            for (int i = index + STR_HEADER_LEN, limit = index + STR_HEADER_LEN + length; i < limit; i++)
            {
                final char c = (char)UNSAFE.getByte(null, address + i);
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

        UNSAFE.putInt(null, address + index, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(null, address + STR_HEADER_LEN + index + i, (byte)c);
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

        UNSAFE.putInt(null, address + index, length);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(null, address + STR_HEADER_LEN + index + i, (byte)c);
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

        UNSAFE.putInt(null, address + index, bits);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(null, address + STR_HEADER_LEN + index + i, (byte)c);
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

        UNSAFE.putInt(null, address + index, bits);

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);
            if (c > 127)
            {
                c = '?';
            }

            UNSAFE.putByte(null, address + STR_HEADER_LEN + index + i, (byte)c);
        }

        return STR_HEADER_LEN + length;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringWithoutLengthAscii(final int index, final int length)
    {
        boundsCheck0(index, length);

        final byte[] dst = new byte[length];
        UNSAFE.copyMemory(null, address + index, dst, ARRAY_BASE_OFFSET, length);

        return new String(dst, US_ASCII);
    }

    /**
     * {@inheritDoc}
     */
    public int getStringWithoutLengthAscii(final int index, final int length, final Appendable appendable)
    {
        boundsCheck0(index, length);

        try
        {
            for (int i = index, limit = index + length; i < limit; i++)
            {
                final char c = (char)UNSAFE.getByte(null, address + i);
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

            UNSAFE.putByte(null, address + index + i, (byte)c);
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

            UNSAFE.putByte(null, address + index + i, (byte)c);
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

            UNSAFE.putByte(null, address + index + i, (byte)c);
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

            UNSAFE.putByte(null, address + index + i, (byte)c);
        }

        return len;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public String getStringUtf8(final int index)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(null, address + index);

        return getStringUtf8(index, length);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringUtf8(final int index, final ByteOrder byteOrder)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        int bits = UNSAFE.getInt(null, address + index);
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
        boundsCheck0(index + STR_HEADER_LEN, length);

        final byte[] stringInBytes = new byte[length];
        UNSAFE.copyMemory(null, address + index + STR_HEADER_LEN, stringInBytes, ARRAY_BASE_OFFSET, length);

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

        UNSAFE.putInt(null, address + index, bytes.length);
        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, null, address + index + STR_HEADER_LEN, bytes.length);

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

        UNSAFE.putInt(null, address + index, bits);
        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, null, address + index + STR_HEADER_LEN, bytes.length);

        return STR_HEADER_LEN + bytes.length;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringWithoutLengthUtf8(final int index, final int length)
    {
        boundsCheck0(index, length);

        final byte[] stringInBytes = new byte[length];
        UNSAFE.copyMemory(null, address + index, stringInBytes, ARRAY_BASE_OFFSET, length);

        return new String(stringInBytes, UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    public int putStringWithoutLengthUtf8(final int index, final String value)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        ensureCapacity(index, bytes.length);

        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, null, address + index, bytes.length);

        return bytes.length;
    }

    ///////////////////////////////////////////////////////////////////////////

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

        final int end = index + length;
        int tally = 0;
        for (int i = index; i < end; i++)
        {
            tally = (tally * 10) + AsciiEncoding.getDigit(i, UNSAFE.getByte(null, address + i));
        }

        return tally;
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

        final int end = index + length;
        long tally = 0L;
        for (int i = index; i < end; i++)
        {
            tally = (tally * 10) + AsciiEncoding.getDigit(i, UNSAFE.getByte(null, address + i));
        }

        return tally;
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
            return AsciiEncoding.getDigit(index, UNSAFE.getByte(null, address + index));
        }

        final int endExclusive = index + length;
        final int first = getByte0(index);
        int i = index;
        if (first == MINUS_SIGN)
        {
            i++;
        }

        int tally = 0;
        for (; i < endExclusive; i++)
        {
            tally = (tally * 10) + AsciiEncoding.getDigit(i, UNSAFE.getByte(null, address + i));
        }

        if (first == MINUS_SIGN)
        {
            tally = -tally;
        }

        return tally;
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
            return AsciiEncoding.getDigit(index, UNSAFE.getByte(null, address + index));
        }

        final int endExclusive = index + length;
        final int first = getByte0(index);
        int i = index;
        if (first == MINUS_SIGN)
        {
            i++;
        }

        long tally = 0;
        for (; i < endExclusive; i++)
        {
            tally = (tally * 10) + AsciiEncoding.getDigit(i, UNSAFE.getByte(null, address + i));
        }

        if (first == MINUS_SIGN)
        {
            tally = -tally;
        }

        return tally;
    }

    /**
     * {@inheritDoc}
     */
    public void putInt(final int index, final int value)
    {
        ensureCapacity(index, SIZE_OF_INT);

        UNSAFE.putInt(null, address + index, value);
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

        long offset = address + index;
        int quotient = value;
        final int digitCount, length;
        if (value < 0)
        {
            quotient = -quotient;
            digitCount = digitCount(quotient);
            length = digitCount + 1;

            ensureCapacity(index, length);

            UNSAFE.putByte(null, offset, MINUS_SIGN);
            offset++;
        }
        else
        {
            length = digitCount = digitCount(quotient);

            ensureCapacity(index, length);
        }

        putPositiveIntAscii(offset, quotient, digitCount);

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

        putPositiveIntAscii(address + index, value, digitCount);

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
            putByte0(index, (byte)(ZERO + digit));
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
        if (value == 0L)
        {
            putByte0(index, ZERO);
            return 1;
        }

        final int digitCount = digitCount(value);

        ensureCapacity(index, digitCount);

        putPositiveLongAscii(address + index, value, digitCount);

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

        long offset = address + index;
        long quotient = value;
        final int digitCount, length;
        if (value < 0)
        {
            quotient = -quotient;
            digitCount = digitCount(quotient);
            length = digitCount + 1;

            ensureCapacity(index, length);

            UNSAFE.putByte(null, offset, MINUS_SIGN);
            offset++;
        }
        else
        {
            length = digitCount = digitCount(quotient);

            ensureCapacity(index, length);
        }

        putPositiveLongAscii(offset, quotient, digitCount);

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

        final ExpandableDirectByteBuffer that = (ExpandableDirectByteBuffer)obj;

        return compareTo(that) == 0;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        int hashCode = 1;

        final long address = this.address;
        for (int i = 0, length = capacity; i < length; i++)
        {
            hashCode = 31 * hashCode + UNSAFE.getByte(null, address + i);
        }

        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(final DirectBuffer that)
    {
        final int thisCapacity = this.capacity();
        final int thatCapacity = that.capacity();
        final byte[] thatByteArray = that.byteArray();
        final long thisOffset = this.addressOffset();
        final long thatOffset = that.addressOffset();

        for (int i = 0, length = Math.min(thisCapacity, thatCapacity); i < length; i++)
        {
            final int cmp = Byte.compare(
                UNSAFE.getByte(null, thisOffset + i),
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
        return "ExpandableDirectByteBuffer{" +
            "address=" + address +
            ", capacity=" + capacity +
            ", byteBuffer=" + byteBuffer +
            '}';
    }

    private void ensureCapacity(final int index, final int length)
    {
        if (index < 0 || length < 0)
        {
            throw new IndexOutOfBoundsException("negative value: index=" + index + " length=" + length);
        }

        final long resultingPosition = index + (long)length;
        final int currentCapacity = capacity;
        if (resultingPosition > currentCapacity)
        {
            if (resultingPosition > MAX_BUFFER_LENGTH)
            {
                throw new IndexOutOfBoundsException(
                    "index=" + index + " length=" + length + " maxCapacity=" + MAX_BUFFER_LENGTH);
            }

            final int newCapacity = calculateExpansion(currentCapacity, resultingPosition);
            final ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);

            getBytes(0, newBuffer, 0, capacity);

            address = address(newBuffer);
            capacity = newCapacity;
            byteBuffer = newBuffer;
        }
    }

    private int calculateExpansion(final int currentLength, final long requiredLength)
    {
        long value = Math.max(currentLength, INITIAL_CAPACITY);

        while (value < requiredLength)
        {
            value = value + (value >> 1);

            if (value > MAX_BUFFER_LENGTH)
            {
                value = MAX_BUFFER_LENGTH;
            }
        }

        return (int)value;
    }

    private void boundsCheck0(final int index, final int length)
    {
        final int currentCapacity = capacity;
        final long resultingPosition = index + (long)length;
        if (index < 0 || length < 0 || resultingPosition > currentCapacity)
        {
            throw new IndexOutOfBoundsException(
                "index=" + index + " length=" + length + " capacity=" + currentCapacity);
        }
    }

    private static void putPositiveIntAscii(
        final long offset, final int value, final int digitCount)
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

            UNSAFE.putByte(null, offset + i, ASCII_DIGITS[p1]);
            UNSAFE.putByte(null, offset + i + 1, ASCII_DIGITS[p1 + 1]);
            UNSAFE.putByte(null, offset + i + 2, ASCII_DIGITS[p2]);
            UNSAFE.putByte(null, offset + i + 3, ASCII_DIGITS[p2 + 1]);
        }

        if (quotient >= 100)
        {
            final int position = (quotient % 100) << 1;
            quotient /= 100;
            UNSAFE.putByte(null, offset + i - 1, ASCII_DIGITS[position + 1]);
            UNSAFE.putByte(null, offset + i - 2, ASCII_DIGITS[position]);
        }

        if (quotient >= 10)
        {
            final int position = quotient << 1;
            UNSAFE.putByte(null, offset + 1, ASCII_DIGITS[position + 1]);
            UNSAFE.putByte(null, offset, ASCII_DIGITS[position]);
        }
        else
        {
            UNSAFE.putByte(null, offset, (byte)(ZERO + quotient));
        }
    }

    private static void putPositiveLongAscii(final long offset, final long value, final int digitCount)
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

            UNSAFE.putByte(null, offset + i, ASCII_DIGITS[u1]);
            UNSAFE.putByte(null, offset + i + 1, ASCII_DIGITS[u1 + 1]);
            UNSAFE.putByte(null, offset + i + 2, ASCII_DIGITS[u2]);
            UNSAFE.putByte(null, offset + i + 3, ASCII_DIGITS[u2 + 1]);
            UNSAFE.putByte(null, offset + i + 4, ASCII_DIGITS[l1]);
            UNSAFE.putByte(null, offset + i + 5, ASCII_DIGITS[l1 + 1]);
            UNSAFE.putByte(null, offset + i + 6, ASCII_DIGITS[l2]);
            UNSAFE.putByte(null, offset + i + 7, ASCII_DIGITS[l2 + 1]);
        }

        putPositiveIntAscii(offset, (int)quotient, i);
    }
}
