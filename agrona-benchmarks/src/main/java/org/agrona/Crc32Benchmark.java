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

import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class Crc32Benchmark
{
    @Param({ "32", "128", "256", "1376", "2048", "4096" })
    private int length;

    private int offset = 32;
    private long address;
    private ByteBuffer directByteBuffer;
    private ByteBuffer heapByteBuffer;
    private CRC32 crc32;

    @Setup(Level.Trial)
    public void setup()
    {
        directByteBuffer = BufferUtil.allocateDirectAligned(length + offset, 64);
        heapByteBuffer = ByteBuffer.allocate(length + offset + 64);
        address = BufferUtil.address(directByteBuffer);
        directByteBuffer.position(offset);
        heapByteBuffer.position(offset);

        for (int i = 0; i < length; i++)
        {
            directByteBuffer.put((byte)i);
            heapByteBuffer.put((byte)i);
        }

        directByteBuffer.flip().position(offset);
        heapByteBuffer.flip().position(offset);
        crc32 = new CRC32();
    }

    @Benchmark
    public int publicApiHeapByteBuffer()
    {
        return callPublicApi(heapByteBuffer);
    }

    @Benchmark
    public int publicApiDirectByteBuffer()
    {
        return callPublicApi(directByteBuffer);
    }

    @Benchmark
    public int crc32Native()
    {
        return Checksums.crc32(0, address, offset, length);
    }

    private int callPublicApi(final ByteBuffer byteBuffer)
    {
        final int limit = byteBuffer.limit();
        final int position = byteBuffer.position();

        crc32.reset();
        crc32.update(byteBuffer);
        final int checksum = (int)crc32.getValue();
        byteBuffer.limit(limit).position(position);

        return checksum;
    }
}
