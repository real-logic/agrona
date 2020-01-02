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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.EpochClock;

import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * A thread safe extension of {@link CountersManager} which allows intra-process read and write access to the same
 * counters buffer. Note that inter-process access is not catered for.
 */
public class ConcurrentCountersManager extends CountersManager
{
    private final ReentrantLock lock = new ReentrantLock();

    public ConcurrentCountersManager(final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer)
    {
        super(metaDataBuffer, valuesBuffer);
    }

    public ConcurrentCountersManager(
        final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer, final Charset labelCharset)
    {
        super(metaDataBuffer, valuesBuffer, labelCharset);
    }

    public ConcurrentCountersManager(
        final AtomicBuffer metaDataBuffer,
        final AtomicBuffer valuesBuffer,
        final Charset labelCharset,
        final EpochClock epochClock,
        final long freeToReuseTimeoutMs)
    {
        super(metaDataBuffer, valuesBuffer, labelCharset, epochClock, freeToReuseTimeoutMs);
    }

    /**
     * {@inheritDoc}
     */
    public int allocate(final String label, final int typeId)
    {
        lock.lock();
        try
        {
            return super.allocate(label, typeId);
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int allocate(final String label, final int typeId, final Consumer<MutableDirectBuffer> keyFunc)
    {
        lock.lock();
        try
        {
            return super.allocate(label, typeId, keyFunc);
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int allocate(
        final int typeId,
        final DirectBuffer keyBuffer,
        final int keyOffset,
        final int keyLength,
        final DirectBuffer labelBuffer,
        final int labelOffset,
        final int labelLength)
    {
        lock.lock();
        try
        {
            return super.allocate(typeId, keyBuffer, keyOffset, keyLength, labelBuffer, labelOffset, labelLength);
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void free(final int counterId)
    {
        lock.lock();
        try
        {
            super.free(counterId);
        }
        finally
        {
            lock.unlock();
        }
    }
}
