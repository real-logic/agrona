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
package org.agrona.concurrent.status;

import org.agrona.LangUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.IntArrayList;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.agrona.BitUtil.SIZE_OF_INT;

/**
 * Manages the allocation and freeing of counters that are normally stored in a memory-mapped file.
 * <p>
 * This class in not threadsafe. Counters should be centrally managed.
 * <p>
 * <b>Values Buffer</b>
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                        Counter Value                          |
 *  |                                                               |
 *  +---------------------------------------------------------------+
 *  |                     120 bytes of padding                     ...
 * ...                                                              |
 *  +---------------------------------------------------------------+
 *  |                   Repeats to end of buffer                   ...
 *  |                                                               |
 * ...                                                              |
 *  +---------------------------------------------------------------+
 * </pre>
 * <p>
 * <b>Meta Data Buffer</b>
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                        Record State                           |
 *  +---------------------------------------------------------------+
 *  |                          Type Id                              |
 *  +---------------------------------------------------------------+
 *  |                      120 bytes for key                       ...
 * ...                                                              |
 *  +-+-------------------------------------------------------------+
 *  |R|                      Label Length                           |
 *  +-+-------------------------------------------------------------+
 *  |                     380 bytes of Label                       ...
 * ...                                                              |
 *  +---------------------------------------------------------------+
 *  |                   Repeats to end of buffer                   ...
 *  |                                                               |
 * ...                                                              |
 *  +---------------------------------------------------------------+
 * </pre>
 */
public class CountersManager extends CountersReader
{
    /**
     * Default type id of a counter when none is supplied.
     */
    public static final int DEFAULT_TYPE_ID = 0;

    private int idHighWaterMark = -1;
    private final IntArrayList freeList = new IntArrayList();

    /**
     * Create a new counter buffer manager over two buffers.
     *
     * @param metaDataBuffer containing the types, keys, and labels for the counters.
     * @param valuesBuffer   containing the values of the counters themselves.
     */
    public CountersManager(final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer)
    {
        super(metaDataBuffer, valuesBuffer);

        valuesBuffer.verifyAlignment();

        if (metaDataBuffer.capacity() < (valuesBuffer.capacity() * 2))
        {
            throw new IllegalArgumentException("Meta data buffer not sufficiently large");
        }
    }

    /**
     * Create a new counter buffer manager over two buffers.
     *
     * @param metaDataBuffer containing the types, keys, and labels for the counters.
     * @param valuesBuffer   containing the values of the counters themselves.
     * @param labelCharset   for the label encoding.
     */
    public CountersManager(
        final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer, final Charset labelCharset)
    {
        super(metaDataBuffer, valuesBuffer, labelCharset);

        valuesBuffer.verifyAlignment();

        if (metaDataBuffer.capacity() < (valuesBuffer.capacity() * 2))
        {
            throw new IllegalArgumentException("Meta data buffer not sufficiently large");
        }
    }

    /**
     * Allocate a new counter with a given label.
     *
     * @param label to describe the counter.
     * @return the id allocated for the counter.
     */
    public int allocate(final String label)
    {
        final int counterId = nextCounterId();
        if ((counterOffset(counterId) + COUNTER_LENGTH) > valuesBuffer.capacity())
        {
            throw new IllegalStateException("Unable to allocated counter, values buffer is full");
        }

        final int recordOffset = metaDataOffset(counterId);
        if ((recordOffset + METADATA_LENGTH) > metaDataBuffer.capacity())
        {
            throw new IllegalStateException("Unable to allocate counter, labels buffer is full");
        }

        try
        {
            metaDataBuffer.putInt(recordOffset + TYPE_ID_OFFSET, DEFAULT_TYPE_ID);
            labelValue(recordOffset, label);

            metaDataBuffer.putIntOrdered(recordOffset, RECORD_ALLOCATED);
        }
        catch (final Exception ex)
        {
            freeList.pushInt(counterId);
            LangUtil.rethrowUnchecked(ex);
        }

        return counterId;
    }

