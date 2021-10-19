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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for the {@link org.agrona.MutableDirectBuffer#putStringAscii(int, String)} and
 * {@link org.agrona.MutableDirectBuffer#putStringAscii(int, CharSequence)} methods.
 */
@Fork(value = 3, jvmArgsPrepend = "-Dagrona.disable.bounds.checks=true")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class MutableDirectBufferPutStringAsciiBenchmark
{
    private static final int BUFFER_CAPACITY = 128;

    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_CAPACITY));
    private final ExpandableArrayBuffer expandableArrayBuffer = new ExpandableArrayBuffer(BUFFER_CAPACITY);
    private final ExpandableDirectByteBuffer expandableDirectByteBuffer
        = new ExpandableDirectByteBuffer(BUFFER_CAPACITY);

    private final CharSequence charSequence = new StringBuilder(
        "Cupcake ipsum dolor sit amet chupa chups sweet jelly topping.");
    private final String string = charSequence.toString();

    /**
     * Benchmark the {@link UnsafeBuffer#putStringAscii(int, String)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int unsafeBufferString()
    {
        return unsafeBuffer.putStringAscii(0, string);
    }

    /**
     * Benchmark the {@link UnsafeBuffer#putStringAscii(int, CharSequence)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int unsafeBufferCharSequence()
    {
        return unsafeBuffer.putStringAscii(0, charSequence);
    }

    /**
     * Benchmark the {@link ExpandableArrayBuffer#putStringAscii(int, String)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableArrayBufferString()
    {
        return expandableArrayBuffer.putStringAscii(0, string);
    }

    /**
     * Benchmark the {@link ExpandableArrayBuffer#putStringAscii(int, CharSequence)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableArrayBufferCharSequence()
    {
        return expandableArrayBuffer.putStringAscii(0, charSequence);
    }

    /**
     * Benchmark the {@link ExpandableDirectByteBuffer#putStringAscii(int, String)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableDirectByteBufferString()
    {
        return expandableDirectByteBuffer.putStringAscii(0, string);
    }

    /**
     * Benchmark the {@link ExpandableDirectByteBuffer#putStringAscii(int, CharSequence)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableDirectByteBufferCharSequence()
    {
        return expandableDirectByteBuffer.putStringAscii(0, charSequence);
    }

    /**
     * Benchmark entry point.
     *
     * @param args program arguments (ignored)
     */
    public static void main(final String[] args) throws RunnerException
    {
        final Options opt = new OptionsBuilder()
            .include(".*" + MutableDirectBufferPutStringAsciiBenchmark.class.getSimpleName() + ".*")
            .build();

        new Runner(opt).run();
    }
}
