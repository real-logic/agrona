/*
 * Copyright 2014-2023 Real Logic Limited.
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
 * Benchmark for the {@link org.agrona.MutableDirectBuffer#putLongAscii(int, long)} method.
 */
@Fork(value = 3, jvmArgsPrepend = "-Dagrona.disable.bounds.checks=true")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class MutableDirectBufferPutLongAsciiBenchmark
{
    private static final int CAPACITY = 32;

    @Param({
        "-9223372036854775808",
        "-1913372036854775855",
        "0",
        "-9182",
        "27085146",
        "1010101010101010",
        "8999999999999999999",
        "9223372036854775807" })
    private long value;

    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(CAPACITY));
    private final ExpandableArrayBuffer expandableArrayBuffer = new ExpandableArrayBuffer(CAPACITY);
    private final ExpandableDirectByteBuffer expandableDirectByteBuffer = new ExpandableDirectByteBuffer(CAPACITY);

    /**
     * Benchmark the {@link UnsafeBuffer#putLongAscii(int, long)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int unsafeBuffer()
    {
        return unsafeBuffer.putLongAscii(0, value);
    }

    /**
     * Benchmark the {@link ExpandableArrayBuffer#putLongAscii(int, long)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableArrayBuffer()
    {
        return expandableArrayBuffer.putLongAscii(0, value);
    }

    /**
     * Benchmark the {@link ExpandableDirectByteBuffer#putLongAscii(int, long)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableDirectByteBuffer()
    {
        return expandableDirectByteBuffer.putLongAscii(0, value);
    }

    /**
     * Benchmark the {@link Long#toString(long)} method.
     *
     * @return string representation of a long value.
     */
    @Benchmark
    public String longToString()
    {
        return Long.toString(value);
    }
}
