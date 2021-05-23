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
package org.agrona;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpandableDirectByteBufferTest
{
    private static final int ROUND_TRIP_ITERATIONS = 10_000_000;

    @Test
    void putIntAsciiRoundTrip()
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(64);
        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final int value = ThreadLocalRandom.current().nextInt();
            final int length = buffer.putIntAscii(0, value);
            final int parsedValue = buffer.parseIntAscii(0, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putLongAsciiRoundTrip()
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(64);
        for (int i = 0; i < ROUND_TRIP_ITERATIONS; i++)
        {
            final long value = ThreadLocalRandom.current().nextLong();
            final int length = buffer.putLongAscii(0, value);
            final long parsedValue = buffer.parseLongAscii(0, length);
            assertEquals(value, parsedValue);
        }
    }

    @Test
    void putNaturalIntAsciiRoundTrip()
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(64);
        ThreadLocalRandom.current().ints(ROUND_TRIP_ITERATIONS, 0, Integer.MAX_VALUE).forEach(
            (value) ->
            {
                final int length = buffer.putNaturalIntAscii(0, value);
                final int parsedValue = buffer.parseNaturalIntAscii(0, length);
                assertEquals(value, parsedValue);
            });
    }

    @Test
    void putNaturalLongAsciiRoundTrip()
    {
        final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer(64);
        ThreadLocalRandom.current().longs(ROUND_TRIP_ITERATIONS, 0, Long.MAX_VALUE).forEach(
            (value) ->
            {
                final int length = buffer.putNaturalLongAscii(0, value);
                final long parsedValue = buffer.parseNaturalLongAscii(0, length);
                assertEquals(value, parsedValue);
            }
        );
    }
}
