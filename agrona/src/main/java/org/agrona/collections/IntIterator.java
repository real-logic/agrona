/*
 *  Copyright 2014-2017 Real Logic Ltd.
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
package org.agrona.collections;

import org.agrona.generation.DoNotSub;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An {@link Iterator} for a sequence of primitive values.
 */
public class IntIterator implements Iterator<Integer>
{
    @DoNotSub private int remaining;
    @DoNotSub private int positionCounter;
    @DoNotSub private int stopCounter;
    protected boolean isPositionValid = false;
    private int[] values;
    private boolean containsMissingValue;

    protected IntIterator()
    {
    }

    /**
     * Reset method for expandable collections.
     *
     * @param values               to be iterated over
     * @param containsMissingValue true iff the collection contains a missing value
     */
    void reset(
        final int[] values,
        final boolean containsMissingValue,
        @DoNotSub final int size)
    {
        this.values = values;
        this.containsMissingValue = containsMissingValue;
        this.remaining = size;

        @DoNotSub final int length = values.length;
        @DoNotSub int i = length;

        if (values[length - 1] != IntHashSet.MISSING_VALUE)
        {
            for (i = 0; i < length; i++)
            {
                if (values[i] == IntHashSet.MISSING_VALUE)
                {
                    break;
                }
            }
        }

        stopCounter = i;
        positionCounter = i + length;
        isPositionValid = false;
    }

    @DoNotSub protected int position()
    {
        return positionCounter & (values.length - 1);
    }

    public boolean hasNext()
    {
        return remaining > 0;
    }

    @DoNotSub public int remaining()
    {
        return remaining;
    }

    public Integer next()
    {
        return nextValue();
    }

    /**
     * Strongly typed alternative of {@link Iterator#next()} to avoid boxing.
     *
     * @return the next int value.
     */
    public int nextValue()
    {
        findNext();

        if (remaining == 1 && containsMissingValue)
        {
            return IntHashSet.MISSING_VALUE;
        }
        else
        {
            return values[position()];
        }
    }

    protected void findNext()
    {
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        isPositionValid = true;

        for (@DoNotSub int i = positionCounter - 1; i >= stopCounter; i--)
        {
            @DoNotSub final int index = i & mask;
            if (values[index] != IntHashSet.MISSING_VALUE)
            {
                positionCounter = i;
                --remaining;
                return;
            }
        }

        if (containsMissingValue)
        {
            --remaining;
            return;
        }

        isPositionValid = false;
        throw new NoSuchElementException();
    }
}
