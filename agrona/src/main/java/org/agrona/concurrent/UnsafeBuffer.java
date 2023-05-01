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
package org.agrona.concurrent;

import org.agrona.AbstractMutableDirectBuffer;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.agrona.BitUtil.*;
import static org.agrona.BufferUtil.*;
import static org.agrona.UnsafeAccess.UNSAFE;
import static org.agrona.collections.ArrayUtil.EMPTY_BYTE_ARRAY;

/**
 * Supports regular, byte ordered, and atomic (memory ordered) access to an underlying buffer. The buffer can be a
 * byte[], one of the various {@link ByteBuffer} implementations, or an off Java heap memory address.
 * <p>
 * {@link ByteOrder} of a wrapped buffer is not applied to the {@link UnsafeBuffer}. {@link UnsafeBuffer}s are
 * effectively stateless and can be used concurrently, the wrapping methods are an exception. To control
 * {@link ByteOrder} use the appropriate method with the {@link ByteOrder} overload.
 * <p>
 * <b>Note:</b> This class has a natural ordering that is inconsistent with equals.
 * Types may be different but equal on buffer contents.
 * <p>
 * <b>Note:</b> The wrap methods on this class are not thread safe. Concurrent access should only happen after a
 * successful wrap.
 */
public class UnsafeBuffer extends AbstractMutableDirectBuffer implements AtomicBuffer
{
    /**
     * @see AtomicBuffer#ALIGNMENT
     */
    public static final int ALIGNMENT = AtomicBuffer.ALIGNMENT;

    /**
     * @see DirectBuffer#DISABLE_BOUNDS_CHECKS_PROP_NAME
     */
    public static final String DISABLE_BOUNDS_CHECKS_PROP_NAME = DirectBuffer.DISABLE_BOUNDS_CHECKS_PROP_NAME;

    /**
     * @see DirectBuffer#SHOULD_BOUNDS_CHECK
     */
    public static final boolean SHOULD_BOUNDS_CHECK = DirectBuffer.SHOULD_BOUNDS_CHECK;

    private ByteBuffer byteBuffer;
    private int wrapAdjustment;

    /**
     * Empty constructor for a reusable wrapper buffer.
     */
    @SuppressWarnings("this-escape")
    public UnsafeBuffer()
    {
        wrap(EMPTY_BYTE_ARRAY);
    }

    /**
     * Attach a view to a {@code byte[]} for providing direct access.
     *
     * @param buffer to which the view is attached.
     * @see #wrap(byte[])
     */
    @SuppressWarnings("this-escape")
    public UnsafeBuffer(final byte[] buffer)
    {
        wrap(buffer);
    }

