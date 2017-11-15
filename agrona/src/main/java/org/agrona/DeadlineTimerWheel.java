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
import java.util.concurrent.TimeUnit;

/**
 * Deadline scheduled Timer Wheel (NOT thread safe)
 * <p>
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
 * <p>
 * <b>Note:</b> Not threadsafe.
 */
public class DeadlineTimerWheel
{
    private static final int INITIAL_TICK_DEPTH = 16;
    private static final long NO_TIMER_SCHEDULED = Long.MAX_VALUE;

    private final long[][] wheel;
    private final long tickInterval;
    private final long startTime;
    private final int mask;
    private final TimeUnit timeUnit;

    private long timerCount;
    private int currentTick;

    /**
     * Handler for expired timers.
     */
    @FunctionalInterface
    public interface TimerHandler
    {
        /**
         * Called when the deadline is past.
         *
         * @param timeUnit for the tick.
         * @param now      for the expired timer.
         * @param timerId  for the expired timer.
         * @return true to continue processing and expire timer or false to keep timer active
         */
        boolean onExpiry(TimeUnit timeUnit, long now, long timerId);
    }

    /**
     * Consumer of timer entries as deadline to timerId.
     */
    @FunctionalInterface
    public interface TimerConsumer
    {
        void accept(long deadline, long timerId);
    }

    /**
     * Construct timer wheel with given parameters.
     *
     * @param timeUnit      for each tick.
     * @param startTime     for the wheel (in given {@link TimeUnit})
     * @param tickInterval  for the wheel (in given {@link TimeUnit})
     * @param ticksPerWheel for the wheel (must be power of 2)
     */
    public DeadlineTimerWheel(
        final TimeUnit timeUnit, final long startTime, final long tickInterval, final int ticksPerWheel)
    {
        this(timeUnit, startTime, tickInterval, ticksPerWheel, INITIAL_TICK_DEPTH);
    }

    /**
     * Construct timer wheel with given parameters.
     *
     * @param timeUnit         for each tick.
     * @param startTime        for the wheel (in given {@link TimeUnit})
     * @param tickInterval     for the wheel (in given {@link TimeUnit})
     * @param ticksPerWheel    for the wheel (must be power of 2)
     * @param initialTickDepth for the wheel to be used for all ticks
     */
    public DeadlineTimerWheel(
        final TimeUnit timeUnit,
        final long startTime,
        final long tickInterval,
        final int ticksPerWheel,
        final int initialTickDepth)
    {
        checkTicksPerWheel(ticksPerWheel);

        this.timeUnit = timeUnit;
        this.mask = ticksPerWheel - 1;
        this.tickInterval = tickInterval;
        this.startTime = startTime;
        this.timerCount = 0;

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
     * Time unit for the ticks.
     *
     * @return time unit for the ticks.
     */
    public TimeUnit timeUnit()
    {
        return timeUnit;
    }

    /**
     * Interval of a tick of the wheel in {@link #timeUnit()}s.
     *
     * @return interval of a tick of the wheel in {@link #timeUnit()}s.
     */
    public long tickInterval()
    {
        return tickInterval;
    }

    /**
     * Deadline of current tick of the wheel in {@link #timeUnit()}s.
     *
     * @return deadline of the current tick of the wheel in {@link #timeUnit()}s.
     */
    public long currentTickDeadline()
    {
        return ((currentTick + 1) * tickInterval) + startTime;
    }

    /**
     * Number of active timers.
     *
     * @return number of active timers.
     */
    public long timerCount()
    {
        return timerCount;
    }

    /**
     * Schedule a timer for a given absolute time as a deadline in {@link #timeUnit()}s. A timerId will be assigned
     * and returned for future reference.
     *
     * @param deadline for the timer to expire.
     * @return timerId for the scheduled timer
     */
    public long scheduleTimer(final long deadline)
    {
        final long ticks = Math.max((deadline - startTime) / tickInterval, currentTick);
        final int wheelIndex = (int)(ticks & mask);
        final long[] array = wheel[wheelIndex];

        for (int i = 0; i < array.length; i++)
        {
            if (NO_TIMER_SCHEDULED == array[i])
            {
                array[i] = deadline;
                timerCount++;

                return timerIdForSlot(wheelIndex, i);
            }
        }

        final long[] newArray = Arrays.copyOf(array, array.length + 1);
        newArray[array.length] = deadline;

        wheel[wheelIndex] = newArray;
        timerCount++;

        return timerIdForSlot(wheelIndex, array.length);
    }

    /**
     * Cancel a previously scheduled timer.
     *
     * @param timerId of the timer to cancel.
     */
    public void cancelTimer(final long timerId)
    {
        final int wheelIndex = tickForTimerId(timerId);
        final int arrayIndex = indexInTickArray(timerId);

        final long[] array = wheel[wheelIndex];

        if (array[arrayIndex] != NO_TIMER_SCHEDULED)
        {
            array[arrayIndex] = NO_TIMER_SCHEDULED;
            timerCount--;
        }
    }

    /**
     * Expire timers that have been past their deadline.
     *
     * @param now               current time to compare deadlines against.
     * @param handler           to call for each expired timer.
     * @param maxTimersToExpire to process in one poll operation.
     * @return number of expired timers.
     */
    public int poll(final long now, final TimerHandler handler, final int maxTimersToExpire)
    {
        int timersExpired = 0;

        if (timerCount > 0)
        {
            final long[] array = wheel[currentTick & mask];

            for (int i = 0, length = array.length; i < length && maxTimersToExpire > timersExpired; i++)
            {
                final long deadline = array[i];

                if (deadline <= now)
                {
                    array[i] = NO_TIMER_SCHEDULED;
                    timerCount--;
                    timersExpired++;

                    if (!handler.onExpiry(timeUnit, now, timerIdForSlot(currentTick & mask, i)))
                    {
                        array[i] = deadline;
                        timerCount++;

                        return timersExpired;
                    }
                }
            }

            if (maxTimersToExpire > timersExpired && currentTickDeadline() <= now)
            {
                currentTick++;
            }
        }

        return timersExpired;
    }

    /**
     * Iterate over wheel and callback deadline and timerId for each active timer.
     *
     * @param consumer to call for each active timer.
     */
    public void forEach(final TimerConsumer consumer)
    {
        long numTimersLeft = timerCount;

        for (int j = currentTick, end = currentTick + wheel.length; j <= end; j++)
        {
            final long[] array = wheel[j & mask];

            for (int i = 0, length = array.length; i < length; i++)
            {
                final long deadline = array[i];

                if (deadline != NO_TIMER_SCHEDULED)
                {
                    consumer.accept(deadline, timerIdForSlot(j & mask, i));

                    if (--numTimersLeft == 0)
                    {
                        return;
                    }
                }
            }
        }
    }

    private static long timerIdForSlot(final int tickOnWheel, final int tickArrayIndex)
    {
        return ((long)tickOnWheel << 32) | tickArrayIndex;
    }

    private static int tickForTimerId(final long timerId)
    {
        return (int)(timerId >> 32);
    }

    private static int indexInTickArray(final long timerId)
    {
        return (int)timerId;
    }

    private static void checkTicksPerWheel(final int ticksPerWheel)
    {
        if (!BitUtil.isPowerOfTwo(ticksPerWheel))
        {
            throw new IllegalArgumentException("ticks per wheel must be a power of 2: " + ticksPerWheel);
        }
    }
}