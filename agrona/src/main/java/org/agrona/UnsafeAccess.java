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

import java.lang.reflect.Field;

/**
 * Obtain access the {@link sun.misc.Unsafe} class for direct memory operations.
 */
@SuppressWarnings("removal")
public final class UnsafeAccess
{
    /**
     * Reference to the {@link sun.misc.Unsafe} instance.
     */
    public static final sun.misc.Unsafe UNSAFE;

    /**
     * Byte array base offset.
     */
    public static final int ARRAY_BYTE_BASE_OFFSET;

    static
    {
        sun.misc.Unsafe unsafe = null;
        try
        {
            final Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);

            unsafe = (sun.misc.Unsafe)f.get(null);
        }
        catch (final Exception ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }

        UNSAFE = unsafe;
        ARRAY_BYTE_BASE_OFFSET = unsafe.arrayBaseOffset(byte[].class);
    }

    private UnsafeAccess()
    {
    }
}
