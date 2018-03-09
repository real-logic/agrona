/*
 * Copyright 2014-2018 Real Logic Ltd.
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
package org.agrona.concurrent.ringbuffer;

import org.agrona.BitUtil;

/**
 * Layout description for the underlying buffer used by a {@link RingBuffer}. The buffer consists
 * of a ring of messages which is a power of 2 in size, followed by a trailer section containing state
 * information for the producers and consumers of the ring.
 */
public class RingBufferDescriptor
{
    /**
     * Offset within the trailer for where the tail value is stored.
     */
    public static final int TAIL_POSITION_OFFSET;

    /**
     * Offset within the trailer for where the head cache value is stored.
     */
    public static final int HEAD_CACHE_POSITION_OFFSET;

    /**
     * Offset within the trailer for where the head value is stored.
     */
    public static final int HEAD_POSITION_OFFSET;

    /**
     * Offset within the trailer for where the correlation counter value is stored.
     */
    public static final int CORRELATION_COUNTER_OFFSET;

    /**
     * Offset within the trailer for where the consumer heartbeat time value is stored.
     */
    public static final int CONSUMER_HEARTBEAT_OFFSET;

    /**
     * Total length of the trailer in bytes.
     */
    public static final int TRAILER_LENGTH;

    static
    {
        int offset = 0;
        offset += (BitUtil.CACHE_LINE_LENGTH * 2);
        TAIL_POSITION_OFFSET = offset;

        offset += (BitUtil.CACHE_LINE_LENGTH * 2);
        HEAD_CACHE_POSITION_OFFSET = offset;

        offset += (BitUtil.CACHE_LINE_LENGTH * 2);
        HEAD_POSITION_OFFSET = offset;

        offset += (BitUtil.CACHE_LINE_LENGTH * 2);
        CORRELATION_COUNTER_OFFSET = offset;

        offset += (BitUtil.CACHE_LINE_LENGTH * 2);
        CONSUMER_HEARTBEAT_OFFSET = offset;

        offset += (BitUtil.CACHE_LINE_LENGTH * 2);
        TRAILER_LENGTH = offset;
    }

    /**
     * Check the the buffer capacity is the correct size (a power of 2 + {@link RingBufferDescriptor#TRAILER_LENGTH}).
     *
     * @param capacity to be checked.
     * @throws IllegalStateException if the buffer capacity is incorrect.
     */
    public static void checkCapacity(final int capacity)
    {
        if (!BitUtil.isPowerOfTwo(capacity - TRAILER_LENGTH))
        {
            final String msg = "Capacity must be a positive power of 2 + TRAILER_LENGTH: capacity=" + capacity;
            throw new IllegalStateException(msg);
        }
    }
}
