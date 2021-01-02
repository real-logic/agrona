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

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * Obtain access the the {@link Unsafe} class for direct memory operations.
 */
public final class UnsafeAccess
{
    /**
     * Reference to the {@link Unsafe} instance.
     */
    public static final Unsafe UNSAFE;
    /**
     * Byte array base offset.
     */
    public static final int ARRAY_BYTE_BASE_OFFSET;

    static
    {
        Unsafe unsafe = null;
        try
        {
            final PrivilegedExceptionAction<Unsafe> action =
                () ->
                {
                    final Field f = Unsafe.class.getDeclaredField("theUnsafe");
                    f.setAccessible(true);

                    return (Unsafe)f.get(null);
                };

            unsafe = AccessController.doPrivileged(action);
        }
        catch (final Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        UNSAFE = unsafe;
        ARRAY_BYTE_BASE_OFFSET = Unsafe.ARRAY_BYTE_BASE_OFFSET;
    }

    private UnsafeAccess()
    {
    }
}
