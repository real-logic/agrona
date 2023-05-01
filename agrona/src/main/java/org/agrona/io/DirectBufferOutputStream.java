/*
 * Copyright 2014-2023 Real Logic Limited.
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

import org.agrona.MutableDirectBuffer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link OutputStream} that wraps an underlying {@link MutableDirectBuffer}.
 */
public class DirectBufferOutputStream extends OutputStream
{
    private MutableDirectBuffer buffer;
    private int offset;
    private int length;
    private int position;

    /**
     * Default constructor.
     */
    public DirectBufferOutputStream()
    {
    }

    /**
     * Constructs output stream wrapping the given buffer.
     *
     * @param buffer to wrap.
     */
    @SuppressWarnings("this-escape")
    public DirectBufferOutputStream(final MutableDirectBuffer buffer)
    {
        wrap(buffer, 0, buffer.capacity());
    }

    /**
     * Constructs output stream wrapping the given buffer at an offset.
     *
     * @param buffer to wrap.
     * @param offset in the buffer.
     * @param length size in bytes to wrap.
     */
    @SuppressWarnings("this-escape")
    public DirectBufferOutputStream(final MutableDirectBuffer buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Wrap the buffer.
     *
     * @param buffer to wrap.
     */
    public void wrap(final MutableDirectBuffer buffer)
    {
        wrap(buffer, 0, buffer.capacity());
    }

    /**
     * Wrap the buffer at an offset.
     *
     * @param buffer to wrap.
     * @param offset in the buffer.
     * @param length size in bytes to wrap.
     */
    public void wrap(final MutableDirectBuffer buffer, final int offset, final int length)
    {
        if (null == buffer)
        {
            throw new NullPointerException("buffer cannot be null");
        }

        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
        this.position = 0;
    }

    /**
     * The position in the buffer from the offset up to which has been written.
     *
     * @return the position in the buffer from the offset up to which has been written.
     */
    public int position()
    {
        return position;
    }

    /**
     * The offset within the underlying buffer at which to start.
     *
     * @return offset within the underlying buffer at which to start.
     */
    public int offset()
    {
        return offset;
    }

    /**
     * The underlying buffer being wrapped.
     *
     * @return the underlying buffer being wrapped.
     */
    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    /**
     * The length of the underlying buffer to use
     *
     * @return length of the underlying buffer to use
     */
    public int length()
    {
        return length;
    }

    /**
     * Write a byte to buffer.
     *
     * @param b to be written.
     * @throws IllegalStateException if insufficient capacity remains in the buffer.
     */
    public void write(final int b)
    {
        if (position == length)
        {
            throw new IllegalStateException("position has reached the end of underlying buffer");
        }

        buffer.putByte(offset + position, (byte)b);
        ++position;
    }

    /**
     * Write a byte[] to the buffer.
     *
     * @param srcBytes  to write
     * @param srcOffset at which to begin reading bytes from the srcBytes.
     * @param length    of the srcBytes to read.
     * @throws IllegalStateException if insufficient capacity remains in the buffer.
     */
    public void write(final byte[] srcBytes, final int srcOffset, final int length)
    {
        final long resultingOffset = position + ((long)length);
        if (resultingOffset > this.length)
        {
            throw new IllegalStateException("insufficient capacity in the buffer");
        }

        buffer.putBytes(offset + position, srcBytes, srcOffset, length);
        position += length;
    }

    /**
     * Write a byte[] to the buffer.
     *
     * @param srcBytes to write
     * @throws IllegalStateException if insufficient capacity remains in the buffer.
     */
    public void write(final byte[] srcBytes)
    {
        write(srcBytes, 0, srcBytes.length);
    }

    /**
     * Override to remove {@link IOException}. This method does nothing.
     */
    public void flush()
    {
    }

    /**
     * Override to remove {@link IOException}. This method does nothing.
     */
    public void close()
    {
    }
}
