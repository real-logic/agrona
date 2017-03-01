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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
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

        assertTrue(copy == result);
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
}