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
package org.agrona.io;

import org.agrona.MutableDirectBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * {@link OutputStream} that wraps an underlying expandable version of a {@link MutableDirectBuffer}.
 */
public class ExpandableDirectBufferOutputStream extends OutputStream
{
    private MutableDirectBuffer buffer;
    private int offset;
    private int position;

    public ExpandableDirectBufferOutputStream()
    {
    }

    public ExpandableDirectBufferOutputStream(final MutableDirectBuffer buffer)
    {
        wrap(buffer, 0);
    }

    public ExpandableDirectBufferOutputStream(final MutableDirectBuffer buffer, final int offset)
    {
        wrap(buffer, offset);
    }

    /**
     * Wrap a given buffer beginning with an offset of 0.
     *
     * @param buffer to wrap
     */
    public void wrap(final MutableDirectBuffer buffer)
    {
        wrap(buffer, 0);
    }

    /**
     * Wrap a given buffer beginning at an offset.
     *
     * @param buffer to wrap
     * @param offset at which the puts will occur.
     */
    public void wrap(final MutableDirectBuffer buffer, final int offset)
    {
        Objects.requireNonNull(buffer, "Buffer must not be null");
        if (!buffer.isExpandable())
        {
            throw new IllegalStateException("buffer must be expandable.");
        }

        this.buffer = buffer;
        this.offset = offset;
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
     * Write a byte to buffer.
     *
     * @param b to be written.
     */
    public void write(final int b) throws IOException
    {
        buffer.putByte(offset + position, (byte)b);
        ++position;
    }

    /**
     * Write a byte[] to the buffer.
     *
     * @param srcBytes  to write
     * @param srcOffset at which to begin reading bytes from the srcBytes.
     * @param length    of the srcBytes to read.
     */
    public void write(final byte[] srcBytes, final int srcOffset, final int length) throws IOException
    {
        buffer.putBytes(offset + position, srcBytes, srcOffset, length);
        position += length;
    }
}
