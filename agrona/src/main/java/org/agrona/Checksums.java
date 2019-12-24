/*
 * Copyright 2019 Real Logic Ltd.
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
import java.lang.reflect.Method;
import java.util.zip.CRC32;

public final class Checksums
{
    private static final MethodHandle METHOD_HANDLE;

    static
    {
        try
        {
            final Method method = CRC32.class.getDeclaredMethod(
                "updateByteBuffer", int.class, long.class, int.class, int.class);
            method.setAccessible(true);
            METHOD_HANDLE = MethodHandles.lookup().unreflect(method);
        }
        catch (final NoSuchMethodException | IllegalAccessException ex)
        {
            throw new Error("failed to resolved CRC methods", ex);
        }
    }

    private Checksums()
    {
    }

    /**
     * Compute CRC-32 checksum on via an address which could be from a {@link java.nio.ByteBuffer}.
     * <p>
     * <em>WARNING: Executing this method non-direct ByteBuffer address may segfault the VM!</em>
     *
     * @param crc     current CRC-32 checksum.
     * @param address at which the underlying storage begins.
     * @param offset  from the address.
     * @param length  of the data from which CRC-32 checksum should be computed.
     * @return CRC-32 checksum.
     */
    public static int crc32(final int crc, final long address, final int offset, final int length)
    {
        try
        {
            return (int)METHOD_HANDLE.invokeExact(crc, address, offset, length);
        }
        catch (final Throwable throwable)
        {
            LangUtil.rethrowUnchecked(throwable);
            return -1;
        }
    }
}
