/*
 *  Copyright 2014 - 2016 Real Logic Ltd.
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


import org.agrona.hints.ThreadHints;

import java.util.concurrent.locks.LockSupport;

/**
 * Idling strategy for threads when they have not work to do.
 *
 * Spin for maxSpins, then
 * {@link Thread#yield()} for maxYields, then
 * {@link java.util.concurrent.locks.LockSupport#parkNanos(long)} on an exponential backoff to maxParkPeriodNs
 */
@SuppressWarnings("unused")
abstract class BackoffIdleStrategyPrePad
{
    long pad01, pad02, pad03, pad04, pad05, pad06, pad07, pad08;
}

abstract class BackoffIdleStrategyData extends BackoffIdleStrategyPrePad
{
    enum State
    {
        NOT_IDLE, SPINNING, YIELDING, PARKING
    }

    protected final long maxSpins;
    protected final long maxYields;
    protected final long minParkPeriodNs;
    protected final long maxParkPeriodNs;

    protected State state;

    protected long spins;
    protected long yields;
    protected long parkPeriodNs;

    BackoffIdleStrategyData(final long maxSpins, final long maxYields, final long minParkPeriodNs, final long maxParkPeriodNs)
    {
        this.maxSpins = maxSpins;
        this.maxYields = maxYields;
        this.minParkPeriodNs = minParkPeriodNs;
        this.maxParkPeriodNs = maxParkPeriodNs;
    }
}

@SuppressWarnings("unused")
public final class BackoffIdleStrategy extends BackoffIdleStrategyData implements IdleStrategy
{
    long pad01, pad02, pad03, pad04, pad05, pad06, pad07, pad08;

    /**
     * Create a set of state tracking idle behavior
     *
     * @param maxSpins        to perform before moving to {@link Thread#yield()}
     * @param maxYields       to perform before moving to {@link java.util.concurrent.locks.LockSupport#parkNanos(long)}
     * @param minParkPeriodNs to use when initiating parking
     * @param maxParkPeriodNs to use when parking
     */
    public BackoffIdleStrategy(
        final long maxSpins, final long maxYields, final long minParkPeriodNs, final long maxParkPeriodNs)
    {
        super(maxSpins, maxYields, minParkPeriodNs, maxParkPeriodNs);
        this.state = State.NOT_IDLE;
    }

    /**
     * {@inheritDoc}
     */
    public void idle(final int workCount)
    {
        if (workCount > 0)
        {
            reset();
            return;
        }
        idle();
    }

    public void idle()
    {
        switch (state)
        {
            case NOT_IDLE:
                state = State.SPINNING;
                spins++;
                break;

            case SPINNING:
                ThreadHints.onSpinWait();
                if (++spins > maxSpins)
                {
                    state = State.YIELDING;
                    yields = 0;
                }
                break;

            case YIELDING:
                if (++yields > maxYields)
                {
                    state = State.PARKING;
                    parkPeriodNs = minParkPeriodNs;
                }
                else
                {
                    Thread.yield();
                }
                break;

            case PARKING:
                LockSupport.parkNanos(parkPeriodNs);
                parkPeriodNs = Math.min(parkPeriodNs << 1, maxParkPeriodNs);
                break;
        }
    }

    public void reset()
    {
        spins = 0;
        yields = 0;
        state = State.NOT_IDLE;
    }
}

