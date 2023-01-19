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

import org.agrona.LangUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
    void isThreadSafe() throws InterruptedException
    {
        final int numThreads = 2;
        final ArrayList<Thread> threads = new ArrayList<>();
        final CountDownLatch start = new CountDownLatch(numThreads);
        final CountDownLatch end = new CountDownLatch(numThreads);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        for (int i = 0; i < numThreads; i++)
        {
            final Thread t = new Thread(() ->
            {
                start.countDown();
                try
                {
                    start.await();

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
                    error.getAndUpdate(existing ->
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
                finally
                {
                    end.countDown();
                }
            });
            threads.add(t);
            t.start();
        }

        end.await();

        if (null != error.get())
        {
            LangUtil.rethrowUnchecked(error.get());
        }
    }
}
