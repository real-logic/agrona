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

import java.util.concurrent.CountDownLatch;

/**
 * One time barrier for blocking one or more threads until a SIGINT signal is received from the operating system
 * or by programmatically calling {@link #signal()}.
 */
public class SigIntBarrier
{
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * Construct and register the barrier ready for use.
     */
    public SigIntBarrier()
    {
        SigInt.register(this::signal);
    }

    /**
     * Programmatically signal awaiting threads.
     */
    public void signal()
    {
        latch.countDown();
    }

    /**
     * Await the reception of the SIGINT signal.
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

