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
 *
 * In all cases reference equality is used.
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

    /**
     * Creates a new array from an old array with an element removed.
     *
     * Returns its input parameter if the element to remove isn't a member.
     *
     * @param oldElements the input array
     * @param elementToRemove the element to remove from oldElements
     * @param <T> the type of elements in the array
     * @return oldElements without elementToRemove
     */
    public static <T> T[] remove(final T[] oldElements, final T elementToRemove)
    {
        final int oldLength = oldElements.length;
        final int newLength = oldLength - 1;
        final T[] newElements = newArray(oldElements, newLength);
        boolean containsElement = false;
        for (int i = 0, j = 0; i < newLength; i++)
        {
            final T element = oldElements[i];
            if (element != elementToRemove)
            {
                newElements[j++] = element;
            }
            else
            {
                containsElement = true;
            }
        }

        return containsElement ? newElements : oldElements;
    }

    /**
     * Allocate a new array of the same type as another array.
     *
     * @param oldElements the old array.
     * @param length the length of the new array
     * @param <T> the type of element in the new array
     * @return the new array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(final T[] oldElements, final int length)
    {
        return (T[]) Array.newInstance(oldElements.getClass().getComponentType(), length);
    }

}
