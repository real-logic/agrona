/*
 * Copyright 2014-2022 Real Logic Limited.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static org.agrona.collections.IntArrayList.DEFAULT_NULL_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class IntArrayListTest
{
    private final IntArrayList list = new IntArrayList();

    @Test
    void shouldReportEmpty()
    {
        assertThat(list.size(), is(0));
        assertThat(list.isEmpty(), is(true));
    }

    @Test
    void shouldAddValue()
    {
        list.add(7);

        assertThat(list.size(), is(1));
        assertThat(list.get(0), is(7));
    }

    @Test
    void shouldAddNull()
    {
        list.add(null);

        assertThat(list.size(), is(1));
        assertNull(list.get(0));
    }

    @Test
    void shouldAddIntValue()
    {
        list.addInt(7);

        assertThat(list.size(), is(1));
        assertThat(list.getInt(0), is(7));
    }

    @Test
    void shouldAddValueAtIndex()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered(list::addInt);

        list.addInt(10, 777);

        assertThat(list.size(), is(count + 1));
        assertThat(list.getInt(10), is(777));
        assertThat(list.getInt(count), is(count - 1));
    }

    @Test
    void shouldAddValueAtIndexWithNearlyFullCapacity()
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
    void shouldSetIntValue()
    {
        list.addInt(7);
        list.setInt(0, 8);

        assertThat(list.size(), is(1));
        assertThat(list.getInt(0), is(8));
    }

    @Test
    void shouldSetValue()
    {
        list.add(7);
        list.set(0, 8);

        assertThat(list.size(), is(1));
        assertThat(list.getInt(0), is(8));
    }

    @Test
    void shouldContainCorrectValues()
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
    void shouldRemoveAtIndexBoxing()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered(list::addInt);

        assertThat(list.remove(10), is(10));

        assertThat(list.size(), is(count - 1));
        assertThat(list.getInt(10), is(11));
    }

    @Test
    void shouldRemoveNullValueAtIndexBoxing()
    {
        list.addInt(34);
        list.add(null);
        list.addInt(15);

        assertNull(list.remove(1));

        assertEquals(2, list.size());
        assertEquals(34, list.getInt(0));
        assertEquals(15, list.getInt(1));
    }

    @Test
    void shouldRemoveAtIndex()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered(list::addInt);

        assertThat(list.removeAt(10), is(10));

        assertThat(list.size(), is(count - 1));
        assertThat(list.getInt(10), is(11));
    }

    @Test
    void shouldRemoveAtIndexForListLengthOne()
    {
        list.addInt(1);

        assertThat(list.fastUnorderedRemove(0), is(1));

        assertTrue(list.isEmpty());
    }

    @Test
    void shouldFastRemoveUnorderedAtIndex()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered(list::addInt);

        assertThat(list.fastUnorderedRemove(10), is(10));

        assertThat(list.size(), is(count - 1));
        assertThat(list.getInt(10), is(19));
    }

    @Test
    void shouldFastRemoveUnorderedByValue()
    {
        final int count = 20;
        IntStream.range(0, count).forEachOrdered((value) -> list.addInt(value * 10));

        assertTrue(list.fastUnorderedRemoveInt(10));

        assertThat(list.size(), is(count - 1));
        assertThat(list.getInt(1), is(190));
    }

    @Test
    void removeIntReturnsFalseIfValueDoesNotExist()
    {
        list.addInt(5);
        list.addInt(0);
        list.addInt(42);

        assertFalse(list.removeInt(8));

        assertEquals(3, list.size());
        assertEquals(5, list.getInt(0));
        assertEquals(0, list.getInt(1));
        assertEquals(42, list.getInt(2));
    }

    @Test
    void removeIntReturnsTrueAfterRemovingFirstMatchingValueFromTheList()
    {
        list.addInt(5);
        list.addInt(-1);
        list.addInt(42);
        list.addInt(8);
        list.addInt(-1);
        list.addInt(0);

        assertTrue(list.removeInt(-1));

        assertEquals(5, list.size());
        assertEquals(5, list.getInt(0));
        assertEquals(42, list.getInt(1));
        assertEquals(8, list.getInt(2));
        assertEquals(-1, list.getInt(3));
        assertEquals(0, list.getInt(4));
    }

    @Test
    void shouldForEachOrderedInt()
    {
        final List<Integer> expected = new ArrayList<>();
        IntStream.range(0, 20).forEachOrdered(expected::add);
        list.addAll(expected);

        final List<Integer> actual = new ArrayList<>();
        list.forEachOrderedInt(actual::add);

        assertThat(actual, is(expected));
    }

    @Test
    void shouldCreateObjectRefArray()
    {
        final int count = 20;
        final List<Integer> expected = new ArrayList<>();
        IntStream.range(0, count).forEachOrdered(expected::add);
        list.addAll(expected);

        assertArrayEquals(expected.toArray(), list.toArray());
    }

    @Test
    void shouldCreateIntArray()
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
    void shouldCreateIntegerArray()
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
    void shouldPushAndThenPopInOrder()
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

    @Test
    void shouldThrowExceptionWhenPoppingEmptyList()
    {
        assertThrows(NoSuchElementException.class, list::popInt);
    }

    @Test
    void shouldEqualGenericList()
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
    void shouldEqualsAndHashcode()
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
    void shouldGenerateStringRepresentation()
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
    void shouldWrapLessThanInitialCapacityThenGrow()
    {
        final int[] array = new int[]{ 1, 2, 3 };
        final IntArrayList list = new IntArrayList();

        list.wrap(array, array.length);

        list.addInt(7);
        assertThat(list.capacity(), is(IntArrayList.INITIAL_CAPACITY));
    }

    @Test
    void shouldWrapLessZeroLengthArrayThenGrow()
    {
        final IntArrayList list = new IntArrayList();

        list.wrap(new int[0], 0);

        list.addInt(7);
        assertThat(list.capacity(), is(IntArrayList.INITIAL_CAPACITY));
    }

    @Test
    void removeThrowsNClassCastExceptionIfValueIsNotInteger()
    {
        assertThrowsExactly(ClassCastException.class, () -> list.remove(Double.valueOf(24.5)));
    }

    @Test
    void removeReturnsFalseForAnUnknownValue()
    {
        list.addInt(42);

        assertFalse(list.remove(Integer.valueOf(5)));

        assertEquals(42, list.get(0));
    }

    @Test
    void removeReturnsTrueAfterRemovingTheFirstOccurrenceOfTheValue()
    {
        list.addInt(42);
        list.addInt(5);
        list.addInt(42);

        assertTrue(list.remove(Integer.valueOf(42)));

        assertEquals(2, list.size());
        assertEquals(5, list.get(0));
        assertEquals(42, list.get(1));
    }

    @Test
    void addAllAppendsAllItemsToTheEndOfTheList()
    {
        final IntArrayList other = new IntArrayList();
        other.addInt(3);
        other.addInt(4);
        other.addInt(5);
        list.addInt(1);
        list.addInt(2);
        list.addInt(3);

        assertTrue(list.addAll(other));

        assertEquals(6, list.size());
        assertEquals(1, list.getInt(0));
        assertEquals(2, list.getInt(1));
        assertEquals(3, list.getInt(2));
        assertEquals(3, list.getInt(3));
        assertEquals(4, list.getInt(4));
        assertEquals(5, list.getInt(5));
    }

    @Test
    void addAllAppendsToItself()
    {
        list.addInt(1);
        list.addInt(2);
        list.addInt(3);

        assertTrue(list.addAll(list));

        assertEquals(6, list.size());
        assertEquals(1, list.getInt(0));
        assertEquals(2, list.getInt(1));
        assertEquals(3, list.getInt(2));
        assertEquals(1, list.getInt(3));
        assertEquals(2, list.getInt(4));
        assertEquals(3, list.getInt(5));
    }

    @Test
    void addAllIsANoOpIfTheSourceListIsEmpty()
    {
        list.addInt(3);

        assertFalse(list.addAll(new IntArrayList()));

        assertEquals(1, list.size());
        assertEquals(3, list.getInt(0));
    }

    @Test
    void addAllWithIndexAddsElementsStartingAtAGivenIndex()
    {
        final IntArrayList other = new IntArrayList();
        other.addInt(17);
        other.addInt(9);
        list.addInt(1);
        list.addInt(2);
        list.addInt(3);
        list.addInt(4);
        list.addInt(5);

        assertTrue(list.addAll(1, other));

        assertEquals(7, list.size());
        assertEquals(1, list.getInt(0));
        assertEquals(17, list.getInt(1));
        assertEquals(9, list.getInt(2));
        assertEquals(2, list.getInt(3));
        assertEquals(3, list.getInt(4));
        assertEquals(4, list.getInt(5));
        assertEquals(5, list.getInt(6));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 4 })
    void addAllWithIndexThrowsIndexOutOfBoundsExceptionIfIndexIsInvalid(final int index)
    {
        final IntArrayList other = new IntArrayList();
        other.addInt(19);

        final IndexOutOfBoundsException exception =
            assertThrowsExactly(IndexOutOfBoundsException.class, () -> list.addAll(index, other));
        assertEquals("index=" + index + " size=0", exception.getMessage());
    }

    @Test
    void addAllWithIndexCanAddFromTheBeginning()
    {
        final IntArrayList other = new IntArrayList();
        other.addInt(4);
        other.addInt(5);
        other.addInt(6);
        list.addInt(1);
        list.addInt(2);

        assertTrue(list.addAll(0, other));

        assertEquals(5, list.size());
        assertEquals(4, list.getInt(0));
        assertEquals(5, list.getInt(1));
        assertEquals(6, list.getInt(2));
        assertEquals(1, list.getInt(3));
        assertEquals(2, list.getInt(4));
    }

    @Test
    void addAllWithIndexCanAddAtTheEnd()
    {
        final IntArrayList other = new IntArrayList();
        other.addInt(4);
        other.addInt(5);
        other.addInt(6);
        list.addInt(1);
        list.addInt(2);

        assertTrue(list.addAll(2, other));

        assertEquals(5, list.size());
        assertEquals(1, list.getInt(0));
        assertEquals(2, list.getInt(1));
        assertEquals(4, list.getInt(2));
        assertEquals(5, list.getInt(3));
        assertEquals(6, list.getInt(4));
    }

    @Test
    void addAllWithIndexCanAddToItself()
    {
        list.addInt(1);
        list.addInt(2);
        list.addInt(3);

        assertTrue(list.addAll(2, list));

        assertEquals(6, list.size());
        assertEquals(1, list.getInt(0));
        assertEquals(2, list.getInt(1));
        assertEquals(1, list.getInt(2));
        assertEquals(2, list.getInt(3));
        assertEquals(3, list.getInt(4));
        assertEquals(3, list.getInt(5));
    }

    @Test
    void containsAllReturnsTrueIfTheListContainsAllOfTheElementsOfSourceList()
    {
        final IntArrayList other = new IntArrayList(2, 42);
        other.addInt(1);
        other.addInt(1);
        other.addInt(1);
        other.addInt(2);
        other.addInt(0);
        other.addInt(0);
        other.addInt(0);
        other.addInt(2);
        other.addInt(1);
        other.add(null);
        list.addInt(-8);
        list.addInt(0);
        list.addInt(1);
        list.addInt(2);
        list.addInt(42);

        assertTrue(list.containsAll(other));
    }

    @Test
    void containsAllReturnsTrueIfTheSourceListIsEmpty()
    {
        final IntArrayList other = new IntArrayList();
        list.addInt(-8);
        list.addInt(42);

        assertTrue(list.containsAll(other));
    }

    @Test
    void containsAllReturnsTrueIfTheNullValueContainedInBothLists()
    {
        final IntArrayList other = new IntArrayList(1, 888);
        other.add(null);
        other.add(5);
        list.addInt(5);
        list.addInt(42);
        list.add(null);

        assertTrue(list.containsAll(other));
    }

    @Test
    void containsAllHandlesNullValueInTheSourceList()
    {
        final List<Integer> other = Arrays.asList(1, 2, null, 100);
        list.addInt(1);
        list.addInt(100);
        list.addInt(2);
        list.addInt(DEFAULT_NULL_VALUE);
        list.addInt(3);
        list.addInt(-1);

        assertTrue(list.containsAll(other));
    }

    @Test
    void containsReturnsTrueIfListContainsValue()
    {
        final int element = 13;
        list.addInt(element);

        assertTrue(list.contains(element));
    }

    @Test
    void containsReturnsTrueIfListContainsNull()
    {
        list.addInt(42);
        list.add(null);

        assertTrue(list.contains(null));
    }

    @Test
    void containsAllReturnsFalseIfAtLeastOneElementOfTheSourceListIsNotFound()
    {
        final IntArrayList other = new IntArrayList();
        other.addInt(1);
        other.addInt(1);
        other.addInt(2);
        other.addInt(10);
        other.addInt(20);
        list.addInt(1);
        list.addInt(2);
        list.addInt(3);
        list.addInt(20);

        assertFalse(list.containsAll(other));
    }

    @Test
    void retainAllIsANoOpIfTheTargetListContainsAllOfTheItemsInTheSourceList()
    {
        final IntArrayList other = new IntArrayList();
        other.addInt(1);
        other.addInt(10);
        other.addInt(100);
        other.addInt(1000);
        list.addInt(100);
        list.addInt(100);
        list.addInt(1);

        assertFalse(list.retainAll(other));

        assertEquals(3, list.size());
        assertEquals(100, list.getInt(0));
        assertEquals(100, list.getInt(1));
        assertEquals(1, list.getInt(2));
    }

    @Test
    void retainAllShouldDeleteAllItemsNotFoundInOtherList()
    {
        final IntArrayList other = new IntArrayList(2, -100);
        other.addInt(1);
        other.addInt(10);
        other.addInt(100);
        other.addInt(1000);
        other.add(null);
        list.addInt(1);
        list.addInt(2);
        list.addInt(2);
        list.addInt(-999);
        list.addInt(2);
        list.addInt(2);
        list.addInt(10);
        list.addInt(2);
        list.addInt(2);
        list.addInt(1);
        list.addInt(10);
        list.addInt(5);
        list.addInt(-1);
        list.add(null);
        list.addInt(100);

        assertTrue(list.retainAll(other));

        assertEquals(6, list.size());
        assertEquals(1, list.getInt(0));
        assertEquals(10, list.getInt(1));
        assertEquals(1, list.getInt(2));
        assertEquals(10, list.getInt(3));
        assertEquals(DEFAULT_NULL_VALUE, list.getInt(4));
        assertEquals(100, list.getInt(5));
    }

    @Test
    void retainAllShouldDeleteNullsIfTheTargetListDoesNotContainAny()
    {
        final IntArrayList other = new IntArrayList(2, -100);
        other.addInt(1);
        other.addInt(10);
        other.addInt(100);
        other.addInt(1000);
        list.addInt(1);
        list.addInt(2);
        list.addInt(2);
        list.addInt(-999);
        list.addInt(2);
        list.addInt(2);
        list.addInt(10);
        list.addInt(2);
        list.addInt(2);
        list.addInt(1);
        list.addInt(10);
        list.addInt(5);
        list.addInt(-1);
        list.add(null);
        list.addInt(100);

        assertTrue(list.retainAll(other));

        assertEquals(5, list.size());
        assertEquals(1, list.getInt(0));
        assertEquals(10, list.getInt(1));
        assertEquals(1, list.getInt(2));
        assertEquals(10, list.getInt(3));
        assertEquals(100, list.getInt(4));
    }

    @Test
    void retainAllShouldEraseTheEntireListIfTheTargetListIsEmpty()
    {
        final IntArrayList other = new IntArrayList();
        list.addInt(1);
        list.addInt(2);
        list.addInt(10);
        list.addInt(2);
        list.addInt(2);
        list.addInt(1);
        list.addInt(10);
        list.addInt(5);
        list.addInt(-1);
        list.addInt(100);

        assertTrue(list.retainAll(other));

        assertEquals(0, list.size());
    }

    @Test
    void removeAllIsANoOpIfTheTargetListIsEmpty()
    {
        final IntArrayList other = new IntArrayList();
        list.addInt(5);
        list.addInt(8);

        assertFalse(list.removeAll(other));

        assertEquals(2, list.size());
        assertEquals(5, list.getInt(0));
        assertEquals(8, list.getInt(1));
    }

    @Test
    void removeAllShouldDeleteAllElementsThatAreContainedInTheTargetList()
    {
        final int nullValue = -1;
        assertNotEquals(DEFAULT_NULL_VALUE, nullValue);
        final IntArrayList other = new IntArrayList(2, nullValue);
        other.addInt(1);
        other.addInt(3);
        other.addInt(5);
        other.addInt(7);
        other.add(null);
        list.addInt(7);
        list.addInt(1);
        list.addInt(2);
        list.addInt(4);
        list.addInt(6);
        list.addInt(1);
        list.addInt(1);
        list.add(null);
        list.addInt(1);
        list.addInt(5);
        list.addInt(8);
        list.addInt(7);
        list.addInt(DEFAULT_NULL_VALUE);

        assertTrue(list.removeAll(other));

        assertEquals(4, list.size());
        assertEquals(2, list.getInt(0));
        assertEquals(4, list.getInt(1));
        assertEquals(6, list.getInt(2));
        assertEquals(8, list.getInt(3));
    }

    @Test
    void removeAllShouldNotDeleteNullValuesIfTheSourceListDoesNotContainAny()
    {
        final IntArrayList other = new IntArrayList(5, 333);
        other.addInt(1);
        other.addInt(3);
        list.addInt(1);
        list.add(null);
        list.addInt(1);
        list.addInt(2);
        list.addInt(3);
        list.addInt(6);
        list.addInt(DEFAULT_NULL_VALUE);

        assertTrue(list.removeAll(other));

        assertEquals(4, list.size());
        assertEquals(DEFAULT_NULL_VALUE, list.getInt(0));
        assertEquals(2, list.getInt(1));
        assertEquals(6, list.getInt(2));
        assertEquals(DEFAULT_NULL_VALUE, list.getInt(3));
    }

    @Test
    void removeIfIntThrowsNullPointerExceptionIsFilterIsNull()
    {
        assertThrowsExactly(NullPointerException.class, () -> list.removeIfInt(null));
    }

    @Test
    void removeIfIntDeletesAllElementsThatMatchFilter()
    {
        final IntPredicate filter = (v) -> v < 0 || v > 10;
        list.add(null);
        list.addInt(2);
        list.addInt(DEFAULT_NULL_VALUE);
        list.addInt(3);
        list.addInt(0);
        list.addInt(42);

        assertTrue(list.removeIfInt(filter));

        assertEquals(3, list.size());
        assertEquals(2, list.getInt(0));
        assertEquals(3, list.getInt(1));
        assertEquals(0, list.getInt(2));
    }

    @Test
    void removeIfIsANoOpIfNoElementsMatchTheFilter()
    {
        final IntPredicate filter = (v) -> v < 0;
        list.addInt(2);
        list.addInt(3);
        list.addInt(0);
        list.addInt(42);

        assertFalse(list.removeIfInt(filter));

        assertEquals(4, list.size());
        assertEquals(2, list.getInt(0));
        assertEquals(3, list.getInt(1));
        assertEquals(0, list.getInt(2));
        assertEquals(42, list.getInt(3));
    }

    @Test
    void removeByObjectReturnsTrueAfterRemovingTheFirstOccurrenceOfTheValue()
    {
        list.addInt(1);
        list.addInt(2);
        list.addInt(1);
        list.addInt(5);

        assertTrue(list.remove(Integer.valueOf(1)));

        assertEquals(3, list.size());
        assertEquals(2, list.getInt(0));
        assertEquals(1, list.getInt(1));
        assertEquals(5, list.getInt(2));
    }

    @Test
    void removeByObjectReturnsTrueAfterRemovingFirstNull()
    {
        list.addInt(1);
        list.addInt(1);
        list.addInt(DEFAULT_NULL_VALUE);
        list.addInt(5);
        list.add(null);

        assertTrue(list.remove(null));

        assertEquals(4, list.size());
        assertEquals(1, list.getInt(0));
        assertEquals(1, list.getInt(1));
        assertEquals(5, list.getInt(2));
        assertEquals(DEFAULT_NULL_VALUE, list.getInt(3));
    }
}
