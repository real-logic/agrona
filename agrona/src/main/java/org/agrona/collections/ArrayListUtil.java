/*
 * Copyright 2014-2023 Real Logic Limited.
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

import java.util.ArrayList;

/**
 * Utility functions for working with {@link ArrayList}s.
 */
public final class ArrayListUtil
{
    private ArrayListUtil()
    {
    }

    /**
     * Removes element at index, but instead of copying all elements to the left, moves into the same slot the last
     * element. This avoids the copy costs, but spoils the list order. If index is the last element it is just removed.
     *
     * @param list  to be modified.
     * @param index to be removed.
     * @param <T>   element type.
     * @throws IndexOutOfBoundsException if index is out of bounds.
     */
    public static <T> void fastUnorderedRemove(final ArrayList<T> list, final int index)
    {
        final int lastIndex = list.size() - 1;
        if (index != lastIndex)
        {
            list.set(index, list.remove(lastIndex));
        }
        else
        {
            list.remove(index);
        }
    }

    /**
     * Removes element at index, but instead of copying all elements to the left, moves into the same slot the last
     * element. This avoids the copy costs, but spoils the list order. If index is the last element it is just removed.
     *
     * @param list      to be modified.
     * @param index     to be removed.
     * @param lastIndex last element index in the list to be swapped into the removed index.
     * @param <T>       element type.
     * @throws IndexOutOfBoundsException if index or lastIndex are out of bounds.
     */
    public static <T> void fastUnorderedRemove(final ArrayList<T> list, final int index, final int lastIndex)
    {
        if (index != lastIndex)
        {
            list.set(index, list.remove(lastIndex));
        }
        else
        {
            list.remove(index);
        }
    }

    /**
     * Removes element but instead of copying all elements to the left, moves into the same slot the last element.
     * This avoids the copy costs, but spoils the list order. If element is the last element then it is just removed.
     *
     * @param list to be modified.
     * @param e    to be removed.
     * @param <T>  element type.
     * @return true if found and removed, false otherwise.
     */
    public static <T> boolean fastUnorderedRemove(final ArrayList<T> list, final T e)
    {
        for (int i = 0, size = list.size(); i < size; i++)
        {
            if (e == list.get(i))
            {
                fastUnorderedRemove(list, i, size - 1);
                return true;
            }
        }

        return false;
    }
}

