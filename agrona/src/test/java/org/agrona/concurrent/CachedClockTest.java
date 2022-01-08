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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CachedClockTest
{
    @Test
    public void shouldUpdateNanoClock()
    {
        final long timestamp = 777L;

        final CachedNanoClock clock = new CachedNanoClock();
        assertThat(clock.nanoTime(), is(0L));

        clock.update(timestamp);
        assertThat(clock.nanoTime(), is(timestamp));
    }

    @Test
    public void shouldUpdateEpochClock()
    {
        final long timestamp = 333L;

        final CachedEpochClock clock = new CachedEpochClock();
        assertThat(clock.time(), is(0L));

        clock.update(timestamp);
        assertThat(clock.time(), is(timestamp));
    }
}
