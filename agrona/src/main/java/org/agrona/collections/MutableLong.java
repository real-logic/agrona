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
 * Holder for an long value that is mutable. Useful for being a counter in a {@link java.util.Map} or for passing by
 * reference.
 */
public class MutableLong extends Number implements Comparable<MutableLong>, Serializable
{
    public long value = 0;

    public MutableLong()
    {
    }

    public MutableLong(final long value)
    {
        this.value = value;
    }

    public long get()
    {
        return value;
    }

    public void set(final long value)
    {
        this.value = value;
    }

    public byte byteValue()
    {
        return (byte)value;
    }

    public short shortValue()
    {
        return (short)value;
    }

    public int intValue()
    {
        return (int)value;
    }

    public long longValue()
    {
        return value;
    }

    public float floatValue()
    {
        return (float)value;
    }

    public double doubleValue()
    {
        return (double)value;
    }

    public void increment()
    {
        value++;
    }

    public long incrementAndGet()
    {
        increment();
        return get();
    }

    public long getAndIncrement()
    {
        final long result = get();
        increment();
        return result;
    }

    public void decrement()
    {
        value--;
    }

    public long decrementAndGet()
    {
        decrement();
        return get();
    }

    public long getAndDecrement()
    {
        final long result = get();
        decrement();
        return result;
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

        final MutableLong that = (MutableLong)o;

        return value == that.value;
    }

    public int hashCode()
    {
        return Long.hashCode(value);
    }

    public String toString()
    {
        return Long.toString(value);
    }

    public int compareTo(final MutableLong that)
    {
        return compare(this.value, that.value);
    }

    public static int compare(final long lhs, final long rhs)
    {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }
}
