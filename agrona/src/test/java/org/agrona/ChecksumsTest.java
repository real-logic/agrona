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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import static org.agrona.BufferUtil.address;
import static org.agrona.BufferUtil.allocateDirectAligned;
import static org.agrona.Checksums.crc32;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChecksumsTest
{
    @Test
    void crc32DirectByteBufferShouldComputeCorrectCrc32Checksum()
    {
        final CRC32 crc32 = new CRC32();
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

    @ParameterizedTest
    @CsvSource({ "-1,10", "33, 5", "0,-10", "0,100" })
    void crc32DirectByteBufferProducesGarbageWhenWrongOffsetOrLengthSpecified(final int offset, final int length)
    {
        final ByteBuffer buffer = allocateDirectAligned(32, 32);
        final long address = address(buffer);

        assertNotEquals(0, crc32(0, address, offset, length));
    }
}