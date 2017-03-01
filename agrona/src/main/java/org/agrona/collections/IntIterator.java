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
 * An iterator for a sequence of primitive values.
 */
public class IntIterator implements Iterator<Integer>
{
    @DoNotSub private int positionCounter;
    @DoNotSub private int stopCounter;
    protected boolean isPositionValid = false;
    private int[] values;
    private boolean containsMissingValue;

    /**
     * Reset method for expandable collections.
     *
     * @param values to be iterated over
     * @param containsMissingValue true iff the hashset contains a missing value
     */
    void reset(final int[] values, final boolean containsMissingValue)
    {
        this.values = values;
        this.containsMissingValue = containsMissingValue;

        @DoNotSub final int length = values.length;

        @DoNotSub int i = length;
        if (values[length - 1] != IntHashSet.MISSING_VALUE)
        {
            i = 0;
            for (@DoNotSub int size = length; i < size; i++)
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
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;

        for (@DoNotSub int i = positionCounter - 1; i >= stopCounter; i--)
        {
            @DoNotSub final int index = i & mask;
            if (values[index] != IntHashSet.MISSING_VALUE)
            {
                return true;
            }
        }

        return containsMissingValue;
    }

    protected void findNext()
    {
        final int[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        isPositionValid = false;

        for (@DoNotSub int i = positionCounter - 1; i >= stopCounter; i--)
        {
            @DoNotSub final int index = i & mask;
            if (values[index] != IntHashSet.MISSING_VALUE)
            {
                positionCounter = i;
                isPositionValid = true;
                return;
            }
        }

        if (!containsMissingValue)
        {
            throw new NoSuchElementException();
        }
    }

    public Integer next()
    {
        return nextValue();
    }

    /**
     * Strongly typed alternative of {@link Iterator#next()} not to avoid boxing.
     *
     * @return the next int value.
     */
    public int nextValue()
    {
        findNext();

        if (isPositionValid)
        {
            return values[position()];
        }
        else
        {
            containsMissingValue = false;
            return IntHashSet.MISSING_VALUE;
        }
    }
}
