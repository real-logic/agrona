/*
 * Copyright 2014-2025 Real Logic Limited.
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
package org.agrona.concurrent.status;

import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link Position} that is backed by an {@link AtomicLong} which is useful for tests.
 */
public class AtomicLongPosition extends Position
{
    private boolean isClosed = false;
    private final int id;
    private final AtomicLong value;

    /**
     * Default constructor.
     */
    public AtomicLongPosition()
    {
        this(0, 0L);
    }

    /**
     * Create a position with a given id and zero as an initial value.
     *
     * @param id to be assigned.
     */
    public AtomicLongPosition(final int id)
    {
        this(id, 0L);
    }

    /**
     * Create a position with a given id and an initial value.
     *
     * @param id           to be assigned.
     * @param initialValue to be assigned.
     */
    public AtomicLongPosition(final int id, final long initialValue)
    {
        this.id = id;
        this.value = new AtomicLong(initialValue);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed()
    {
        return isClosed;
    }

    /**
     * {@inheritDoc}
     */
    public int id()
    {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public long get()
    {
        return value.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getVolatile()
    {
        return value.get();
    }

    @Override
    public long getAcquire()
    {
        return value.getAcquire();
    }

    /**
     * {@inheritDoc}
     */
    public void set(final long value)
    {
        this.value.lazySet(value);
    }

    /**
     * {@inheritDoc}
     */
    public void setOrdered(final long value)
    {
        setRelease(value);
    }

    /**
     * {@inheritDoc}
     */
    public void setRelease(final long value)
    {
        this.value.setRelease(value);
    }

    /**
     * {@inheritDoc}
     */
    public void setVolatile(final long value)
    {
        this.value.set(value);
    }

    /**
     * {@inheritDoc}
     */
    public boolean proposeMax(final long proposedValue)
    {
        return proposeMaxRelease(proposedValue);
    }

    /**
     * {@inheritDoc}
     */
    public boolean proposeMaxOrdered(final long proposedValue)
    {
        return proposeMaxRelease(proposedValue);
    }

    /**
     * {@inheritDoc}
     */
    public boolean proposeMaxRelease(final long proposedValue)
    {
        boolean updated = false;

        if (value.get() < proposedValue)
        {
            value.setRelease(proposedValue);
            updated = true;
        }

        return updated;
    }

    /**
     * {@inheritDoc}
     */
    public void close()
    {
        isClosed = true;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "AtomicLongPosition{" +
            "isClosed=" + isClosed() +
            ", id=" + id +
            ", value=" + (isClosed() ? -1 : value) +
            '}';
    }
}
