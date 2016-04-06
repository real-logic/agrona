/*
 * Copyright 2014-2015 Real Logic Ltd.
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
package org.agrona;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Common functions for usages across buffer implementations.
 */
public class BufferUtil
{
    public static final byte[] NULL_BYTES = "null".getBytes(StandardCharsets.UTF_8);
    public static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();
    public static final long ARRAY_BASE_OFFSET = UnsafeAccess.UNSAFE.arrayBaseOffset(byte[].class);

    private static final MethodHandle BUFFER_ADDRESS;

    static
    {
        try
        {
            final Field field = Buffer.class.getDeclaredField("address");
            field.setAccessible(true);
            BUFFER_ADDRESS = MethodHandles.lookup().unreflectGetter(field);
        }
        catch (final NoSuchFieldException | IllegalAccessException ex)
        {
            throw new IllegalStateException("Can not access java.nio.Buffer.address", ex);
        }
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
            throw new IndexOutOfBoundsException(String.format("index=%d, length=%d, capacity=%d", index, length, capacity));
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
            throw new IndexOutOfBoundsException(String.format("index=%d, length=%d, capacity=%d", index, length, capacity));
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
        long address = 0;
        try
        {
            address = (long)BUFFER_ADDRESS.invoke(buffer);
        }
        catch (final Throwable t)
        {
            LangUtil.rethrowUnchecked(t);
        }

        return address;
    }
}
