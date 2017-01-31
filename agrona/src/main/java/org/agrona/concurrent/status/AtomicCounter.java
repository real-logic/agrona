/*
 * Copyright 2014 - 2016 Real Logic Ltd.
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

import org.agrona.UnsafeAccess;
import org.agrona.concurrent.AtomicBuffer;

import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * Atomic counter that is backed by an {@link AtomicBuffer} that can be read across threads and processes.
 */
public class AtomicCounter implements AutoCloseable
{
    private boolean isClosed = false;
    private final int counterId;
    private final long addressOffset;
    private final byte[] buffer;
    private final CountersManager countersManager;

    AtomicCounter(final AtomicBuffer buffer, final int counterId, final CountersManager countersManager)
    {
        this.counterId = counterId;
        this.countersManager = countersManager;
        this.buffer = buffer.byteArray();

        final int counterOffset = CountersManager.counterOffset(counterId);
        buffer.boundsCheck(counterOffset, SIZE_OF_LONG);
        this.addressOffset = buffer.addressOffset() + counterOffset;
    }

    /**
     * Has this counter been closed?
     *
     * @return true if this counter has already been closed.
     */
    public boolean isClosed()
    {
        return isClosed;
    }

    /**
     * Perform an atomic increment that will not lose updates across threads.
     *
     * @return the previous value of the counter
     */
    public long increment()
    {
        return UnsafeAccess.UNSAFE.getAndAddLong(buffer, addressOffset, 1);
    }

    /**
     * Perform an atomic increment that is not safe across threads.
     *
     * @return the previous value of the counter
     */
    public long orderedIncrement()
    {
        final long currentValue = UnsafeAccess.UNSAFE.getLong(buffer, addressOffset);
        UnsafeAccess.UNSAFE.putOrderedLong(buffer, addressOffset, currentValue + 1);

        return currentValue;
    }

    /**
     * Set the counter with volatile semantics.
     *
     * @param value to be set with volatile semantics.
     */
    public void set(final long value)
    {
        UnsafeAccess.UNSAFE.putLongVolatile(buffer, addressOffset, value);
    }

    /**
     * Set the counter with ordered semantics.
     *
     * @param value to be set with ordered semantics.
     */
    public void setOrdered(final long value)
    {
        UnsafeAccess.UNSAFE.putOrderedLong(buffer, addressOffset, value);
    }

    /**
     * Set the counter with normal semantics.
     *
     * @param value to be set with normal semantics.
     */
    public void setWeak(final long value)
    {
        UnsafeAccess.UNSAFE.putLong(buffer, addressOffset, value);
    }

    /**
     * Add an increment to the counter that will not lose updates across threads.
     *
     * @param increment to be added.
     * @return the previous value of the counter
     */
    public long add(final long increment)
    {
        return UnsafeAccess.UNSAFE.getAndAddLong(buffer, addressOffset, increment);
    }

    /**
     * Add an increment to the counter with ordered store semantics.
     *
     * @param increment to be added with ordered store semantics.
     * @return the previous value of the counter
     */
    public long addOrdered(final long increment)
    {
        final long currentValue = UnsafeAccess.UNSAFE.getLong(buffer, addressOffset);
        UnsafeAccess.UNSAFE.putOrderedLong(buffer, addressOffset, currentValue + increment);

        return currentValue;
    }

    /**
     * Get the latest value for the counter.
     *
     * @return the latest value for the counter.
     */
    public long get()
    {
        return UnsafeAccess.UNSAFE.getLongVolatile(buffer, addressOffset);
    }

    /**
     * Get the value of the counter using weak ordering semantics. This is the same a standard read of a field.
     *
     * @return the  value for the counter.
     */
    public long getWeak()
    {
        return UnsafeAccess.UNSAFE.getLong(buffer, addressOffset);
    }

    /**
     * Free the counter slot for reuse.
     */
    public void close()
    {
        if (!isClosed)
        {
            isClosed = true;
            countersManager.free(counterId);
        }
    }
}
