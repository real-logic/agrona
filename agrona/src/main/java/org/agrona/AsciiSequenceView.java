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

/**
 * View over a {@link DirectBuffer} which contains an ASCII string for a given range.
 */
public class AsciiSequenceView implements CharSequence
{
    private DirectBuffer buffer;
    private int offset;
    private int length;

    /**
     * Default constructor.
     */
    public AsciiSequenceView()
    {
    }

    /**
     * Construct a view over a {@link DirectBuffer} from an offset for a given length..
     *
     * @param buffer containing the ASCII sequence.
     * @param offset at which the ASCII sequence begins.
     * @param length of the ASCII sequence in bytes.
     */
    public AsciiSequenceView(final DirectBuffer buffer, final int offset, final int length)
    {
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
    }

    /**
     * {@inheritDoc}
     */
    public int length()
    {
        return length;
    }

    /**
     * Gets the underlying buffer which this is a view over.
     *
     * @return the underlying buffer which this is a view over.
     */
    public DirectBuffer buffer()
    {
        return buffer;
    }

    /**
     * Gets the offset into the underlying buffer.
     *
     * @return the offset into the underlying buffer.
     */
    public int offset()
    {
        return offset;
    }

    /**
     * {@inheritDoc}
     */
    public char charAt(final int index)
    {
        if (index < 0 || index >= length)
        {
            throw new StringIndexOutOfBoundsException("index=" + index + " length=" + length);
        }

        return (char)buffer.getByte(offset + index);
    }

    /**
     * {@inheritDoc}
     */
    public AsciiSequenceView subSequence(final int start, final int end)
    {
        if (start < 0)
        {
            throw new StringIndexOutOfBoundsException("start=" + start);
        }

        if (end > length)
        {
            throw new StringIndexOutOfBoundsException("end=" + end + " length=" + length);
        }

        if (end - start < 0)
        {
            throw new StringIndexOutOfBoundsException("start=" + start + " end=" + end);
        }

        return new AsciiSequenceView(buffer, offset + start, end - start);
    }

    /**
     * Wrap a range of an existing buffer containing an ASCII sequence.
     *
     * @param buffer containing the ASCII sequence.
     * @param offset at which the ASCII sequence begins.
     * @param length of the ASCII sequence in bytes.
     * @return this for a fluent API.
     */
    public AsciiSequenceView wrap(final DirectBuffer buffer, final int offset, final int length)
    {
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;

        return this;
    }

    /**
     * Reset the view to null.
     */
    public void reset()
    {
        this.buffer = null;
        this.offset = 0;
        this.length = 0;
    }

    /**
     * Copy the bytes from a view into a provided buffer.
     *
     * @param dstBuffer into which the bytes will be copied.
     * @param dstOffset offset in the destination buffer to begin the copy.
     * @return the number of bytes copied.
     */
    public int getBytes(final MutableDirectBuffer dstBuffer, final int dstOffset)
    {
        if (null == buffer || length <= 0)
        {
            return 0;
        }

        dstBuffer.putBytes(dstOffset, buffer, offset, length);

        return length;
    }

    public String toString()
    {
        if (null == buffer || length <= 0)
        {
            return "";
        }

        return buffer.getStringWithoutLengthAscii(offset, length);
    }
}
