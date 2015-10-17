/*
 * Copyright 2015 Real Logic Ltd.
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
package uk.co.real_logic.agrona.concurrent;

/**
 * Low-latency idle strategy to be employed in loops that do significant work on each iteration such that any work in the
 * idle strategy would be wasteful.
 */
public class NoOpIdleStrategy implements IdleStrategy
{
    /**
     * If the caller of this {@link IdleStrategy} spins in a 'counted' loop this may cause a TTSP (Time To SafePoint) problem.
     * If this is the case for a particular application you can solve it by preventing this method from being inlined
     * by using a Hotspot compiler command,
     * -XX:CompileCommand=dontinline,uk.co.real_logic.agrona.concurrent.NoOpIdleStrategy::idle
     *
     * @see uk.co.real_logic.agrona.concurrent.IdleStrategy#idle(int)
     */
    public void idle(final int workCount)
    {
    }
}
