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

import java.util.Objects;

/**
 * Utility to allow the registration of a SIGINT handler that hides the unsupported {@link sun.misc.Signal} class.
 */
public final class SigInt
{
    private SigInt()
    {
    }

    /**
     * Register a task to be run when a SIGINT is received.
     *
     * @param task to run on reception of the signal.
     */
    public static void register(final Runnable task)
    {
        register("INT", task);
    }

    static void register(final String signalName, final Runnable task)
    {
        Objects.requireNonNull(task);
        sun.misc.Signal.handle(new sun.misc.Signal(signalName), (signal) -> task.run());
    }
}
