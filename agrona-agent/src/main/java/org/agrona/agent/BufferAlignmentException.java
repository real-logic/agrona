/*
 * Copyright 2014-2023 Real Logic Limited.
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
package org.agrona.agent;

/**
 * Runtime Exception thrown by {@link BufferAlignmentAgent} when an unaligned memory access is detected.
 * <p>
 * Package-protected to discourage catching since this as agent should be used only for testing and debugging.
 */
public class BufferAlignmentException extends RuntimeException
{
    private static final long serialVersionUID = 4196043654912374628L;
    private final int index;
    private final long addressOffset;

    /**
     * Create exception with details about the unaligned access.
     *
     * @param prefix        for the error message.
     * @param index         at which the unaligned access occurred.
     * @param addressOffset pointing to the beginning of the underlying buffer.
     */
    public BufferAlignmentException(final String prefix, final int index, final long addressOffset)
    {
        super(prefix + " (index=" + index + ", addressOffset=" + addressOffset + ")");
        this.index = index;
        this.addressOffset = addressOffset;
    }

    /**
     * Returns an index at which unaligned access occurred.
     *
     * @return index.
     */
    public int index()
    {
        return index;
    }

    /**
     * Returns an address offset into the start of the underlying buffer.
     *
     * @return address of the beginning of the underlying buffer.
     */
    public long addressOffset()
    {
        return addressOffset;
    }
}
