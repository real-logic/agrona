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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static java.lang.invoke.MethodType.methodType;
import static org.agrona.BitUtil.isPowerOfTwo;
import static org.agrona.UnsafeAccess.UNSAFE;

/**
 * Common functions for buffer implementations.
 */
public final class BufferUtil
{
    private static final MethodHandle INVOKE_CLEANER;
    private static final MethodHandle GET_CLEANER;
    private static final MethodHandle CLEAN;
    public static final byte[] NULL_BYTES = "null".getBytes(StandardCharsets.UTF_8);
    public static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();
    public static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    public static final long BYTE_BUFFER_HB_FIELD_OFFSET;
    public static final long BYTE_BUFFER_OFFSET_FIELD_OFFSET;
    public static final long BYTE_BUFFER_ADDRESS_FIELD_OFFSET;

    static
    {
        try
        {
            BYTE_BUFFER_HB_FIELD_OFFSET = UNSAFE.objectFieldOffset(
                ByteBuffer.class.getDeclaredField("hb"));

            BYTE_BUFFER_OFFSET_FIELD_OFFSET = UNSAFE.objectFieldOffset(
                ByteBuffer.class.getDeclaredField("offset"));

            BYTE_BUFFER_ADDRESS_FIELD_OFFSET = UNSAFE.objectFieldOffset(Buffer.class.getDeclaredField("address"));

            MethodHandle invokeCleaner = null;
            MethodHandle getCleaner = null;
            MethodHandle clean = null;
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            try
            {
                invokeCleaner = lookup.findVirtual(
                    UNSAFE.getClass(), "invokeCleaner", methodType(void.class, ByteBuffer.class));
            }
            catch (final NoSuchMethodException ex)
            {
                // JDK 8 fallback
                final Class<?> directBuffer = Class.forName("sun.nio.ch.DirectBuffer");
                final Class<?> cleaner = Class.forName("sun.misc.Cleaner");
                getCleaner =
                    lookup.findVirtual(directBuffer, "cleaner", methodType(cleaner));
                clean = lookup.findVirtual(cleaner, "clean", methodType(void.class));

            }
            INVOKE_CLEANER = invokeCleaner;
            GET_CLEANER = getCleaner;
            CLEAN = clean;
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private BufferUtil()
    {
    }

    /**
     * Bounds check the access range and throw a {@link IndexOutOfBoundsException} if exceeded.
     *
     * @param buffer to be checked.
     * @param index  at which the access will begin.
     * @param length of the range accessed.
     */
    public static void boundsCheck(final byte[] buffer, final long index, final int length)
    {
        final int capacity = buffer.length;
        final long resultingPosition = index + (long)length;
        if (index < 0 || resultingPosition > capacity)
        {
            throw new IndexOutOfBoundsException("index=" + index + " length=" + length + " capacity=" + capacity);
        }
    }

    /**
     * Bounds check the access range and throw a {@link IndexOutOfBoundsException} if exceeded.
     *
     * @param buffer to be checked.
     * @param index  at which the access will begin.
     * @param length of the range accessed.
     */
    public static void boundsCheck(final ByteBuffer buffer, final long index, final int length)
    {
        final int capacity = buffer.capacity();
        final long resultingPosition = index + (long)length;
        if (index < 0 || resultingPosition > capacity)
        {
            throw new IndexOutOfBoundsException("index=" + index + " length=" + length + " capacity=" + capacity);
        }
    }

    /**
     * Get the address at which the underlying buffer storage begins.
     *
     * @param buffer that wraps the underlying storage.
     * @return the memory address at which the buffer storage begins.
     */
    public static long address(final ByteBuffer buffer)
    {
        if (!buffer.isDirect())
        {
            throw new IllegalArgumentException("buffer.isDirect() must be true");
        }

        return UNSAFE.getLong(buffer, BYTE_BUFFER_ADDRESS_FIELD_OFFSET);
    }

    /**
     * Get the array from a read-only {@link ByteBuffer} similar to {@link ByteBuffer#array()}.
     *
     * @param buffer that wraps the underlying array.
     * @return the underlying array.
     */
    public static byte[] array(final ByteBuffer buffer)
    {
        if (buffer.isDirect())
        {
            throw new IllegalArgumentException("buffer must wrap an array");
        }

        return (byte[])UNSAFE.getObject(buffer, BYTE_BUFFER_HB_FIELD_OFFSET);
    }

    /**
     * Get the array offset from a read-only {@link ByteBuffer} similar to {@link ByteBuffer#arrayOffset()}.
     *
     * @param buffer that wraps the underlying array.
     * @return the underlying array offset at which this ByteBuffer starts.
     */
    public static int arrayOffset(final ByteBuffer buffer)
    {
        return UNSAFE.getInt(buffer, BYTE_BUFFER_OFFSET_FIELD_OFFSET);
    }

    /**
     * Allocate a new direct {@link ByteBuffer} that is aligned on a given alignment boundary.
     *
     * @param capacity  required for the buffer.
     * @param alignment boundary at which the buffer should begin.
     * @return a new {@link ByteBuffer} with the required alignment.
     * @throws IllegalArgumentException if the alignment is not a power of 2.
     */
    public static ByteBuffer allocateDirectAligned(final int capacity, final int alignment)
    {
        if (!isPowerOfTwo(alignment))
        {
            throw new IllegalArgumentException("Must be a power of 2: alignment=" + alignment);
        }

        final ByteBuffer buffer = ByteBuffer.allocateDirect(capacity + alignment);

        final long address = address(buffer);
        final int remainder = (int)(address & (alignment - 1));
        final int offset = alignment - remainder;

        buffer.limit(capacity + offset);
        buffer.position(offset);

        return buffer.slice();
    }

    /**
     * Free the underlying direct {@link ByteBuffer} by invoking {@code Cleaner} on it. No op if {@code null} or if the
     * underlying {@link ByteBuffer} non-direct.
     *
     * @param buffer to be freed
     * @see ByteBuffer#isDirect()
     */
    public static void free(final DirectBuffer buffer)
    {
        if (null == buffer)
        {
            return;
        }
        free(buffer.byteBuffer());
    }

    /**
     * Free direct {@link ByteBuffer} by invoking {@code Cleaner} on it. No op if {@code null} or non-direct
     * {@link ByteBuffer}.
     *
     * @param buffer to be freed
     * @see ByteBuffer#isDirect()
     */
    public static void free(final ByteBuffer buffer)
    {
        if (null == buffer || !buffer.isDirect())
        {
            return;
        }
        try
        {
            if (null != INVOKE_CLEANER) // JDK 9+
            {
                INVOKE_CLEANER.invokeExact(UNSAFE, buffer);
            }
            else // JDK 8
            {
                final Object cleaner = GET_CLEANER.invoke(buffer);
                if (null != cleaner)
                {
                    CLEAN.invoke(cleaner);
                }
            }
        }
        catch (final Throwable throwable)
        {
            LangUtil.rethrowUnchecked(throwable);
        }
    }
}
