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
package org.agrona.concurrent.status;

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;

import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.agrona.BufferUtil.allocateDirectAligned;
import static org.agrona.concurrent.status.CountersReader.COUNTER_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomicCounterTest
{
    @ParameterizedTest
    @MethodSource("buffers")
    void canWrapDifferentKindsOfBuffers(final AtomicBuffer buffer)
    {
        final long value = 42;
        final int counterId = 5;
        final AtomicCounter counter = new AtomicCounter(buffer, counterId);

        counter.set(value);
        counter.increment();
        assertEquals(value + 1, counter.get());
    }

    private static List<AtomicBuffer> buffers()
    {
        return Arrays.asList(
            new UnsafeBuffer(new long[10 * COUNTER_LENGTH / SIZE_OF_LONG]),
            new UnsafeBuffer(allocateDirectAligned(10 * COUNTER_LENGTH, SIZE_OF_LONG)));
    }
}
