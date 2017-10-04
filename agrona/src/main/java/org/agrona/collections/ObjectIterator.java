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

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator for a sequence of values.
 *
 * @param <T> type of values stored in the collection being iterated.
 */
public class ObjectIterator<T> implements Iterator<T>, Serializable
{
    private static final Object MISSING_VALUE = null;

    private int remaining;
    private int positionCounter;
    private int stopCounter;
    protected boolean isPositionValid = false;
    private T[] values;

    protected ObjectIterator()
    {
    }

    /**
     * Reset method for expandable collections.
     *
     * @param values to be iterated over
     * @param size   of the collection in contained items.
     */
    void reset(final T[] values, final int size)
    {
        this.remaining = size;
        this.values = values;
        final int length = values.length;
        int i = length;

        if (values[length - 1] != MISSING_VALUE)
        {
            i = 0;
            for (; i < length; i++)
            {
                if (values[i] == MISSING_VALUE)
                {
                    break;
                }
            }
        }

        stopCounter = i;
        positionCounter = i + length;
        isPositionValid = false;
    }

    public boolean hasNext()
    {
        return remaining > 0;
    }

    public T next()
    {
        return nextValue();
    }

    /**
     * @return the next int value.
     */
    @SuppressWarnings("unchecked")
    public T nextValue()
    {
        findNext();

        return values[position()];
    }

    protected int position()
    {
        return positionCounter & (values.length - 1);
    }

    protected void findNext()
    {
        final Object[] values = this.values;
        final int mask = values.length - 1;
        isPositionValid = false;

        for (int i = positionCounter - 1; i >= stopCounter; i--)
        {
            final int index = i & mask;
            if (values[index] != MISSING_VALUE)
            {
                positionCounter = i;
                isPositionValid = true;
                --remaining;
                return;
            }
        }

        throw new NoSuchElementException();
    }
}
