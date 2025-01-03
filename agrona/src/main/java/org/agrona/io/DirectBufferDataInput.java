/*
 * Copyright 2014-2025 Real Logic Limited.
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
package org.agrona.io;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * A data input implementation that reads from a DirectBuffer. It adheres to the contract defined in {@link DataInput}
 * description including throwing checked exception on end of file. It adds few more methods to read strings without
 * allocations.
 * <p>
 * Note about byte ordering: by default, this class conforms to {@link DataInput} contract and uses
 * {@link ByteOrder#BIG_ENDIAN} byte order which allows it to read data produced by JDK {@link java.io.DataOutput}
 * implementations. Agrona buffers use {@link ByteOrder#LITTLE_ENDIAN} (unless overridden). Use
 * {@link #byteOrder(ByteOrder)} method to switch between JDK and Agrona compatibility.
 */
public class DirectBufferDataInput implements DataInput
{
    private DirectBuffer buffer;

    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    private int length;
    private int position;

    /**
     * Wrap given {@link DirectBuffer}.
     *
     * @param buffer to wrap.
     */
    @SuppressWarnings("this-escape")
    public DirectBufferDataInput(final DirectBuffer buffer)
    {
        wrap(buffer, 0, buffer.capacity());
    }

    /**
     * Wrap given {@link DirectBuffer}.
     *
     * @param buffer to wrap.
     * @param offset into the buffer.
     * @param length in bytes.
     */
    @SuppressWarnings("this-escape")
    public DirectBufferDataInput(final DirectBuffer buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Wrap given {@link DirectBuffer}.
     *
     * @param buffer to wrap.
     */
    public void wrap(final DirectBuffer buffer)
    {
        wrap(buffer, 0, buffer.capacity());
    }

    /**
     * Wrap given {@link DirectBuffer}.
     *
     * @param buffer to wrap.
     * @param offset into the buffer.
     * @param length in bytes.
     */
    public void wrap(final DirectBuffer buffer, final int offset, final int length)
    {
        if (null == buffer)
        {
            throw new NullPointerException("buffer cannot be null");
        }

        boundsCheckWrap(offset, length, buffer.capacity());

        this.buffer = buffer;
        this.length = length + offset;
        this.position = offset;
    }

    /**
     * Sets the byte order. By default, this class conforms to {@link DataInput} contract and uses
     * {@link ByteOrder#BIG_ENDIAN} which allows it to read data produced by JDK {@link java.io.DataOutput}
     * implementations. Agrona buffers use {@link ByteOrder#LITTLE_ENDIAN} (unless overridden).
     * Use this method to switch compatibility between these two worlds.
     *
     * @param byteOrder of the underlying buffer.
     */
    public void byteOrder(final ByteOrder byteOrder)
    {
        if (null == byteOrder)
        {
            throw new IllegalArgumentException("byteOrder cannot be null");
        }

        this.byteOrder = byteOrder;
    }

    /**
     * Return the number of bytes remaining in the buffer.
     *
     * @return the number of bytes remaining.
     */
    public int remaining()
    {
        return length - position;
    }

    /**
     * {@inheritDoc}
     */
    public void readFully(final byte[] destination) throws EOFException
    {
        if (destination == null)
        {
            throw new NullPointerException("Destination must not be null");
        }

        readFully(destination, 0, destination.length);
    }

    /**
     * {@inheritDoc}
     */
    public void readFully(final byte[] destination, final int destinationOffset, final int length) throws EOFException
    {
        if (destination == null)
        {
            throw new NullPointerException("Destination must not be null");
        }

        if (destinationOffset < 0)
        {
            throw new IndexOutOfBoundsException("invalid destinationOffset: " + destinationOffset);
        }

        if (length < 0)
        {
            throw new IndexOutOfBoundsException("invalid length: " + length);
        }

        if (destinationOffset + length > destination.length)
        {
            throw new IndexOutOfBoundsException(
                "destinationOffset=" + destinationOffset + " length=" + length + " not valid for length=" + length);
        }

        boundsCheck0(length);
        buffer.getBytes(position, destination, destinationOffset, length);
        position += length;
    }

    /**
     * {@inheritDoc}
     */
    public int skipBytes(final int n)
    {
        final int toSkip = Math.min(n, remaining());
        position += toSkip;

        return toSkip;
    }

    /**
     * {@inheritDoc}
     */
    public boolean readBoolean() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_BYTE);

