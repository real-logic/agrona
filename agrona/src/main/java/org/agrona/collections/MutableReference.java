/*
 * Copyright 2014-2020 Real Logic Limited.
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

import java.io.Serializable;

/**
 * Mutable reference that is useful for capturing an object reference when using lambdas.
 *
 * @param <T> type of the reference.
 */
@SuppressWarnings("serial")
public class MutableReference<T> implements Serializable
{
    private static final long serialVersionUID = 321431528882718691L;
    /**
     * For convenient access.
     */
    public T ref;

    /**
     * Default constructor.
     */
    public MutableReference()
    {
    }

    /**
     * Set the reference at construction.
     *
     * @param ref to be set.
     */
    public MutableReference(final T ref)
    {
        this.ref = ref;
    }

    /**
     * Get the current value of the reference.
     *
     * @return the current value of the reference.
     */
    public T get()
    {
        return ref;
    }

    /**
     * Set the current value of the reference.
     *
     * @param ref to be set.
     */
    public void set(final T ref)
    {
        this.ref = ref;
    }

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

        final MutableReference<?> that = (MutableReference<?>)o;

        return ref != null ? ref.equals(that.ref) : that.ref == null;
    }

    public int hashCode()
    {
        return ref != null ? ref.hashCode() : 0;
    }

    public String toString()
    {
        return null == ref ? "null" : ref.toString();
    }
}
