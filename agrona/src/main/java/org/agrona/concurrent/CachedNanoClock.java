/*
 * Copyright 2014-2017 Real Logic Ltd.
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

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * A {@link NanoClock} that caches a timestamp which can be updated with {@link #update(long)}.
 *
 * Instances are threadsafe with the read being volatile.
 */
public class CachedNanoClock implements NanoClock
{
    private static final AtomicLongFieldUpdater<CachedNanoClock> TIME_UPDATER =
        AtomicLongFieldUpdater.newUpdater(CachedNanoClock.class, "timeNs");

    private volatile long timeNs;

    public long nanoTime()
    {
        return timeNs;
    }

    /**
     * Update the value of the timestamp.
     *
     * @param timeNs value to update the timestamp.
     */
    public void update(final long timeNs)
    {
        TIME_UPDATER.lazySet(this, timeNs);
    }
}
