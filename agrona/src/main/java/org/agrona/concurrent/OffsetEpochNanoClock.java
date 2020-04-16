/*
 * Copyright 2020 Real Logic Limited.
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * An accurate, zero-gc, pure-java, EpochNanoClock that calculates an initial nanoTime() offset and then uses
 * that offset to adjust the return value of System.nanoTime() into the UNIX epoch.
 *
 * @see org.agrona.concurrent.SystemEpochNanoClock
 */
public class OffsetEpochNanoClock implements EpochNanoClock
{
    private static final long DEFAULT_MEASUREMENT_THRESHOLD_IN_NS = 250;
    private static final int DEFAULT_MAX_MEASUREMENT_RETRIES = 100;

    private final long initialNanoTime;
    private final long initialCurrentTimeNanos;
    private final boolean isWithinThreshold;

    public OffsetEpochNanoClock()
    {
        this(DEFAULT_MAX_MEASUREMENT_RETRIES, DEFAULT_MEASUREMENT_THRESHOLD_IN_NS);
    }

    public OffsetEpochNanoClock(final int maxMeasurementRetries, final long measurementThresholdInNs)
    {
        // Loop attempts to find a measurement that is accurate to a given threshold
        long bestInitialCurrentTimeNanos = 0, bestInitialNanoTime = 0;
        long bestNanoTimeWindow = Long.MAX_VALUE;

        for (int i = 0; i < maxMeasurementRetries; i++)
        {
            final long firstNanoTime = System.nanoTime();
            final long initialCurrentTimeMillis = System.currentTimeMillis();
            final long secondNanoTime = System.nanoTime();

            final long nanoTimeWndow = secondNanoTime - firstNanoTime;
            if (nanoTimeWndow < measurementThresholdInNs)
            {
                initialCurrentTimeNanos = MILLISECONDS.toNanos(initialCurrentTimeMillis);
                initialNanoTime = (firstNanoTime + secondNanoTime) / 2;
                isWithinThreshold = true;
                return;
            }
            else if (nanoTimeWndow < bestNanoTimeWindow)
            {
                bestInitialCurrentTimeNanos = MILLISECONDS.toNanos(initialCurrentTimeMillis);
                bestInitialNanoTime = (firstNanoTime + secondNanoTime) / 2;
                bestNanoTimeWindow = nanoTimeWndow;
            }
        }

        // If we never get a time below the threshold, pick the narrowest window we've seen so far.
        initialCurrentTimeNanos = bestInitialCurrentTimeNanos;
        initialNanoTime = bestInitialNanoTime;
        isWithinThreshold = false;
    }

    public long nanoTime()
    {
        final long nanoTimeAdjustment = System.nanoTime() - initialNanoTime;
        return initialCurrentTimeNanos + nanoTimeAdjustment;
    }

    public boolean isWithinThreshold()
    {
        return isWithinThreshold;
    }
}
