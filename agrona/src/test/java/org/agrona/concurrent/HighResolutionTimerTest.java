/*
 * Copyright 2014-2024 Real Logic Limited.
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

import org.agrona.LangUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighResolutionTimerTest
{
    @Test
    void shouldEnableHighResolutionTimers()
    {
        assertFalse(HighResolutionTimer.isEnabled());

        HighResolutionTimer.enable();
        assertTrue(HighResolutionTimer.isEnabled());

        HighResolutionTimer.disable();
        assertFalse(HighResolutionTimer.isEnabled());
    }

    @Test
    @Timeout(30)
    void shouldBeThreadSafe() throws InterruptedException
    {
        final int threadCount = 2;
        final CountDownLatch startLatch = new CountDownLatch(threadCount);
        final Thread[] threads = new Thread[threadCount];
        final AtomicReference<Throwable> errorRef = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++)
        {
            final Thread t = new Thread(
                () ->
                {
                    startLatch.countDown();

                    try
                    {
                        startLatch.await();

                        for (int j = 0; j < 1000; j++)
                        {
                            if (HighResolutionTimer.isEnabled())
                            {
                                HighResolutionTimer.disable();
                            }
                            else
                            {
                                HighResolutionTimer.enable();
                            }
                        }
                    }
                    catch (final Throwable e)
                    {
                        errorRef.getAndUpdate(
                            (existing) ->
                            {
                                if (null != existing)
                                {
                                    existing.addSuppressed(e);
                                    return existing;
                                }
                                else
                                {
                                    return e;
                                }
                            });
                    }
                });

            threads[i] = t;
            t.setName("high-res-timer-runner");
            t.setDaemon(true);
            t.start();
        }

        for (final Thread t : threads)
        {
            t.join();
        }

        if (null != errorRef.get())
        {
            LangUtil.rethrowUnchecked(errorRef.get());
        }
    }
}
