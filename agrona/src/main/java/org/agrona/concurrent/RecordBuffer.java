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
package org.agrona.concurrent;

import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.agrona.BitUtil.SIZE_OF_INT;

/**
 * A record buffer is an off-heap buffer with a series of records
 * and a header that can be written to or read from multiple threads.
 * <p>
 * The buffer has a position field at the start, then a variable
 * sized header, then a series of records. Each record has a status
 * tag and a key.
 * <p>
 * NB: it's possible for an element to be overwritten as you're reading
 * Out of the buffer, take care.
 *
 * <pre>
 *  +----------------------------+
 *  |           Header           |
 *  |                           ...
 *  +----------------------------+
 *  |        Position Field      |
 *  +----------------------------+
 *  |        Status Field 0      |
 *  +----------------------------+
 *  |         Key Field 0        |
 *  +----------------------------+
 *  |           Record 0         |
 *  |                           ...
 *  +----------------------------+
 *  |        Status Field 1      |
 *  +----------------------------+
 *  |         Key Field 1        |
 *  +----------------------------+
 *  |           Record 1         |
 *  |                           ...
 *  +----------------------------+
 *  |                           ...
 * </pre>
 */
// TODO: consider optimising this class by indexing on the key, rather than just linear scanning
public class RecordBuffer
{
    /**
     * Special offset indiciating that the record was not claimed.
     */
    public static final int DID_NOT_CLAIM_RECORD = -1;

    private static final int UNUSED = 0;
    private static final int PENDING = 1;
    private static final int COMMITTED = 2;

    private static final int SIZE_OF_POSITION_FIELD = SIZE_OF_INT;

    /**
     * Status field is an int rather than a byte so that it can be CAS'd.
     */
    private static final int SIZE_OF_STATUS_FIELD = SIZE_OF_INT;
    private static final int SIZE_OF_KEY_FIELD = SIZE_OF_INT;
    private static final int SIZE_OF_RECORD_FRAME = SIZE_OF_STATUS_FIELD + SIZE_OF_KEY_FIELD;

    private static final long PAUSE_TIME_NS = MICROSECONDS.toNanos(1000);

    private final AtomicBuffer buffer;
    private final int positionFieldOffset;
    private final int endOfPositionField;
    private final int slotSize;

    /**
     * Callback interface for reading elements out of the buffer.
     *
     * @see RecordBuffer#forEach(RecordHandler)
     */
    public interface RecordHandler
    {
        /**
         * Called once for each committed record in the buffer.
         *
         * @param key    the key for the record in question.
         * @param offset the offset within the buffer that the record starts at.
         */
        void onRecord(int key, int offset);
    }

    /**
     * Interface for safely writing to the buffer.
     *
     * @see RecordBuffer#withRecord(int, RecordWriter)
     */
    public interface RecordWriter
    {
        /**
         * Write an updated record within this callback.
         *
         * @param offset the offset within the buffer that has been claimed.
         */
        void writeRecord(int offset);
    }

    /**
     * @param buffer     to wrap.
     * @param headerSize in bytes.
     * @param recordSize in bytes.
     */
    public RecordBuffer(final AtomicBuffer buffer, final int headerSize, final int recordSize)
    {
        this.buffer = buffer;
        this.positionFieldOffset = headerSize;
        this.endOfPositionField = headerSize + SIZE_OF_POSITION_FIELD;
        this.slotSize = recordSize + SIZE_OF_RECORD_FRAME;
    }

    /**
     * Initialise the buffer if you're the first thread to begin writing.
     */
    public void initialise()
    {
        movePosition(endOfPositionField);
    }

    /**
     * Check if the buffer has been initialised.
     *
     * @return true if the buffer has been initialised.
     */
    public boolean isInitialised()
    {
        return position() != 0;
    }