    /**
     * Attach a view to a {@code byte[]} for providing direct access.
     *
     * @param buffer to which the view is attached.
     * @param offset in bytes within the buffer to begin.
     * @param length in bytes of the buffer included in the view.
     * @see #wrap(byte[], int, int)
     */
    @SuppressWarnings("this-escape")
    public UnsafeBuffer(final byte[] buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Attach a view to a {@link ByteBuffer} for providing direct access, the {@link ByteBuffer} can be
     * heap based or direct.
     *
     * @param buffer to which the view is attached.
     */
    @SuppressWarnings("this-escape")
    public UnsafeBuffer(final ByteBuffer buffer)
    {
        wrap(buffer);
    }

    /**
     * Attach a view to a {@link ByteBuffer} for providing direct access, the {@link ByteBuffer} can be
     * heap based or direct.
     *
     * @param buffer to which the view is attached.
     * @param offset in bytes within the buffer to begin.
     * @param length in bytes of the buffer included in the view.
     */
    @SuppressWarnings("this-escape")
    public UnsafeBuffer(final ByteBuffer buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Attach a view to an existing {@link DirectBuffer}
     *
     * @param buffer to which the view is attached.
     */
    @SuppressWarnings("this-escape")
    public UnsafeBuffer(final DirectBuffer buffer)
    {
        wrap(buffer);
    }

    /**
     * Attach a view to an existing {@link DirectBuffer}
     *
     * @param buffer to which the view is attached.
     * @param offset in bytes within the buffer to begin.
     * @param length in bytes of the buffer included in the view.
     */
    @SuppressWarnings("this-escape")
    public UnsafeBuffer(final DirectBuffer buffer, final int offset, final int length)
    {
        wrap(buffer, offset, length);
    }

    /**
     * Attach a view to an off-heap memory region by address. This is useful for interacting with native libraries.
     *
     * @param address where the memory begins off-heap
     * @param length  of the buffer from the given address
     */
    @SuppressWarnings("this-escape")
    public UnsafeBuffer(final long address, final int length)
    {
        wrap(address, length);
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final byte[] buffer)
    {
        capacity = buffer.length;
        addressOffset = ARRAY_BASE_OFFSET;
        byteBuffer = null;
        wrapAdjustment = 0;

        if (buffer != byteArray)
        {
            byteArray = buffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final byte[] buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheckWrap(offset, length, buffer.length);
        }

        capacity = length;
        addressOffset = ARRAY_BASE_OFFSET + offset;
        byteBuffer = null;
        wrapAdjustment = offset;

        if (buffer != byteArray)
        {
            byteArray = buffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final ByteBuffer buffer)
    {
        capacity = buffer.capacity();

        if (buffer != byteBuffer)
        {
            byteBuffer = buffer;
        }

        if (buffer.isDirect())
        {
            byteArray = null;
            addressOffset = address(buffer);
            wrapAdjustment = 0;
        }
        else
        {
            byteArray = BufferUtil.array(buffer);
            final int arrayOffset = arrayOffset(buffer);
            addressOffset = ARRAY_BASE_OFFSET + arrayOffset;
            wrapAdjustment = arrayOffset;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final ByteBuffer buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheckWrap(offset, length, buffer.capacity());
        }

        capacity = length;

        if (buffer != byteBuffer)
        {
            byteBuffer = buffer;
        }

        if (buffer.isDirect())
        {
            byteArray = null;
            addressOffset = address(buffer) + offset;
            wrapAdjustment = offset;
        }
        else
        {
            byteArray = BufferUtil.array(buffer);
            final int totalOffset = arrayOffset(buffer) + offset;
            addressOffset = ARRAY_BASE_OFFSET + totalOffset;
            wrapAdjustment = totalOffset;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final DirectBuffer buffer)
    {
        capacity = buffer.capacity();
        addressOffset = buffer.addressOffset();
        wrapAdjustment = buffer.wrapAdjustment();

        final byte[] array = buffer.byteArray();
        if (array != this.byteArray)
        {
            this.byteArray = array;
        }

        final ByteBuffer byteBuffer = buffer.byteBuffer();
        if (byteBuffer != this.byteBuffer)
        {
            this.byteBuffer = byteBuffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final DirectBuffer buffer, final int offset, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheckWrap(offset, length, buffer.capacity());
        }

        capacity = length;
        addressOffset = buffer.addressOffset() + offset;
        wrapAdjustment = buffer.wrapAdjustment() + offset;

        final byte[] array = buffer.byteArray();
        if (array != this.byteArray)
        {
            this.byteArray = array;
        }

        final ByteBuffer byteBuffer = buffer.byteBuffer();
        if (byteBuffer != this.byteBuffer)
        {
            this.byteBuffer = byteBuffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void wrap(final long address, final int length)
    {
        capacity = length;
        addressOffset = address;
        byteArray = null;
        byteBuffer = null;
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
    public int wrapAdjustment()
    {
        return wrapAdjustment;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpandable()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void verifyAlignment()
    {
        if (null != byteArray)
        {
            final String msg = "AtomicBuffer was created from a byte[] and is not correctly aligned by " + ALIGNMENT;
            if (STRICT_ALIGNMENT_CHECKS)
            {
                throw new IllegalStateException(msg);
            }
            else
            {
                System.err.println(msg);
            }
        }
        else if (0 != (addressOffset & (ALIGNMENT - 1)))
        {
            throw new IllegalStateException(
                "AtomicBuffer is not correctly aligned: addressOffset=" + addressOffset + " is not divisible by " +
                ALIGNMENT);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLongVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getLongVolatile(byteArray, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putLongVolatile(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putLongVolatile(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public void putLongOrdered(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        UNSAFE.putOrderedLong(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public long addLongOrdered(final int index, final long increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        final long offset = addressOffset + index;
        final byte[] array = this.byteArray;
        final long value = UNSAFE.getLong(array, offset);
        UNSAFE.putOrderedLong(array, offset, value + increment);

        return value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean compareAndSetLong(final int index, final long expectedValue, final long updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.compareAndSwapLong(byteArray, addressOffset + index, expectedValue, updateValue);
    }

    /**
     * {@inheritDoc}
     */
    public long getAndSetLong(final int index, final long value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndSetLong(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public long getAndAddLong(final int index, final long delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_LONG);
        }

        return UNSAFE.getAndAddLong(byteArray, addressOffset + index, delta);
    }

    /**
     * {@inheritDoc}
     */
    public int getIntVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getIntVolatile(byteArray, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putIntVolatile(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putIntVolatile(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public void putIntOrdered(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        UNSAFE.putOrderedInt(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public int addIntOrdered(final int index, final int increment)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        final long offset = addressOffset + index;
        final byte[] array = this.byteArray;
        final int value = UNSAFE.getInt(array, offset);
        UNSAFE.putOrderedInt(array, offset, value + increment);

        return value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean compareAndSetInt(final int index, final int expectedValue, final int updateValue)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.compareAndSwapInt(byteArray, addressOffset + index, expectedValue, updateValue);
    }

    /**
     * {@inheritDoc}
     */
    public int getAndSetInt(final int index, final int value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndSetInt(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public int getAndAddInt(final int index, final int delta)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_INT);
        }

        return UNSAFE.getAndAddInt(byteArray, addressOffset + index, delta);
    }

    /**
     * {@inheritDoc}
     */
    public short getShortVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        return UNSAFE.getShortVolatile(byteArray, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putShortVolatile(final int index, final short value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_SHORT);
        }

        UNSAFE.putShortVolatile(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public byte getByteVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_BYTE);
        }

        return UNSAFE.getByteVolatile(byteArray, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putByteVolatile(final int index, final byte value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_BYTE);
        }

        UNSAFE.putByteVolatile(byteArray, addressOffset + index, value);
    }

    /**
     * {@inheritDoc}
     */
    public char getCharVolatile(final int index)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        return UNSAFE.getCharVolatile(byteArray, addressOffset + index);
    }

    /**
     * {@inheritDoc}
     */
    public void putCharVolatile(final int index, final char value)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, SIZE_OF_CHAR);
        }

        UNSAFE.putCharVolatile(byteArray, addressOffset + index, value);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "UnsafeBuffer{" +
            "addressOffset=" + addressOffset +
            ", capacity=" + capacity +
            ", byteArray=" + (null == byteArray ? "null" : ("byte[" + byteArray.length + "]")) +
            ", byteBuffer=" + byteBuffer +
            '}';
    }

    protected final void ensureCapacity(final int index, final int length)
    {
        if (SHOULD_BOUNDS_CHECK)
        {
            boundsCheck0(index, length);
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
