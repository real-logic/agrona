/*
 * Copyright 2014-2019 Real Logic Ltd.
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
import java.lang.reflect.Method;

import static java.lang.invoke.MethodHandles.lookup;

public final class Checksums
{
    private static final MethodHandle CRC_METHOD_HANDLE;
    private static final boolean USE_CRC32C;

    static
    {
        MethodHandle methodHandle;
        boolean useCRC32C = false;

        try
        {
            methodHandle = findMethodHandle("java.util.zip.CRC32C", "updateDirectByteBuffer");
            useCRC32C = true;
        }
        catch (final Exception ex1)
        {
            try
            {
                methodHandle = findMethodHandle("java.util.zip.CRC32", "updateByteBuffer");
            }
            catch (final Exception ex2)
            {
                ex2.addSuppressed(ex1);
                throw new Error("failed to resolve CRC methods", ex2);
            }
        }

        CRC_METHOD_HANDLE = methodHandle;
        USE_CRC32C = useCRC32C;
    }

    private static MethodHandle findMethodHandle(final String className, final String methodName) throws Exception
    {
        final Class<?> klass = Class.forName(className);
        final Method method = klass.getDeclaredMethod(methodName, int.class, long.class, int.class, int.class);
        method.setAccessible(true);

        return lookup().unreflect(method);
    }

    private Checksums()
    {
    }

    /**
     * Compute CRC-32 (or CRC-32C) checksum on via an address which could be from a {@link java.nio.ByteBuffer}.
     * <p>
     * <em>WARNING: Executing this method non-direct ByteBuffer address may segfault the VM!</em>
     *
     * @param crc     current CRC-32 checksum.
     * @param address at which the underlying storage begins.
     * @param offset  from the address.
     * @param length  of the data from which CRC-32 checksum should be computed.
     * @return CRC-32 (or CRC-32C) checksum.
     */
    public static int crc32(final int crc, final long address, final int offset, final int length)
    {
        try
        {
            if (USE_CRC32C)
            {
                return ~(int)CRC_METHOD_HANDLE.invokeExact(~crc, address, offset, offset + length);
            }
            else
            {
                return (int)CRC_METHOD_HANDLE.invokeExact(crc, address, offset, length);
            }
        }
        catch (final Throwable throwable)
        {
            LangUtil.rethrowUnchecked(throwable);
            return -1; // make compiler happy
        }
    }
}
