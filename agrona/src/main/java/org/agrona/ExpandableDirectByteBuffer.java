/*
 * Copyright 2014-2024 Real Logic Limited.
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
import java.nio.ByteOrder;

import static org.agrona.BufferUtil.address;

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
public class ExpandableDirectByteBuffer extends AbstractMutableDirectBuffer
{
    /**
     * Maximum length to which the underlying buffer can grow.
     */
    public static final int MAX_BUFFER_LENGTH = Integer.MAX_VALUE - 8;

    /**
     * Initial capacity of the buffer from which it will expand.
     */
    public static final int INITIAL_CAPACITY = 128;

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
        addressOffset = address(byteBuffer);
        capacity = initialCapacity;
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
    public void checkLimit(final int limit)
    {
        ensureCapacity(limit, 0);
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "ExpandableDirectByteBuffer{" +
            "address=" + addressOffset +
            ", capacity=" + capacity +
            ", byteBuffer=" + byteBuffer +
            '}';
    }

    protected final void ensureCapacity(final int index, final int length)
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
            final long newAddress = address(newBuffer);

            getBytes(0, newBuffer, 0, currentCapacity);

            byteBuffer = newBuffer;
            addressOffset = newAddress;
            capacity = newCapacity;
        }
    }

    private static int calculateExpansion(final int currentLength, final long requiredLength)
    {
        long value = Math.max(currentLength, 2);

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
}
