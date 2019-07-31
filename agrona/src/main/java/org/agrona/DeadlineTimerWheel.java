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
package org.agrona;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Timer Wheel for timers scheduled to expire on a deadline, (NOT thread safe).
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
 * array grows when needed, but does not shrink.
 * <p>
 * <b>Caveats</b>
 * <p>
 * Timers that expire in the same tick will not be ordered with one another. As ticks are
 * fairly coarse resolution normally, this means that some timers may expire out of order.
 * <p>
 * <b>Note:</b> Not threadsafe.
 */
public class DeadlineTimerWheel
{
    /**
     * Represents a deadline not set in the wheel.
     */
    public static final long NULL_DEADLINE = Long.MAX_VALUE;

    private static final int INITIAL_TICK_ALLOCATION = 16;

    private final TimeUnit timeUnit;
    private final int ticksPerWheel;
    private final int tickResolution;
    private final int tickMask;
    private final int resolutionBitsToShift;
    private int tickAllocation;
    private int allocationBitsToShift;
    private int currentTick;
    private int pollIndex;
    private long startTime;
    private long timerCount;

    private long[] wheel;

    /**
     * Handler for processing expired timers.
     *
     * @see DeadlineTimerWheel#poll(long, TimerHandler, int)
     */
    @FunctionalInterface
    public interface TimerHandler
    {
        /**
         * Called when the deadline has expired.
         *
         * @param timeUnit for the time.
         * @param now      for the expired timer.
         * @param timerId  for the expired timer.
         * @return true to consume the timer, or false to keep timer active and abort further polling.
         */
        boolean onTimerExpiry(TimeUnit timeUnit, long now, long timerId);
    }

    /**
     * Consumer of timer entries as deadline to timerId.
     *
     * @see DeadlineTimerWheel#forEach(TimerConsumer)
     */
    @FunctionalInterface
    public interface TimerConsumer
    {
        void accept(long deadline, long timerId);
    }

    /**
     * Construct timer wheel and configure timing with default initial allocation.
     *
     * @param timeUnit       for the values used to express the time.
     * @param startTime      for the wheel (in given {@link TimeUnit}).
     * @param tickResolution for the wheel, i.e. how many {@link TimeUnit}s per tick.
     * @param ticksPerWheel  or spokes, for the wheel (must be power of 2).
     */
    public DeadlineTimerWheel(
        final TimeUnit timeUnit, final long startTime, final int tickResolution, final int ticksPerWheel)
    {
        this(timeUnit, startTime, tickResolution, ticksPerWheel, INITIAL_TICK_ALLOCATION);
    }

    /**
     * Construct timer wheel and configure timing with provided initial allocation.
     *
     * @param timeUnit              for the values used to express the time.
     * @param startTime             for the wheel (in given {@link TimeUnit}).
     * @param tickResolution        for the wheel, i.e. how many {@link TimeUnit}s per tick.
     * @param ticksPerWheel         or spokes, for the wheel (must be power of 2).
     * @param initialTickAllocation space allocated per tick of the wheel (must be power of 2).
     */
    public DeadlineTimerWheel(
        final TimeUnit timeUnit,
        final long startTime,
        final int tickResolution,
        final int ticksPerWheel,
        final int initialTickAllocation)
    {
        checkTicksPerWheel(ticksPerWheel);
        checkResolution(tickResolution);
        checkInitialTickAllocation(initialTickAllocation);

        this.timeUnit = timeUnit;
        this.ticksPerWheel = ticksPerWheel;
        this.tickAllocation = initialTickAllocation;
        this.tickMask = ticksPerWheel - 1;
        this.tickResolution = tickResolution;
        this.resolutionBitsToShift = Integer.numberOfTrailingZeros(tickResolution);
        this.allocationBitsToShift = Integer.numberOfTrailingZeros(initialTickAllocation);
        this.startTime = startTime;

        wheel = new long[ticksPerWheel * initialTickAllocation];
        Arrays.fill(wheel, NULL_DEADLINE);
    }

    /**
     * Time unit for the time values.
     *
     * @return time unit for the ticks.
     */
    public TimeUnit timeUnit()
    {
        return timeUnit;
    }

