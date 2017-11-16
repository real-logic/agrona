/*
 *  Copyright 2014-2017 Real Logic Ltd.
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

import org.agrona.DirectBuffer;

import java.io.InputStream;

/**
 * An {@link InputStream} that wraps a {@link DirectBuffer}.
 */
public class DirectBufferInputStream extends InputStream
{
    private DirectBuffer buffer;
    private int offset;
    private int length;
    private int position;

    public DirectBufferInputStream()
    {
    }

    public DirectBufferInputStream(final DirectBuffer buffer)
    {
        wrap(buffer, 0, buffer.capacity());
    }

    public DirectBufferInputStream(final DirectBuffer buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    public void wrap(final DirectBuffer buffer)
    {
        wrap(buffer, 0, buffer.capacity());
    }

    public void wrap(final DirectBuffer buffer, final int offset, final int length)
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
     * The offset within the underlying buffer at which to start.
     *
     * @return offset within the underlying buffer at which to start.
     */
    public int offset()
    {
        return offset;
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
     * The underlying buffer being wrapped.
     *
     * @return the underlying buffer being wrapped.
     */
    public DirectBuffer buffer()
    {
        return buffer;
    }

    public boolean markSupported()
    {
        return false;
    }

    public int available()
    {
        return length - position;
    }

    public long skip(final long n)
    {
        final int skipped = (int)Math.min(n, available());
        position += skipped;

        return skipped;
    }

    public int read()
    {
        int b = -1;
        if (position < length)
        {
            b = buffer.getByte(offset + position) & 0xFF;
            ++position;
        }

        return b;
    }

    public int read(final byte[] dstBytes, final int dstOffset, final int length)
    {
        int bytesRead = -1;

        if (position < this.length)
        {
            bytesRead = Math.min(length, available());
            buffer.getBytes(offset + position, dstBytes, dstOffset, bytesRead);
            position += bytesRead;
        }

        return bytesRead;
    }

    public void close()
    {
    }
}
