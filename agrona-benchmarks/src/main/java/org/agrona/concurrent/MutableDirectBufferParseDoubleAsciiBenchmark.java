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

import org.agrona.ExpandableArrayBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for the {@link org.agrona.MutableDirectBuffer#parseDoubleAscii(int, int)} method.
 */
@Fork(value = 3, jvmArgsPrepend = "-Dagrona.disable.bounds.checks=true")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class MutableDirectBufferParseDoubleAsciiBenchmark
{
    private static final int CAPACITY = 512;
    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(CAPACITY));
    private final ExpandableArrayBuffer expandableArrayBuffer = new ExpandableArrayBuffer(CAPACITY);
    private final ExpandableDirectByteBuffer expandableDirectByteBuffer = new ExpandableDirectByteBuffer(CAPACITY);

    @Param({
        "0",
        "1.25",
        "1000000",
        "2.2250738585072009e-308",
        "7317770170789331.0",
        "0.156250000000000000000000000000000000000000",
        "-2402844368454405395.2",
        "1090544144181609348835077142190",
        "-2e3000",
        "Infinity"
    })
    private String value;
    private int length;

    /**
     * Initialize benchmark state.
     */
    @Setup(Level.Trial)
    public void setup()
    {
        unsafeBuffer.putStringWithoutLengthAscii(0, value);
        expandableArrayBuffer.putStringWithoutLengthAscii(0, value);
        expandableDirectByteBuffer.putStringWithoutLengthAscii(0, value);
        length = value.length();
    }

    /**
     * Benchmark the {@link UnsafeBuffer#parseDoubleAscii(int, int)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public double unsafeBuffer()
    {
        return unsafeBuffer.parseDoubleAscii(0, length);
    }

    /**
     * Benchmark the {@link ExpandableArrayBuffer#parseDoubleAscii(int, int)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public double expandableArrayBuffer()
    {
        return expandableArrayBuffer.parseDoubleAscii(0, length);
    }

    /**
     * Benchmark the {@link ExpandableDirectByteBuffer#parseDoubleAscii(int, int)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public double expandableDirectByteBuffer()
    {
        return expandableDirectByteBuffer.parseDoubleAscii(0, length);
    }

    /**
     * Benchmark the {@link Double#parseDouble(String)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public double parseDouble()
    {
        return Double.parseDouble(value);
    }
}