    /**
     * Read each record out of the buffer in turn.
     *
     * @param handler the handler is called once for each record in the buffer.
     */
    public void forEach(final RecordHandler handler)
    {
        int offset = endOfPositionField;

        final int position = position();
        while (offset < position)
        {
            if (statusVolatile(offset) == COMMITTED)
            {
                final int key = key(offset);
                handler.onRecord(key, offset + SIZE_OF_RECORD_FRAME);
            }

            offset += slotSize;
        }
    }

    /**
     * Search for the first record with the specified key.
     *
     * @param key the key to search for.
     * @return the offset of the start of the record within the buffer or {@code DID_NOT_CLAIM_RECORD} if no record
     * with the specified key.
     */
    public int get(final int key)
    {
        int offset = endOfPositionField;

        final int position = position();
        while (offset < position)
        {
            if (statusVolatile(offset) == COMMITTED && key == key(offset))
            {
                return offset + SIZE_OF_RECORD_FRAME;
            }

            offset += slotSize;
        }

        return DID_NOT_CLAIM_RECORD;
    }

    /**
     * High level and safe way of writing a record to the buffer.
     *
     * @param key    the key to associate the record with.
     * @param writer the callback which is passed the record to write.
     * @return whether the write operation succeeded or not.
     */
    public boolean withRecord(final int key, final RecordWriter writer)
    {
        final int claimedOffset = claimRecord(key);
        if (claimedOffset == DID_NOT_CLAIM_RECORD)
        {
            return false;
        }

        try
        {
            writer.writeRecord(claimedOffset);
        }
        finally
        {
            commit(claimedOffset);
        }

        return true;
    }

    /**
     * Claim a record in the buffer. Each record has a unique key.
     *
     * @param key the key to claim the record with.
     * @return the offset at which record was claimed or {@code DID_NOT_CLAIM_RECORD} if the claim failed.
     * @see RecordBuffer#commit(int)
     */
    public int claimRecord(final int key)
    {
        int offset = endOfPositionField;

        while (offset < position())
        {
            if (key == key(offset))
            {
                // If someone else is writing something with the same key then abort
                if (statusVolatile(offset) == PENDING)
                {
                    return DID_NOT_CLAIM_RECORD;
                }
                else // state == COMMITTED
                {
                    compareAndSetStatus(offset, COMMITTED, PENDING);
                    return offset + SIZE_OF_RECORD_FRAME;
                }
            }

            offset += slotSize;
        }

        if ((offset + slotSize) > buffer.capacity())
        {
            return DID_NOT_CLAIM_RECORD;
        }

        final int claimOffset = movePosition(slotSize);
        compareAndSetStatus(claimOffset, UNUSED, PENDING);
        key(claimOffset, key);

        return claimOffset + SIZE_OF_RECORD_FRAME;
    }

    /**
     * Commit a claimed record into the buffer.
     *
     * @param claimedOffset the offset of the record to commit.
     * @see RecordBuffer#claimRecord(int)
     */
    public void commit(final int claimedOffset)
    {
        compareAndSetStatus(claimedOffset - SIZE_OF_RECORD_FRAME, PENDING, COMMITTED);
    }

    private int statusVolatile(final int offset)
    {
        return buffer.getIntVolatile(offset);
    }

    private void compareAndSetStatus(final int offset, final int oldStatus, final int newStatus)
    {
        while (!buffer.compareAndSetInt(offset, oldStatus, newStatus))
        {
            LockSupport.parkNanos(PAUSE_TIME_NS);
        }
    }

    private int key(final int offset)
    {
        return buffer.getInt(offset + SIZE_OF_STATUS_FIELD);
    }

    private void key(final int offset, final int key)
    {
        buffer.putInt(offset + SIZE_OF_STATUS_FIELD, key);
    }

    private int position()
    {
        return buffer.getIntVolatile(positionFieldOffset);
    }

    private int movePosition(final int delta)
    {
        return buffer.getAndAddInt(positionFieldOffset, delta);
    }
}
