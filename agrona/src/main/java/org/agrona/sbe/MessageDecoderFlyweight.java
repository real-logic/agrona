/*
 * Copyright 2014-2021 Real Logic Limited.
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
package org.agrona.sbe;

import org.agrona.DirectBuffer;

/**
 * A flyweight for decoding an SBE message from a buffer.
 */
public interface MessageDecoderFlyweight extends MessageFlyweight, DecoderFlyweight
{
    /**
     * Wrap a buffer containing an encoded message for decoding.
     *
     * @param buffer            containing the encoded message.
     * @param offset            in the buffer at which the decoding should begin.
     * @param actingBlockLength the root block length the decoder should act on.
     * @param actingVersion     the version of the encoded message.
     * @return the {@link MessageDecoderFlyweight} for fluent API design.
     */
    MessageDecoderFlyweight wrap(DirectBuffer buffer, int offset, int actingBlockLength, int actingVersion);

    /**
     * Populate the supplied {@link StringBuilder} with the string representation of the message.
     *
     * @param builder destination for the string representation.
     * @return the supplied builder for a fluent API.
     */
    StringBuilder appendTo(StringBuilder builder);

    /**
     * Gets the total length of this flyweight by moving from the end of the block though all the variable
     * length fields.
     *
     * @return the total decoded length for the flyweight.
     */
    default int sbeDecodedLength()
    {
        throw new UnsupportedOperationException("not implemented by this version of SBE");
    }
}
