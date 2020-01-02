/*
 * Copyright 2014-2020 Real Logic Ltd.
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
package org.agrona.concurrent.ringbuffer;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;

/**
 * Ring-buffer for the concurrent exchanging of binary encoded messages from producer(s) to consumer(s)
 * in a FIFO manner.
 */
public interface RingBuffer
{
    /**
     * Get the capacity of the ring-buffer in bytes for exchange.
     *
     * @return the capacity of the ring-buffer in bytes for exchange.
     */
    int capacity();

    /**
     * Non-blocking write of an message to an underlying ring-buffer.
     *
     * @param msgTypeId type of the message encoding.
     * @param srcBuffer containing the encoded binary message.
     * @param srcIndex  at which the encoded message begins.
     * @param length    of the encoded message in bytes.
     * @return true if written to the ring-buffer, or false if insufficient space exists.
     * @throws IllegalArgumentException if the length is greater than {@link RingBuffer#maxMsgLength()}
     */
    boolean write(int msgTypeId, DirectBuffer srcBuffer, int srcIndex, int length);

    /**
     * Read as many messages as are available to the end of the ring buffer.
     * <p>
     * If the ring buffer wraps or encounters a type of record, such a a padding record, then an implementation
     * may choose to return and expect the caller to try again. The {@link #size()} method may be called to
     * determine of a backlog of message bytes remains in the ring buffer.
     *
     * @param handler to be called for processing each message in turn.
     * @return the number of messages that have been processed.
     */
    int read(MessageHandler handler);

    /**
     * Read as many messages as are available to end of the ring buffer to up a supplied maximum.
     * <p>
     * If the ring buffer wraps or encounters a type of record, such a a padding record, then an implementation
     * may choose to return and expect the caller to try again. The {@link #size()} method may be called to
     * determine of a backlog of message bytes remains in the ring buffer.
     *
     * @param handler           to be called for processing each message in turn.
     * @param messageCountLimit the number of messages will be read in a single invocation.
     * @return the number of messages that have been processed.
     */
    int read(MessageHandler handler, int messageCountLimit);

    /**
     * The maximum message length in bytes supported by the underlying ring buffer.
     *
     * @return the maximum message length in bytes supported by the underlying ring buffer.
     */
    int maxMsgLength();

    /**
     * Get the next value that can be used for a correlation id on an message when a response needs to be correlated.
     * <p>
     * This method should be thread safe.
     *
     * @return the next value in the correlation sequence.
     */
    long nextCorrelationId();

    /**
     * Get the underlying buffer used by the RingBuffer for storage.
     *
     * @return the underlying buffer used by the RingBuffer for storage.
     */
    AtomicBuffer buffer();

    /**
     * Set the time of the last consumer heartbeat.
     * <p>
     * <b>Note:</b> The value for time must be valid across processes which means {@link System#nanoTime()}
     * is not a valid option.
     *
     * @param time of the last consumer heartbeat.
     */
    void consumerHeartbeatTime(long time);

    /**
     * The time of the last consumer heartbeat.
     *
     * @return the time of the last consumer heartbeat.
     */
    long consumerHeartbeatTime();

    /**
     * The position in bytes from start up of the producers. The figure includes the headers.
     * This is the range they are working with but could still be in the act of working with.
     *
     * @return number of bytes produced by the producers in claimed space.
     */
    long producerPosition();

    /**
     * The position in bytes from start up for the consumers. The figure includes the headers.
     *
     * @return the count of bytes consumed by the consumers.
     */
    long consumerPosition();

    /**
     * Size of the buffer backlog in bytes between producers and consumers. The value includes the size of headers.
     * <p>
     * This method gives a concurrent snapshot of the buffer whereby a concurrent read or write may be
     * partially complete and thus the value should be taken as an indication.
     *
     * @return size of the backlog of bytes in the buffer between producers and consumers.
     */
    int size();

    /**
     * Unblock a multi-producer ring buffer when a producer has died during the act of offering. The operation will
     * scan from the consumer position up to the producer position.
     * <p>
     * If no action is required at the position then none will be taken.
     *
     * @return true of an unblocking action was taken otherwise false.
     */
    boolean unblock();
}
