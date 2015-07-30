/*
 * Copyright 2014 - 2015 Real Logic Ltd.
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

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Utility class for operating on arrays as if they were collections. This is useful for
 * critical paths where operations like add and remove are seldomly used, but iterating
 * is common and checkcast comparatively expensive.
 *
 * In all cases the array being mutated is assumed to be full.
 */
public final class ArrayUtil
{

    public static <T> T[] add(final T[] oldElements, final T elementToAdd)
    {
        final int length = oldElements.length;
        final T[] newElements = Arrays.copyOf(oldElements, length + 1);
        newElements[length] = elementToAdd;
        return newElements;
    }

    public static <T> T[] remove(final T[] oldElements, final T elementToRemove)
    {
        final int length = oldElements.length;
        final T[] newElements = newArray(oldElements, length);
        for (int i = 0, j = 0; i < length; i++)
        {
            final T element = oldElements[i];
            if (element != elementToRemove)
            {
                newElements[j++] = element;
            }
        }

        return newElements;
    }

    /**
     * Allocate a new array of the same type as the old array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(final T[] oldElements, final int length)
    {
        return (T[]) Array.newInstance(oldElements.getClass().getComponentType(), length - 1);
    }

}
