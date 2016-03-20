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
package uk.co.real_logic.agrona.concurrent.status;

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * Reports a position by recording it in an {@link UnsafeBuffer}.
 */
public final class UnsafeBufferPosition implements Position
{
    private final int counterId;
    private final int offset;
    private final UnsafeBuffer buffer;
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
        this.buffer = buffer;
        this.counterId = counterId;
        this.countersManager = countersManager;
        this.offset = CountersManager.counterOffset(counterId);
    }

    public int id()
    {
        return counterId;
    }

    public long get()
    {
        return buffer.getLong(offset);
    }

    public long getVolatile()
    {
        return buffer.getLongVolatile(offset);
    }

    public void set(final long value)
    {
        buffer.putLong(offset, value);
    }

    public void setOrdered(final long value)
    {
        buffer.putLongOrdered(offset, value);
    }

    public boolean proposeMax(final long proposedValue)
    {
        final UnsafeBuffer buffer = this.buffer;
        final int offset = this.offset;
        boolean updated = false;

        if (buffer.getLong(offset) < proposedValue)
        {
            buffer.putLong(offset, proposedValue);
            updated = true;
        }

        return updated;
    }

    public boolean proposeMaxOrdered(final long proposedValue)
    {
        final UnsafeBuffer buffer = this.buffer;
        final int offset = this.offset;
        boolean updated = false;

        if (buffer.getLong(offset) < proposedValue)
        {
            buffer.putLongOrdered(offset, proposedValue);
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
