/*
 * Copyright 2014-2025 Real Logic Limited.
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
package org.agrona.collections;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for the {@link IntArrayList}.
 */
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Benchmark)
public class IntArrayListBenchmark
{
    private static final int SEED = 42;

    @Param({ "16", "1024", "65536" })
    private int size;

    private IntArrayList list;

    /**
     * Default constructor.
     */
    public IntArrayListBenchmark()
    {
    }

    /**
     * Setup.
     */
    @Setup
    public void setup()
    {
        final Random random = new Random(SEED);

        list = new IntArrayList();
        for (int i = 0; i < size; i++)
        {
            list.addInt(random.nextInt());
        }
    }

    /**
     * Add {@code size} elements to a new list created with the default arguments.
     *
     * @return the list.
     */
    @Benchmark
    public IntArrayList addInt()
    {
        final IntArrayList list = new IntArrayList();

        for (int i = 0; i < size; i++)
        {
            list.addInt(i);
        }

        return list;
    }

    /**
     * Merge sort an existing list of random elements into a new list.
     *
     * @return the sorted list.
     */
    @Benchmark
    public IntArrayList mergeSort()
    {
        return mergeSort(list, 0, list.size());
    }

    private static IntArrayList mergeSort(final IntArrayList list, final int from, final int to)
    {
        final int length = to - from;
        if (length <= 1)
        {
            final IntArrayList result = new IntArrayList();
            if (1 == length)
            {
                result.addInt(list.getInt(from));
            }

            return result;
        }

        final int mid = (from + to) / 2;
        final IntArrayList left = mergeSort(list, from, mid);
        final IntArrayList right = mergeSort(list, mid, to);

        final IntArrayList result = new IntArrayList();
        final int leftSize = left.size();
        final int rightSize = right.size();
        int i = 0;
        int j = 0;

        while (i < leftSize && j < rightSize)
        {
            final int l = left.getInt(i);
            final int r = right.getInt(j);
            if (l <= r)
            {
                result.addInt(l);
                i++;
            }
            else
            {
                result.addInt(r);
                j++;
            }
        }

        while (i < leftSize)
        {
            result.addInt(left.getInt(i));
            ++i;
        }

        while (j < rightSize)
        {
            result.addInt(right.getInt(j));
            ++j;
        }

        return result;
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
            .include(IntArrayListBenchmark.class.getName()).shouldFailOnError(true).build())
            .run();
    }
}
