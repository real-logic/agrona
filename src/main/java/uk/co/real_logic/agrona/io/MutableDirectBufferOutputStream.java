/*
 *  Copyright 2014 - 2016 Real Logic Ltd.
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
package uk.co.real_logic.agrona.io;

import uk.co.real_logic.agrona.MutableDirectBuffer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link OutputStream} that wraps an underlying {@link MutableDirectBuffer}.
 */
public class MutableDirectBufferOutputStream extends OutputStream
{
    private MutableDirectBuffer buffer;
    private int offset;
    private int length;
    private int position;

    public MutableDirectBufferOutputStream()
    {
    }

    public MutableDirectBufferOutputStream(final MutableDirectBuffer buffer)
    {
        wrap(buffer, 0, buffer.capacity());
    }

    public MutableDirectBufferOutputStream(final MutableDirectBuffer buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    public void wrap(final MutableDirectBuffer buffer)
    {
        wrap(buffer, 0, buffer.capacity());
    }

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
    public void write(final int b) throws IOException
    {
        if (position == length)
        {
            throw new IllegalStateException("insufficient capacity in the buffer");
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
    public void write(final byte[] srcBytes, final int srcOffset, final int length) throws IOException
    {
        if (position + length >= this.length)
        {
            throw new IllegalStateException("insufficient capacity in the buffer");
        }

        buffer.putBytes(offset + position, srcBytes, srcOffset, length);
        position += length;
    }
}
