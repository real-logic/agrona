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
import org.agrona.concurrent.UnsafeBuffer;

import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * Reports a position by recording it in an {@link UnsafeBuffer}.
 */
public class UnsafeBufferPosition implements Position
{
    private final int counterId;
    private final long addressOffset;
    private final byte[] buffer;
    private final CountersManager countersManager;

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
        this.buffer = buffer.byteArray();

        final int counterOffset = CountersManager.counterOffset(counterId);
        buffer.boundsCheck(counterOffset, SIZE_OF_LONG);
        this.addressOffset = buffer.addressOffset() + counterOffset;
    }

    public int id()
    {
        return counterId;
    }

    public long get()
    {
        return UnsafeAccess.UNSAFE.getLong(buffer, addressOffset);
    }

    public long getVolatile()
    {
        return UnsafeAccess.UNSAFE.getLongVolatile(buffer, addressOffset);
    }

    public void set(final long value)
    {
        UnsafeAccess.UNSAFE.putLong(buffer, addressOffset, value);
    }

    public void setOrdered(final long value)
    {
        UnsafeAccess.UNSAFE.putOrderedLong(buffer, addressOffset, value);
    }

    public boolean proposeMax(final long proposedValue)
    {
        boolean updated = false;

        if (UnsafeAccess.UNSAFE.getLong(buffer, addressOffset) < proposedValue)
        {
            UnsafeAccess.UNSAFE.putLong(buffer, addressOffset, proposedValue);
            updated = true;
        }

        return updated;
    }

    public boolean proposeMaxOrdered(final long proposedValue)
    {
        boolean updated = false;

        if (UnsafeAccess.UNSAFE.getLong(buffer, addressOffset) < proposedValue)
        {
            UnsafeAccess.UNSAFE.putOrderedLong(buffer, addressOffset, proposedValue);
            updated = true;
        }

        return updated;
    }

    public void close()
    {
        if (null != countersManager)
        {
            countersManager.free(counterId);
        }
    }
}
