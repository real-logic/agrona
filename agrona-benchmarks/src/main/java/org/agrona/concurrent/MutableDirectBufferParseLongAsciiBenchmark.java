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
 * Benchmark for the {@link org.agrona.MutableDirectBuffer#parseLongAscii(int, int)} method.
 */
@Fork(value = 3, jvmArgsPrepend = "-Dagrona.disable.bounds.checks=true")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class MutableDirectBufferParseLongAsciiBenchmark
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
    private String value;
    private int length;

    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(CAPACITY));
    private final ExpandableArrayBuffer expandableArrayBuffer = new ExpandableArrayBuffer(CAPACITY);
    private final ExpandableDirectByteBuffer expandableDirectByteBuffer = new ExpandableDirectByteBuffer(CAPACITY);

    /**
     * Setup test data.
     */
    @Setup
    public void setup()
    {
        length = value.length();
        unsafeBuffer.putStringWithoutLengthAscii(0, value);
        expandableArrayBuffer.putStringWithoutLengthAscii(0, value);
        expandableDirectByteBuffer.putStringWithoutLengthAscii(0, value);
    }

    /**
     * Benchmark the {@link UnsafeBuffer#parseLongAscii(int, int)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public long unsafeBuffer()
    {
        return unsafeBuffer.parseLongAscii(0, length);
    }

    /**
     * Benchmark the {@link ExpandableArrayBuffer#parseLongAscii(int, int)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public long expandableArrayBuffer()
    {
        return expandableArrayBuffer.parseLongAscii(0, length);
    }

    /**
     * Benchmark the {@link ExpandableDirectByteBuffer#parseLongAscii(int, int)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public long expandableDirectByteBuffer()
    {
        return expandableDirectByteBuffer.parseLongAscii(0, length);
    }

    /**
     * Benchmark the {@link Long#parseLong(String)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public long longParseLong()
    {
        return Long.parseLong(value);
    }
}