    /**
     * Allocate a new counter with a given label.
     * <p>
     * The key function will be called with a buffer with the exact length of available key space
     * in the record for the user to store what they want for the key. No offset is required.
     *
     * @param label   to describe the counter.
     * @param typeId  for the type of counter.
     * @param keyFunc for setting the key value for the counter.
     * @return the id allocated for the counter.
     */
    public int allocate(final String label, final int typeId, final Consumer<MutableDirectBuffer> keyFunc)
    {
        final int counterId = nextCounterId();
        if ((counterOffset(counterId) + COUNTER_LENGTH) > valuesBuffer.capacity())
        {
            throw new IllegalStateException("Unable to allocated counter, values buffer is full");
        }

        final int recordOffset = metaDataOffset(counterId);
        if ((recordOffset + METADATA_LENGTH) > metaDataBuffer.capacity())
        {
            throw new IllegalStateException("Unable to allocate counter, labels buffer is full");
        }

        try
        {
            metaDataBuffer.putInt(recordOffset + TYPE_ID_OFFSET, typeId);
            keyFunc.accept(new UnsafeBuffer(metaDataBuffer, recordOffset + KEY_OFFSET, MAX_KEY_LENGTH));
            labelValue(recordOffset, label);

            metaDataBuffer.putIntOrdered(recordOffset, RECORD_ALLOCATED);
        }
        catch (final Exception ex)
        {
            freeList.pushInt(counterId);
            LangUtil.rethrowUnchecked(ex);
        }

        return counterId;
    }

    /**
     * Allocate a counter record and wrap it with a new {@link AtomicCounter} for use.
     *
     * @param label to describe the counter.
     * @return a newly allocated {@link AtomicCounter}
     */
    public AtomicCounter newCounter(final String label)
    {
        return new AtomicCounter(valuesBuffer, allocate(label), this);
    }

    /**
     * Allocate a counter record and wrap it with a new {@link AtomicCounter} for use.
     *
     * @param label   to describe the counter.
     * @param typeId  for the type of counter.
     * @param keyFunc for setting the key value for the counter.
     * @return a newly allocated {@link AtomicCounter}
     */
    public AtomicCounter newCounter(final String label, final int typeId, final Consumer<MutableDirectBuffer> keyFunc)
    {
        return new AtomicCounter(valuesBuffer, allocate(label, typeId, keyFunc), this);
    }

    /**
     * Free the counter identified by counterId.
     *
     * @param counterId the counter to freed
     */
    public void free(final int counterId)
    {
        metaDataBuffer.putIntOrdered(metaDataOffset(counterId), RECORD_RECLAIMED);
        freeList.pushInt(counterId);
    }

    /**
     * Set an {@link AtomicCounter} value based on counterId.
     *
     * @param counterId to be set.
     * @param value     to set for the counter.
     */
    public void setCounterValue(final int counterId, final long value)
    {
        valuesBuffer.putLongOrdered(counterOffset(counterId), value);
    }

    private int nextCounterId()
    {
        if (freeList.isEmpty())
        {
            return ++idHighWaterMark;
        }

        final int counterId = freeList.popInt();
        valuesBuffer.putLongOrdered(counterOffset(counterId), 0L);

        return counterId;
    }

    private void labelValue(final int recordOffset, final String label)
    {
        if (StandardCharsets.US_ASCII == labelCharset)
        {
            metaDataBuffer.putStringAscii(
                recordOffset + LABEL_OFFSET,
                label.length() > MAX_LABEL_LENGTH ? label.substring(0, MAX_LABEL_LENGTH) : label);
        }
        else
        {
            final byte[] bytes = label.getBytes(labelCharset);

            if (bytes.length > MAX_LABEL_LENGTH)
            {
                metaDataBuffer.putInt(recordOffset + LABEL_OFFSET, MAX_LABEL_LENGTH);
                metaDataBuffer.putBytes(recordOffset + LABEL_OFFSET + SIZE_OF_INT, bytes, 0, MAX_LABEL_LENGTH);
            }
            else
            {
                metaDataBuffer.putInt(recordOffset + LABEL_OFFSET, bytes.length);
                metaDataBuffer.putBytes(recordOffset + LABEL_OFFSET + SIZE_OF_INT, bytes);
            }
        }
    }
}
