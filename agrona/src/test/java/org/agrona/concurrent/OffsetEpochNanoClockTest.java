/*
 * Copyright 2014-2020 Real Logic Limited.
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

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class OffsetEpochNanoClockTest
{
    @Test
    public void shouldFindSaneEpochTimestamp()
    {
        final OffsetEpochNanoClock clock = new OffsetEpochNanoClock();

        assertSaneEpochTimestamp(clock);
    }

    private void assertSaneEpochTimestamp(final OffsetEpochNanoClock clock)
    {
        final long beginMs = System.currentTimeMillis();
        sleep();

        final long nanoTime = clock.nanoTime();
        final long timeMs = NANOSECONDS.toMillis(nanoTime);

        sleep();
        final long endMs = System.currentTimeMillis();

        assertThat(timeMs, greaterThanOrEqualTo(beginMs));
        assertThat(timeMs, lessThanOrEqualTo(endMs));
    }

    @Test
    public void shouldResampleSaneEpochTimestamp()
    {
        final OffsetEpochNanoClock clock = new OffsetEpochNanoClock();

        clock.sample();

        assertSaneEpochTimestamp(clock);
    }

    private void sleep()
    {
        try
        {
            Thread.sleep(1);
        }
        catch (final InterruptedException ignore)
        {
            Thread.currentThread().interrupt();
        }
    }
}
