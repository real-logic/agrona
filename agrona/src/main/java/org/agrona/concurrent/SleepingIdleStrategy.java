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

import java.util.concurrent.locks.LockSupport;

/**
 * When idle this strategy is to sleep for a specified period in nanoseconds.
 * <p>
 * This class uses {@link LockSupport#parkNanos(long)} to idle.
 */
public final class SleepingIdleStrategy implements IdleStrategy
{
    private final long sleepPeriodNs;

    /**
     * Constructed a new strategy that will sleep for a given period when idle.
     *
     * @param sleepPeriodNs period in nanosecond for which the strategy will sleep when work count is 0.
     */
    public SleepingIdleStrategy(final long sleepPeriodNs)
    {
        this.sleepPeriodNs = sleepPeriodNs;
    }

    /**
     *  {@inheritDoc}
     */
    public void idle(final int workCount)
    {
        if (workCount > 0)
        {
            return;
        }

        LockSupport.parkNanos(sleepPeriodNs);
    }

    /**
     *  {@inheritDoc}
     */
    public void idle()
    {
        LockSupport.parkNanos(sleepPeriodNs);
    }

    /**
     *  {@inheritDoc}
     */
    public void reset()
    {
    }

    public String toString()
    {
        return "SleepingIdleStrategy{" +
            "sleepPeriodNs=" + sleepPeriodNs +
            '}';
    }
}
