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
 * Description of the structure for a record in the broadcast buffer.
 * All messages are stored in records with the following format.
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                         Sequence                              |
 *  |                         Indicator                             |
 *  +-+-------------------------------------------------------------+
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
public class RecordDescriptor
{
    /**
     * Offset within the record at which the sequence indicator field begins.
     */
    public static final int SEQUENCE_INDICATOR_OFFSET = 0;

    /**
     * Offset within the record at which the record length field begins.
     */
    public static final int LENGTH_OFFSET = SEQUENCE_INDICATOR_OFFSET + BitUtil.SIZE_OF_LONG;

    /**
     * Offset within the record at which the message type field begins.
     */
    public static final int TYPE_OFFSET = LENGTH_OFFSET + BitUtil.SIZE_OF_INT;

    /**
     * Length of the record header in bytes.
     */
    public static final int HEADER_LENGTH = BitUtil.SIZE_OF_LONG + BitUtil.SIZE_OF_INT * 2;

    /**
     * Alignment as a multiple of bytes for each record.
     */
    public static final int RECORD_ALIGNMENT = BitUtil.SIZE_OF_LONG;

    /**
     * Calculate the avaialble message length for the given valid recordSize (ie {@link #checkRecordSize(int)}).
     *
     * @param recordSize of the message record.
     * @return the required record length
     */
    public static int calculateAvailableMessageLength(final int recordSize)
    {
        return recordSize - HEADER_LENGTH;
    }

    /**
     * Check recordSize is a valid.
     *
     * @param recordSize to be checked.
     * @throws IllegalStateException if the recordSize is not sufficient to hold {@link #HEADER_LENGTH}
     *                               or is not {@link #RECORD_ALIGNMENT}-aligned.
     */
    public static void checkRecordSize(final int recordSize)
    {
        final int availableEncodedMessageLength = recordSize - HEADER_LENGTH;
        if (availableEncodedMessageLength < 0)
        {
            throw new IllegalStateException("The given recordSize has insufficient space to hold the " +
                "HEADER of a record: recordSize=" + recordSize);
        }
        if (!BitUtil.isAligned(recordSize, RECORD_ALIGNMENT))
        {
            throw new IllegalStateException(
                "The given recordSize is not " + RECORD_ALIGNMENT + "-aligned: recordSize=" + recordSize);
        }
    }

    /**
     * Calculate the real maximum supported message length for a record of given encoded message length.
     *
     * @param encodedMessageSize of the message record.
     * @return the required record length
     */
    public static int calculateMaxMessageLength(final int encodedMessageSize)
    {
        final int recordSize = calculateRecordSize(encodedMessageSize);
        final int availableEncodedMessageLength = recordSize - HEADER_LENGTH;
        return availableEncodedMessageLength;
    }

    /**
     * The buffer offset at which the sequence indicator field begins.
     *
     * @param recordOffset at which the frame begins.
     * @return the offset at which the sequence indicator field begins.
     */
    public static int sequenceIndicatorOffset(final int recordOffset)
    {
        return recordOffset + SEQUENCE_INDICATOR_OFFSET;
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
            final String msg = "Type id must be greater than zero, msgTypeId=" + msgTypeId;
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Calculate the required record length to hold {@code encodedMessageSize} message bytes.
     *
     * @param encodedMessageSize of the message record.
     * @return the required record length
     */
    public static int calculateRecordSize(final int encodedMessageSize)
    {
        return BitUtil.align(HEADER_LENGTH + encodedMessageSize, RECORD_ALIGNMENT);
    }

    /**
     * Calculate the starting offset of a record on a given {@code sequence}.
     *
     * @param sequence   of the record inside the buffer.
     * @param mask       of the real capacity in number of messages of the buffer.
     * @param recordSize the record size as obtained by {@link #calculateRecordSize(int)}.
     * @return the offset of a record on a given {@code sequence}
     */
    public static int calculatedRecordOffset(final long sequence, final int mask, final int recordSize)
    {
        return (int)(sequence & mask) * recordSize;
    }
}
