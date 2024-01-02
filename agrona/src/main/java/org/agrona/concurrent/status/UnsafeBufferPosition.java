/*
 * Copyright 2014-2024 Real Logic Limited.
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

import org.agrona.UnsafeAccess;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * Reports a position by recording it in an {@link UnsafeBuffer}.
 */
public class UnsafeBufferPosition extends Position
{
    private boolean isClosed = false;
    private final int counterId;
    private final long addressOffset;
    private final byte[] byteArray;
    private final CountersManager countersManager;

    @SuppressWarnings({ "FieldCanBeLocal", "unused" })
    private final ByteBuffer byteBuffer; // retained to keep the buffer from being GC'ed

    /**
     * Map a position over a buffer.
     *
     * @param buffer    containing the counter.
     * @param counterId identifier of the counter.
     */
    public UnsafeBufferPosition(final UnsafeBuffer buffer, final int counterId)
    {
        this(buffer, counterId, null);
    }

    /**
     * Map a position over a buffer and this indicator owns the counter for reclamation.
     *
     * @param buffer          containing the counter.
     * @param counterId       identifier of the counter.
     * @param countersManager to be used for freeing the counter when this is closed.
     */
    public UnsafeBufferPosition(final UnsafeBuffer buffer, final int counterId, final CountersManager countersManager)
    {
        this.counterId = counterId;
        this.countersManager = countersManager;
        this.byteArray = buffer.byteArray();
        this.byteBuffer = buffer.byteBuffer();

        final int counterOffset = CountersManager.counterOffset(counterId);
        buffer.boundsCheck(counterOffset, SIZE_OF_LONG);
        this.addressOffset = buffer.addressOffset() + counterOffset;
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
        return counterId;
    }

    /**
     * {@inheritDoc}
     */
    public long get()
    {
        return UnsafeAccess.UNSAFE.getLong(byteArray, addressOffset);
    }

    /**
     * {@inheritDoc}
     */
    public long getVolatile()
    {
        return UnsafeAccess.UNSAFE.getLongVolatile(byteArray, addressOffset);
    }

    /**
     * {@inheritDoc}
     */
    public void set(final long value)
    {
        UnsafeAccess.UNSAFE.putLong(byteArray, addressOffset, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setOrdered(final long value)
    {
        UnsafeAccess.UNSAFE.putOrderedLong(byteArray, addressOffset, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setVolatile(final long value)
    {
        UnsafeAccess.UNSAFE.putLongVolatile(byteArray, addressOffset, value);
    }

    /**
     * {@inheritDoc}
     */
    public boolean proposeMax(final long proposedValue)
    {
        boolean updated = false;

        final byte[] array = byteArray;
        final long offset = addressOffset;
        if (UnsafeAccess.UNSAFE.getLong(array, offset) < proposedValue)
        {
            UnsafeAccess.UNSAFE.putLong(array, offset, proposedValue);
            updated = true;
        }

        return updated;
    }

    /**
     * {@inheritDoc}
     */
    public boolean proposeMaxOrdered(final long proposedValue)
    {
        boolean updated = false;

        final byte[] array = byteArray;
        final long offset = addressOffset;
        if (UnsafeAccess.UNSAFE.getLong(array, offset) < proposedValue)
        {
            UnsafeAccess.UNSAFE.putOrderedLong(array, offset, proposedValue);
            updated = true;
        }

        return updated;
    }

    /**
     * {@inheritDoc}
     */
    public void close()
    {
        if (!isClosed)
        {
            isClosed = true;
            if (null != countersManager)
            {
                countersManager.free(counterId);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "UnsafeBufferPosition{" +
            "isClosed=" + isClosed() +
            ", counterId=" + counterId +
            ", value=" + (isClosed() ? -1 : getVolatile()) +
            '}';
    }
}
