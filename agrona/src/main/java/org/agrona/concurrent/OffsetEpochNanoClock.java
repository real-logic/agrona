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

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * An accurate, zero-gc, pure-java, {@link EpochNanoClock} that calculates an initial epoch nano time based on
 * {@link System#currentTimeMillis()} and then uses that offset to adjust the return value of
 * {@link System#nanoTime()} to the UNIX epoch.
 * <p>
 * The {@link #sample()} method can be used in order to reset these initial values if your system clock gets updated.
 *
 * @see org.agrona.concurrent.SystemEpochNanoClock
 */
public class OffsetEpochNanoClock implements EpochNanoClock
{
    private static final int DEFAULT_MAX_MEASUREMENT_RETRIES = 100;
    private static final long DEFAULT_MEASUREMENT_THRESHOLD_NS = 250;
    private static final long DEFAULT_RESAMPLE_INTERVAL_NS = HOURS.toNanos(1);

    private final int maxMeasurementRetries;
    private final long measurementThresholdNs;
    private final long resampleIntervalNs;

    private long initialNanoTime;
    private long initialCurrentNanoTime;
    private boolean isWithinThreshold;

    public OffsetEpochNanoClock()
    {
        this(DEFAULT_MAX_MEASUREMENT_RETRIES, DEFAULT_MEASUREMENT_THRESHOLD_NS, DEFAULT_RESAMPLE_INTERVAL_NS);
    }

    /**
     * Constructs the clock with custom configuration parameters.
     *
     * @param maxMeasurementRetries  the maximum number of times that this clock will attempt to re-sample the initial
     *                               time values.
     * @param measurementThresholdNs the desired accuracy window for the initial clock samples.
     * @param resampleIntervalNs     the desired interval before the samples are automatically recalculated. The seed
     *                               recalculation enables the system to minimise clock drift if the system clock is
     *                               updated.
     */
    public OffsetEpochNanoClock(
        final int maxMeasurementRetries, final long measurementThresholdNs, final long resampleIntervalNs)
    {
        this.maxMeasurementRetries = maxMeasurementRetries;
        this.measurementThresholdNs = measurementThresholdNs;
        this.resampleIntervalNs = resampleIntervalNs;

        sample();
    }

    /**
     * Explicitly resample the initial seeds.
     */
    public void sample()
    {
        // Loop attempts to find a measurement that is accurate to a given threshold
        long bestInitialCurrentNanoTime = 0, bestInitialNanoTime = 0;
        long bestNanoTimeWindow = Long.MAX_VALUE;

        for (int i = 0; i < maxMeasurementRetries; i++)
        {
            final long firstNanoTime = System.nanoTime();
            final long initialCurrentTimeMillis = System.currentTimeMillis();
            final long secondNanoTime = System.nanoTime();

            final long nanoTimeWindow = secondNanoTime - firstNanoTime;
            if (nanoTimeWindow < measurementThresholdNs)
            {
                initialCurrentNanoTime = MILLISECONDS.toNanos(initialCurrentTimeMillis);
                initialNanoTime = (firstNanoTime + secondNanoTime) >> 1;
                isWithinThreshold = true;
                return;
            }
            else if (nanoTimeWindow < bestNanoTimeWindow)
            {
                bestInitialCurrentNanoTime = MILLISECONDS.toNanos(initialCurrentTimeMillis);
                bestInitialNanoTime = (firstNanoTime + secondNanoTime) >> 1;
                bestNanoTimeWindow = nanoTimeWindow;
            }
        }

        // If we never get a time below the threshold, pick the narrowest window we've seen so far.
        initialCurrentNanoTime = bestInitialCurrentNanoTime;
        initialNanoTime = bestInitialNanoTime;
        isWithinThreshold = false;
    }

    public long nanoTime()
    {
        final long nanoTimeAdjustment = System.nanoTime() - initialNanoTime;
        if (nanoTimeAdjustment < 0 || nanoTimeAdjustment > resampleIntervalNs)
        {
            sample();
            return nanoTime();
        }

        return initialCurrentNanoTime + nanoTimeAdjustment;
    }

    /**
     * Gets whether the clock sampled the initial time offset accurately.
     *
     * @return true if the clock sampled the initial time offset accurately.
     */
    public boolean isWithinThreshold()
    {
        return isWithinThreshold;
    }
}
