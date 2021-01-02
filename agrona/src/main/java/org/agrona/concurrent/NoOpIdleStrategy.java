/*
 * Copyright 2014-2021 Real Logic Limited.
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

/**
 * Low-latency idle strategy to be employed in loops that do significant work on each iteration such that any
 * work in the idle strategy would be wasteful.
 */
public final class NoOpIdleStrategy implements IdleStrategy
{
    /**
     * Name to be returned from {@link #alias()}.
     */
    public static final String ALIAS = "noop";

    /**
     * As there is no instance state then this object can be used to save on allocation.
     */
    public static final NoOpIdleStrategy INSTANCE = new NoOpIdleStrategy();

    /**
     * <b>Note</b>: this implementation will result in no safepoint poll once inlined.
     *
     *  {@inheritDoc}
     */
    public void idle(final int workCount)
    {
    }

    /**
     * <b>Note</b>: this implementation will result in no safepoint poll once inlined.
     *
     *  {@inheritDoc}
     */
    public void idle()
    {
    }

    /**
     *  {@inheritDoc}
     */
    public void reset()
    {
    }

    /**
     *  {@inheritDoc}
     */
    public String alias()
    {
        return ALIAS;
    }

    public String toString()
    {
        return "NoOpIdleStrategy{alias=" + ALIAS + "}";
    }
}
