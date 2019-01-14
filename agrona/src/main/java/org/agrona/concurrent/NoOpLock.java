/*
 * Copyright 2014-2019 Real Logic Ltd.
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
package org.agrona.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * A {@link Lock} implementation that is a no operation, i.e. it effectively does nothing.
 * <p>
 * Useful for effectively eliding a lock in a single threaded environment.
 */
public final class NoOpLock implements Lock
{
    /**
     * Proceeds as if the lock has been acquired.
     */
    public void lock()
    {
    }

    /**
     * Proceeds as if the lock has been acquired.
     */
    public void lockInterruptibly()
    {
    }

    /**
     * Always succeeds.
     *
     * @return always true.
     */
    public boolean tryLock()
    {
        return true;
    }

    /**
     * Always succeeds.
     *
     * @return always true.
     */
    public boolean tryLock(final long time, final TimeUnit unit)
    {
        return true;
    }

    /**
     * The lock has never been taken so no effect.
     */
    public void unlock()
    {
    }

    /**
     * Not supported.
     *
     * @return never returns.
     * @throws UnsupportedOperationException always.
     */
    public Condition newCondition()
    {
        throw new UnsupportedOperationException();
    }
}
