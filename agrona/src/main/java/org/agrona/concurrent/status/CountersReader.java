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
package org.agrona.concurrent.status;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.collections.IntObjConsumer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.agrona.BitUtil.*;

/**
 * Reads the counters metadata and values buffers.
 * <p>
 * This class is threadsafe and can be used across threads.
 * <p>
 * <b>Values Buffer</b>
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                        Counter Value                          |
 *  |                                                               |
 *  +---------------------------------------------------------------+
 *  |                       Registration Id                         |
 *  |                                                               |
 *  +---------------------------------------------------------------+
 *  |                          Owner Id                             |
 *  |                                                               |
 *  +---------------------------------------------------------------+
 *  |                        Reference Id                           |
 *  |                                                               |
 *  +---------------------------------------------------------------+
 *  |                      96 bytes of padding                     ...
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
 *  |                  Free-for-reuse Deadline (ms)                 |
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
public class CountersReader
{
    /**
     * Callback function for consuming metadata records of counters.
     */
    @FunctionalInterface
    public interface MetaData
    {
        /**
         * Accept a metadata record.
         *
         * @param counterId of the counter.
         * @param typeId    of the counter.
         * @param keyBuffer for the counter.
         * @param label     for the counter.
         */
        void accept(int counterId, int typeId, DirectBuffer keyBuffer, String label);
    }

    /**
     * Callback function for consuming basic counter details and value.
     */
    @FunctionalInterface
    public interface CounterConsumer
    {
        /**
         * Accept the value for a counter.
         *
         * @param value     of the counter.
         * @param counterId of the counter
         * @param label     for the counter.
         */
        void accept(long value, int counterId, String label);
    }

    /**
     * Default type id of a counter when none is supplied.
     */
    public static final int DEFAULT_TYPE_ID = 0;

    /**
     * Default registration id of a counter when none is set.
     */
    public static final long DEFAULT_REGISTRATION_ID = 0;

    /**
     * Default owner id of a counter when none is set.
     */
    public static final long DEFAULT_OWNER_ID = 0;

    /**
     * Default reference id of a counter when none is set.
     */
    public static final long DEFAULT_REFERENCE_ID = 0;

    /**
     * Can be used to representing a null counter id when passed as an argument.
     */
    public static final int NULL_COUNTER_ID = -1;

    /**
     * Record has not been used.
     */
    public static final int RECORD_UNUSED = 0;

    /**
     * Record currently allocated for use.
     */
    public static final int RECORD_ALLOCATED = 1;

    /**
     * Record was active and now has been reclaimed.
     */
    public static final int RECORD_RECLAIMED = -1;

    /**
     * Deadline to indicate counter is not free to be reused.
     */
    public static final long NOT_FREE_TO_REUSE = Long.MAX_VALUE;

    /**
     * Offset in the record at which the registration id field is stored. When a counter is allocated the action
     * can be given a registration id to indicate a specific term of use. This can be useful to differentiate the
     * reuse of a counter id for another purpose even with the same type id.
     */
    public static final int REGISTRATION_ID_OFFSET = SIZE_OF_LONG;

    /**
     * Offset in the record at which the owner id field is stored. The owner is an abstract concept which can be
     * used to associate counters to an owner for lifecycle management.
     */
    public static final int OWNER_ID_OFFSET = REGISTRATION_ID_OFFSET + SIZE_OF_LONG;

    /**
     * Offset in the record at which the reference id field is stored. This id can be used to associate this
     * counter with a registration id for something else, such as an Image, Subscription, Publication, etc.
     */
    public static final int REFERENCE_ID_OFFSET = OWNER_ID_OFFSET + SIZE_OF_LONG;

    /**
     * Offset in the record at which the type id field is stored.
     */
    public static final int TYPE_ID_OFFSET = SIZE_OF_INT;

    /**
     * Offset in the record at which the deadline (in milliseconds) for when counter may be reused.
     */
    public static final int FREE_FOR_REUSE_DEADLINE_OFFSET = TYPE_ID_OFFSET + SIZE_OF_INT;

    /**
     * Offset in the record at which the key is stored.
     */
    public static final int KEY_OFFSET = FREE_FOR_REUSE_DEADLINE_OFFSET + SIZE_OF_LONG;

    /**
     * Offset in the record at which the label is stored.
     */
    public static final int LABEL_OFFSET = BitUtil.CACHE_LINE_LENGTH * 2;

    /**
     * Length of a counter label length including length prefix.
     */
    public static final int FULL_LABEL_LENGTH = BitUtil.CACHE_LINE_LENGTH * 6;

    /**
     * Maximum length of a label not including its length prefix.
     */
    public static final int MAX_LABEL_LENGTH = FULL_LABEL_LENGTH - SIZE_OF_INT;

    /**
     * Maximum length a key can be.
     */
    public static final int MAX_KEY_LENGTH = (CACHE_LINE_LENGTH * 2) - (SIZE_OF_INT * 2) - SIZE_OF_LONG;

    /**
     * Length of a metadata record in bytes.
     */
    public static final int METADATA_LENGTH = LABEL_OFFSET + FULL_LABEL_LENGTH;

    /**
     * Length of the space allocated to a counter that includes padding to avoid false sharing.
     */
    public static final int COUNTER_LENGTH = BitUtil.CACHE_LINE_LENGTH * 2;

    /**
     * Max counter ID.
     */
    protected final int maxCounterId;

    /**
     * Meta-data buffer.
     */
    protected final AtomicBuffer metaDataBuffer;

    /**
     * Values buffer.
     */
    protected final AtomicBuffer valuesBuffer;

    /**
     * Charset for the label.
     */
    protected final Charset labelCharset;

    /**
     * Construct a reader over buffers containing the values and associated metadata.
     * <p>
     * Counter labels default to {@link StandardCharsets#UTF_8}.
     *
     * @param metaDataBuffer containing the counter metadata.
     * @param valuesBuffer   containing the counter values.
     */
    public CountersReader(final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer)
    {
        this(metaDataBuffer, valuesBuffer, StandardCharsets.UTF_8);
    }

    /**
     * Construct a reader over buffers containing the values and associated metadata.
     *
     * @param metaDataBuffer containing the counter metadata.
     * @param valuesBuffer   containing the counter values.
     * @param labelCharset   for the label encoding.
     */
    public CountersReader(
        final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer, final Charset labelCharset)
    {
        this.maxCounterId = (valuesBuffer.capacity() / COUNTER_LENGTH) - 1;
        this.valuesBuffer = valuesBuffer;
        this.metaDataBuffer = metaDataBuffer;
        this.labelCharset = labelCharset;
    }

    /**
     * Get the maximum counter id which can be supported given the length of the values buffer.
     *
     * @return the maximum counter id which can be supported given the length of the values buffer.
     */
    public int maxCounterId()
    {
        return maxCounterId;
    }

    /**
     * Get the buffer containing the metadata for the counters.
     *
     * @return the buffer containing the metadata for the counters.
     */
    public AtomicBuffer metaDataBuffer()
    {
        return metaDataBuffer;
    }

    /**
     * Get the buffer containing the values for the counters.
     *
     * @return the buffer containing the values for the counters.
     */
    public AtomicBuffer valuesBuffer()
    {
        return valuesBuffer;
    }

    /**
     * The {@link Charset} used for the encoded label.
     *
     * @return the {@link Charset} used for the encoded label.
     */
    public Charset labelCharset()
    {
        return labelCharset;
    }

    /**
     * The offset in the counter buffer for a given counterId.
     *
     * @param counterId for which the offset should be provided.
     * @return the offset in the counter buffer.
     */
    public static int counterOffset(final int counterId)
    {
        return counterId * COUNTER_LENGTH;
    }

    /**
     * The offset in the metadata buffer for a given id.
     *
     * @param counterId for the record.
     * @return the offset at which the metadata record begins.
     */
    public static int metaDataOffset(final int counterId)
    {
        return counterId * METADATA_LENGTH;
    }

    /**
     * Iterate over all labels in the label buffer.
     *
     * @param consumer function to be called for each label.
     */
    public void forEach(final IntObjConsumer<String> consumer)
    {
        int counterId = 0;
        final AtomicBuffer metaDataBuffer = this.metaDataBuffer;

        for (int i = 0, capacity = metaDataBuffer.capacity(); i < capacity; i += METADATA_LENGTH)
        {
            final int recordStatus = metaDataBuffer.getIntVolatile(i);

            if (RECORD_ALLOCATED == recordStatus)
            {
                consumer.accept(counterId, labelValue(metaDataBuffer, i));
            }
            else if (RECORD_UNUSED == recordStatus)
            {
                break;
            }

            counterId++;
        }
    }

    /**
     * Iterate over the counters and provide the value and basic metadata.
     *
     * @param consumer for each allocated counter.
     */
    public void forEach(final CounterConsumer consumer)
    {
        int counterId = 0;
        final AtomicBuffer metaDataBuffer = this.metaDataBuffer;
        final AtomicBuffer valuesBuffer = this.valuesBuffer;

        for (int offset = 0, capacity = metaDataBuffer.capacity(); offset < capacity; offset += METADATA_LENGTH)
        {
            final int recordStatus = metaDataBuffer.getIntVolatile(offset);
            if (RECORD_ALLOCATED == recordStatus)
            {
                final String label = labelValue(metaDataBuffer, offset);
                final long value = valuesBuffer.getLongVolatile(counterOffset(counterId));
                consumer.accept(value, counterId, label);
            }
            else if (RECORD_UNUSED == recordStatus)
            {
                break;
            }

            counterId++;
        }
    }

    /**
     * Iterate over all the metadata in the buffer.
     *
     * @param metaData function to be called for each metadata record.
     */
    public void forEach(final MetaData metaData)
    {
        int counterId = 0;
        final AtomicBuffer metaDataBuffer = this.metaDataBuffer;

        for (int offset = 0, capacity = metaDataBuffer.capacity(); offset < capacity; offset += METADATA_LENGTH)
        {
            final int recordStatus = metaDataBuffer.getIntVolatile(offset);
            if (RECORD_ALLOCATED == recordStatus)
            {
                final int typeId = metaDataBuffer.getInt(offset + TYPE_ID_OFFSET);
                final String label = labelValue(metaDataBuffer, offset);
                final DirectBuffer keyBuffer = new UnsafeBuffer(metaDataBuffer, offset + KEY_OFFSET, MAX_KEY_LENGTH);

                metaData.accept(counterId, typeId, keyBuffer, label);
            }
            else if (RECORD_UNUSED == recordStatus)
            {
                break;
            }

            counterId++;
        }
    }

    /**
     * Iterate over allocated counters and find the first matching a given registration id.
     *
     * @param registrationId to find.
     * @return the counter if found otherwise {@link #NULL_COUNTER_ID}.
     */
    public int findByRegistrationId(final long registrationId)
    {
        int counterId = -1;
        final AtomicBuffer metaDataBuffer = this.metaDataBuffer;
        final int capacity = metaDataBuffer.capacity();

        for (int offset = 0, i = 0; offset < capacity; offset += METADATA_LENGTH, i++)
        {
            final int recordStatus = metaDataBuffer.getIntVolatile(offset);
            if (RECORD_ALLOCATED == recordStatus)
            {
                if (registrationId == valuesBuffer.getLongVolatile(counterOffset(i) + REGISTRATION_ID_OFFSET))
                {
                    counterId = i;
                    break;
                }
            }
            else if (RECORD_UNUSED == recordStatus)
            {
                break;
            }
        }

        return counterId;
    }

    /**
     * Iterate over allocated counters and find the first matching a given type id and registration id.
     *
     * @param typeId         to find.
     * @param registrationId to find.
     * @return the counter if found otherwise {@link #NULL_COUNTER_ID}.
     */
    public int findByTypeIdAndRegistrationId(final int typeId, final long registrationId)
    {
        int counterId = -1;
        final AtomicBuffer metaDataBuffer = this.metaDataBuffer;
        final int capacity = metaDataBuffer.capacity();

        for (int offset = 0, i = 0; offset < capacity; offset += METADATA_LENGTH, i++)
        {
            final int recordStatus = metaDataBuffer.getIntVolatile(offset);
            if (RECORD_ALLOCATED == recordStatus)
            {
                if (typeId == metaDataBuffer.getInt(offset + TYPE_ID_OFFSET) &&
                    registrationId == valuesBuffer.getLongVolatile(counterOffset(i) + REGISTRATION_ID_OFFSET))
                {
                    counterId = i;
                    break;
                }
            }
            else if (RECORD_UNUSED == recordStatus)
            {
                break;
            }
        }

        return counterId;
    }

    /**
     * Get the value for a given counter id as a volatile read.
     *
     * @param counterId to be read.
     * @return the current value of the counter.
     */
    public long getCounterValue(final int counterId)
    {
        validateCounterId(counterId);
        return valuesBuffer.getLongVolatile(counterOffset(counterId));
    }

    /**
     * Get the registration id for a given counter id as a volatile read. The registration identity may be assigned
     * when the counter is allocated to help avoid ABA issues if the counter id is reused.
     *
     * @param counterId to be read.
     * @return the current registration id of the counter.
     * @see #DEFAULT_REGISTRATION_ID
     */
    public long getCounterRegistrationId(final int counterId)
    {
        validateCounterId(counterId);
        return valuesBuffer.getLongVolatile(counterOffset(counterId) + REGISTRATION_ID_OFFSET);
    }

    /**
     * Get the owner id for a given counter id as a normal read. The owner identity may be assigned when the
     * counter is allocated to help associate it with the abstract concept of an owner for lifecycle management.
     *
     * @param counterId to be read.
     * @return the current owner id of the counter.
     * @see #DEFAULT_OWNER_ID
     */
    public long getCounterOwnerId(final int counterId)
    {
        validateCounterId(counterId);
        return valuesBuffer.getLong(counterOffset(counterId) + OWNER_ID_OFFSET);
    }

    /**
     * Get the reference id for a given counter id as a normal read. The id may be assigned when the
     * counter is allocated to help associate this counter with a registration id for an Image, Subscription,
     * Publication, etc.
     *
     * @param counterId to be read.
     * @return the current reference id of the counter.
     * @see #DEFAULT_REFERENCE_ID
     */
    public long getCounterReferenceId(final int counterId)
    {
        validateCounterId(counterId);
        return valuesBuffer.getLong(counterOffset(counterId) + REFERENCE_ID_OFFSET);
    }

    /**
     * Get the state for a given counter id as a volatile read.
     *
     * @param counterId to be read.
     * @return the current state of the counter.
     * @see #RECORD_UNUSED
     * @see #RECORD_ALLOCATED
     * @see #RECORD_RECLAIMED
     */
    public int getCounterState(final int counterId)
    {
        validateCounterId(counterId);
        return metaDataBuffer.getIntVolatile(metaDataOffset(counterId));
    }

    /**
     * Get the type id for a given counter id.
     *
     * @param counterId to be read.
     * @return the type id for a given counter id.
     * @see #DEFAULT_TYPE_ID
     */
    public int getCounterTypeId(final int counterId)
    {
        validateCounterId(counterId);
        return metaDataBuffer.getInt(metaDataOffset(counterId) + TYPE_ID_OFFSET);
    }

    /**
     * Get the deadline (ms) for when a given counter id may be reused.
     *
     * @param counterId to be read.
     * @return deadline (ms) for when a given counter id may be reused or {@link #NOT_FREE_TO_REUSE} if currently
     * in use.
     */
    public long getFreeForReuseDeadline(final int counterId)
    {
        validateCounterId(counterId);
        return metaDataBuffer.getLong(metaDataOffset(counterId) + FREE_FOR_REUSE_DEADLINE_OFFSET);
    }

    /**
     * Get the label for a given counter id as a volatile read.
     *
     * @param counterId to be read.
     * @return the label for the given counter id.
     */
    public String getCounterLabel(final int counterId)
    {
        validateCounterId(counterId);
        return labelValue(metaDataBuffer, metaDataOffset(counterId));
    }

    /**
     * Validate if counter Id is valid.
     *
     * @param counterId to validate.
     * @throws IllegalArgumentException if {@code counterId < 0 || counterId > maxCounterId}.
     */
    protected void validateCounterId(final int counterId)
    {
        if (counterId < 0 || counterId > maxCounterId)
        {
            throw new IllegalArgumentException(
                "counter id " + counterId + " out of range: 0 - maxCounterId=" + maxCounterId);
        }
    }

    private String labelValue(final AtomicBuffer metaDataBuffer, final int recordOffset)
    {
        final int labelLength = metaDataBuffer.getIntVolatile(recordOffset + LABEL_OFFSET);
        final byte[] stringInBytes = new byte[labelLength];
        metaDataBuffer.getBytes(recordOffset + LABEL_OFFSET + SIZE_OF_INT, stringInBytes);

        return new String(stringInBytes, labelCharset);
    }
}
