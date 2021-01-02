/*
 * Copyright 2014-2021 Real Logic Limited.
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
import org.agrona.concurrent.AtomicBuffer;

import java.nio.ByteBuffer;

import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * {@link StatusIndicator} which wraps an {@link AtomicBuffer} with a given counter id.
 * @see CountersManager
 */
public class UnsafeBufferStatusIndicator extends StatusIndicator
{
    private final int counterId;
    private final long addressOffset;
    private final byte[] byteArray;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ByteBuffer byteBuffer; // retained to keep the buffer from being GC'ed

    /**
     * Map a status indicator over a buffer.
     *
     * @param buffer    containing the indicator.
     * @param counterId identifier of the indicator.
     */
    public UnsafeBufferStatusIndicator(final AtomicBuffer buffer, final int counterId)
    {
        this.counterId = counterId;
        this.byteArray = buffer.byteArray();
        this.byteBuffer = buffer.byteBuffer();

        final int counterOffset = CountersManager.counterOffset(counterId);
        buffer.boundsCheck(counterOffset, SIZE_OF_LONG);
        this.addressOffset = buffer.addressOffset() + counterOffset;
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
    public void setOrdered(final long value)
    {
        UnsafeAccess.UNSAFE.putOrderedLong(byteArray, addressOffset, value);
    }

    /**
     * {@inheritDoc}
     */
    public long getVolatile()
    {
        return UnsafeAccess.UNSAFE.getLongVolatile(byteArray, addressOffset);
    }

    public String toString()
    {
        return "UnsafeBufferStatusIndicator{" +
            "counterId=" + counterId +
            "value=" + getVolatile() +
            '}';
    }
}