        return buffer.getByte(position++) != 0;
    }

    /**
     * {@inheritDoc}
     */
    public byte readByte() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_BYTE);

        return buffer.getByte(position++);
    }

    /**
     * {@inheritDoc}
     */
    public int readUnsignedByte() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_BYTE);

        return buffer.getByte(position++) & 0xFF;
    }

    /**
     * {@inheritDoc}
     */
    public short readShort() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_SHORT);

        final short result = buffer.getShort(position, byteOrder);
        position += BitUtil.SIZE_OF_SHORT;

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int readUnsignedShort() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_SHORT);

        final int result = buffer.getShort(position, byteOrder) & 0xFFFF;
        position += BitUtil.SIZE_OF_SHORT;

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public char readChar() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_CHAR);

        final char result = buffer.getChar(position, byteOrder);
        position += BitUtil.SIZE_OF_CHAR;

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int readInt() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_INT);

        final int result = buffer.getInt(position, byteOrder);
        position += BitUtil.SIZE_OF_INT;

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long readLong() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_LONG);

        final long result = buffer.getLong(position, byteOrder);
        position += BitUtil.SIZE_OF_LONG;

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public float readFloat() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_FLOAT);

        final float result = buffer.getFloat(position, byteOrder);
        position += BitUtil.SIZE_OF_FLOAT;

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public double readDouble() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_DOUBLE);

        final double result = buffer.getDouble(position, byteOrder);
        position += BitUtil.SIZE_OF_DOUBLE;

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String readLine() throws IOException
    {
        if (remaining() == 0)
        {
            return null;
        }

        final StringBuilder result = new StringBuilder();
        readLine(result);

        return result.toString();
    }

    /**
     * Reads the next line of text from the input stream. It reads successive bytes, converting each byte separately
     * into a character, until it encounters a line terminator or end of file; Adheres to the contract of
     * {@link DataInput#readLine()}.
     *
     * @param appendable to append the chars to.
     * @return number of bytes the stream advanced while reading the line (including line terminators).
     * @throws IOException propagated from {@link Appendable#append(char)}.
     */
    public int readLine(final Appendable appendable) throws IOException
    {
        final int startingPosition = position;

        while (remaining() > 0)
        {
            final int nextByte = readByte();
            if (nextByte == '\n')
            {
                return position - startingPosition;
            }

            if (nextByte == '\r')
            {
                if (remaining() > 0)
                {
                    final byte peek = readByte();
                    if (peek != '\n')
                    {
                        skipBytes(-1);
                    }
                }

                return position - startingPosition;
            }

            appendable.append((char)nextByte);
        }

        return position - startingPosition;
    }

    /**
     * {@inheritDoc}
     */
    public String readUTF() throws EOFException
    {
        boundsCheck0(BitUtil.SIZE_OF_SHORT);
        final short size = readShort();

        boundsCheck0(size);
        final String stringUtf8 = buffer.getStringWithoutLengthUtf8(position, size);

        position += size;
        return stringUtf8;
    }

    /**
     * Reads in a string that has been encoded using UTF-8 format by
     * {@link org.agrona.MutableDirectBuffer#putStringUtf8(int, String)}.
     * <p>
     * This is a thin wrapper over {@link DirectBuffer#getStringUtf8(int, ByteOrder)}. Honours byte order set by
     * {@link #byteOrder(ByteOrder)}.
     *
     * @return the String as represented by the ASCII encoded bytes.
     */
    public String readStringUTF8()
    {
        final String stringUtf8 = buffer.getStringUtf8(position, byteOrder);

        position += stringUtf8.length();
        return stringUtf8;
    }

    /**
     * Reads in a string that has been encoded using ASCII format by
     * {@link org.agrona.MutableDirectBuffer#putStringAscii(int, CharSequence)}.
     * <p>
     * This is a thin wrapper over {@link DirectBuffer#getStringAscii(int, ByteOrder)}. Honours byte order set by
     * {@link #byteOrder(ByteOrder)}
     *
     * @return the String as represented by the ASCII encoded bytes.
     */
    public String readStringAscii()
    {
        final String stringAscii = buffer.getStringAscii(position, byteOrder);

        position += stringAscii.length();
        return stringAscii;
    }

    /**
     * Get a String from bytes encoded in ASCII format that is length prefixed and append to an {@link Appendable}.
     * This is a thin wrapper over {@link DirectBuffer#getStringAscii(int, Appendable, ByteOrder)}. Honours byte order
     * set by {@link #byteOrder(ByteOrder)}.
     *
     * @param appendable to append the chars to.
     * @return the number of bytes copied.
     */
    public int readStringAscii(final Appendable appendable)
    {
        final int bytesRead = buffer.getStringAscii(position, appendable, byteOrder);
        position += bytesRead;

        return bytesRead;
    }

    private void boundsCheck0(final int requestedReadBytes) throws EOFException
    {
        final long resultingPosition = position + requestedReadBytes;
        if (resultingPosition > length)
        {
            throw new EOFException(
                "position=" + position + " requestedReadBytes=" + requestedReadBytes + " capacity=" + length);
        }
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
}
