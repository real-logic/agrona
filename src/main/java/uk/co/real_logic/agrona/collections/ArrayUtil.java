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
 * critical paths where operations like add and remove are seldom used, but iterating
 * is common and checkcast and indirection are comparatively expensive.
 *
 * In all cases the array being mutated is assumed to be full.
 *
 * In all cases reference equality is used.
 */
public final class ArrayUtil
{

    public static final int UNKNOWN_INDEX = -1;

    /**
     * Add an element to an array resulting in a new array.
     *
     * @param oldElements  to have the new element added.
     * @param elementToAdd for the new array.
     * @param <T>          type of the array.
     * @return a new array that is one bigger and containing the new element at the end.
     */
    public static <T> T[] add(final T[] oldElements, final T elementToAdd)
    {
        final int length = oldElements.length;
        final T[] newElements = Arrays.copyOf(oldElements, length + 1);
        newElements[length] = elementToAdd;

        return newElements;
    }

    /**
     * Remove an element from an array resulting in a new array if the element was found otherwise the old array.
     *
     * Returns its input parameter if the element to remove isn't a member.
     *
     * @param oldElements     to have the element removed from.
     * @param elementToRemove being searched for by identity semantics.
     * @param <T>             type of the array.
     * @return a new array without the element if found otherwise the original array.
     */
    public static <T> T[] remove(final T[] oldElements, final T elementToRemove)
    {
        final int length = oldElements.length;
        int index = UNKNOWN_INDEX;

        for (int i = 0; i < length; i++)
        {
            if (oldElements[i] == elementToRemove)
            {
                index = i;
            }
        }

        return remove(oldElements, index);
    }

    public static <T> T[] remove(final T[] oldElements, final int index)
    {
        if (index == UNKNOWN_INDEX)
        {
            return oldElements;
        }

        final int oldLength = oldElements.length;
        final int newLength = oldLength - 1;
        final T[] newElements = newArray(oldElements, newLength);

        for (int i = 0, j = 0; i < oldLength; i++)
        {
            if (index != i)
            {
                newElements[j++] = oldElements[i];
            }
        }

        return newElements;
    }

    /**
     * Allocate a new array of the same type as another array.
     *
     * @param oldElements on which the new array is based.
     * @param length      of the new array.
     * @param <T>         type of the array.
     * @return            the new array of requested length.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(final T[] oldElements, final int length)
    {
        return (T[])Array.newInstance(oldElements.getClass().getComponentType(), length);
    }

    /**
     * Ensure an array has the required capacity. Resizing only if needed.
     *
     * @param oldElements    to ensure that are long enough.
     * @param requiredLength to ensure.
     * @param <T>            type of the array.
     * @return               an array of the required length.
     */
    public static <T> T[] ensureCapacity(final T[] oldElements, final int requiredLength)
    {
        T[] result = oldElements;

        if (oldElements.length < requiredLength)
        {
            result = Arrays.copyOf(oldElements, requiredLength);
        }

        return result;
    }
}
