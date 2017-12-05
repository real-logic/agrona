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
import org.agrona.BufferUtil;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * Receiver that copies messages that have been broadcast to enable a simpler API for the client.
 */
public final class CopyBroadcastReceiver
{
    private final BroadcastReceiver receiver;
    private final UnsafeBuffer scratchBuffer;
    private final boolean failWhileSlow;

    /**
     * Wrap a {@link BroadcastReceiver} to simplify the API for receiving messages.
     *
     * @param failWhileSlow if {@code true} fails throwing {@link IllegalStateException} if not able to
     *                      keep-up with the transmitter, {@code false} otherwise
     * @param receiver      to be wrapped.
     */
    public CopyBroadcastReceiver(final BroadcastReceiver receiver, final boolean failWhileSlow)
    {
        this.receiver = receiver;
        final int maxTransmissionLength = RecordDescriptor.calculateAvailableMessageLength(receiver.recordSize());
        this.scratchBuffer = new UnsafeBuffer(
            BufferUtil.allocateDirectAligned(maxTransmissionLength, BitUtil.CACHE_LINE_LENGTH * 2));
        // If we're reconnecting to a broadcast buffer then we need to
        // scan ourselves up to date, otherwise we risk "falling behind"
        // the buffer due to the time taken to catchup.
        receiver.keepUpWithTrasmitter();
        this.failWhileSlow = failWhileSlow;
    }

    /**
     * Wrap a {@link BroadcastReceiver} to simplify the API for receiving messages.
     * <p>
     * It fails throwing {@link IllegalStateException} if not able to keep-up with the transmitter.
     *
     * @param receiver to be wrapped.
     */
    public CopyBroadcastReceiver(final BroadcastReceiver receiver)
    {
        this(receiver, true);
    }

    /**
     * Receive as many messages as are available from the broadcast buffer.
     *
     * @param handler to be called for each message received.
     * @param limit   the number of messages will be read in a single invocation.
     * @return the number of messages that have been received.
     */
    public int receive(final MessageHandler handler, final int limit)
    {
        final UnsafeBuffer scratchBuffer = this.scratchBuffer;
        final BroadcastReceiver receiver = this.receiver;
        final boolean failWhileSlow = this.failWhileSlow;
        for (int i = 0; i < limit; i++)
        {
            int received;
            while ((received = receive(receiver, failWhileSlow, scratchBuffer, handler)) == 0)
            {

            }
            if (received < 0)
            {
                return i;
            }
        }
        return limit;
    }

    private static int receive(
        final BroadcastReceiver receiver,
        final boolean failWhileSlow,
        final UnsafeBuffer scratchBuffer,
        final MessageHandler handler)
    {
        switch (receiver.receiveNext())
        {
            case LOSS:
                if (failWhileSlow)
                {
                    throw new IllegalStateException("Unable to keep up with broadcast buffer");
                }
                return 0;
            case ANY_AVAILABLE:
                final int length = receiver.length();
                final int msgTypeId = receiver.typeId();
                scratchBuffer.putBytes(0, receiver.buffer(), receiver.offset(), length);
                if (!receiver.validate())
                {
                    if (failWhileSlow)
                    {
                        throw new IllegalStateException("Unable to keep up with broadcast buffer");
                    }
                    return 0;
                }
                else
                {
                    handler.onMessage(msgTypeId, scratchBuffer, 0, length);
                    return 1;
                }
            case NOT_AVAILABLE:
                return -1;
            default:
                throw new AssertionError();
        }
    }
}
