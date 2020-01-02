/*
 * Copyright 2014-2020 Real Logic Limited.
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

import static org.agrona.BitUtil.SIZE_OF_LONG;

import org.agrona.UnsafeAccess;
import org.agrona.concurrent.AtomicBuffer;

import java.nio.ByteBuffer;

/**
 * Atomic counter that is backed by an {@link AtomicBuffer} that can be read across threads and processes.
 */
public class AtomicCounter implements AutoCloseable
{
    private boolean isClosed = false;
    private final int id;
    private final long addressOffset;
    private final byte[] byteArray;
    private final CountersManager countersManager;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ByteBuffer byteBuffer; // retained to keep the buffer from being GC'ed

    /**
     * Map a counter over a buffer. This version will NOT free the counter on close.
     *
     * @param buffer    containing the counter.
     * @param counterId identifier of the counter.
     */
    public AtomicCounter(final AtomicBuffer buffer, final int counterId)
    {
        this(buffer, counterId, null);
    }

    /**
     * Map a counter over a buffer. This version will free the counter on close.
     *
     * @param buffer          containing the counter.
     * @param counterId       identifier for the counter.
     * @param countersManager to be called to free the counter on close.
     */
    public AtomicCounter(final AtomicBuffer buffer, final int counterId, final CountersManager countersManager)
    {
        this.id = counterId;
        this.countersManager = countersManager;
        this.byteBuffer = buffer.byteBuffer();
        this.byteArray = buffer.byteArray();

        final int counterOffset = CountersManager.counterOffset(counterId);
        buffer.boundsCheck(counterOffset, SIZE_OF_LONG);
        this.addressOffset = buffer.addressOffset() + counterOffset;
    }

    /**
     * Identity for the counter within the {@link CountersManager}.
     *
     * @return identity for the counter within the {@link CountersManager}.
     */
    public int id()
    {
        return id;
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
     * Return the label for the counter within the {@link CountersManager}.
     *
     * @return the label for the counter within the {@link CountersManager}.
     */
    public String label()
    {
        return null != countersManager ? countersManager.getCounterLabel(id) : null;
    }

    /**
     * Update the label for the counter constructed with a {@link CountersManager}.
     *
     * @param newLabel for the counter with a {@link CountersManager}.
     * @throws IllegalStateException is not constructed {@link CountersManager}.
     */
    public void updateLabel(final String newLabel)
    {
        if (null != countersManager)
        {
            countersManager.setCounterLabel(id, newLabel);
        }
        else
        {
            throw new IllegalStateException("Not constructed with CountersManager");
        }
    }

    /**
     * Append to the label for a counter constructed with a {@link CountersManager}.
     *
     * @param suffix for the counter within a {@link CountersManager}.
     * @throws IllegalStateException is not constructed {@link CountersManager}.
     */
    public void appendToLabel(final String suffix)
    {
        if (null != countersManager)
        {
            countersManager.appendToLabel(id, suffix);
        }
        else
        {
            throw new IllegalStateException("Not constructed with CountersManager");
        }
    }

    /**
     * Perform an atomic increment that will not lose updates across threads.
     *
     * @return the previous value of the counter
     */
    public long increment()
    {
        return UnsafeAccess.UNSAFE.getAndAddLong(byteArray, addressOffset, 1);
    }

    /**
     * Perform an atomic increment that is not safe across threads.
     *
     * @return the previous value of the counter
     */
    public long incrementOrdered()
    {
        final long currentValue = UnsafeAccess.UNSAFE.getLong(byteArray, addressOffset);
        UnsafeAccess.UNSAFE.putOrderedLong(byteArray, addressOffset, currentValue + 1);

        return currentValue;
    }

    /**
     * Set the counter with volatile semantics.
     *
     * @param value to be set with volatile semantics.
     */
    public void set(final long value)
    {
        UnsafeAccess.UNSAFE.putLongVolatile(byteArray, addressOffset, value);
    }

    /**
     * Set the counter with ordered semantics.
     *
     * @param value to be set with ordered semantics.
     */
    public void setOrdered(final long value)
    {
        UnsafeAccess.UNSAFE.putOrderedLong(byteArray, addressOffset, value);
    }

    /**
     * Set the counter with normal semantics.
     *
     * @param value to be set with normal semantics.
     */
    public void setWeak(final long value)
    {
        UnsafeAccess.UNSAFE.putLong(byteArray, addressOffset, value);
    }

    /**
     * Add an increment to the counter that will not lose updates across threads.
     *
     * @param increment to be added.
     * @return the previous value of the counter
     */
    public long getAndAdd(final long increment)
    {
        return UnsafeAccess.UNSAFE.getAndAddLong(byteArray, addressOffset, increment);
    }

    /**
     * Add an increment to the counter with ordered store semantics.
     *
     * @param increment to be added with ordered store semantics.
     * @return the previous value of the counter
     */
    public long getAndAddOrdered(final long increment)
    {
        final long currentValue = UnsafeAccess.UNSAFE.getLong(byteArray, addressOffset);
        UnsafeAccess.UNSAFE.putOrderedLong(byteArray, addressOffset, currentValue + increment);

        return currentValue;
    }

    /**
     * Get the current value of a counter and atomically set it to a new value.
     *
     * @param value to be set.
     * @return the previous value of the counter
     */
    public long getAndSet(final long value)
    {
        return UnsafeAccess.UNSAFE.getAndSetLong(byteArray, addressOffset, value);
    }

    /**
     * Compare the current value to expected and if true then set to the update value atomically.
     *
     * @param expectedValue for the counter.
     * @param updateValue   for the counter.
     * @return true if successful otherwise false.
     */
    public boolean compareAndSet(final long expectedValue, final long updateValue)
    {
        return UnsafeAccess.UNSAFE.compareAndSwapLong(byteArray, addressOffset, expectedValue, updateValue);
    }

    /**
     * Get the latest value for the counter with volatile semantics.
     *
     * @return the latest value for the counter.
     */
    public long get()
    {
        return UnsafeAccess.UNSAFE.getLongVolatile(byteArray, addressOffset);
    }

    /**
     * Get the value of the counter using weak ordering semantics. This is the same a standard read of a field.
     *
     * @return the  value for the counter.
     */
    public long getWeak()
    {
        return UnsafeAccess.UNSAFE.getLong(byteArray, addressOffset);
    }

    /**
     * Set the value to a new proposedValue if greater than the current value with memory ordering semantics.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public boolean proposeMax(final long proposedValue)
    {
        boolean updated = false;

        if (UnsafeAccess.UNSAFE.getLong(byteArray, addressOffset) < proposedValue)
        {
            UnsafeAccess.UNSAFE.putLong(byteArray, addressOffset, proposedValue);
            updated = true;
        }

        return updated;
    }

    /**
     * Set the value to a new proposedValue if greater than the current value with memory ordering semantics.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public boolean proposeMaxOrdered(final long proposedValue)
    {
        boolean updated = false;

        if (UnsafeAccess.UNSAFE.getLong(byteArray, addressOffset) < proposedValue)
        {
            UnsafeAccess.UNSAFE.putOrderedLong(byteArray, addressOffset, proposedValue);
            updated = true;
        }

        return updated;
    }

    /**
     * Free the counter slot for reuse.
     */
    public void close()
    {
        if (!isClosed)
        {
            isClosed = true;
            if (null != countersManager)
            {
                countersManager.free(id);
            }
        }
    }

    public String toString()
    {
        return "AtomicCounter{" +
            "isClosed=" + isClosed() +
            ", id=" + id +
            ", value=" + get() +
            '}';
    }
}
