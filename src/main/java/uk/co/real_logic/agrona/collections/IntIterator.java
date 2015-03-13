/*
 * Copyright 2015 Real Logic Ltd.
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
package uk.co.real_logic.agrona.collections;

import java.util.Iterator;

/**
 * An iterator for a sequence of primitive integers.
 */
public class IntIterator implements Iterator<Integer>
{
    private final int missingValue;
    private final int[] values;

    private int position = 0;

    public IntIterator(final int missingValue, final int[] values)
    {
        this.missingValue = missingValue;
        this.values = values;
    }

    public boolean hasNext()
    {
        final int[] values = this.values;
        while (position < values.length)
        {
            if (values[position] != missingValue)
            {
                return true;
            }

            position++;
        }

        return false;
    }

    public Integer next()
    {
        return nextValue();
    }

    public int nextValue()
    {
        final int value = values[position];
        position++;
        return value;
    }

    void reset()
    {
        position = 0;
    }
}
