/*
 * Copyright 2014-2024 Real Logic Limited.
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
 * Mutable boolean valid that is useful for capturing a value when using lambdas or collections.
 */
public class MutableBoolean
{
    /**
     * For convenient access.
     */
    public boolean value;

    /**
     * Default constructor.
     */
    public MutableBoolean()
    {
    }

    /**
     * Construct with a default value.
     *
     * @param value to be set initially.
     */
    public MutableBoolean(final boolean value)
    {
        this.value = value;
    }

    /**
     * Get the current value.
     *
     * @return the current value.
     */
    public boolean get()
    {
        return value;
    }

    /**
     * Set the current value.
     *
     * @param value to be set.
     */
    public void set(final boolean value)
    {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final MutableBoolean that = (MutableBoolean)o;

        return value == that.value;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return Boolean.hashCode(value);
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return Boolean.toString(value);
    }
}
