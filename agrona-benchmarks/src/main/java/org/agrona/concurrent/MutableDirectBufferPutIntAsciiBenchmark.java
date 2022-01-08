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

import org.agrona.ExpandableArrayBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for the {@link org.agrona.MutableDirectBuffer#putIntAscii(int, int)} method.
 */
@Fork(value = 3, jvmArgsPrepend = "-Dagrona.disable.bounds.checks=true")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class MutableDirectBufferPutIntAsciiBenchmark
{
    private static final int CAPACITY = 16;

    @Param({ "-2147483648", "-1234567890", "0", "-9182", "27085146", "1999999999", "2147483647" })
    private int value;

    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(CAPACITY));
    private final ExpandableArrayBuffer expandableArrayBuffer = new ExpandableArrayBuffer(CAPACITY);
    private final ExpandableDirectByteBuffer expandableDirectByteBuffer = new ExpandableDirectByteBuffer(CAPACITY);

    /**
     * Benchmark the {@link UnsafeBuffer#putIntAscii(int, int)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int unsafeBuffer()
    {
        return unsafeBuffer.putIntAscii(0, value);
    }

    /**
     * Benchmark the {@link ExpandableArrayBuffer#putIntAscii(int, int)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableArrayBuffer()
    {
        return expandableArrayBuffer.putIntAscii(0, value);
    }

    /**
     * Benchmark the {@link ExpandableDirectByteBuffer#putIntAscii(int, int)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableDirectByteBuffer()
    {
        return expandableDirectByteBuffer.putIntAscii(0, value);
    }

    /**
     * Benchmark the {@link Integer#toString(int)} method.
     *
     * @return string representation of an int value.
     */
    @Benchmark
    public String integerToString()
    {
        return Integer.toString(value);
    }
}
