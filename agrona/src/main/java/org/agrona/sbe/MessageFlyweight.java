/*
 * Copyright 2014-2025 Real Logic Limited.
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

/**
 * Common behaviour to SBE Message encoder and decoder flyweights.
 */
public interface MessageFlyweight extends Flyweight
{
    /**
     * The length of the root block in bytes.
     *
     * @return the length of the root block in bytes.
     */
    int sbeBlockLength();

    /**
     * The SBE template identifier for the message.
     *
     * @return the SBE template identifier for the message.
     */
    int sbeTemplateId();

    /**
     * The semantic type of the message which is typically the semantic equivalent in the FIX repository.
     *
     * @return the semantic type of the message which is typically the semantic equivalent in the FIX repository.
     */
    String sbeSemanticType();

    /**
     * The current limit in the buffer at which the message is being encoded or decoded.
     *
     * @return the current limit in the buffer at which the message is being encoded or decoded.
     */
    int limit();

    /**
     * The current limit in the buffer at which the message is being encoded or decoded.
     *
     * @param limit in the buffer at which the message is being encoded or decoded.
     */
    void limit(int limit);
}
