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

import java.io.Serializable;

/**
 * Holder for a long value that is mutable. Useful for being a counter in a {@link java.util.Map} or for passing by
 * reference.
 */
public class MutableLong extends Number implements Comparable<MutableLong>, Serializable
{
    private static final long serialVersionUID = -3537098518545563995L;
    /**
     * The value. Default value is {@code 0}.
     */
    public long value = 0;

    /**
     * Default constructor.
     */
    public MutableLong()
    {
    }

    /**
     * Creates an instance with a value.
     *
     * @param value to assign.
     */
    public MutableLong(final long value)
    {
        this.value = value;
    }

    /**
     * Get the value.
     *
     * @return the value.
     */
    public long get()
    {
        return value;
    }

    /**
     * Set the value.
     *
     * @param value to be assigned.
     */
    public void set(final long value)
    {
        this.value = value;
    }

    /**
     * Return value as {@code byte}.
     *
     * @return value as {@code byte}.
     */
    public byte byteValue()
    {
        return (byte)value;
    }

    /**
     * Return value as {@code short}.
     *
     * @return value as {@code short}.
     */
    public short shortValue()
    {
        return (short)value;
    }

    /**
     * Return value as {@code int}.
     *
     * @return value as {@code int}.
     */
    public int intValue()
    {
        return (int)value;
    }

    /**
     * Return value as {@code long}.
     *
     * @return value as {@code long}.
     */
    public long longValue()
    {
        return value;
    }

    /**
     * Return value as {@code float}.
     *
     * @return value as {@code float}.
     */
    public float floatValue()
    {
        return (float)value;
    }

    /**
     * Return value as {@code double}.
     *
     * @return value as {@code double}.
     */
    public double doubleValue()
    {
        return (double)value;
    }

    /**
     * Increment the value.
     */
    public void increment()
    {
        value++;
    }

    /**
     * Increment and return the value.
     *
     * @return the value after increment.
     */
    public long incrementAndGet()
    {
        return ++value;
    }

    /**
     * Get and increment the value.
     *
     * @return the value before increment.
     */
    public long getAndIncrement()
    {
        return value++;
    }

    /**
     * Decrement the value.
     */
    public void decrement()
    {
        value--;
    }

    /**
     * Decrement and get the value.
     *
     * @return value after the decrement.
     */
    public long decrementAndGet()
    {
        return --value;
    }

    /**
     * Get the value and decrement it.
     *
     * @return the value before the decrement.
     */
    public long getAndDecrement()
    {
        return value--;
    }

    /**
     * Get the value and add {@code delta} to it.
     *
     * @param delta to add.
     * @return the value before the change.
     */
    public long getAndAdd(final long delta)
    {
        final long result = value;
        value += delta;
        return result;
    }

    /**
     * Add the {@code delta} and get the value.
     *
     * @param delta to add.
     * @return the value after the change.
     */
    public long addAndGet(final long delta)
    {
        return value += delta;
    }

    /**
     *  {@inheritDoc}
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

        final MutableLong that = (MutableLong)o;

        return value == that.value;
    }

    /**
     *  {@inheritDoc}
     */
    public int hashCode()
    {
        return Long.hashCode(value);
    }

    /**
     *  {@inheritDoc}
     */
    public String toString()
    {
        return Long.toString(value);
    }

    /**
     *  {@inheritDoc}
     */
    public int compareTo(final MutableLong that)
    {
        return compare(this.value, that.value);
    }

    /**
     * Compare two long values. Calling this method is equivalent to calling:
     * <pre>
     *     Long.compare(lhs, rhs);
     * </pre>
     *
     * @param lhs first value.
     * @param rhs second value.
     * @return the value {@code 0} if {@code lhs == rhs};
     * a value less than {@code 0} if {@code lhs < rhs}; and
     * a value greater than {@code 0} if {@code lhs > rhs}
     */
    public static int compare(final long lhs, final long rhs)
    {
        return Long.compare(lhs, rhs);
    }
}
