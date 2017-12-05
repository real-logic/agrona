/*
 * Copyright 2014-2017 Real Logic Ltd.
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
package org.agrona.concurrent.broadcast.fixed;

import org.agrona.BitUtil;

/**
 * Layout of the broadcast buffer. The buffer consists of a ring of fixed size messages that is a power of 2 in size
 * multiplied for the configured record size obtained by {@link RecordDescriptor#calculateRecordSize(int)}.
 * This is followed by a trailer section containing state information about the ring.
 */
public class BroadcastBufferDescriptor
{
    /**
     * Offset within the trailer for where the latest sequence value is stored.
     */
    public static final int LATEST_COUNTER_OFFSET;

    /**
     * Offset within the trailer for where the record size value is stored.
     */
    public static final int RECORD_SIZE_OFFSET;

    /**
     * Total size of the trailer
     */
    public static final int TRAILER_LENGTH;

    static
    {
        int offset = 0;
        LATEST_COUNTER_OFFSET = offset;
        offset += (BitUtil.CACHE_LINE_LENGTH * 2);
        RECORD_SIZE_OFFSET = offset;
        TRAILER_LENGTH = offset + BitUtil.CACHE_LINE_LENGTH * 2;
    }

    /**
     * Check the buffer capacity is the correct size.
     *
     * @param maxMessageSize the maximum encoded message size
     * @param capacity       to be checked.
     * @throws IllegalStateException if the buffer capacity is not a power of 2
     *                               multiple of {@link RecordDescriptor#calculateRecordSize(int)} of the given {@code maxMessageSize}.
     */
    public static void checkCapacity(final int maxMessageSize, final int capacity)
    {
        final int recordSize = RecordDescriptor.calculateRecordSize(maxMessageSize);
        if (capacity % recordSize != 0)
        {
            final String msg = "Capacity must be a positive power of 2 multiple of " + recordSize +
                " bytes + TRAILER_LENGTH: capacity=" + capacity;
            throw new IllegalStateException(msg);
        }
        final int records = capacity / recordSize;
        if (!BitUtil.isPowerOfTwo(records))
        {
            final String msg = "Capacity must be a positive power of 2 multiple of " + recordSize +
                " bytes + TRAILER_LENGTH: capacity=" + capacity;
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Calculate the buffer capacity (excluding the {@link #TRAILER_LENGTH}).
     *
     * @param maxMessageSize the maximum encoded message size
     * @param messages       the capacity in number of messages of the buffer
     * @return the required capacity of the buffer
     */
    public static int calculateCapacity(final int maxMessageSize, final int messages)
    {
        return BitUtil.findNextPositivePowerOfTwo(messages) * RecordDescriptor.calculateRecordSize(maxMessageSize);
    }


}
