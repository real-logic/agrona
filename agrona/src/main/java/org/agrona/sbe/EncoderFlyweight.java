/*
 * Copyright 2014-2018 Real Logic Ltd.
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
package org.agrona.sbe;

import org.agrona.MutableDirectBuffer;

/**
 * A flyweight for encoding an SBE type.
 */
public interface EncoderFlyweight extends Flyweight
{
    /**
     * Buffer in which the flyweight is encoded.
     *
     * @return buffer in which the flyweight is encoded.
     */
    MutableDirectBuffer buffer();

    /**
     * Wrap a buffer for encoding at a given offset.
     *
     * @param buffer to be wrapped and into which the type will be encoded.
     * @param offset at which the encoded object will be begin.
     * @return the {@link EncoderFlyweight} for fluent API design.
     */
    EncoderFlyweight wrap(MutableDirectBuffer buffer, int offset);
}

