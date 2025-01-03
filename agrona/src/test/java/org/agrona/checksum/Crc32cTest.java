/*
 * Copyright 2014-2025 Real Logic Limited.
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
package org.agrona.checksum;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32C;

import static org.agrona.BufferUtil.address;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Crc32cTest
{
    @Test
    void compute()
    {
        final Random random = new Random(54893045794L);
        final int offset = 7;
        final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 + offset);
        final long address = address(buffer);

        for (int i = 1; i <= 1024; i++)
        {
            final int length = i;
            final byte[] data = new byte[length];
            random.nextBytes(data);
            buffer.clear().position(offset);
            buffer.put(data);
            buffer.flip().position(offset);
            final CRC32C crc32c = new CRC32C();
            crc32c.update(buffer);
            final int checksum = (int)crc32c.getValue();
            assertEquals(
                checksum, Crc32c.INSTANCE.compute(address, offset, length), () -> "Failed on length: " + length);
        }
    }
}
