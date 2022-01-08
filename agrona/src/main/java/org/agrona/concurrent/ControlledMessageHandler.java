/*
 * Copyright 2014-2022 Real Logic Limited.
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

import org.agrona.MutableDirectBuffer;

/**
 * Callback interface for processing of messages that are read from a buffer.
 */
@FunctionalInterface
public interface ControlledMessageHandler
{
    /**
     * Action to be taken on return from {@link #onMessage(int, MutableDirectBuffer, int, int)}.
     */
    enum Action
    {
        /**
         * Abort the current read operation and do not advance the position for this message.
         */
        ABORT,

        /**
         * Break from the current read operation and commit the position as of the end of the current message
         * being handled.
         */
        BREAK,

        /**
         * Continue processing but commit the position as of the end of the current message so that
         * flow control is applied to this point.
         */
        COMMIT,

        /**
         * Continue processing until limit or no messages with position commit at end of read as the in
         * {@link #onMessage(int, MutableDirectBuffer, int, int)}.
         */
        CONTINUE,
    }

    /**
     * Called for the processing of each message read from a buffer in turn.
     *
     * @param msgTypeId type of the encoded message.
     * @param buffer    containing the encoded message.
     * @param index     at which the encoded message begins.
     * @param length    in bytes of the encoded message.
     * @return {@link Action} to be taken to control how the read progresses.
     */
    Action onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length);
}
