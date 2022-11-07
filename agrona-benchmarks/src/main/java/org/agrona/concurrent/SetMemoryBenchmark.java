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
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static org.agrona.BufferUtil.allocateDirectAligned;

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
    private Type buffer;
    @Param({ "0", "3" })
    private int index;
    @Param({ "1", "2", "4", "16", "50", "99", "128", "1024" })
    private int length;

    private UnsafeBuffer unsafeBuffer;

    /**
     * Type of the {@link ByteBuffer} to create.
     */
    public enum Type
    {
        ARRAY,
        DIRECT
    }

    /**
     * Setup.
     */
    @Setup
    public void setup()
    {
        index = 0;
        final int capacity = BitUtil.findNextPositivePowerOfTwo(index + length);
        switch (buffer)
        {
            case ARRAY:
                unsafeBuffer = new UnsafeBuffer(new byte[capacity]);
                break;
            case DIRECT:
                unsafeBuffer = new UnsafeBuffer(allocateDirectAligned(capacity, 32));
                break;
        }
    }

    /**
     * Benchmark the {@link UnsafeBuffer#putIntAscii(int, int)} method.
     */
    @Benchmark
    public void benchmark()
    {
        unsafeBuffer.setMemory(index, length, (byte)0xFF);
    }

    /**
     * Runner method that allows starting benchmark directly.
     *
     * @param args for the main method.
     * @throws RunnerException in case if JMH throws while starting the benchmark.
     */
    public static void main(final String[] args) throws RunnerException
    {
        new Runner(new OptionsBuilder()
            .include(SetMemoryBenchmark.class.getName()).shouldFailOnError(true).build())
            .run();
    }
}
