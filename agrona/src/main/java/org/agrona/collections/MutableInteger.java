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
package org.agrona.collections;

/**
 * Holder for an int value that is mutable. Useful for being a counter in a {@link java.util.Map} or for passing by
 * reference.
 */
public class MutableInteger extends Number implements Comparable<MutableInteger>
{
    private static final long serialVersionUID = 985259236882848264L;
    /**
     * The value. Default value is {@code 0}.
     */
    public int value = 0;

    /**
     * Default constructor.
     */
    public MutableInteger()
    {
    }

    /**
     * Creates an instance with a value.
     *
     * @param value to assign.
     */
    public MutableInteger(final int value)
    {
        this.value = value;
    }

    /**
     * Get the value.
     *
     * @return the value.
     */
    public int get()
    {
        return value;
    }

    /**
     * Set the value.
     *
     * @param value to be assigned.
     */
    public void set(final int value)
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
        return value;
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
        return value;
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
    public int incrementAndGet()
    {
        return ++value;
    }

    /**
     * Get and increment the value.
     *
     * @return the value before increment.
     */
    public int getAndIncrement()
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
    public int decrementAndGet()
    {
        return --value;
    }

    /**
     * Get the value and decrement it.
     *
     * @return the value before the decrement.
     */
    public int getAndDecrement()
    {
        return value--;
    }

    /**
     * Get the value and add {@code delta} to it.
     *
     * @param delta to add.
     * @return the value before the change.
     */
    public int getAndAdd(final int delta)
    {
        final int result = value;
        value += delta;
        return result;
    }

    /**
     * Add the {@code delta} and get the value.
     *
     * @param delta to add.
     * @return the value after the change.
     */
    public int addAndGet(final int delta)
    {
        return value += delta;
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

        final MutableInteger that = (MutableInteger)o;

        return value == that.value;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return Integer.hashCode(value);
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return Integer.toString(value);
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(final MutableInteger that)
    {
        return compare(this.value, that.value);
    }

    /**
     * Compare two values. Invoking this method is equivalent to:
     * <pre>
     *     Integer.compare(lhs, rhs);
     * </pre>
     *
     * @param lhs first value to compare.
     * @param rhs second value to compare.
     * @return zero if values are equal, negative value if {@code lhs} is less than {@code rhs} or
     * positive value otherwise.
     */
    public static int compare(final int lhs, final int rhs)
    {
        return Integer.compare(lhs, rhs);
    }
}
