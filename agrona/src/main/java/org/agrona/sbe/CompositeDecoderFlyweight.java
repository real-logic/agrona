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

import org.agrona.DirectBuffer;

/**
 * A flyweight for decoding an SBE Composite type.
 */
public interface CompositeDecoderFlyweight extends DecoderFlyweight
{
    /**
     * Wrap a buffer for decoding at a given offset.
     *
     * @param buffer containing the encoded SBE Composite type.
     * @param offset at which the encoded SBE Composite type begins.
     * @return the {@link CompositeDecoderFlyweight} for fluent API design.
     */
    CompositeDecoderFlyweight wrap(DirectBuffer buffer, int offset);
}

