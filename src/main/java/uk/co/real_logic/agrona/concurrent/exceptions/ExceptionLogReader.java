/*
 * Copyright 2016 Real Logic Ltd.
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
package uk.co.real_logic.agrona.concurrent.exceptions;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;

import static uk.co.real_logic.agrona.concurrent.exceptions.DistinctExceptionLog.*;

/**
 * Reader for the log created by the {@link DistinctExceptionLog}.
 *
 * The read methods are thread safe.
 */
public class ExceptionLogReader
{
    /**
     * Read all the exceptions in a log since the creation of the log.
     *
     * @param buffer  containing the {@link DistinctExceptionLog}.
     * @param handler to be called for each exception encountered.
     * @return the number of entries that has been read.
     */
    public static int read(final AtomicBuffer buffer, final ExceptionHandler handler)
    {
        return read(buffer, handler, 0);
    }

    /**
     * Read all the exceptions in a log since a timestamp.
     *
     * @param buffer         containing the {@link DistinctExceptionLog}.
     * @param handler        to be called for each exception encountered.
     * @param sinceTimestamp for filtering exceptions that have been recorded since this time.
     * @return the number of entries that has been read.
     */
    public static int read(final AtomicBuffer buffer, final ExceptionHandler handler, final long sinceTimestamp)
    {
        int entries = 0;
        int offset = 0;
        final int capacity = buffer.capacity();

        while (offset < capacity)
        {
            final int length = buffer.getIntVolatile(offset + LENGTH_OFFSET);
            if (0 == length)
            {
                break;
            }

            final long lastObservationTimestamp = buffer.getLongVolatile(offset + LAST_OBSERVATION_TIMESTAMP_OFFSET);
            if (lastObservationTimestamp >= sinceTimestamp)
            {
                ++entries;

                handler.onException(
                    buffer.getInt(offset + OBSERVATION_COUNT_OFFSET),
                    buffer.getLong(offset + FIRST_OBSERVATION_TIMESTAMP_OFFSET),
                    lastObservationTimestamp,
                    buffer.getStringUtf8(offset + ENCODED_EXCEPTION_OFFSET, length - ENCODED_EXCEPTION_OFFSET));
            }

            offset += BitUtil.align(length, RECORD_ALIGNMENT);
        }

        return entries;
    }
}
