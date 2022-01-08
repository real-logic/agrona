/*
 * Copyright 2014-2022 Real Logic Limited.
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
package org.agrona.concurrent;

import org.agrona.BitUtil;
import org.agrona.BufferUtil;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for the {@link org.agrona.MutableDirectBuffer#setMemory(int, int, byte)} method.
 */
@Fork(value = 3, jvmArgsPrepend = "-Dagrona.disable.bounds.checks=true")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class SetMemoryBenchmark
{
    @Param
    private Type type;
    @Param({ "0", "1" })
    private int index;
    @Param({ "128", "1024" })
    private int length;

    private UnsafeBuffer buffer;

    /**
     * Type of the {@link ByteBuffer} to create.
     */
    public enum Type
    {
        ARRAY,
        HEAP_BB,
        DIRECT_BB,
        DIRECT_ALIGNED_BB;
    }

    /**
     * Setup.
     */
    @Setup
    public void setup()
    {
        final int capacity = BitUtil.findNextPositivePowerOfTwo(1 + index + length);
        switch (type)
        {
            case ARRAY:
                buffer = new UnsafeBuffer(new byte[capacity]);
                break;
            case HEAP_BB:
                buffer = new UnsafeBuffer(ByteBuffer.allocate(capacity));
                break;
            case DIRECT_BB:
                buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(capacity));
                break;
            case DIRECT_ALIGNED_BB:
                buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(capacity, 32));
                break;
        }
    }

    /**
     * Benchmark the {@link UnsafeBuffer#putIntAscii(int, int)} method.
     */
    @Benchmark
    public void benchmark()
    {
        buffer.setMemory(index, length, (byte)0xFF);
    }
}
