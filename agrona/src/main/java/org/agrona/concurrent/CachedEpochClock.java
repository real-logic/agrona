/*
 * Copyright 2014-2019 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.concurrent;

import static org.agrona.UnsafeAccess.UNSAFE;

/**
 * Pad out a cacheline to the left of a value to prevent false sharing.
 */
class CachedEpochClockPadding
{
    @SuppressWarnings("unused")
    protected long p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;
}

/**
 * Value for the sequence that is expected to be padded.
 */
class CachedEpochClockValue extends CachedEpochClockPadding
{
    protected volatile long timeMs;
}

/**
 * An {@link EpochClock} that caches a timestamp which can be updated with {@link #update(long)}.
 * <p>
 * Instances are threadsafe with the read being volatile.
 */
public class CachedEpochClock extends CachedEpochClockValue implements EpochClock
{
    private static final long VALUE_OFFSET;

    static
    {
        try
        {
            VALUE_OFFSET = UNSAFE.objectFieldOffset(CachedEpochClockValue.class.getDeclaredField("timeMs"));
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unused")
    protected long p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;

    public long time()
    {
        return timeMs;
    }

    /**
     * Update the value of the timestamp in with ordered semantics.
     *
     * @param timeMs value to update the timestamp.
     */
    public void update(final long timeMs)
    {
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, timeMs);
    }

    /**
     * Update the value of the timestamp in with weak ordering semantics.
     *
     * @param timeMs value to update the timestamp.
     */
    public void updateWeak(final long timeMs)
    {
        UNSAFE.putLong(this, VALUE_OFFSET, timeMs);
    }
}
