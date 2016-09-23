/*
 *  Copyright 2014 - 2016 Real Logic Ltd.
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
 * An iterator for a sequence of values.
 */
public class ObjIterator<T> implements Iterator<T>
{
    private static final Object missingValue = null;
    @DoNotSub private int positionCounter;
    @DoNotSub private int stopCounter;
    protected boolean isPositionValid = false;
    private T[] values;

    /**
     * Construct an {@link Iterator} over an array of values.
     *
     * @param values       to iterate over.
     */
    public ObjIterator(final T[] values)
    {
        reset(values);
    }

    /**
     * Reset methods for fixed size collections.
     */
    void reset()
    {
        reset(values);
    }

    /**
     * Reset method for expandable collections.
     *
     * @param values to be iterated over
     */
    void reset(final T[] values)
    {
        this.values = values;
        @DoNotSub final int length = values.length;

        @DoNotSub int i = length;
        if (values[length - 1] != missingValue)
        {
            i = 0;
            for (@DoNotSub int size = length; i < size; i++)
            {
                if (values[i] == missingValue)
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
        final T[] values = this.values;
        @DoNotSub final int mask = values.length - 1;

        for (@DoNotSub int i = positionCounter - 1; i >= stopCounter; i--)
        {
            @DoNotSub final int index = i & mask;
            if (values[index] != missingValue)
            {
                return true;
            }
        }

        return false;
    }

    protected void findNext()
    {
        final T[] values = this.values;
        @DoNotSub final int mask = values.length - 1;
        isPositionValid = false;

        for (@DoNotSub int i = positionCounter - 1; i >= stopCounter; i--)
        {
            @DoNotSub final int index = i & mask;
            if (values[index] != missingValue)
            {
                positionCounter = i;
                isPositionValid = true;
                return;
            }
        }

        throw new NoSuchElementException();
    }

    public T next()
    {
        return nextValue();
    }

    /**
     * @return the next int value.
     */
    public T nextValue()
    {
        findNext();

        return values[position()];
    }
}
