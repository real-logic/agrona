/*
 * Copyright 2014-2019 Real Logic Ltd.
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

import org.agrona.hints.ThreadHints;

import java.util.concurrent.locks.LockSupport;

abstract class BackoffIdleStrategyPrePad
{
    @SuppressWarnings("unused")
    long p01, p02, p03, p04, p05, p06, p07;
}

abstract class BackoffIdleStrategyData extends BackoffIdleStrategyPrePad
{
    protected static final int NOT_IDLE = 0;
    protected static final int SPINNING = 1;
    protected static final int YIELDING = 2;
    protected static final int PARKING = 3;

    protected final long maxSpins;
    protected final long maxYields;
    protected final long minParkPeriodNs;
    protected final long maxParkPeriodNs;

    protected int state = NOT_IDLE;
    protected long spins;
    protected long yields;
    protected long parkPeriodNs;

    BackoffIdleStrategyData(
        final long maxSpins, final long maxYields, final long minParkPeriodNs, final long maxParkPeriodNs)
    {
        this.maxSpins = maxSpins;
        this.maxYields = maxYields;
        this.minParkPeriodNs = minParkPeriodNs;
        this.maxParkPeriodNs = maxParkPeriodNs;
    }
}

/**
 * Idling strategy for threads when they have no work to do.
 * <p>
 * Spin for maxSpins, then
 * {@link Thread#yield()} for maxYields, then
 * {@link java.util.concurrent.locks.LockSupport#parkNanos(long)} on an exponential backoff to maxParkPeriodNs
 */
public final class BackoffIdleStrategy extends BackoffIdleStrategyData implements IdleStrategy
{
    public static final long DEFAULT_MAX_SPINS = 10L;
    public static final long DEFAULT_MAX_YIELDS = 5L;
    public static final long DEFAULT_MIN_PARK_PERIOD_NS = 1000L;
    public static final long DEFAULT_MAX_PARK_PERIOD_NS = 1_000_000L;

    @SuppressWarnings("unused")
    long p01, p02, p03, p04, p05, p06, p07;

    /**
     * Default constructor using {@link #DEFAULT_MAX_SPINS}, {@link #DEFAULT_MAX_YIELDS},
     * {@link #DEFAULT_MIN_PARK_PERIOD_NS}, and {@link #DEFAULT_MAX_YIELDS}.
     */
    public BackoffIdleStrategy()
    {
        super(DEFAULT_MAX_SPINS, DEFAULT_MAX_YIELDS, DEFAULT_MIN_PARK_PERIOD_NS, DEFAULT_MAX_PARK_PERIOD_NS);
    }

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
    }

    /**
     * {@inheritDoc}
     */
    public void idle(final int workCount)
    {
        if (workCount > 0)
        {
            reset();
        }
        else
        {
            idle();
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void idle()
    {
        switch (state)
        {
            case NOT_IDLE:
                state = SPINNING;
                spins++;
                break;

            case SPINNING:
                ThreadHints.onSpinWait();
                if (++spins > maxSpins)
                {
                    state = YIELDING;
                    yields = 0;
                }
                break;

            case YIELDING:
                if (++yields > maxYields)
                {
                    state = PARKING;
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

    /**
     *  {@inheritDoc}
     */
    public void reset()
    {
        spins = 0;
        yields = 0;
        parkPeriodNs = minParkPeriodNs;
        state = NOT_IDLE;
    }

    public String toString()
    {
        return "BackoffIdleStrategy{" +
            "maxSpins=" + maxSpins +
            ", maxYields=" + maxYields +
            ", minParkPeriodNs=" + minParkPeriodNs +
            ", maxParkPeriodNs=" + maxParkPeriodNs +
            '}';
    }
}

