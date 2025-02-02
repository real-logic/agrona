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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.UnsafeApi;
import org.agrona.concurrent.AtomicBuffer;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * Atomic counter that is backed by an {@link AtomicBuffer} that can be read across threads and processes.
 * <p>
 * In most cases you want to match the appropriate methods:
 * <ol>
 *     <li>an {@link #increment} (which has volatile semantics) should be combined with a {@link #get}.</li>
 *     <li>an {@link #incrementRelease} with a {@link #getAcquire}.</li>
 *     <li>an {@link #incrementOpaque} with a {@link #getOpaque}.</li>
 *     <li>an {@link #incrementPlain} with a {@link #getPlain}.</li>
 * </ol>
 * If the methods aren't matched then chances are there either is a data race or race condition due to too few
 * constraints, or suboptimal performance due to too many constraints.
 */
public class AtomicCounter implements AutoCloseable
{
    private boolean isClosed = false;
    private final int id;
    private final long addressOffset;
    private final byte[] byteArray;
    private CountersManager countersManager;

    @SuppressWarnings({ "FieldCanBeLocal", "unused" })
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
     * Disconnect from {@link CountersManager} if allocated, so it can be closed without freeing the slot.
     */
    public void disconnectCountersManager()
    {
        countersManager = null;
    }

    /**
     * Close counter and free the counter slot for reuse of connected to {@link CountersManager}.
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
     * @return this for a fluent API.
     * @throws IllegalStateException is not constructed {@link CountersManager}.
     */
    public AtomicCounter appendToLabel(final String suffix)
    {
        if (null != countersManager)
        {
            countersManager.appendToLabel(id, suffix);
        }
        else
        {
            throw new IllegalStateException("Not constructed with CountersManager");
        }

        return this;
    }

    /**
     * Update the key for a counter constructed with a {@link CountersManager}.
     *
     * @param keyFunc callback to use to update the counter's key
     * @throws IllegalStateException is not constructed {@link CountersManager}.
     */
    public void updateKey(final Consumer<MutableDirectBuffer> keyFunc)
    {
        if (null != countersManager)
        {
            countersManager.setCounterKey(id, keyFunc);
        }
        else
        {
            throw new IllegalStateException("Not constructed with CountersManager");
        }
    }

    /**
     * Update the key for a counter constructed with a {@link CountersManager}.
     *
     * @param keyBuffer contains key data to be copied into the counter.
     * @param offset    start of the key data within the keyBuffer
     * @param length    length of the data within the keyBuffer (must be &lt;= {@link CountersReader#MAX_KEY_LENGTH})
     * @throws IllegalStateException is not constructed {@link CountersManager}.
     */
    public void updateKey(final DirectBuffer keyBuffer, final int offset, final int length)
    {
        if (null != countersManager)
        {
            countersManager.setCounterKey(id, keyBuffer, offset, length);
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
        return UnsafeApi.getAndAddLong(byteArray, addressOffset, 1);
    }

    /**
     * Perform an atomic increment that is not safe across threads.
     * <p>
     * This method is identical to {@link #incrementRelease()} and that method should be used instead.
     *
     * @return the previous value of the counter
     */
    public long incrementOrdered()
    {
        return incrementRelease();
    }

    /**
     * Perform a non-atomic increment with release semantics.
     * <p>
     * It can result into lost updates due to race condition when called concurrently.
     * <p>
     * The load has plain memory semantics and the store has release memory semantics.
     * <p>
     * The typical use-case is when there is a single writer thread and one or more reader threads and causality
     * needs to be preserved using the {@link #getAcquire()}.
     * <p>
     * This method is likely to outperform the {@link #increment()}. So if there is just a single mutator thread, and
     * one or more reader threads, then it is likely you will prefer this method.
     * <p>
     * If no memory ordering is needed, have a look at the {@link #incrementOpaque()}.
     *
     * @return the previous value of the counter
     * @since 2.1.0
     */
    public long incrementRelease()
    {
        final byte[] array = byteArray;
        final long offset = addressOffset;
        final long currentValue = UnsafeApi.getLong(array, offset);
        UnsafeApi.putLongRelease(array, offset, currentValue + 1);
        return currentValue;
    }

    /**
     * Perform a non-atomic increment using opaque semantics.
     * <p>
     * It can result into lost updates due to race condition when called concurrently.
     * <p>
     * The load has plain memory semantics and the store has opaque memory semantics.
     * <p>
     * The typical use-case is when there is a single writer thread and one or more reader threads and surrounding
     * loads/stores don't need to be ordered.
     * <p>
     * This method should be at least fast as {@link #incrementRelease()} since it has weaker memory semantics.
     * So if there is just a single mutator thread, and one or more reader threads and surrounding loads/stores don't
     * need to be ordered, then it is likely you will prefer this method.
     *
     * @return the previous value of the counter
     * @since 2.1.0
     */
    public long incrementOpaque()
    {
        final byte[] array = byteArray;
        final long offset = addressOffset;
        final long currentValue = UnsafeApi.getLong(array, offset);
        UnsafeApi.putLongOpaque(array, offset, currentValue + 1);
        return currentValue;
    }

    /**
     * Increment the counter.
     * <p>
     * This method is not atomic and this can lead to lost-updates due to race conditions and data races. This
     * load and store have plain memory semantics.
     * <p>
     * The typical use-case for this method is when writer and reader are the same thread.
     *
     * @return the previous value of the counter
     * @since 2.1.0
     */
    public long incrementPlain()
    {
        final byte[] array = byteArray;
        final long offset = addressOffset;
        final long currentValue = UnsafeApi.getLong(array, offset);
        UnsafeApi.putLong(array, offset, currentValue + 1);
        return currentValue;
    }

    /**
     * Perform an atomic decrement that will not lose updates across threads.
     * <p>
     * The loads and store have volatile memory semantics.
     *
     * @return the previous value of the counter
     */
    public long decrement()
    {
        return UnsafeApi.getAndAddLong(byteArray, addressOffset, -1);
    }

    /**
     * Perform an atomic decrement that is not safe across threads.
     *
     * @return the previous value of the counter
     * @since 2.1.0
     */
    public long decrementOrdered()
    {
        return decrementRelease();
    }

    /**
     * Decrements the counter non-atomically with release semantics.
     * <p>
     * It can result into lost updates to race condition when called concurrently.
     * <p>
     * The load has plain memory semantics and the store has release memory semantics.
     * <p>
     * The typical use-case is when there is one mutator thread, that calls this method, and one or more reader threads.
     * <p>
     * This method is likely to outperform the {@link #increment()} and probably will be a better alternative.
     *
     * @return the previous value of the counter
     * @since 2.1.0
     */
    public long decrementRelease()
    {
        final byte[] array = byteArray;
        final long offset = addressOffset;
        final long currentValue = UnsafeApi.getLong(array, offset);
        UnsafeApi.putLongRelease(array, offset, currentValue - 1);

        return currentValue;
    }

    /**
     * Perform a non-atomic decrement using opaque semantics.
     * <p>
     * It can result into lost updates due to race condition when called concurrently.
     * <p>
     * The load has plain memory semantics and the store has opaque memory semantics.
     * <p>
     * The typical use-case is when there is a single writer thread and one or more reader threads and surrounding
     * loads/stores don't need to be ordered.
     * <p>
     * This method should be at least fast as {@link #decrementRelease()} since it has weaker memory semantics.
     * So if there is just a single mutator thread, and one or more reader threads and surrounding loads/stores don't
     * need to be ordered, then it is likely you will prefer this method.
     *
     * @return the previous value of the counter
     * @since 2.1.0
     */
    public long decrementOpaque()
    {
        final byte[] array = byteArray;
        final long offset = addressOffset;
        final long currentValue = UnsafeApi.getLong(array, offset);
        UnsafeApi.putLongOpaque(array, offset, currentValue - 1);

        return currentValue;
    }

    /**
     * Decrements the counter.
     * <p>
     * This method is not atomic and this can lead to lost-updates due to race conditions. This load and store
     * have plain memory semantics.
     * <p>
     * The typical use-case for this method is when writer and reader are the same thread.
     *
     * @return the previous value of the counter
     * @since 2.1.0
     */
    public long decrementPlain()
    {
        final byte[] array = byteArray;
        final long offset = addressOffset;
        final long currentValue = UnsafeApi.getLong(array, offset);
        UnsafeApi.putLong(array, offset, currentValue - 1);

        return currentValue;
    }

    /**
     * Set the counter with volatile semantics.
     *
     * @param value to be set with volatile semantics.
     */
    public void set(final long value)
    {
        UnsafeApi.putLongVolatile(byteArray, addressOffset, value);
    }

    /**
     * Set the counter with ordered semantics.
     * <p>
     * This method is identical to {@link #setRelease(long)} and that method should be used instead.
     *
     * @param value to be set with ordered semantics.
     */
    public void setOrdered(final long value)
    {
        setRelease(value);
    }

    /**
     * Set the counter value atomically.
     * <p>
     * The store has release memory semantics.
     *
     * @param value to be set
     * @since 2.1.0
     */
    public void setRelease(final long value)
    {
        UnsafeApi.putLongRelease(byteArray, addressOffset, value);
    }

    /**
     * Set the counter value atomically.
     * <p>
     * The store has opaque memory semantics.
     *
     * @param value to be set
     * @since 2.1.0
     */
    public void setOpaque(final long value)
    {
        UnsafeApi.putLongOpaque(byteArray, addressOffset, value);
    }

    /**
     * Set the counter with normal semantics.
     * <p>
     * This method is identical to {@link #setPlain(long)} and that method should be used instead.
     *
     * @param value to be set with normal semantics.
     */
    public void setWeak(final long value)
    {
        setPlain(value);
    }

    /**
     * Set the counter value with plain memory semantics.
     *
     * @param value to be set with normal semantics.
     * @since 2.1.0
     */
    public void setPlain(final long value)
    {
        UnsafeApi.putLong(byteArray, addressOffset, value);
    }

    /**
     * Add an increment to the counter that will not lose updates across threads.
     *
     * @param increment to be added.
     * @return the previous value of the counter
     */
    public long getAndAdd(final long increment)
    {
        return UnsafeApi.getAndAddLong(byteArray, addressOffset, increment);
    }

    /**
     * Add an increment to the counter with ordered store semantics.
     * <p>
     * This method is identical to {@link #getAndAddRelease(long)} and that method should be used instead.
     *
     * @param increment to be added with ordered store semantics.
     * @return the previous value of the counter
     */
    public long getAndAddOrdered(final long increment)
    {
        return getAndAddRelease(increment);
    }

    /**
     * Adds an increment to the counter non-atomically.
     * <p>
     * This method is not atomic; it can suffer from lost-updates due to race conditions.
     * <p>
     * The load has plain memory semantics and the store has release memory semantics.
     * <p>
     * The typical use-case is when there is one mutator thread, that calls this method, and one or more reader
     * threads.
     *
     * @param increment to be added
     * @return the previous value of the counter
     * @since 2.1.0
     */
    public long getAndAddRelease(final long increment)
    {
        final byte[] array = byteArray;
        final long offset = addressOffset;
        final long currentValue = UnsafeApi.getLong(array, offset);
        UnsafeApi.putLongRelease(array, offset, currentValue + increment);

        return currentValue;
    }

    /**
     * Adds an increment to the counter non-atomically.
     * <p>
     * This method is not atomic; it can suffer from lost-updates due to race conditions.
     * <p>
     * The load has plain memory semantics and the store has opaque memory semantics.
     * <p>
     * The typical use-case is when there is one mutator thread, that calls this method, and one or more reader
     * threads.
     * <p>
     * If ordering of surrounding loads/stores isn't important, then this method is likely to be faster than
     * {@link #getAndAddRelease(long)} because it has less strict memory ordering requirements.
     *
     * @param increment to be added
     * @return the previous value of the counter
     * @since 2.1.0
     */
    public long getAndAddOpaque(final long increment)
    {
        final byte[] array = byteArray;
        final long offset = addressOffset;
        final long currentValue = UnsafeApi.getLong(array, offset);
        UnsafeApi.putLongOpaque(array, offset, currentValue + increment);

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
        return UnsafeApi.getAndSetLong(byteArray, addressOffset, value);
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
        return UnsafeApi.compareAndSetLong(byteArray, addressOffset, expectedValue, updateValue);
    }

    /**
     * Get the value for the counter with volatile semantics.
     *
     * @return the value for the counter.
     */
    public long get()
    {
        return UnsafeApi.getLongVolatile(byteArray, addressOffset);
    }

    /**
     * Get the value for the counter with acquire semantics.
     *
     * @return the value for the counter.
     * @since 2.1.0
     */
    public long getAcquire()
    {
        return UnsafeApi.getLongAcquire(byteArray, addressOffset);
    }

    /**
     * Get the value for the counter with opaque semantics.
     *
     * @return the value for the counter.
     * @since 2.1.0
     */
    public long getOpaque()
    {
        return UnsafeApi.getLongOpaque(byteArray, addressOffset);
    }

    /**
     * Get the value of the counter using weak ordering semantics. This is the same a standard read of a field.
     * <p>
     * This call is identical to {@link #getPlain()} and that method is preferred.
     *
     * @return the  value for the counter.
     */
    public long getWeak()
    {
        return getPlain();
    }

    /**
     * Get the value of the counter using plain memory semantics. This is the same a standard read of a field.
     *
     * @return the  value for the counter.
     * @since 2.1.0
     */
    public long getPlain()
    {
        return UnsafeApi.getLong(byteArray, addressOffset);
    }

    /**
     * Set the value to a new proposedValue if greater than the current value with plain memory semantics.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public boolean proposeMax(final long proposedValue)
    {
        boolean updated = false;

        final byte[] array = byteArray;
        final long offset = addressOffset;
        if (UnsafeApi.getLong(array, offset) < proposedValue)
        {
            UnsafeApi.putLong(array, offset, proposedValue);
            updated = true;
        }

        return updated;
    }

    /**
     * Set the value to a new proposedValue if greater than the current value with memory ordering semantics.
     * <p>
     * This method is identical to {@link #proposeMaxRelease(long)} and that method should be used instead.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public boolean proposeMaxOrdered(final long proposedValue)
    {
        return proposeMaxRelease(proposedValue);
    }

    /**
     * Set the value to a new proposedValue if greater than the current value.
     * <p>
     * This call is not atomic and can suffer from lost updates to race conditions.
     * <p>
     * The load has plain memory semantics and the store has release memory semantics.
     * <p>
     * The typical use-case is when there is one mutator thread, that calls this method, and one or more reader threads.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     * @since 2.1.0
     */
    public boolean proposeMaxRelease(final long proposedValue)
    {
        boolean updated = false;

        final byte[] array = byteArray;
        final long offset = addressOffset;
        if (UnsafeApi.getLong(array, offset) < proposedValue)
        {
            UnsafeApi.putLongRelease(array, offset, proposedValue);
            updated = true;
        }

        return updated;
    }

    /**
     * Set the value to a new proposedValue if greater than the current value.
     * <p>
     * This call is not atomic and can suffer from lost updates to race conditions.
     * <p>
     * The load has plain memory semantics and the store has opaque memory semantics.
     * <p>
     * The typical use-case is when there is one mutator thread, that calls this method, and one or more reader threads.
     * <p>
     * This method is likely to outperform {@link #proposeMaxRelease(long)} since this method has less memory ordering
     * requirements.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     * @since 2.1.0
     */
    public boolean proposeMaxOpaque(final long proposedValue)
    {
        boolean updated = false;

        final byte[] array = byteArray;
        final long offset = addressOffset;
        if (UnsafeApi.getLong(array, offset) < proposedValue)
        {
            UnsafeApi.putLongOpaque(array, offset, proposedValue);
            updated = true;
        }

        return updated;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "AtomicCounter{" +
            "isClosed=" + isClosed() +
            ", id=" + id +
            ", value=" + (isClosed() ? -1 : get()) +
            ", countersManager=" + countersManager +
            '}';
    }
}