    /**
     * Resolution of a tick of the wheel in {@link #timeUnit()}s.
     *
     * @return resolution of a tick of the wheel in {@link #timeUnit()}s.
     */
    public long tickResolution()
    {
        return tickResolution;
    }

    /**
     * The number of ticks, or spokes, per wheel.
     *
     * @return number of ticks, or spokes, per wheel.
     */
    public int ticksPerWheel()
    {
        return ticksPerWheel;
    }

    /**
     * The start time tick for the wheel from which it advances.
     *
     * @return start time tick for the wheel from which it advances.
     */
    public long startTime()
    {
        return startTime;
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
     * Reset the start time of the wheel.
     *
     * @param startTime to set the wheel to.
     * @throws IllegalStateException if wheel has any active timers.
     */
    public void resetStartTime(final long startTime)
    {
        if (timerCount > 0)
        {
            throw new IllegalStateException("can not reset startTime with active timers");
        }

        this.startTime = startTime;
        this.currentTick = 0;
        this.pollIndex = 0;
    }

    /**
     * Time of current tick of the wheel in {@link #timeUnit()}s.
     *
     * @return time of the current tick of the wheel in {@link #timeUnit()}s.
     */
    public long currentTickTime()
    {
        return ((currentTick + 1L) << resolutionBitsToShift) + startTime;
    }

    /**
     * Set the current tick of the wheel to examine on the next {@link #poll}.
     *
     * If the time passed in is less than the current time, nothing is changed.
     * No timers will be expired when winding forward and thus are still in the wheel and will be expired as
     * encountered in the wheel during {@link #poll} operations. No guarantee of order for expired timers is
     * assumed when later polled.
     *
     * @param now current time to advance to or stay at current time.
     */
    public void currentTickTime(final long now)
    {
        currentTick = (int)Math.max((now - startTime) >> resolutionBitsToShift, currentTick);
    }

    /**
     * Clear out all active timers in the wheel.
     */
    public void clear()
    {
        long remainingTimers = timerCount;
        if (0 == remainingTimers)
        {
            return;
        }

        for (int i = 0, length = wheel.length; i < length; i++)
        {
            if (NULL_DEADLINE != wheel[i])
            {
                wheel[i] = NULL_DEADLINE;

                if (--remainingTimers <= 0)
                {
                    timerCount = 0;
                    return;
                }
            }
        }
    }

    /**
     * Schedule a timer for a given absolute time as a deadline in {@link #timeUnit()}s. A timerId will be assigned
     * and returned for future reference.
     *
     * @param deadline after which the timer should expire.
     * @return timerId for the scheduled timer.
     */
    public long scheduleTimer(final long deadline)
    {
        final long deadlineTick = Math.max((deadline - startTime) >> resolutionBitsToShift, currentTick);
        final int spokeIndex = (int)(deadlineTick & tickMask);
        final int tickStartIndex = spokeIndex << allocationBitsToShift;

        for (int i = 0; i < tickAllocation; i++)
        {
            final int index = tickStartIndex + i;

            if (NULL_DEADLINE == wheel[index])
            {
                wheel[index] = deadline;
                timerCount++;

                return timerIdForSlot(spokeIndex, i);
            }
        }

        return increaseCapacity(deadline, spokeIndex);
    }

    /**
     * Cancel a previously scheduled timer.
     *
     * @param timerId of the timer to cancel.
     * @return true if successful otherwise false if the timerId did not exist.
     */
    public boolean cancelTimer(final long timerId)
    {
        final int spokeIndex = tickForTimerId(timerId);
        final int tickIndex = indexInTickArray(timerId);
        final int wheelIndex = (spokeIndex << allocationBitsToShift) + tickIndex;

        if (spokeIndex < ticksPerWheel)
        {
            if (tickIndex < tickAllocation && NULL_DEADLINE != wheel[wheelIndex])
            {
                wheel[wheelIndex] = NULL_DEADLINE;
                timerCount--;

                return true;
            }
        }

        return false;
    }

    /**
     * Poll for timers expired by the deadline passing.
     *
     * @param now         current time to compare deadlines against.
     * @param handler     to call for each expired timer.
     * @param expiryLimit to process in one poll operation.
     * @return number of expired timers.
     */
    public int poll(final long now, final TimerHandler handler, final int expiryLimit)
    {
        int timersExpired = 0;

        if (timerCount > 0)
        {
            final int spokeIndex = currentTick & tickMask;

            for (int i = 0, length = tickAllocation; i < length && expiryLimit > timersExpired; i++)
            {
                final int wheelIndex = (spokeIndex << allocationBitsToShift) + pollIndex;
                final long deadline = wheel[wheelIndex];

                if (deadline <= now)
                {
                    wheel[wheelIndex] = NULL_DEADLINE;
                    timerCount--;
                    timersExpired++;

                    if (!handler.onTimerExpiry(timeUnit, now, timerIdForSlot(spokeIndex, pollIndex)))
                    {
                        wheel[wheelIndex] = deadline;
                        timerCount++;

                        return --timersExpired;
                    }
                }

                pollIndex = (pollIndex + 1) >= length ? 0 : (pollIndex + 1);
            }

            if (expiryLimit > timersExpired && currentTickTime() <= now)
            {
                currentTick++;
                pollIndex = 0;
            }
            else if (pollIndex >= tickAllocation)
            {
                pollIndex = 0;
            }
        }
        else if (currentTickTime() <= now)
        {
            currentTick++;
            pollIndex = 0;
        }

        return timersExpired;
    }

    /**
     * Iterate over wheel so all active timers can be consumed without expiring them.
     *
     * @param consumer to call for each active timer.
     */
    public void forEach(final TimerConsumer consumer)
    {
        long timersRemaining = timerCount;

        for (int i = 0, length = wheel.length; i < length; i++)
        {
            final long deadline = wheel[i];

            if (NULL_DEADLINE != deadline)
            {
                consumer.accept(deadline, timerIdForSlot(i >> allocationBitsToShift, i & tickMask));

                if (--timersRemaining <= 0)
                {
                    return;
                }
            }
        }
    }

    /**
     * Get the deadline for the given timerId.
     *
     * @param timerId of the timer to return the deadline of.
     * @return deadline for the given timerId or {@link #NULL_DEADLINE} if timerId is not running.
     */
    public long deadline(final long timerId)
    {
        final int spokeIndex = tickForTimerId(timerId);
        final int tickIndex = indexInTickArray(timerId);
        final int wheelIndex = (spokeIndex << allocationBitsToShift) + tickIndex;

        if (spokeIndex < ticksPerWheel)
        {
            if (tickIndex < tickAllocation)
            {
                return wheel[wheelIndex];
            }
        }

        return NULL_DEADLINE;
    }

    private long increaseCapacity(final long deadline, final int spokeIndex)
    {
        final int newTickAllocation = this.tickAllocation << 1;
        final int newAllocationBitsToShift = Integer.numberOfTrailingZeros(newTickAllocation);

        final long newCapacity = (long)ticksPerWheel * newTickAllocation;
        if (newCapacity > 1 << 30)
        {
            throw new IllegalStateException("max capacity reached at tickAllocation=" + tickAllocation);
        }

        final long[] newWheel = new long[(int)newCapacity];
        Arrays.fill(newWheel, NULL_DEADLINE);

        for (int j = 0; j < ticksPerWheel; j++)
        {
            final int oldTickStartIndex = j << allocationBitsToShift;
            final int newTickStartIndex = j << newAllocationBitsToShift;
            System.arraycopy(wheel, oldTickStartIndex, newWheel, newTickStartIndex, tickAllocation);
        }

        newWheel[(spokeIndex << newAllocationBitsToShift) + tickAllocation] = deadline;
        final long timerId = timerIdForSlot(spokeIndex, tickAllocation);
        timerCount++;

        tickAllocation = newTickAllocation;
        allocationBitsToShift = newAllocationBitsToShift;
        wheel = newWheel;

        return timerId;
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

    private static void checkResolution(final int tickResolution)
    {
        if (!BitUtil.isPowerOfTwo(tickResolution))
        {
            throw new IllegalArgumentException("tick resolution must be a power of 2: " + tickResolution);
        }
    }

    private static void checkInitialTickAllocation(final int tickAllocation)
    {
        if (!BitUtil.isPowerOfTwo(tickAllocation))
        {
            throw new IllegalArgumentException("tick allocation must be a power of 2: " + tickAllocation);
        }
    }
}
