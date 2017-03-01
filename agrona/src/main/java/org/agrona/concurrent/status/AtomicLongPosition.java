/*
 *  Copyright 2014-2017 Real Logic Ltd.
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
package org.agrona.concurrent.status;

import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link Position} that is backed by an {@link AtomicLong} that is useful for tests.
 */
public class AtomicLongPosition implements Position
{
    private boolean isClosed = false;
    private final AtomicLong value = new AtomicLong();

    public boolean isClosed()
    {
        return isClosed;
    }

    public int id()
    {
        return 0;
    }

    public long get()
    {
        return value.get();
    }

    public long getVolatile()
    {
        return value.get();
    }

    public void set(final long value)
    {
        this.value.lazySet(value);
    }

    public void setOrdered(final long value)
    {
        this.value.lazySet(value);
    }

    public boolean proposeMax(final long proposedValue)
    {
        return proposeMaxOrdered(proposedValue);
    }

    public boolean proposeMaxOrdered(final long proposedValue)
    {
        boolean updated = false;

        if (get() < proposedValue)
        {
            setOrdered(proposedValue);
            updated = true;
        }

        return updated;
    }

    public void close()
    {
        isClosed = true;
    }
}
