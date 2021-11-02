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
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for the {@link org.agrona.MutableDirectBuffer#putDoubleAscii(int, double)} method.
 */
@Fork(value = 3, jvmArgsPrepend = "-Dagrona.disable.bounds.checks=true")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class MutableDirectBufferPutDoubleAsciiBenchmark
{
    private static final int CAPACITY = 512;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.#######################");

    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(CAPACITY));
    private final ExpandableArrayBuffer expandableArrayBuffer = new ExpandableArrayBuffer(CAPACITY);
    private final ExpandableDirectByteBuffer expandableDirectByteBuffer = new ExpandableDirectByteBuffer(CAPACITY);

    private int index;
    private double[] values;

    /**
     * Initialize benchmark state.
     */
    @Setup(Level.Trial)
    public void setup()
    {
        values = new Random(4732847239L).doubles(8192, -10_000_000, 10_000_000).toArray();
    }

    /**
     * Benchmark the {@link UnsafeBuffer#putDoubleAscii(int, double)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int unsafeBuffer()
    {
        final double[] values = this.values;
        return unsafeBuffer.putDoubleAscii(0, values[(++index) & (values.length - 1)]);
    }

    /**
     * Benchmark the {@link ExpandableArrayBuffer#putDoubleAscii(int, double)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableArrayBuffer()
    {
        final double[] values = this.values;
        return expandableArrayBuffer.putDoubleAscii(0, values[(++index) & (values.length - 1)]);
    }

    /**
     * Benchmark the {@link ExpandableDirectByteBuffer#putDoubleAscii(int, double)} method.
     *
     * @return length in bytes of the written value.
     */
    @Benchmark
    public int expandableDirectByteBuffer()
    {
        final double[] values = this.values;
        return expandableDirectByteBuffer.putDoubleAscii(0, values[(++index) & (values.length - 1)]);
    }

    /**
     * Benchmark the {@link Double#toString(double)} method.
     *
     * @return string representation of a double value.
     */
    @Benchmark
    public String doubleToString()
    {
        final double[] values = this.values;
        return Double.toString(values[(++index) & (values.length - 1)]);
    }

    /**
     * Benchmark the {@link DecimalFormat#format(double)} method.
     *
     * @return string representation of a double value.
     */
    @Benchmark
    public String decimalFormat()
    {
        final double[] values = this.values;
        return DECIMAL_FORMAT.format(values[(++index) & (values.length - 1)]);
    }
}
