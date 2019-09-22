/*
 * Copyright 2014-2019 Real Logic Ltd.
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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class IntArrayListTest
{
    private final IntArrayList list = new IntArrayList();

    @Test
    public void shouldReportEmpty()
    {
        assertThat(list.size(), is(0));
        assertThat(list.isEmpty(), is(true));
    }

    @Test
    public void shouldAddValue()
    {
        list.add(7);

        assertThat(list.size(), is(1));
        assertThat(list.get(0), is(7));
    }

    @Test
    public void shouldAddNull()
    {
        list.add(null);

        assertThat(list.size(), is(1));
        assertNull(list.get(0));
    }

    @Test
    public void shouldAddIntValue()
    {
        list.addInt(7);

        assertThat(list.size(), is(1));
        assertThat(list.getInt(0), is(7));
    }

    @Test
    public void shouldAddValueAtIndex()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered(list::addInt);

        list.addInt(10, 777);

        assertThat(list.size(), is(count + 1));
        assertThat(list.getInt(10), is(777));
        assertThat(list.getInt(count), is(count - 1));
    }

    @Test
    public void shouldAddValueAtIndexWithNearlyFullCapacity()
    {
        final int count = IntArrayList.INITIAL_CAPACITY - 1;
        final int value = count + 1;
        IntStream.range(0, count).forEachOrdered(list::addInt);

        list.addInt(0, value);

        assertThat(list.size(), is(count + 1));
        assertThat(list.getInt(0), is(value));
        assertThat(list.getInt(count), is(count - 1));
    }

    @Test
    public void shouldSetIntValue()
    {
        list.addInt(7);
        list.setInt(0, 8);

        assertThat(list.size(), is(1));
        assertThat(list.getInt(0), is(8));
    }

    @Test
    public void shouldSetValue()
    {
        list.add(7);
        list.set(0, 8);

        assertThat(list.size(), is(1));
        assertThat(list.getInt(0), is(8));
    }

    @Test
    public void shouldContainCorrectValues()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered(list::addInt);

        for (int i = 0; i < count; i++)
        {
            assertTrue(list.containsInt(i));
        }

        assertFalse(list.containsInt(-1));
        assertFalse(list.containsInt(20));
    }

    @Test
    public void shouldRemoveAtIndex()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered(list::addInt);

        assertThat(list.remove(10), is(10));

        assertThat(list.size(), is(count - 1));
        assertThat(list.getInt(10), is(11));
    }

    @Test
    public void shouldRemoveAtIndexForListLengthOne()
    {
        list.addInt(1);

        assertThat(list.fastUnorderedRemove(0), is(1));

        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldFastRemoveUnorderedAtIndex()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered(list::addInt);

        assertThat(list.fastUnorderedRemove(10), is(10));

        assertThat(list.size(), is(count - 1));
        assertThat(list.getInt(10), is(19));
    }

    @Test
    public void shouldFastRemoveUnorderedByValue()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered((value) -> list.addInt(value * 10));

        assertTrue(list.fastUnorderedRemoveInt(10));

        assertThat(list.size(), is(count - 1));
        assertThat(list.getInt(1), is(190));
    }

    @Test
    public void shouldForEachOrderedInt()
    {
        final List<Integer> expected = new ArrayList<>();
        IntStream.range(0, 20).forEachOrdered(expected::add);
        list.addAll(expected);

        final List<Integer> actual = new ArrayList<>();
        list.forEachOrderedInt(actual::add);

        assertThat(actual, is(expected));
    }

    @Test
    public void shouldCreateObjectRefArray()
    {
        final int count = 20;
        final List<Integer> expected = new ArrayList<>();
        IntStream.range(0, count).forEachOrdered(expected::add);
        list.addAll(expected);

        assertArrayEquals(expected.toArray(), list.toArray());
    }

    @Test
    public void shouldCreateIntArray()
    {
        final int count = 20;
        final int[] expected = new int[count];
        for (int i = 0; i < count; i++)
        {
            list.add(i);
            expected[i] = i;
        }

        assertArrayEquals(expected, list.toIntArray());

        final int[] copy = new int[count];
        final int[] result = list.toIntArray(copy);

        assertSame(copy, result);
        assertArrayEquals(expected, result);
    }

    @Test
    public void shouldCreateIntegerArray()
    {
        final int count = 20;
        final Integer[] expected = new Integer[count];
        for (int i = 0; i < count; i++)
        {
            list.add(i);
            expected[i] = i;
        }

        final Integer[] integers = list.toArray(new Integer[0]);
        assertEquals(expected.getClass(), integers.getClass());
        assertArrayEquals(expected, integers);
    }

    @Test
    public void shouldPushAndThenPopInOrder()
    {
        final int count = 7;
        for (int i = 0; i < count; i++)
        {
            list.pushInt(i);
        }

        for (int i = count - 1; i >= 0; i--)
        {
            assertThat(list.popInt(), is(i));
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void shouldThrowExceptionWhenPoppingEmptyList()
    {
        list.popInt();
    }

    @Test
    public void shouldEqualGenericList()
    {
        final int count = 7;
        final List<Integer> genericList = new ArrayList<>();

        for (int i = 0; i < count; i++)
        {
            list.add(i);
            genericList.add(i);
        }

        list.add(null);
        genericList.add(null);

        assertEquals(list, genericList);
    }

    @Test
    public void shouldEqualsAndHashcode()
    {
        final ArrayList<Integer> genericList = new ArrayList<>();
        final int count = 20;

        for (int i = 0; i < count; i++)
        {
            list.addInt(i);
            genericList.add(i);
        }

        assertEquals(genericList.hashCode(), list.hashCode());
        assertEquals(genericList, list);
        assertEquals(list, genericList);
    }

    @Test
    public void shouldGenerateStringRepresentation()
    {
        final int[] testEntries = { 3, 1, -1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            list.add(testEntry);
        }

        final String mapAsAString = "[3, 1, -1, 19, 7, 11, 12, 7]";
        assertThat(list.toString(), equalTo(mapAsAString));
    }

    @Test
    public void shouldWrapLessThanInitialCapacityThenGrow()
    {
        final int[] array = new int[]{ 1, 2, 3 };
        final IntArrayList list = new IntArrayList();

        list.wrap(array, array.length);

        list.addInt(7);
        assertThat(list.capacity(), is(IntArrayList.INITIAL_CAPACITY));
    }

    @Test
    public void shouldWrapLessZeroLengthArrayThenGrow()
    {
        final IntArrayList list = new IntArrayList();

        list.wrap(new int[0], 0);

        list.addInt(7);
        assertThat(list.capacity(), is(IntArrayList.INITIAL_CAPACITY));
    }
}