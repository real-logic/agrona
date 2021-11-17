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
package org.agrona.collections;

/**
 * Sentinel value used in collections supporting null value references.
 */
public final class NullReference
{
    /**
     * Single instance of the {@link NullReference} class.
     */
    public static final NullReference INSTANCE = new NullReference();

    private NullReference()
    {
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object obj)
    {
        return obj == this;
    }
}
