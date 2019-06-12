package org.agrona.concurrent.ringbuffer;

import org.agrona.MutableDirectBuffer;

@FunctionalInterface
public interface WriteHandler<T>
{
    /**
     * This is called by a {@link RingBuffer}'s write method when there is sufficient length.
     *
     * @param buffer ring-buffer's underlying {@link MutableDirectBuffer} to write to.
     * @param offset the current offset to write to the buffer. If you don't write to this offset and
     *     the requested length you will corrupt the ring-buffer.
     * @param t The object to write to the buffer.
     */
    void handle(MutableDirectBuffer buffer, int offset, T t);
}
