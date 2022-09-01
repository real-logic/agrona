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
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ObjectHashSetStringTest
{
    static Iterable<ObjectHashSet<String>> data()
    {
        return Arrays.asList(
            new ObjectHashSet<>(INITIAL_CAPACITY),
            new ObjectHashSet<>(INITIAL_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, false));
    }

    private static final int INITIAL_CAPACITY = 100;

    @ParameterizedTest
    @MethodSource("data")
    void containsAddedElement(final ObjectHashSet<String> testSet)
    {
        assertTrue(testSet.add("1"));

        assertTrue(testSet.contains("1"));
    }

    @ParameterizedTest
    @MethodSource("data")
    void addingAnElementTwiceDoesNothing(final ObjectHashSet<String> testSet)
    {
        assertTrue(testSet.add("1"));

        assertFalse(testSet.add("1"));
    }

    @ParameterizedTest
    @MethodSource("data")
    void removingAnElementFromAnEmptyListDoesNothing(final ObjectHashSet<String> testSet)
    {
        assertFalse(testSet.remove("0"));
    }

    @ParameterizedTest
    @MethodSource("data")
    void removingAPresentElementRemovesIt(final ObjectHashSet<String> testSet)
    {
        assertTrue(testSet.add("1"));

        assertTrue(testSet.remove("1"));

        assertFalse(testSet.contains("1"));
    }

    @ParameterizedTest
    @MethodSource("data")
    void sizeIsInitiallyZero(final ObjectHashSet<String> testSet)
    {
        assertEquals(0, testSet.size());
    }

    @ParameterizedTest
    @MethodSource("data")
    void sizeIncrementsWithNumberOfAddedElements(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        assertEquals(2, testSet.size());
    }

    @ParameterizedTest
    @MethodSource("data")
    void sizeContainsNumberOfNewElements(final ObjectHashSet<String> testSet)
    {
        testSet.add("1");
        testSet.add("1");

        assertEquals(1, testSet.size());
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsListElements(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        assertIteratorHasElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsStartFromTheBeginningEveryTime(final ObjectHashSet<String> testSet)
    {
        iteratorsListElements(testSet);

        assertIteratorHasElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsListElementsWithoutHasNext(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        assertIteratorHasElementsWithoutHasNext(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsStartFromTheBeginningEveryTimeWithoutHasNext(final ObjectHashSet<String> testSet)
    {
        iteratorsListElementsWithoutHasNext(testSet);

        assertIteratorHasElementsWithoutHasNext(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsThrowNoSuchElementException(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        assertThrows(NoSuchElementException.class, () -> exhaustIterator(testSet));
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsThrowNoSuchElementExceptionFromTheBeginningEveryTime(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        try
        {
            exhaustIterator(testSet);
        }
        catch (final NoSuchElementException ignore)
        {
        }

        assertThrows(NoSuchElementException.class, () -> exhaustIterator(testSet));
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorHasNoElements(final ObjectHashSet<String> testSet)
    {
        assertFalse(testSet.iterator().hasNext());
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorThrowExceptionForEmptySet(final ObjectHashSet<String> testSet)
    {
        assertThrows(NoSuchElementException.class, () -> testSet.iterator().next());
    }

    @ParameterizedTest
    @MethodSource("data")
    void clearRemovesAllElementsOfTheSet(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        testSet.clear();

        assertEquals(0, testSet.size());
        assertFalse(testSet.contains("1"));
        assertFalse(testSet.contains("1001"));
    }

    @ParameterizedTest
    @MethodSource("data")
    void differenceReturnsNullIfBothSetsEqual(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> other = new ObjectHashSet<>(100);
        addTwoElements(other);

        assertNull(testSet.difference(other));
    }

    @ParameterizedTest
    @MethodSource("data")
    void differenceReturnsSetDifference(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> other = new ObjectHashSet<>(100);
        other.add("1");

        final ObjectHashSet<String> diff = testSet.difference(other);
        assertThat(diff, containsInAnyOrder("1001"));
    }

    @ParameterizedTest
    @MethodSource("data")
    void twoEmptySetsAreEqual(final ObjectHashSet<String> testSet)
    {
        final ObjectHashSet<?> other = new ObjectHashSet<>(100);
        assertEquals(testSet, other);
    }

    @ParameterizedTest
    @MethodSource("data")
    void setsWithTheSameValuesAreEqual(final ObjectHashSet<String> testSet)
    {
        final ObjectHashSet<String> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);
        addTwoElements(other);

        assertEquals(testSet, other);
    }

    @ParameterizedTest
    @MethodSource("data")
    void setsWithTheDifferentSizesAreNotEqual(final ObjectHashSet<String> testSet)
    {
        final ObjectHashSet<String> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);

        other.add("1001");

        assertNotEquals(testSet, other);
    }

    @ParameterizedTest
    @MethodSource("data")
    void setsWithTheDifferentValuesAreNotEqual(final ObjectHashSet<String> testSet)
    {
        final ObjectHashSet<String> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);

        other.add("2");
        other.add("1001");

        assertNotEquals(testSet, other);
    }

    @ParameterizedTest
    @MethodSource("data")
    void twoEmptySetsHaveTheSameHashcode(final ObjectHashSet<String> testSet)
    {
        assertEquals(testSet.hashCode(), new ObjectHashSet<String>(100).hashCode());
    }

    @ParameterizedTest
    @MethodSource("data")
    void reducesSizeWhenElementRemoved(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        testSet.remove("1001");

        assertEquals(1, testSet.size());
    }

    @ParameterizedTest
    @MethodSource("data")
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    void toArrayCopiesElementsIntoSufficientlySizedArray(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final String[] result = testSet.toArray(new String[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @ParameterizedTest
    @MethodSource("data")
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    void toArrayCopiesElementsIntoNewArray(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final String[] result = testSet.toArray(new String[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @ParameterizedTest
    @MethodSource("data")
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    void toArraySupportsEmptyCollection(final ObjectHashSet<String> testSet)
    {
        final String[] result = testSet.toArray(new String[testSet.size()]);

        assertArrayEquals(result, new String[]{});
    }

    @Test
    void chainCompactionShouldNotCauseElementsToBeMovedBeforeTheirHash()
    {
        final ObjectHashSet<String> requiredFields = new ObjectHashSet<>(14);

        requiredFields.add("8");
        requiredFields.add("9");
        requiredFields.add("35");
        requiredFields.add("49");
        requiredFields.add("56");

        assertTrue(requiredFields.remove("8"), "Failed to remove 8");
        assertTrue(requiredFields.remove("9"), "Failed to remove 9");

        assertThat(requiredFields, containsInAnyOrder("35", "49", "56"));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldResizeWhenItHitsCapacity(final ObjectHashSet<String> testSet)
    {
        for (int i = 0; i < 2 * INITIAL_CAPACITY; i++)
        {
            assertTrue(testSet.add(String.valueOf(i)));
        }

        for (int i = 0; i < 2 * INITIAL_CAPACITY; i++)
        {
            assertTrue(testSet.contains(String.valueOf(i)));
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void containsSubset(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> subset = new ObjectHashSet<>(100);

        subset.add("1");

        assertTrue(testSet.containsAll(subset));
    }

    @ParameterizedTest
    @MethodSource("data")
    void doesNotContainDisjointSet(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> other = new ObjectHashSet<>(100);

        other.add("1");
        other.add("1002");

        assertFalse(testSet.containsAll(other));
    }

    @ParameterizedTest
    @MethodSource("data")
    void doesNotContainSuperset(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> superset = new ObjectHashSet<>(100);

        addTwoElements(superset);
        superset.add("15");

        assertFalse(testSet.containsAll(superset));
    }

    @ParameterizedTest
    @MethodSource("data")
    void addingEmptySetDoesNothing(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        assertFalse(testSet.addAll(new ObjectHashSet<>(100)));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void addingSubsetDoesNothing(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> subset = new ObjectHashSet<>(100);

        subset.add("1");

        assertFalse(testSet.addAll(subset));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void addingEqualSetDoesNothing(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> equal = new ObjectHashSet<>(100);

        addTwoElements(equal);

        assertFalse(testSet.addAll(equal));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void containsValuesAddedFromDisjointSet(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> disjoint = new ObjectHashSet<>(100);

        disjoint.add("2");
        disjoint.add("1002");

        assertTrue(testSet.addAll(disjoint));
        assertTrue(testSet.contains("1"));
        assertTrue(testSet.contains("1001"));
        assertTrue(testSet.containsAll(disjoint));
    }

    @ParameterizedTest
    @MethodSource("data")
    void containsValuesAddedFromIntersectingSet(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> intersecting = new ObjectHashSet<>(100);

        intersecting.add("1");
        intersecting.add("1002");

        assertTrue(testSet.addAll(intersecting));
        assertTrue(testSet.contains("1"));
        assertTrue(testSet.contains("1001"));
        assertTrue(testSet.containsAll(intersecting));
    }

    @ParameterizedTest
    @MethodSource("data")
    void removingEmptySetDoesNothing(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        assertFalse(testSet.removeAll(new ObjectHashSet<>(100)));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void removingDisjointSetDoesNothing(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> disjoint = new ObjectHashSet<>(100);

        disjoint.add("2");
        disjoint.add("1002");

        assertFalse(testSet.removeAll(disjoint));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void doesNotContainRemovedIntersectingSet(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> intersecting = new ObjectHashSet<>(100);

        intersecting.add("1");
        intersecting.add("1002");

        assertTrue(testSet.removeAll(intersecting));
        assertTrue(testSet.contains("1001"));
        assertFalse(testSet.containsAll(intersecting));
    }

    @ParameterizedTest
    @MethodSource("data")
    void isEmptyAfterRemovingEqualSet(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> equal = new ObjectHashSet<>(100);

        addTwoElements(equal);

        assertTrue(testSet.removeAll(equal));
        assertTrue(testSet.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("data")
    void removeElementsFromIterator(final ObjectHashSet<String> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<String>.ObjectIterator iter = testSet.iterator();
        //noinspection Java8CollectionRemoveIf
        while (iter.hasNext())
        {
            if (iter.next().equals("1"))
            {
                iter.remove();
            }
        }

        assertThat(testSet, contains("1001"));
        assertThat(testSet, hasSize(1));
    }

    @Test
    void shouldForEachValues()
    {
        final ObjectHashSet<String> set = new ObjectHashSet<>();
        for (int i = 0; i < 11; i++)
        {
            final String val = Integer.toString(i);
            set.add(val);
        }

        final Collection<String> copyToSetOne = new HashSet<>();
        for (final String s : set)
        {
            //noinspection UseBulkOperation
            copyToSetOne.add(s);
        }

        final Collection<String> copyToSetTwo = new HashSet<>();
        set.forEach(copyToSetTwo::add);

        assertEquals(copyToSetTwo, copyToSetOne);
    }

    private static void addTwoElements(final ObjectHashSet<String> obj)
    {
        obj.add("1");
        obj.add("1001");
    }

    private void assertIteratorHasElements(final ObjectHashSet<String> testSet)
    {
        final Iterator<String> iter = testSet.iterator();
        final Set<String> values = new HashSet<>();

        assertTrue(iter.hasNext());
        values.add(iter.next());
        assertTrue(iter.hasNext());
        values.add(iter.next());
        assertFalse(iter.hasNext());

        assertContainsElements(values);
    }

    private void assertIteratorHasElementsWithoutHasNext(final ObjectHashSet<String> testSet)
    {
        final Iterator<String> iter = testSet.iterator();
        final Set<String> values = new HashSet<>();

        values.add(iter.next());
        values.add(iter.next());

        assertContainsElements(values);
    }

    private static void assertArrayContainingElements(final String[] result)
    {
        assertThat(result, arrayContainingInAnyOrder("1", "1001"));
    }

    private static void assertContainsElements(final Set<String> other)
    {
        assertThat(other, containsInAnyOrder("1", "1001"));
    }

    private void exhaustIterator(final ObjectHashSet<String> testSet)
    {
        final Iterator<String> iterator = testSet.iterator();
        iterator.next();
        iterator.next();
        iterator.next();
    }
}
