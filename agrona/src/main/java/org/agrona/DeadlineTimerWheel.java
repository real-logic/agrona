/*
 *  Copyright 2014-2017 Real Logic Ltd.
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
package org.agrona;

import java.util.Arrays;

/**
 * Timer Wheel (NOT thread safe)
 * <p>
 * Assumes single-writer principle and timers firing on processing thread.
 * Low (or NO) garbage.
 *
 * <h3>Implementation Details</h3>
 *
 * Based on netty's HashedTimerWheel, which is based on
 * <a href="http://cseweb.ucsd.edu/users/varghese/">George Varghese</a> and
 * Tony Lauck's paper,
 * <a href="http://cseweb.ucsd.edu/users/varghese/PAPERS/twheel.ps.Z">'Hashed
 * and Hierarchical Timing Wheels: data structures to efficiently implement a
 * timer facility'</a>.  More comprehensive slides are located
 * <a href="http://www.cse.wustl.edu/~cdgill/courses/cs6874/TimingWheels.ppt">here</a>.
 * <p>
 * Wheel is backed by arrays. Timer cancellation is O(1). Timer scheduling might be slightly
 * longer if a lot of timers are in the same tick. The underlying tick contains an array. That
 * array grows when needed, but does not currently shrink.
 * <p>
 * <b>Caveats</b>
 * <p>
 * Timers that expire in the same tick will not be ordered with one another. As ticks are
 * fairly large normally, this means that some timers may expire out of order.
 */
public class DeadlineTimerWheel
{
    private static final int INITIAL_TICK_DEPTH = 16;
    private static final long NO_TIMER_SCHEDULED = Long.MAX_VALUE;

    private final long[][] wheel;
    private final long tickDurationNs;
    private final long startTimeNs;
    private final int mask;

    private long numTimers;
    private int currentTick;

    /**
     * Callbacks for expiring timers.
     */
    @FunctionalInterface
    public interface TimerHandler
    {
        /**
         * Expiring timeout.
         *
         * @param timeNowNs for the expiring timer.
         * @param timerCorrelationId for the expiring timer.
         */
        void onTimeout(long timeNowNs, long timerCorrelationId);
    }

    /**
     * Construct timer wheel with given parameters.
     *
     * @param startTimeNs for the wheel (in nanoseconds)
     * @param tickDurationNs for the wheel (in nanoseconds)
     * @param ticksPerWheel for the wheel (must be power of 2)
     */
    public DeadlineTimerWheel(final long startTimeNs, final long tickDurationNs, final int ticksPerWheel)
    {
        this(startTimeNs, tickDurationNs, ticksPerWheel, INITIAL_TICK_DEPTH);
    }

    /**
     * Construct timer wheel with given parameters.
     *
     * @param startTimeNs for the wheel (in nanoseconds)
     * @param tickDurationNs for the wheel (in nanoseconds)
     * @param ticksPerWheel for the wheel (must be power of 2)
     * @param initialTickDepth for the wheel to be used for all ticks
     */
    public DeadlineTimerWheel(
        final long startTimeNs, final long tickDurationNs, final int ticksPerWheel, final int initialTickDepth)
    {
        checkTicksPerWheel(ticksPerWheel);

        this.mask = ticksPerWheel - 1;
        this.tickDurationNs = tickDurationNs;
        this.startTimeNs = startTimeNs;
        this.numTimers = 0;

        wheel = new long[ticksPerWheel][];

        for (int i = 0; i < wheel.length; i++)
        {
            wheel[i] = new long[initialTickDepth];

            for (int j = 0; j < wheel[i].length; j++)
            {
                wheel[i][j] = NO_TIMER_SCHEDULED;
            }
        }
    }

    /**
     * Duration of a tick of the wheel in nanoseconds.
     *
     * @return duration of a tick of the wheel in nanoseconds.
     */
    public long tickDurationNs()
    {
        return tickDurationNs;
    }

    /**
     * Deadline of current tick of the wheel in nanoseconds.
     *
     * @return deadline of the current tick of the wheel in nanoseconds.
     */
    public long currentTickDeadlineNs()
    {
        return ((currentTick + 1) * tickDurationNs) + startTimeNs;
    }

    /**
     * Number of currently active timers.
     *
     * @return number of currently active timers.
     */
    public long numTimers()
    {
        return numTimers;
    }

    /**
     * Schedule a timer for a given absolute time in nanoseconds.
     *
     * @param deadlineTimeNs for the timer to expire.
     * @return correlationId for the scheduled timer
     */
    public long scheduleTimeout(final long deadlineTimeNs)
    {
        final long ticks = Math.max((deadlineTimeNs - startTimeNs) / tickDurationNs, currentTick);
        final int wheelIndex = (int)(ticks & mask);
        final long[] array = wheel[wheelIndex];

        for (int i = 0; i < array.length; i++)
        {
            if (NO_TIMER_SCHEDULED == array[i])
            {
                array[i] = deadlineTimeNs;
                numTimers++;

                return correlationIdForSlot(wheelIndex, i);
            }
        }

        final long[] newArray = Arrays.copyOf(array, array.length + 1);
        newArray[array.length] = deadlineTimeNs;

        wheel[wheelIndex] = newArray;
        numTimers++;

        return correlationIdForSlot(wheelIndex, array.length);
    }

    /**
     * Cancel a previously scheduled timer.
     *
     * @param timerCorrelationId of the timer to cancel.
     */
    public void cancelTimeout(final long timerCorrelationId)
    {
        final int wheelIndex = tickForCorrelationId(timerCorrelationId);
        final int arrayIndex = indexInTickArray(timerCorrelationId);

        final long[] array = wheel[wheelIndex];

        if (array[arrayIndex] != NO_TIMER_SCHEDULED)
        {
            array[arrayIndex] = NO_TIMER_SCHEDULED;
            numTimers--;
        }
    }

    /**
     * Expire timers that have been scheduled to expire by the passed time.
     *
     * @param timeNowNs to use to determine timers to expire.
     * @param timerHandler to call for each expiring timer.
     * @param maxTimersToExpire before returning.
     * @return number of expired timers.
     */
    public int poll(final long timeNowNs, final TimerHandler timerHandler, final int maxTimersToExpire)
    {
        int timersExpired = 0;

        if (numTimers > 0)
        {
            final long[] array = wheel[currentTick & mask];

            for (int i = 0, length = array.length; i < length && maxTimersToExpire > timersExpired; i++)
            {
                if (array[i] <= timeNowNs)
                {
                    timerHandler.onTimeout(timeNowNs, correlationIdForSlot(currentTick & mask, i));
                    array[i] = NO_TIMER_SCHEDULED;
                    numTimers--;
                    timersExpired++;
                }
            }

            if (currentTickDeadlineNs() <= timeNowNs)
            {
                currentTick++;
            }
        }

        return timersExpired;
    }

    private static long correlationIdForSlot(final int tickOnWheel, final int indexInTickArray)
    {
        return ((long)tickOnWheel << 32) | indexInTickArray;
    }

    private static int tickForCorrelationId(final long correlationId)
    {
        return (int)(correlationId >> 32);
    }

    private static int indexInTickArray(final long correlationId)
    {
        return (int)correlationId;
    }

    private static void checkTicksPerWheel(final int ticksPerWheel)
    {
        if (!BitUtil.isPowerOfTwo(ticksPerWheel))
        {
            throw new IllegalArgumentException("ticks per wheel must be a power of 2: ticks=" + ticksPerWheel);
        }
    }
}