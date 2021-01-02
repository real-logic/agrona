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
package org.agrona.concurrent.broadcast;

import static org.agrona.BitUtil.SIZE_OF_INT;

/**
 * Description of the structure for a record in the broadcast buffer.
 * All messages are stored in records with the following format.
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |R|                        Length                               |
 *  +-+-------------------------------------------------------------+
 *  |                           Type                                |
 *  +---------------------------------------------------------------+
 *  |                      Encoded Message                         ...
 * ...                                                              |
 *  +---------------------------------------------------------------+
 * </pre>
 * <p>
 * (R) bits are reserved.
 */
public final class RecordDescriptor
{
    /**
     * Message type is padding to prevent fragmentation in the buffer.
     */
    public static final int PADDING_MSG_TYPE_ID = -1;

    /**
     * Offset within the record at which the record length field begins.
     */
    public static final int LENGTH_OFFSET = 0;

    /**
     * Offset within the record at which the message type field begins.
     */
    public static final int TYPE_OFFSET = LENGTH_OFFSET + SIZE_OF_INT;

    /**
     * Length of the record header in bytes.
     */
    public static final int HEADER_LENGTH = SIZE_OF_INT * 2;

    /**
     * Alignment as a multiple of bytes for each record.
     */
    public static final int RECORD_ALIGNMENT = HEADER_LENGTH;

    private RecordDescriptor()
    {
    }

    /**
     * Calculate the maximum supported message length for a buffer of given capacity.
     *
     * @param capacity of the log buffer.
     * @return the maximum supported size for a message.
     */
    public static int calculateMaxMessageLength(final int capacity)
    {
        return capacity / 8;
    }

    /**
     * The buffer offset at which the message length field begins.
     *
     * @param recordOffset at which the frame begins.
     * @return the offset at which the message length field begins.
     */
    public static int lengthOffset(final int recordOffset)
    {
        return recordOffset + LENGTH_OFFSET;
    }

    /**
     * The buffer offset at which the message type field begins.
     *
     * @param recordOffset at which the frame begins.
     * @return the offset at which the message type field begins.
     */
    public static int typeOffset(final int recordOffset)
    {
        return recordOffset + TYPE_OFFSET;
    }

    /**
     * The buffer offset at which the encoded message begins.
     *
     * @param recordOffset at which the frame begins.
     * @return the offset at which the encoded message begins.
     */
    public static int msgOffset(final int recordOffset)
    {
        return recordOffset + HEADER_LENGTH;
    }

    /**
     * Check that and message id is in the valid range.
     *
     * @param msgTypeId to be checked.
     * @throws IllegalArgumentException if the id is not in the valid range.
     */
    public static void checkTypeId(final int msgTypeId)
    {
        if (msgTypeId < 1)
        {
            final String msg = "type id must be greater than zero, msgTypeId=" + msgTypeId;
            throw new IllegalArgumentException(msg);
        }
    }
}
