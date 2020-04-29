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
import org.agrona.LangUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.IntArrayList;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.EpochClock;
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
 *  |                   Free-for-reuse Deadline                     |
 *  |                                                               |
 *  +---------------------------------------------------------------+
 *  |                      112 bytes for key                       ...
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

    private final long freeToReuseTimeoutMs;
    private int idHighWaterMark = -1;
    private final IntArrayList freeList = new IntArrayList();
    private final EpochClock epochClock;

    /**
     * Create a new counter buffer manager over two buffers.
     *
     * @param metaDataBuffer       containing the types, keys, and labels for the counters.
     * @param valuesBuffer         containing the values of the counters themselves.
     * @param labelCharset         for the label encoding.
     * @param epochClock           to use for determining time for keep counter from being reused after being freed.
     * @param freeToReuseTimeoutMs timeout (in milliseconds) to keep counter from being reused after being freed.
     */
    public CountersManager(
        final AtomicBuffer metaDataBuffer,
        final AtomicBuffer valuesBuffer,
        final Charset labelCharset,
        final EpochClock epochClock,
        final long freeToReuseTimeoutMs)
    {
        super(metaDataBuffer, valuesBuffer, labelCharset);

        valuesBuffer.verifyAlignment();
        this.epochClock = epochClock;
        this.freeToReuseTimeoutMs = freeToReuseTimeoutMs;

        if (metaDataBuffer.capacity() < (valuesBuffer.capacity() * 2))
        {
            throw new IllegalArgumentException("metadata buffer not sufficiently large");
        }
    }

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
        this.epochClock = () -> 0;
        this.freeToReuseTimeoutMs = 0;

        if (metaDataBuffer.capacity() < (valuesBuffer.capacity() * 2))
        {
            throw new IllegalArgumentException("metadata buffer not sufficiently large");
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
        this(metaDataBuffer, valuesBuffer, labelCharset, () -> 0, 0);
    }

    /**
     * Allocate a new counter with a given label with a default type of {@link #DEFAULT_TYPE_ID}.
     *
     * @param label to describe the counter.
     * @return the id allocated for the counter.
     */
    public int allocate(final String label)
    {
        return allocate(label, DEFAULT_TYPE_ID);
    }

    /**
     * Allocate a new counter with a given label and type.
     *
     * @param label  to describe the counter.
     * @param typeId for the type of counter.
     * @return the id allocated for the counter.
     */
    public int allocate(final String label, final int typeId)
    {
        final int counterId = nextCounterId();
        checkCountersCapacity(counterId);

        final int recordOffset = metaDataOffset(counterId);
        checkMetaDataCapacity(recordOffset);

        try
        {
            metaDataBuffer.putInt(recordOffset + TYPE_ID_OFFSET, typeId);
            metaDataBuffer.putLong(recordOffset + FREE_FOR_REUSE_DEADLINE_OFFSET, NOT_FREE_TO_REUSE);
            putLabel(recordOffset, label);

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
        checkCountersCapacity(counterId);

        final int recordOffset = metaDataOffset(counterId);
        checkMetaDataCapacity(recordOffset);

        try
        {
            metaDataBuffer.putInt(recordOffset + TYPE_ID_OFFSET, typeId);
            keyFunc.accept(new UnsafeBuffer(metaDataBuffer, recordOffset + KEY_OFFSET, MAX_KEY_LENGTH));
            metaDataBuffer.putLong(recordOffset + FREE_FOR_REUSE_DEADLINE_OFFSET, NOT_FREE_TO_REUSE);
            putLabel(recordOffset, label);

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
     * Allocate a counter with the minimum of allocation by allowing the label an key to be provided and copied.
     * <p>
     * If the keyBuffer is null then a copy of the key is not attempted.
     *
     * @param typeId      for the counter.
     * @param keyBuffer   containing the optional key for the counter.
     * @param keyOffset   within the keyBuffer at which the key begins.
     * @param keyLength   of the key in the keyBuffer.
     * @param labelBuffer containing the mandatory label for the counter.
     * @param labelOffset within the labelBuffer at which the label begins.
     * @param labelLength of the label in the labelBuffer.
     * @return the id allocated for the counter.
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
        final int counterId = nextCounterId();
        checkCountersCapacity(counterId);

        final int recordOffset = metaDataOffset(counterId);
        checkMetaDataCapacity(recordOffset);

        try
        {
            metaDataBuffer.putInt(recordOffset + TYPE_ID_OFFSET, typeId);
            metaDataBuffer.putLong(recordOffset + FREE_FOR_REUSE_DEADLINE_OFFSET, NOT_FREE_TO_REUSE);

            if (null != keyBuffer)
            {
                final int length = Math.min(keyLength, MAX_KEY_LENGTH);
                metaDataBuffer.putBytes(recordOffset + KEY_OFFSET, keyBuffer, keyOffset, length);
            }

            final int length = Math.min(labelLength, MAX_LABEL_LENGTH);
            metaDataBuffer.putInt(recordOffset + LABEL_OFFSET, length);
            metaDataBuffer.putBytes(recordOffset + LABEL_OFFSET + SIZE_OF_INT, labelBuffer, labelOffset, length);

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
     * Allocate a counter record and wrap it with a new {@link AtomicCounter} for use with a default type
     * of {@link #DEFAULT_TYPE_ID}.
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
     * @param label  to describe the counter.
     * @param typeId for the type of counter.
     * @return a newly allocated {@link AtomicCounter}
     */
    public AtomicCounter newCounter(final String label, final int typeId)
    {
        return new AtomicCounter(valuesBuffer, allocate(label, typeId), this);
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
     * Allocate a counter record and wrap it with a new {@link AtomicCounter} for use.
     * <p>
     * If the keyBuffer is null then a copy of the key is not attempted.
     *
     * @param typeId      for the counter.
     * @param keyBuffer   containing the optional key for the counter.
     * @param keyOffset   within the keyBuffer at which the key begins.
     * @param keyLength   of the key in the keyBuffer.
     * @param labelBuffer containing the mandatory label for the counter.
     * @param labelOffset within the labelBuffer at which the label begins.
     * @param labelLength of the label in the labelBuffer.
     * @return the id allocated for the counter.
     */
    public AtomicCounter newCounter(
        final int typeId,
        final DirectBuffer keyBuffer,
        final int keyOffset,
        final int keyLength,
        final DirectBuffer labelBuffer,
        final int labelOffset,
        final int labelLength)
    {
        return new AtomicCounter(
            valuesBuffer,
            allocate(typeId, keyBuffer, keyOffset, keyLength, labelBuffer, labelOffset, labelLength),
            this);
    }

    /**
     * Free the counter identified by counterId.
     *
     * @param counterId the counter to freed
     */
    public void free(final int counterId)
    {
        final int recordOffset = metaDataOffset(counterId);

        metaDataBuffer.putIntOrdered(recordOffset, RECORD_RECLAIMED);
        metaDataBuffer.setMemory(recordOffset + KEY_OFFSET, MAX_KEY_LENGTH, (byte)0);
        metaDataBuffer.putLong(
            recordOffset + FREE_FOR_REUSE_DEADLINE_OFFSET, epochClock.time() + freeToReuseTimeoutMs);
        freeList.addInt(counterId);
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

    /**
     * Set an {@link AtomicCounter} label based on counterId.
     *
     * @param counterId to be set.
     * @param label     to set for the counter.
     */
    public void setCounterLabel(final int counterId, final String label)
    {
        putLabel(metaDataOffset(counterId), label);
    }

    /**
     * Set an {@link AtomicCounter} key based on counterId, using a consumer callback to update the key buffer.
     *
     * @param counterId to be set.
     * @param keyFunc   callback used to set the key.
     */
    public void setCounterKey(final int counterId, final Consumer<MutableDirectBuffer> keyFunc)
    {
        keyFunc.accept(new UnsafeBuffer(metaDataBuffer, metaDataOffset(counterId) + KEY_OFFSET, MAX_KEY_LENGTH));
    }

    /**
     * Set an {@link AtomicCounter} key based on counterId, copying the value from the supplied buffer.
     *
     * @param id        to be set
     * @param keyBuffer containing the updated key
     * @param offset    offset into buffer
     * @param length    length of data to copy
     */
    public void setCounterKey(final int id, final DirectBuffer keyBuffer, final int offset, final int length)
    {
        if (length > MAX_KEY_LENGTH)
        {
            throw new IllegalArgumentException("Supplied key is too long: " + length + ", max: " + MAX_KEY_LENGTH);
        }

        metaDataBuffer.putBytes(metaDataOffset(id) + KEY_OFFSET, keyBuffer, offset, length);
    }


    /**
     * Set an {@link AtomicCounter} label based on counterId.
     *
     * @param counterId to be set.
     * @param label     to set for the counter.
     */
    public void appendToLabel(final int counterId, final String label)
    {
        appendLabel(metaDataOffset(counterId), label);
    }

    private int nextCounterId()
    {
        final long nowMs = epochClock.time();

        for (int i = 0, size = freeList.size(); i < size; i++)
        {
            final int counterId = freeList.getInt(i);

            final long deadlineMs = metaDataBuffer.getLongVolatile(
                metaDataOffset(counterId) + FREE_FOR_REUSE_DEADLINE_OFFSET);

            if (nowMs >= deadlineMs)
            {
                freeList.remove(i);
                valuesBuffer.putLongOrdered(counterOffset(counterId), 0L);

                return counterId;
            }
        }

        return ++idHighWaterMark;
    }

    private void putLabel(final int recordOffset, final String label)
    {
        if (StandardCharsets.US_ASCII == labelCharset)
        {
            final int length = metaDataBuffer.putStringWithoutLengthAscii(
                recordOffset + LABEL_OFFSET + SIZE_OF_INT, label, 0, MAX_LABEL_LENGTH);
            metaDataBuffer.putIntOrdered(recordOffset + LABEL_OFFSET, length);
        }
        else
        {
            final byte[] bytes = label.getBytes(labelCharset);
            final int length = Math.min(bytes.length, MAX_LABEL_LENGTH);

            metaDataBuffer.putBytes(recordOffset + LABEL_OFFSET + SIZE_OF_INT, bytes, 0, length);
            metaDataBuffer.putIntOrdered(recordOffset + LABEL_OFFSET, length);
        }
    }

    private void appendLabel(final int recordOffset, final String suffix)
    {
        final int existingLength = metaDataBuffer.getInt(recordOffset + LABEL_OFFSET);
        final int maxSuffixLength = MAX_LABEL_LENGTH - existingLength;

        if (StandardCharsets.US_ASCII == labelCharset)
        {
            final int suffixLength = metaDataBuffer.putStringWithoutLengthAscii(
                recordOffset + LABEL_OFFSET + SIZE_OF_INT + existingLength, suffix, 0, maxSuffixLength);

            metaDataBuffer.putIntOrdered(recordOffset + LABEL_OFFSET, existingLength + suffixLength);
        }
        else
        {
            final byte[] suffixBytes = suffix.getBytes(labelCharset);
            final int suffixLength = Math.min(suffixBytes.length, maxSuffixLength);

            metaDataBuffer.putBytes(
                recordOffset + LABEL_OFFSET + SIZE_OF_INT + existingLength, suffixBytes, 0, suffixLength);
            metaDataBuffer.putIntOrdered(recordOffset + LABEL_OFFSET, existingLength + suffixLength);
        }
    }

    private void checkCountersCapacity(final int counterId)
    {
        if ((counterOffset(counterId) + COUNTER_LENGTH) > valuesBuffer.capacity())
        {
            throw new IllegalStateException("unable to allocate counter, values buffer is full");
        }
    }

    private void checkMetaDataCapacity(final int recordOffset)
    {
        if ((recordOffset + METADATA_LENGTH) > metaDataBuffer.capacity())
        {
            throw new IllegalStateException("unable to allocate counter, metadata buffer is full");
        }
    }
}
