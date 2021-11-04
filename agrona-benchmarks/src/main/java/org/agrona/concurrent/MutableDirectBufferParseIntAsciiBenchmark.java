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
 * Benchmark for the {@link org.agrona.MutableDirectBuffer#parseIntAscii(int, int)} method.
 */
@Fork(value = 3, jvmArgsPrepend = "-Dagrona.disable.bounds.checks=true")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class MutableDirectBufferParseIntAsciiBenchmark
{
    private static final int CAPACITY = 16;

    @Param({ "-2147483648", "-1234567890", "0", "-9182", "27085146", "1999999999", "2147483647" })
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
     * Benchmark the {@link UnsafeBuffer#parseIntAscii(int, int)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public int unsafeBuffer()
    {
        return unsafeBuffer.parseIntAscii(0, length);
    }

    /**
     * Benchmark the {@link ExpandableArrayBuffer#parseIntAscii(int, int)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public int expandableArrayBuffer()
    {
        return expandableArrayBuffer.parseIntAscii(0, length);
    }

    /**
     * Benchmark the {@link ExpandableDirectByteBuffer#parseIntAscii(int, int)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public int expandableDirectByteBuffer()
    {
        return expandableDirectByteBuffer.parseIntAscii(0, length);
    }

    /**
     * Benchmark the {@link Integer#parseInt(String)} method.
     *
     * @return parsed value.
     */
    @Benchmark
    public int integerParseInt()
    {
        return Integer.parseInt(value);
    }
}
