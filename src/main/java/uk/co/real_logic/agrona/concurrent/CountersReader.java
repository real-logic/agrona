/*
 * Copyright 2015 Real Logic Ltd.
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

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;

import java.util.function.BiConsumer;

import static uk.co.real_logic.agrona.BitUtil.CACHE_LINE_LENGTH;
import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_INT;

/**
 * Reads the counters metadata and values buffers.
 *
 * This class is threadsafe and can be used across threads.
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
 *  |                  124 bytes of Label in UTF-8                 ...
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
     * Record has not been used.
     */
    public static final int RECORD_UNUSED = 0;

    /**
     * Record currently in active use.
     */
    public static final int RECORD_ACTIVE = 1;

    /**
     * Record was active and now has been reclaimed.
     */
    public static final int RECORD_RECLAIMED = -1;

    /**
     * Length of a meta data record in bytes.
     */
    public static final int METADATA_LENGTH = BitUtil.CACHE_LINE_LENGTH * 4;

    /**
     * Offset in the record at which the type id field is stored.
     */
    public static final int TYPE_ID_OFFSET = SIZE_OF_INT;

    /**
     * Offset in the record at which the key is stored.
     */
    public static final int KEY_OFFSET = TYPE_ID_OFFSET + SIZE_OF_INT;

    /**
     * Offset in the record at which the label is stored.
     */
    public static final int LABEL_OFFSET = BitUtil.CACHE_LINE_LENGTH * 2;

    /**
     * Length of a counter label length including length prefix.
     */
    public static final int FULL_LABEL_LENGTH = BitUtil.CACHE_LINE_LENGTH * 2;

    /**
     * Maximum length of a label not including its length prefix.
     */
    public static final int MAX_LABEL_LENGTH = FULL_LABEL_LENGTH - SIZE_OF_INT;

    /**
     * Maximum length a key can be.
     */
    public static final int MAX_KEY_LENGTH = (CACHE_LINE_LENGTH * 2) - (SIZE_OF_INT * 2);

    /**
     * Length of the space allocated to a counter that includes padding to avoid false sharing.
     */
    public static final int COUNTER_LENGTH = BitUtil.CACHE_LINE_LENGTH * 2;

    protected final AtomicBuffer metaDataBuffer;
    protected final AtomicBuffer valuesBuffer;

    /**
     * Construct a reader over buffers containing the values and associated metadata.
     *
     * @param metaDataBuffer containing the counter metadata.
     * @param valuesBuffer   containing the counter values.
     */
    public CountersReader(final AtomicBuffer metaDataBuffer, final AtomicBuffer valuesBuffer)
    {
        this.valuesBuffer = valuesBuffer;
        this.metaDataBuffer = metaDataBuffer;
    }

    /**
     * The offset in the counter buffer for a given counterId.
     *
     * @param counterId for which the offset should be provided.
     * @return the offset in the counter buffer.
     */
    public static int counterOffset(int counterId)
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
    public void forEach(final BiConsumer<Integer, String> consumer)
    {
        int recordOffset = 0;
        int id = 0;
        int recordStatus;

        while ((recordStatus = metaDataBuffer.getIntVolatile(recordOffset)) != RECORD_UNUSED)
        {
            if (RECORD_ACTIVE == recordStatus)
            {
                final String label = metaDataBuffer.getStringUtf8(recordOffset + LABEL_OFFSET);
                consumer.accept(id, label);
            }

            recordOffset += METADATA_LENGTH;
            id++;
        }
    }

    /**
     * Iterate over all the metadata in the buffer.
     *
     * @param metadataConsumer function to be called for each metadata record.
     */
    public void forEach(final MetadataConsumer metadataConsumer)
    {
        int recordOffset = 0;
        int id = 0;
        int recordStatus;

        while ((recordStatus = metaDataBuffer.getIntVolatile(recordOffset)) != RECORD_UNUSED)
        {
            if (RECORD_ACTIVE == recordStatus)
            {
                final int typeId = metaDataBuffer.getInt(recordOffset + TYPE_ID_OFFSET);
                final String label = metaDataBuffer.getStringUtf8(recordOffset + LABEL_OFFSET);
                final DirectBuffer key = new UnsafeBuffer(metaDataBuffer, recordOffset + KEY_OFFSET, MAX_KEY_LENGTH);

                metadataConsumer.accept(id, typeId, key, label);
            }

            recordOffset += METADATA_LENGTH;
            id++;
        }
    }

    /**
     * Get the value for a given counter id as a volatile read.
     *
     * @param counterId to be read.
     * @return the current value of the counter.
     */
    public long getCounterValue(final int counterId)
    {
        return valuesBuffer.getLongVolatile(counterOffset(counterId));
    }

    /**
     * Callback function for consuming metadata records of counters.
     */
    @FunctionalInterface
    public interface MetadataConsumer
    {
        /**
         * Accept a metadata record.
         *
         * @param id    of the counter.
         * @param type  of the counter.
         * @param key   for the counter.
         * @param label for the counter.
         */
        void accept(int id, int type, DirectBuffer key, String label);
    }
}
