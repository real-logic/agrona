/*
 * Copyright 2014 - 2016 Real Logic Ltd.
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
package uk.co.real_logic.agrona.concurrent;

import uk.co.real_logic.agrona.MutableDirectBuffer;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;

import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_INT;

/**
 * Manages the allocation and freeing of counters that are normally stored in a memory-mapped file.
 *
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
 *
 * <b>Metadata Buffer</b>
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |R|                      Label Length                           |
 *  +---------------------------------------------------------------+
 *  |                  124 bytes of Label in UTF-8                 ...
 * ...                                                              |
 *  +---------------------------------------------------------------+
 *  |                          Type Id                              |
 *  +---------------------------------------------------------------+
 *  |                      124 bytes for key                       ...
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

    /**
     * Default function to set a key when none is supplied.
     */
    public static final Consumer<MutableDirectBuffer> DEFAULT_KEY_FUNC = (ignore) -> { };

    private int idHighWaterMark = -1;
    private final Deque<Integer> freeList = new LinkedList<>();

    /**
     * Create a new counter buffer manager over two buffers.
     *
     * @param metadataBuffer containing the types, keys, and labels for the counters.
     * @param valuesBuffer   containing the values of the counters themselves.
     */
    public CountersManager(final AtomicBuffer metadataBuffer, final AtomicBuffer valuesBuffer)
    {
        super(valuesBuffer, metadataBuffer);
        valuesBuffer.verifyAlignment();

        if (metadataBuffer.capacity() < (valuesBuffer.capacity() * 2))
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
        return allocate(label, DEFAULT_TYPE_ID, DEFAULT_KEY_FUNC);
    }

    /**
     * Allocate a new counter with a given label.
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
            throw new IllegalArgumentException("Unable to allocated counter, values buffer is full");
        }

        final int recordOffset = metadataOffset(counterId);
        if ((recordOffset + METADATA_LENGTH) > metadataBuffer.capacity())
        {
            throw new IllegalArgumentException("Unable to allocate counter, labels buffer is full");
        }

        metadataBuffer.putInt(recordOffset + FULL_LABEL_LENGTH, typeId);
        final int keyOffset = FULL_LABEL_LENGTH + SIZE_OF_INT;
        keyFunc.accept(new UnsafeBuffer(metadataBuffer, recordOffset + keyOffset, METADATA_LENGTH - keyOffset));

        metadataBuffer.putStringUtf8(recordOffset, label, MAX_LABEL_LENGTH);

        return counterId;
    }

    public AtomicCounter newCounter(final String label)
    {
        return new AtomicCounter(valuesBuffer, allocate(label), this);
    }

    /**
     * Free the counter identified by counterId.
     *
     * @param counterId the counter to freed
     */
    public void free(final int counterId)
    {
        metadataBuffer.putInt(metadataOffset(counterId), UNREGISTERED_LABEL_LENGTH);
        freeList.push(counterId);
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

        final int counterId = freeList.pop();
        valuesBuffer.putLongOrdered(counterOffset(counterId), 0L);

        return counterId;
    }
}
