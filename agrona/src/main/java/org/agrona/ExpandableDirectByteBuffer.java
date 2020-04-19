/*
 * Copyright 2014-2020 Real Logic Limited.
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
import static org.agrona.BitUtil.*;
import static org.agrona.BufferUtil.*;
import static org.agrona.AsciiEncoding.*;
import static org.agrona.UnsafeAccess.UNSAFE;

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
public final class ExpandableDirectByteBuffer implements MutableDirectBuffer
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

    public void wrap(final byte[] buffer)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final byte[] buffer, final int offset, final int length)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final ByteBuffer buffer)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final ByteBuffer buffer, final int offset, final int length)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final DirectBuffer buffer)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final DirectBuffer buffer, final int offset, final int length)
    {
        throw new UnsupportedOperationException();
    }

    public void wrap(final long address, final int length)
    {
        throw new UnsupportedOperationException();
    }

    public long addressOffset()
    {
        return address;
    }

    public byte[] byteArray()
    {
        return null;
    }

    public ByteBuffer byteBuffer()
    {
        return byteBuffer;
    }

    public void setMemory(final int index, final int length, final byte value)
    {
        lengthCheck(length);
        ensureCapacity(index, length);

        final long indexOffset = address + index;
        if (0 == (indexOffset & 1) && length > 64)
        {
            // This horrible filth is to encourage the JVM to call memset() when address is even.
            // TODO: check if this still applies when Java 9 is out!!!
            UNSAFE.putByte(null, indexOffset, value);
            UNSAFE.setMemory(null, indexOffset + 1, length - 1, value);
        }
        else
        {
            UNSAFE.setMemory(null, indexOffset, length, value);
        }
    }

    public int capacity()
    {
        return capacity;
    }

    public boolean isExpandable()
    {
        return true;
    }

    public void checkLimit(final int limit)
    {
        if (limit < 0)
        {
            throw new IndexOutOfBoundsException("limit cannot be negative: limit=" + limit);
        }

        ensureCapacity(limit, SIZE_OF_BYTE);
    }

    ///////////////////////////////////////////////////////////////////////////

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

    public long getLong(final int index)
    {
        boundsCheck0(index, SIZE_OF_LONG);

        return UNSAFE.getLong(null, address + index);
    }

    public void putLong(final int index, final long value)
    {
        ensureCapacity(index, SIZE_OF_LONG);

        UNSAFE.putLong(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

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

    public int getInt(final int index)
    {
        boundsCheck0(index, SIZE_OF_INT);

        return UNSAFE.getInt(null, address + index);
    }

    ///////////////////////////////////////////////////////////////////////////

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

    public double getDouble(final int index)
    {
        boundsCheck0(index, SIZE_OF_DOUBLE);

        return UNSAFE.getDouble(null, address + index);
    }

    public void putDouble(final int index, final double value)
    {
        ensureCapacity(index, SIZE_OF_DOUBLE);

        UNSAFE.putDouble(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

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

    public float getFloat(final int index)
    {
        boundsCheck0(index, SIZE_OF_FLOAT);

        return UNSAFE.getFloat(null, address + index);
    }

    public void putFloat(final int index, final float value)
    {
        ensureCapacity(index, SIZE_OF_FLOAT);

        UNSAFE.putFloat(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

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

    public short getShort(final int index)
    {
        boundsCheck0(index, SIZE_OF_SHORT);

        return UNSAFE.getShort(null, address + index);
    }

    public void putShort(final int index, final short value)
    {
        ensureCapacity(index, SIZE_OF_SHORT);

        UNSAFE.putShort(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

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

    public void getBytes(final int index, final byte[] dst)
    {
        getBytes(index, dst, 0, dst.length);
    }

    public void getBytes(final int index, final byte[] dst, final int offset, final int length)
    {
        lengthCheck(length);
        boundsCheck0(index, length);
        BufferUtil.boundsCheck(dst, offset, length);

        UNSAFE.copyMemory(null, address + index, dst, ARRAY_BASE_OFFSET + offset, length);
    }

    public void getBytes(final int index, final MutableDirectBuffer dstBuffer, final int dstIndex, final int length)
    {
        dstBuffer.putBytes(dstIndex, this, index, length);
    }

    public void getBytes(final int index, final ByteBuffer dstBuffer, final int length)
    {
        final int dstOffset = dstBuffer.position();
        getBytes(index, dstBuffer, dstOffset, length);
        dstBuffer.position(dstOffset + length);
    }

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

    public void putBytes(final int index, final byte[] src)
    {
        putBytes(index, src, 0, src.length);
    }

    public void putBytes(final int index, final byte[] src, final int offset, final int length)
    {
        ensureCapacity(index, length);

        lengthCheck(length);
        BufferUtil.boundsCheck(src, offset, length);

        UNSAFE.copyMemory(src, ARRAY_BASE_OFFSET + offset, null, address + index, length);
    }

    public void putBytes(final int index, final ByteBuffer srcBuffer, final int length)
    {
        final int srcIndex = srcBuffer.position();
        putBytes(index, srcBuffer, srcIndex, length);
        srcBuffer.position(srcIndex + length);
    }

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

    public char getChar(final int index)
    {
        boundsCheck0(index, SIZE_OF_CHAR);

        return UNSAFE.getChar(null, address + index);
    }

    public void putChar(final int index, final char value)
    {
        ensureCapacity(index, SIZE_OF_CHAR);

        UNSAFE.putChar(null, address + index, value);
    }

    ///////////////////////////////////////////////////////////////////////////

    public String getStringAscii(final int index)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(null, address + index);

        return getStringAscii(index, length);
    }

    public int getStringAscii(final int index, final Appendable appendable)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(null, address + index);

        return getStringAscii(index, length, appendable);
    }

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

    public String getStringAscii(final int index, final int length)
    {
        boundsCheck0(index + STR_HEADER_LEN, length);

        final byte[] dst = new byte[length];
        UNSAFE.copyMemory(null, address + index + STR_HEADER_LEN, dst, ARRAY_BASE_OFFSET, length);

        return new String(dst, US_ASCII);
    }

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

    public String getStringWithoutLengthAscii(final int index, final int length)
    {
        boundsCheck0(index, length);

        final byte[] dst = new byte[length];
        UNSAFE.copyMemory(null, address + index, dst, ARRAY_BASE_OFFSET, length);

        return new String(dst, US_ASCII);
    }

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

    ///////////////////////////////////////////////////////////////////////////

    public String getStringUtf8(final int index)
    {
        boundsCheck0(index, STR_HEADER_LEN);

        final int length = UNSAFE.getInt(null, address + index);

        return getStringUtf8(index, length);
    }

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

    public String getStringUtf8(final int index, final int length)
    {
        boundsCheck0(index + STR_HEADER_LEN, length);

        final byte[] stringInBytes = new byte[length];
        UNSAFE.copyMemory(null, address + index + STR_HEADER_LEN, stringInBytes, ARRAY_BASE_OFFSET, length);

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

    public String getStringWithoutLengthUtf8(final int index, final int length)
    {
        boundsCheck0(index, length);

        final byte[] stringInBytes = new byte[length];
        UNSAFE.copyMemory(null, address + index, stringInBytes, ARRAY_BASE_OFFSET, length);

        return new String(stringInBytes, UTF_8);
    }

    public int putStringWithoutLengthUtf8(final int index, final String value)
    {
        final byte[] bytes = value != null ? value.getBytes(UTF_8) : NULL_BYTES;
        ensureCapacity(index, bytes.length);

        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, null, address + index, bytes.length);

        return bytes.length;
    }

    ///////////////////////////////////////////////////////////////////////////

    public int parseNaturalIntAscii(final int index, final int length)
    {
        boundsCheck0(index, length);

        final int end = index + length;
        int tally = 0;
        for (int i = index; i < end; i++)
        {
            tally = (tally * 10) + AsciiEncoding.getDigit(i, getByte0(i));
        }

        return tally;
    }

    public long parseNaturalLongAscii(final int index, final int length)
    {
        boundsCheck0(index, length);

        final int end = index + length;
        long tally = 0L;
        for (int i = index; i < end; i++)
        {
            tally = (tally * 10) + AsciiEncoding.getDigit(i, getByte0(i));
        }

        return tally;
    }

    public int parseIntAscii(final int index, final int length)
    {
        boundsCheck0(index, length);

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
            tally = (tally * 10) + AsciiEncoding.getDigit(i, getByte0(i));
        }

        if (first == MINUS_SIGN)
        {
            tally = -tally;
        }

        return tally;
    }

    public long parseLongAscii(final int index, final int length)
    {
        boundsCheck0(index, length);

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
            tally = (tally * 10) + AsciiEncoding.getDigit(i, getByte0(i));
        }

        if (first == MINUS_SIGN)
        {
            tally = -tally;
        }

        return tally;
    }

    public void putInt(final int index, final int value)
    {
        ensureCapacity(index, SIZE_OF_INT);

        UNSAFE.putInt(null, address + index, value);
    }

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

        int start = index;
        int quotient = value;
        int length = 1;
        if (value < 0)
        {
            putByte0(index, MINUS_SIGN);
            start++;
            length++;
            quotient = -quotient;
        }

        int i = endOffset(quotient);
        length += i;

        ensureCapacity(index, length);

        while (i >= 0)
        {
            final int remainder = quotient % 10;
            quotient = quotient / 10;
            byteBuffer.put(i + start, (byte)(ZERO + remainder));
            i--;
        }

        return length;
    }

    public int putNaturalIntAscii(final int index, final int value)
    {
        if (value == 0)
        {
            putByte0(index, ZERO);
            return 1;
        }

        int i = endOffset(value);
        final int length = i + 1;

        ensureCapacity(index, length);

        int quotient = value;
        while (i >= 0)
        {
            final int remainder = quotient % 10;
            quotient = quotient / 10;
            byteBuffer.put(i + index, (byte)(ZERO + remainder));

            i--;
        }

        return length;
    }

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
            throw new NumberFormatException(String.format("Cannot write %d in %d bytes", value, length));
        }
    }

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

    public int putNaturalLongAscii(final int index, final long value)
    {
        if (value == 0L)
        {
            putByte0(index, ZERO);
            return 1;
        }

        int i = endOffset(value);
        final int length = i + 1;

        ensureCapacity(index, length);

        long quotient = value;
        while (i >= 0)
        {
            final long remainder = quotient % 10;
            quotient = quotient / 10;
            byteBuffer.put(i + index, (byte)(ZERO + remainder));

            i--;
        }

        return length;
    }

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

        int start = index;
        long quotient = value;
        int length = 1;
        if (value < 0)
        {
            putByte0(index, MINUS_SIGN);
            start++;
            length++;
            quotient = -quotient;
        }

        int i = endOffset(quotient);
        length += i;

        ensureCapacity(index, length);

        while (i >= 0)
        {
            final long remainder = quotient % 10L;
            quotient = quotient / 10L;
            byteBuffer.put(i + start, (byte)(ZERO + remainder));
            i--;
        }

        return length;
    }

    ///////////////////////////////////////////////////////////////////////////

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
            if (currentCapacity >= MAX_BUFFER_LENGTH)
            {
                throw new IndexOutOfBoundsException(
                    "index=" + index + " length=" + length + " maxCapacity=" + MAX_BUFFER_LENGTH);
            }

            final int newCapacity = calculateExpansion(currentCapacity, (int)resultingPosition);
            final ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);

            getBytes(0, newBuffer, 0, capacity);

            address = address(newBuffer);
            capacity = newCapacity;
            byteBuffer = newBuffer;
        }
    }

    private int calculateExpansion(final int currentLength, final int requiredLength)
    {
        long value = currentLength;

        while (value < requiredLength)
        {
            value = value + (value >> 1);

            if (value > Integer.MAX_VALUE)
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

    private void lengthCheck(final int length)
    {
        if (length < 0)
        {
            throw new IllegalArgumentException("negative length: " + length);
        }
    }

    public void boundsCheck(final int index, final int length)
    {
        boundsCheck0(index, length);
    }

    public int wrapAdjustment()
    {
        return 0;
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

        final ExpandableDirectByteBuffer that = (ExpandableDirectByteBuffer)obj;

        return compareTo(that) == 0;
    }

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

    public String toString()
    {
        return "ExpandableDirectByteBuffer{" +
            "address=" + address +
            ", capacity=" + capacity +
            ", byteBuffer=" + byteBuffer +
            '}';
    }
}
