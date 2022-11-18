/*
 * Copyright 2014-2022 Real Logic Limited.
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

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.agrona.BufferUtil.ARRAY_BASE_OFFSET;

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
public class ExpandableArrayBuffer extends AbstractMutableDirectBuffer
{
    /**
     * Maximum length to which the underlying buffer can grow. Some JVMs store state in the last few bytes.
     */
    public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    /**
     * Initial capacity of the buffer from which it will expand as necessary.
     */
    public static final int INITIAL_CAPACITY = 128;

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
        capacity = initialCapacity;
        addressOffset = ARRAY_BASE_OFFSET;
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
    public ByteBuffer byteBuffer()
    {
        return null;
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
    public int wrapAdjustment()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "ExpandableArrayBuffer{" +
            ", capacity=" + capacity +
            ", byteArray=" + byteArray + // lgtm [java/print-array]
            '}';
    }

    protected final void ensureCapacity(final int index, final int length)
    {
        if (index < 0 || length < 0)
        {
            throw new IndexOutOfBoundsException("negative value: index=" + index + " length=" + length);
        }

        final long resultingPosition = index + (long)length;
        if (resultingPosition > capacity)
        {
            if (resultingPosition > MAX_ARRAY_LENGTH)
            {
                throw new IndexOutOfBoundsException(
                    "index=" + index + " length=" + length + " maxCapacity=" + MAX_ARRAY_LENGTH);
            }

            final int newCapacity = calculateExpansion(capacity, resultingPosition);
            byteArray = Arrays.copyOf(byteArray, newCapacity);
            capacity = newCapacity;
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
}
