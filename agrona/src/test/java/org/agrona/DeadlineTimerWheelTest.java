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

import org.agrona.collections.MutableLong;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class DeadlineTimerWheelTest
{
    @Test(expected = IllegalArgumentException.class)
    public void shouldExceptionOnNonPowerOf2TicksPerWheel()
    {
        new DeadlineTimerWheel(TimeUnit.NANOSECONDS, 0, 16, 10);
    }

    @Test(timeout = 1000)
    public void shouldBeAbleToScheduleTimerOnEdgeOfTick()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 1024);

        final long id = wheel.scheduleTimer(5 * wheel.tickInterval());

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    assertThat(timerId, is(id));
                    firedTimestamp.value = now;
                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp.value);

        // this is the first tick after the timer, so it should be on this edge
        assertThat(firedTimestamp.value, is(TimeUnit.MILLISECONDS.toNanos(6)));
    }

    @Test(timeout = 1000)
    public void shouldHandleNonZeroStartTime()
    {
        long controlTimestamp = TimeUnit.MILLISECONDS.toNanos(100);
        final MutableLong firedTimestamp = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 1024);

        final long id = wheel.scheduleTimer(controlTimestamp + (5 * wheel.tickInterval()));

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    assertThat(timerId, is(id));
                    firedTimestamp.value = now;
                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp.value);

        // this is the first tick after the timer, so it should be on this edge
        assertThat(firedTimestamp.value, is(TimeUnit.MILLISECONDS.toNanos(106)));
    }

    @Test(timeout = 1000)
    public void shouldHandleNanoTimeUnitTimers()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 1024);

        final long id = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(5) + 1);

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    assertThat(timerId, is(id));
                    firedTimestamp.value = now;
                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp.value);

        // this is the first tick after the timer, so it should be on this edge
        assertThat(firedTimestamp.value, is(TimeUnit.MILLISECONDS.toNanos(6)));
    }


    @Test(timeout = 1000)
    public void shouldHandleMultipleRounds()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 16);

        final long id = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(63));

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    assertThat(timerId, is(id));
                    firedTimestamp.value = now;
                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp.value);

        // this is the first tick after the timer, so it should be on this edge
        assertThat(firedTimestamp.value, is(TimeUnit.MILLISECONDS.toNanos(64)));
    }

    @Test(timeout = 1000)
    public void shouldBeAbleToCancelTimer()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 256);

        final long id = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(63));

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    assertThat(timerId, is(id));
                    firedTimestamp.value = now;
                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp.value && controlTimestamp < TimeUnit.MILLISECONDS.toNanos(16));

        wheel.cancelTimer(id);

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    firedTimestamp.value = now;
                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp.value && controlTimestamp < TimeUnit.MILLISECONDS.toNanos(128));

        assertThat(firedTimestamp.value, is(-1L));
    }

    @Test(timeout = 1000)
    public void shouldHandleExpiringTimersInPreviousTicks()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 256);

        final long id = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));

        final long pollStartTimeNs = TimeUnit.MILLISECONDS.toNanos(32);
        controlTimestamp += pollStartTimeNs;

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    assertThat(timerId, is(id));
                    firedTimestamp.value = now;
                    return true;
                },
                Integer.MAX_VALUE);

            if (wheel.currentTickDeadline() > pollStartTimeNs)
            {
                controlTimestamp += wheel.tickInterval();
            }
        }
        while (-1 == firedTimestamp.value && controlTimestamp < TimeUnit.MILLISECONDS.toNanos(128));

        assertThat(firedTimestamp.value, is(pollStartTimeNs));
    }

    @Test(timeout = 1000)
    public void shouldHandleMultipleTimersInDifferentTicks()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp1 = new MutableLong(-1);
        final MutableLong firedTimestamp2 = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 256);

        final long id1 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));
        final long id2 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(23));

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    if (timerId == id1)
                    {
                        firedTimestamp1.value = now;
                    }
                    else if (timerId == id2)
                    {
                        firedTimestamp2.value = now;
                    }

                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

        assertThat(firedTimestamp1.value, is(TimeUnit.MILLISECONDS.toNanos(16)));
        assertThat(firedTimestamp2.value, is(TimeUnit.MILLISECONDS.toNanos(24)));
    }

    @Test(timeout = 1000)
    public void shouldHandleMultipleTimersInSameTickSameRound()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp1 = new MutableLong(-1);
        final MutableLong firedTimestamp2 = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 8);

        final long id1 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));
        final long id2 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    if (timerId == id1)
                    {
                        firedTimestamp1.value = now;
                    }
                    else if (timerId == id2)
                    {
                        firedTimestamp2.value = now;
                    }

                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

        assertThat(firedTimestamp1.value, is(TimeUnit.MILLISECONDS.toNanos(16)));
        assertThat(firedTimestamp2.value, is(TimeUnit.MILLISECONDS.toNanos(16)));
    }

    @Test(timeout = 1000)
    public void shouldHandleMultipleTimersInSameTickDifferentRound()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp1 = new MutableLong(-1);
        final MutableLong firedTimestamp2 = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 8);

        final long id1 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));
        final long id2 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(23));

        do
        {
            wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    if (timerId == id1)
                    {
                        firedTimestamp1.value = now;
                    }
                    else if (timerId == id2)
                    {
                        firedTimestamp2.value = now;
                    }

                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

        assertThat(firedTimestamp1.value, is(TimeUnit.MILLISECONDS.toNanos(16)));
        assertThat(firedTimestamp2.value, is(TimeUnit.MILLISECONDS.toNanos(24)));
    }

    @Test(timeout = 1000)
    public void shouldLimitExpiringTimers()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp1 = new MutableLong(-1);
        final MutableLong firedTimestamp2 = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 8);

        final long id1 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));
        final long id2 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));

        int numExpired = 0;

        do
        {
            numExpired += wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    assertThat(timerId, is(id1));
                    firedTimestamp1.value = now;
                    return true;
                },
                1);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp1.value && -1 == firedTimestamp2.value);

        assertThat(numExpired, is(1));

        do
        {
            numExpired += wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    assertThat(timerId, is(id2));
                    firedTimestamp2.value = now;
                    return true;
                },
                1);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp1.value && -1 == firedTimestamp2.value);

        assertThat(numExpired, is(2));

        assertThat(firedTimestamp1.value, is(TimeUnit.MILLISECONDS.toNanos(16)));
        assertThat(firedTimestamp2.value, is(TimeUnit.MILLISECONDS.toNanos(16) + wheel.tickInterval()));
    }

    @Test(timeout = 1000)
    public void shouldHandleFalseReturnToExpireTimerAgain()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp1 = new MutableLong(-1);
        final MutableLong firedTimestamp2 = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 8);

        final long id1 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));
        final long id2 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));

        int numExpired = 0;

        do
        {
            numExpired += wheel.poll(
                controlTimestamp,
                (timeUnit, now, timerId) ->
                {
                    if (timerId == id1)
                    {
                        if (-1 == firedTimestamp1.value)
                        {
                            firedTimestamp1.value = now;
                            return false;
                        }

                        firedTimestamp1.value = now;
                    }
                    else if (timerId == id2)
                    {
                        firedTimestamp2.value = now;
                    }

                    return true;
                },
                Integer.MAX_VALUE);

            controlTimestamp += wheel.tickInterval();
        }
        while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

        assertThat(firedTimestamp1.value, is(TimeUnit.MILLISECONDS.toNanos(17)));
        assertThat(firedTimestamp2.value, is(TimeUnit.MILLISECONDS.toNanos(17)));
        assertThat(numExpired, is(3));
    }

    @Test(timeout = 1000)
    public void shouldCopeWithExceptionFromHandler()
    {
        long controlTimestamp = 0;
        final MutableLong firedTimestamp1 = new MutableLong(-1);
        final MutableLong firedTimestamp2 = new MutableLong(-1);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 8);

        final long id1 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));
        final long id2 = wheel.scheduleTimer(controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15));

        int numExpired = 0;
        Exception e = null;
        do
        {
            try
            {
                numExpired += wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        if (timerId == id1)
                        {
                            firedTimestamp1.value = now;
                            throw new IllegalStateException();
                        }
                        else if (timerId == id2)
                        {
                            firedTimestamp2.value = now;
                        }

                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickInterval();
            }
            catch (final Exception ex)
            {
                e = ex;
            }
        }
        while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

        assertThat(firedTimestamp1.value, is(TimeUnit.MILLISECONDS.toNanos(16)));
        assertThat(firedTimestamp2.value, is(TimeUnit.MILLISECONDS.toNanos(16)));
        assertThat(numExpired, is((1)));
        assertThat(wheel.timerCount(), is(0L));
        assertNotNull(e);
    }

    @Test(timeout = 1000)
    public void shouldBeAbleToIterateOverTimers()
    {
        final long controlTimestamp = 0;
        final long deadline1 = controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15);
        final long deadline2 = controlTimestamp + TimeUnit.MILLISECONDS.toNanos(15 + 7);
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TimeUnit.NANOSECONDS, controlTimestamp, TimeUnit.MILLISECONDS.toNanos(1), 8);

        final long id1 = wheel.scheduleTimer(deadline1);
        final long id2 = wheel.scheduleTimer(deadline2);

        final DeadlineTimerWheel.TimerHandler handler = mock(DeadlineTimerWheel.TimerHandler.class);

        when(handler.onExpiry(any(TimeUnit.class), anyLong(), anyLong())).thenReturn(true);
        wheel.forEach(handler);
        verify(handler).onExpiry(any(TimeUnit.class), eq(deadline1), eq(id1));
        verify(handler).onExpiry(any(TimeUnit.class), eq(deadline2), eq(id2));
        verifyNoMoreInteractions(handler);
    }
}
