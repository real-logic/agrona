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

final class CharSequenceKey implements CharSequence
{
    private final String data;

    CharSequenceKey(final String data)
    {
        this.data = data;
    }

    public int length()
    {
        return data.length();
    }

    public char charAt(final int index)
    {
        return data.charAt(index);
    }

    public CharSequence subSequence(final int start, final int end)
    {
        return data.substring(start, end);
    }

    public boolean equals(final Object o)
    {
        if (this == o)
        {
            throw new IllegalArgumentException("This equality violation!");
        }
        else if (o instanceof CharSequenceKey)
        {
            return data.equals(((CharSequenceKey)o).data);
        }
        else if (o instanceof String)
        {
            return data.equals(o);
        }
        else if (o instanceof CharSequence)
        {
            final CharSequence cs = (CharSequence)o;
            final int length = length();

            if (length != cs.length())
            {
                return false;
            }

            for (int i = 0; i < length; i++)
            {
                if (charAt(i) != cs.charAt(i))
                {
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    public int hashCode()
    {
        return data.hashCode();
    }

    public String toString()
    {
        return data;
    }
}
