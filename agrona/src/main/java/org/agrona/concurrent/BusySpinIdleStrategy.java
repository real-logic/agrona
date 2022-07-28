/*
 * Copyright 2014-2022 Real Logic Limited.
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

import org.agrona.hints.ThreadHints;

/**
 * Busy spin strategy targeted at lowest possible latency. This strategy will monopolise a thread to achieve the lowest
 * possible latency. Useful for creating bubbles in the execution pipeline of tight busy spin loops with no other logic
 * than status checks on progress.
 */
public final class BusySpinIdleStrategy implements IdleStrategy
{
    /**
     * Name to be returned from {@link #alias()}.
     */
    public static final String ALIAS = "spin";

    /**
     * As there is no instance state then this object can be used to save on allocation.
     */
    public static final BusySpinIdleStrategy INSTANCE = new BusySpinIdleStrategy();

    /**
     * <b>Note</b>: this implementation will result in no safepoint poll once inlined.
     * <p>
     * {@inheritDoc}
     */
    public void idle(final int workCount)
    {
        if (workCount > 0)
        {
            return;
        }

        ThreadHints.onSpinWait();
    }

    /**
     * {@inheritDoc}
     */
    public void idle()
    {
        ThreadHints.onSpinWait();
    }

    /**
     * {@inheritDoc}
     */
    public void reset()
    {
    }

    /**
     * {@inheritDoc}
     */
    public String alias()
    {
        return ALIAS;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "BusySpinIdleStrategy{alias=" + ALIAS + "}";
    }
}
