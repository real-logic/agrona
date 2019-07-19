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

import sun.misc.Signal;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * One time barrier for blocking one or more threads until a SIGINT or SIGTERM signal is received from the operating
 * system or by programmatically calling {@link #signal()}. Useful for shutting down a service.
 */
public class ShutdownSignalBarrier
{
    /**
     * Signals the barrier will be registered for.
     */
    public static final String[] SIGNAL_NAMES = { "INT", "TERM" };
    private static final ArrayList<CountDownLatch> LATCHES = new ArrayList<>();

    static
    {
        for (final String signalName : SIGNAL_NAMES)
        {
            Signal.handle(new Signal(signalName), (signal) -> LATCHES.forEach(CountDownLatch::countDown));
        }
    }

    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * Construct and register the barrier ready for use.
     */
    public ShutdownSignalBarrier()
    {
        synchronized (LATCHES)
        {
            LATCHES.add(latch);
        }
    }

    /**
     * Programmatically signal awaiting threads.
     */
    public void signal()
    {
        synchronized (LATCHES)
        {
            LATCHES.remove(latch);
            latch.countDown();
        }
    }

    /**
     * Programmatically signal all awaiting threads.
     */
    public void signalAll()
    {
        synchronized (LATCHES)
        {
            LATCHES.forEach(CountDownLatch::countDown);
            LATCHES.clear();
        }
    }

    /**
     * Remove the barrier from the shutdown signals.
     */
    public void remove()
    {
        synchronized (LATCHES)
        {
            LATCHES.remove(latch);
        }
    }

    /**
     * Await the reception of the shutdown signal.
     */
    public void await()
    {
        try
        {
            latch.await();
        }
        catch (final InterruptedException ignore)
        {
            Thread.currentThread().interrupt();
        }
    }
}
