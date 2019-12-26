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

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static org.agrona.BufferUtil.address;
import static org.agrona.BufferUtil.allocateDirectAligned;
import static org.agrona.Checksums.crc32;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChecksumsTest
{
    @Test
    void crc32ShouldComputeCorrectCrc32Checksum()
    {
        final Checksum crc32 = lookupChecksumImplementation();
        final byte[] data = new byte[23];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = (byte)i;
            crc32.update(i);
        }

        final int checksum1 = (int)crc32.getValue();
        crc32.update(new byte[10], 0, 10);
        final int checksum2 = (int)crc32.getValue();

        final ByteBuffer buffer = allocateDirectAligned(100, 64);
        buffer.position(5);
        buffer.put(data);
        final long address = address(buffer);

        assertEquals(checksum1, crc32(0, address, 5, 23));
        assertEquals(checksum2, crc32(checksum1, address, 28, 10));
        assertNotEquals(checksum2, crc32(0, address, 28, 10));
        assertEquals(checksum1, crc32(0, address, 5, 23));
    }

    private Checksum lookupChecksumImplementation()
    {
        try
        {
            final Class<?> klass = Class.forName("java.util.zip.CRC32C");
            return (Checksum)klass.getDeclaredConstructor().newInstance();
        }
        catch (final ClassNotFoundException e)
        {
            return new CRC32(); // JDK 8 version
        }
        catch (final ReflectiveOperationException e)
        {
            LangUtil.rethrowUnchecked(e);
            return null; // un-reachable
        }
    }
}