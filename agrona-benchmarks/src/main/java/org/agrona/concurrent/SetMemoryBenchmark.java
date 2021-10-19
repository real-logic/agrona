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
package org.agrona.concurrent;

import org.agrona.BufferUtil;
import org.openjdk.jmh.annotations.*;

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
    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(2048, 32));
    @Param({ "0", "1" })
    private int index;
    @Param({ "128", "1024" })
    private int length;

    /**
     * Benchmark the {@link UnsafeBuffer#putIntAscii(int, int)} method.
     */
    @Benchmark
    public void benchmark()
    {
        unsafeBuffer.setMemory(index, length, (byte)0xFF);
    }
}
