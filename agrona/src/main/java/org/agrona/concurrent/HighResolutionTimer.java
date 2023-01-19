/*
 * Copyright 2014-2023 Real Logic Limited.
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

import java.util.concurrent.atomic.AtomicReference;

/**
 * Control the use of high resolution timers on Windows by a bit of hackery.
 */
public class HighResolutionTimer
{
    private static final AtomicReference<Thread> THREAD = new AtomicReference<>();

    /**
     * Has the high resolution timer been enabled?
     *
     * @return true if we believe it is enabled otherwise false.
     */
    public static boolean isEnabled()
    {
        return null != THREAD.get();
    }

    /**
     * Attempt to enable high resolution timers.
     */
    public static void enable()
    {
        if (null == THREAD.get())
        {
            final Thread t = new Thread(HighResolutionTimer::run);
            if (THREAD.compareAndSet(null, t))
            {
                t.setDaemon(true);
                t.setName("high-resolution-timer-hack");
                t.start();
            }
        }
    }

    /**
     * Attempt to disable the high resolution timers.
     */
    public static void disable()
    {
        final Thread thread = THREAD.getAndSet(null);
        if (null != thread)
        {
            thread.interrupt();
        }
    }

    private static void run()
    {
        try
        {
            Thread.sleep(Long.MAX_VALUE);
        }
        catch (final InterruptedException ignore)
        {
        }

        THREAD.set(null);
    }
}
