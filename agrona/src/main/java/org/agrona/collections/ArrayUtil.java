/*
 * Copyright 2014-2020 Real Logic Limited.
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

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Utility class for operating on arrays as if they were collections. This is useful for
 * critical paths where operations like add and remove are seldom used, but iterating
 * is common and checkcast and indirection are comparatively expensive.
 * <p>
 * In all cases the array being mutated is assumed to be full.
 * <p>
 * In all cases reference equality is used.
 */
public final class ArrayUtil
{
    /**
     * Constant indicating an invalid/unknown array index.
     */
    public static final int UNKNOWN_INDEX = -1;

    /**
     * Empty boolean array.
     */
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];

    /**
     * Empty byte array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * Empty char array.
     */
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];

    /**
     * Empty short array.
     */
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];

    /**
     * Empty int array.
     */
    public static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * Empty float array.
     */
    public static final float[] EMPTY_FLOAT_ARRAY = new float[0];

    /**
     * Empty boolean array.
     */
    public static final long[] EMPTY_LONG_ARRAY = new long[0];

    /**
     * Empty double array.
     */
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

    /**
     * Empty Object array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * Empty String array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Maximum capacity to which an array can grow.
     */
    public static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    private ArrayUtil()
    {
    }

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
     * <p>
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

    /**
     * Remove an element from an array resulting in a new array if the index was inside the array otherwise the old
     * array.
     * <p>
     * Returns the old elements array if the index isn't inside the array.
     *
     * @param oldElements to have the element removed from.
     * @param index       to remove the element at.
     * @param <T>         type of the array.
     * @return a new array without the element if the index is inside the array otherwise the original array.
     */
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
     * @return the new array of requested length.
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
     * @return an array of the required length.
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
