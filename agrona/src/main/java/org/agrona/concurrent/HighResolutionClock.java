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

import java.time.Instant;

/**
 * Clock that provides the number of time units since the 1 Jan 1970 UTC.
 * <p>
 * This implementation my be replaced on some platforms for greater performance.
 * <p>
 * <b>Note:</b> The actual provided resolution may be a higher granularity than the possible precision.
 * For example, on Java 8 many JVMs will only advance a millisecond per tick so when requesting micros or nanos
 * then the returned time may be a multiple of milliseconds. Later JVMs tend to improve on this.
 */
public class HighResolutionClock
{
    /**
     * The number of milliseconds since the 1 Jan 1970 UTC.
     *
     * @return the number of milliseconds since the 1 Jan 1970 UTC.
     */
    public static long epochMillis()
    {
        return System.currentTimeMillis();
    }

    /**
     * The number of microseconds since the 1 Jan 1970 UTC.
     *
     * @return the number of microseconds since the 1 Jan 1970 UTC.
     */
    public static long epochMicros()
    {
        final Instant now = Instant.now();
        final long seconds = now.getEpochSecond();
        final long nanosFromSecond = now.getNano();

        return (seconds * 1_000_000) + (nanosFromSecond / 1_000);
    }

    /**
     * The number of nanoseconds since the 1 Jan 1970 UTC.
     *
     * @return the number of nanoseconds since the 1 Jan 1970 UTC.
     */
    public static long epochNanos()
    {
        final Instant now = Instant.now();
        final long seconds = now.getEpochSecond();
        final long nanosFromSecond = now.getNano();

        return (seconds * 1_000_000_000) + nanosFromSecond;
    }
}
