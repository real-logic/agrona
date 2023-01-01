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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class ObjectHashSetIntegerTest
{
    static Iterable<ObjectHashSet<Integer>> data()
    {
        return Arrays.asList(
            new ObjectHashSet<>(INITIAL_CAPACITY),
            new ObjectHashSet<>(INITIAL_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, false));
    }

    private static final int INITIAL_CAPACITY = 100;

    @ParameterizedTest
    @MethodSource("data")
    void initiallyContainsNoElements(final ObjectHashSet<Integer> testSet)
    {
        for (int i = 0; i < 10_000; i++)
        {
            assertFalse(testSet.contains(i));
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void initiallyContainsNoBoxedElements(final ObjectHashSet<Integer> testSet)
    {
        for (int i = 0; i < 10_000; i++)
        {
            //noinspection UnnecessaryBoxing
            assertFalse(testSet.contains(Integer.valueOf(i)));
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void containsAddedElement(final ObjectHashSet<Integer> testSet)
    {
        assertTrue(testSet.add(1));

        assertTrue(testSet.contains(1));
    }

    @ParameterizedTest
    @MethodSource("data")
    void addingAnElementTwiceDoesNothing(final ObjectHashSet<Integer> testSet)
    {
        assertTrue(testSet.add(1));

        assertFalse(testSet.add(1));
    }

    @ParameterizedTest
    @MethodSource("data")
    void containsAddedBoxedElements(final ObjectHashSet<Integer> testSet)
    {
        assertTrue(testSet.add(1));
        //noinspection UnnecessaryBoxing
        assertTrue(testSet.add(Integer.valueOf(2)));

        //noinspection UnnecessaryBoxing
        assertTrue(testSet.contains(Integer.valueOf(1)));
        assertTrue(testSet.contains(2));
    }

    @ParameterizedTest
    @MethodSource("data")
    void doesNotContainMissingValue(final ObjectHashSet<Integer> testSet)
    {
        assertFalse(testSet.contains(2048));
    }

    @ParameterizedTest
    @MethodSource("data")
    void removingAnElementFromAnEmptyListDoesNothing(final ObjectHashSet<Integer> testSet)
    {
        assertFalse(testSet.remove(0));
    }

    @ParameterizedTest
    @MethodSource("data")
    void removingAPresentElementRemovesIt(final ObjectHashSet<Integer> testSet)
    {
        assertTrue(testSet.add(1));

        assertTrue(testSet.remove(1));

        assertFalse(testSet.contains(1));
    }

    @ParameterizedTest
    @MethodSource("data")
    void sizeIsInitiallyZero(final ObjectHashSet<Integer> testSet)
    {
        assertEquals(0, testSet.size());
    }

    @ParameterizedTest
    @MethodSource("data")
    void sizeIncrementsWithNumberOfAddedElements(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);
        assertEquals(2, testSet.size());
    }

    @ParameterizedTest
    @MethodSource("data")
    @SuppressWarnings("OverwrittenKey")
    void sizeContainsNumberOfNewElements(final ObjectHashSet<Integer> testSet)
    {
        testSet.add(1);
        testSet.add(1);

        assertEquals(1, testSet.size());
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsListElements(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        assertIteratorHasElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsStartFromTheBeginningEveryTime(final ObjectHashSet<Integer> testSet)
    {
        iteratorsListElements(testSet);

        assertIteratorHasElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsListElementsWithoutHasNext(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        assertIteratorHasElementsWithoutHasNext(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsStartFromTheBeginningEveryTimeWithoutHasNext(final ObjectHashSet<Integer> testSet)
    {
        iteratorsListElementsWithoutHasNext(testSet);

        assertIteratorHasElementsWithoutHasNext(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsThrowNoSuchElementException(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        assertThrows(NoSuchElementException.class, () -> exhaustIterator(testSet));
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorsThrowNoSuchElementExceptionFromTheBeginningEveryTime(final ObjectHashSet<Integer> testSet)
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
    void iteratorHasNoElements(final ObjectHashSet<Integer> testSet)
    {
        assertFalse(testSet.iterator().hasNext());
    }

    @ParameterizedTest
    @MethodSource("data")
    void iteratorThrowExceptionForEmptySet(final ObjectHashSet<Integer> testSet)
    {
        assertThrows(NoSuchElementException.class, () -> testSet.iterator().next());
    }

    @ParameterizedTest
    @MethodSource("data")
    void clearRemovesAllElementsOfTheSet(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        testSet.clear();

        assertEquals(0, testSet.size());
        assertFalse(testSet.contains(1));
        assertFalse(testSet.contains(1001));
    }

    @ParameterizedTest
    @MethodSource("data")
    void differenceReturnsNullIfBothSetsEqual(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);
        addTwoElements(other);

        assertNull(testSet.difference(other));
    }

    @ParameterizedTest
    @MethodSource("data")
    void differenceReturnsSetDifference(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);
        other.add(1);

        final ObjectHashSet<Integer> diff = testSet.difference(other);
        assertThat(diff, containsInAnyOrder(1001));
    }

    @ParameterizedTest
    @MethodSource("data")
    void copiesOtherIntHashSet(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);
        other.copy(testSet);

        assertContainsElements(other);
    }

    @ParameterizedTest
    @MethodSource("data")
    void twoEmptySetsAreEqual(final ObjectHashSet<Integer> testSet)
    {
        final ObjectHashSet<?> other = new ObjectHashSet<>(100);
        assertEquals(testSet, other);
    }

    @ParameterizedTest
    @MethodSource("data")
    void setsWithTheSameValuesAreEqual(final ObjectHashSet<Integer> testSet)
    {
        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);
        addTwoElements(other);

        assertEquals(testSet, other);
    }

    @ParameterizedTest
    @MethodSource("data")
    void setsWithTheDifferentSizesAreNotEqual(final ObjectHashSet<Integer> testSet)
    {
        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);

        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @ParameterizedTest
    @MethodSource("data")
    void setsWithTheDifferentValuesAreNotEqual(final ObjectHashSet<Integer> testSet)
    {
        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);

        other.add(2);
        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @ParameterizedTest
    @MethodSource("data")
    void twoEmptySetsHaveTheSameHashcode(final ObjectHashSet<Integer> testSet)
    {
        assertEquals(testSet.hashCode(), new ObjectHashSet<Integer>(100).hashCode());
    }

    @ParameterizedTest
    @MethodSource("data")
    void setsWithTheSameValuesHaveTheSameHashcode(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> secondSet = new ObjectHashSet<>(100);
        addTwoElements(secondSet);

        assertEquals(testSet.hashCode(), secondSet.hashCode());
    }

    @ParameterizedTest
    @MethodSource("data")
    void reducesSizeWhenElementRemoved(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        testSet.remove(1001);

        assertEquals(1, testSet.size());
    }

    @ParameterizedTest
    @MethodSource("data")
    void toArrayThrowsNullPointerExceptionForNullArgument(final ObjectHashSet<Integer> testSet)
    {
        final Integer[] into = null;
        assertThrows(NullPointerException.class, () -> testSet.toArray(into));
    }

    @ParameterizedTest
    @MethodSource("data")
    void toArrayCopiesElementsIntoSufficientlySizedArray(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final Integer[] result = testSet.toArray(new Integer[0]);

        assertArrayContainingElements(result);
    }

    @ParameterizedTest
    @MethodSource("data")
    void toArrayCopiesElementsIntoNewArray(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final Integer[] result = testSet.toArray(new Integer[0]);

        assertArrayContainingElements(result);
    }

    @ParameterizedTest
    @MethodSource("data")
    void toArraySupportsEmptyCollection(final ObjectHashSet<Integer> testSet)
    {
        final Integer[] result = testSet.toArray(new Integer[0]);

        assertArrayEquals(result, new Integer[]{});
    }

    @Test
    void chainCompactionShouldNotCauseElementsToBeMovedBeforeTheirHash()
    {
        final ObjectHashSet<Integer> requiredFields = new ObjectHashSet<>(14);

        requiredFields.add(8);
        requiredFields.add(9);
        requiredFields.add(35);
        requiredFields.add(49);
        requiredFields.add(56);

        assertTrue(requiredFields.remove(8), "Failed to remove 8");
        assertTrue(requiredFields.remove(9), "Failed to remove 9");

        assertThat(requiredFields, containsInAnyOrder(35, 49, 56));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldResizeWhenItHitsCapacity(final ObjectHashSet<Integer> testSet)
    {
        for (int i = 0; i < 2 * INITIAL_CAPACITY; i++)
        {
            assertTrue(testSet.add(i));
        }

        for (int i = 0; i < 2 * INITIAL_CAPACITY; i++)
        {
            assertTrue(testSet.contains(i));
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void containsEmptySet(final ObjectHashSet<Integer> testSet)
    {
        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        assertTrue(testSet.containsAll(other));
    }

    @ParameterizedTest
    @MethodSource("data")
    void containsSubset(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> subset = new ObjectHashSet<>(100);

        subset.add(1);

        assertTrue(testSet.containsAll(subset));
    }

    @ParameterizedTest
    @MethodSource("data")
    void doesNotContainDisjointSet(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        other.add(1);
        other.add(1002);

        assertFalse(testSet.containsAll(other));
    }

    @ParameterizedTest
    @MethodSource("data")
    void doesNotContainSuperset(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> superset = new ObjectHashSet<>(100);

        addTwoElements(superset);
        superset.add(15);

        assertFalse(testSet.containsAll(superset));
    }

    @ParameterizedTest
    @MethodSource("data")
    void addingEmptySetDoesNothing(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        assertFalse(testSet.addAll(new ObjectHashSet<>(100)));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void addingSubsetDoesNothing(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> subset = new ObjectHashSet<>(100);

        subset.add(1);

        assertFalse(testSet.addAll(subset));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void addingEqualSetDoesNothing(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> equal = new ObjectHashSet<>(100);

        addTwoElements(equal);

        assertFalse(testSet.addAll(equal));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void containsValuesAddedFromDisjointSet(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> disjoint = new ObjectHashSet<>(100);

        disjoint.add(2);
        disjoint.add(1002);

        assertTrue(testSet.addAll(disjoint));
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(1001));
        assertTrue(testSet.containsAll(disjoint));
    }

    @ParameterizedTest
    @MethodSource("data")
    void containsValuesAddedFromIntersectingSet(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> intersecting = new ObjectHashSet<>(100);

        intersecting.add(1);
        intersecting.add(1002);

        assertTrue(testSet.addAll(intersecting));
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(1001));
        assertTrue(testSet.containsAll(intersecting));
    }

    @ParameterizedTest
    @MethodSource("data")
    void removingEmptySetDoesNothing(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        assertFalse(testSet.removeAll(new ObjectHashSet<>(100)));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void removingDisjointSetDoesNothing(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> disjoint = new ObjectHashSet<>(100);

        disjoint.add(2);
        disjoint.add(1002);

        assertFalse(testSet.removeAll(disjoint));
        assertContainsElements(testSet);
    }

    @ParameterizedTest
    @MethodSource("data")
    void doesNotContainRemovedIntersectingSet(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> intersecting = new ObjectHashSet<>(100);

        intersecting.add(1);
        intersecting.add(1002);

        assertTrue(testSet.removeAll(intersecting));
        assertTrue(testSet.contains(1001));
        assertFalse(testSet.containsAll(intersecting));
    }

    @ParameterizedTest
    @MethodSource("data")
    void isEmptyAfterRemovingEqualSet(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> equal = new ObjectHashSet<>(100);

        addTwoElements(equal);

        assertTrue(testSet.removeAll(equal));
        assertTrue(testSet.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("data")
    void removeElementsFromIterator(final ObjectHashSet<Integer> testSet)
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer>.ObjectIterator intIterator = testSet.iterator();
        //noinspection Java8CollectionRemoveIf
        while (intIterator.hasNext())
        {
            if (intIterator.next().equals(1))
            {
                intIterator.remove();
            }
        }

        assertThat(testSet, contains(1001));
        assertThat(testSet, hasSize(1));
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldGenerateStringRepresentation(final ObjectHashSet<Integer> testSet)
    {
        final int[] testEntries = { 3, 1, -1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            testSet.add(testEntry);
        }

        final String mapAsAString = "{-1, 11, 19, 7, 1, 12, 3}";
        assertThat(testSet.toString(), equalTo(mapAsAString));
    }

    @Test
    void shouldIterateOverExpandedSet()
    {
        final HashSet<Integer> refSet = new HashSet<>(5);
        final ObjectHashSet<Integer> testSet = new ObjectHashSet<>(5);

        for (int i = 0; i < 20; i++)
        {
            refSet.add(i);
            testSet.add(i);
        }

        final ObjectHashSet<Integer>.ObjectIterator iter = testSet.iterator();
        for (int i = 0; i < 20; i++)
        {
            assertTrue(iter.hasNext());
            assertTrue(refSet.contains(iter.next()));
        }

        assertFalse(iter.hasNext());
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldHaveCompatibleEqualsAndHashcode(final ObjectHashSet<Integer> testSet)
    {
        final HashSet<Integer> compatibleSet = new HashSet<>();
        final long seed = System.nanoTime();
        final Random r = new Random(seed);
        for (int i = 0; i < 1024; i++)
        {
            final int value = r.nextInt();
            compatibleSet.add(value);
            testSet.add(value);
        }

        assertEquals(testSet, compatibleSet, "Fail with seed:" + seed);
        assertEquals(compatibleSet, testSet, "Fail with seed:" + seed);
        assertEquals(compatibleSet.hashCode(), testSet.hashCode(), "Fail with seed:" + seed);
    }

    private static void addTwoElements(final ObjectHashSet<Integer> obj)
    {
        obj.add(1);
        obj.add(1001);
    }

    private void assertIteratorHasElements(final ObjectHashSet<Integer> testSet)
    {
        final Iterator<Integer> iterator = testSet.iterator();
        final Set<Integer> values = new HashSet<>();

        assertTrue(iterator.hasNext());
        values.add(iterator.next());
        assertTrue(iterator.hasNext());
        values.add(iterator.next());
        assertFalse(iterator.hasNext());

        assertContainsElements(values);
    }

    private void assertIteratorHasElementsWithoutHasNext(final ObjectHashSet<Integer> testSet)
    {
        final Iterator<Integer> iterator = testSet.iterator();
        final Set<Integer> values = new HashSet<>();

        values.add(iterator.next());
        values.add(iterator.next());

        assertContainsElements(values);
    }

    private static void assertArrayContainingElements(final Integer[] result)
    {
        assertThat(result, arrayContainingInAnyOrder(1, 1001));
    }

    private static void assertContainsElements(final Set<Integer> other)
    {
        assertThat(other, containsInAnyOrder(1, 1001));
    }

    private void exhaustIterator(final ObjectHashSet<Integer> testSet)
    {
        final ObjectHashSet<Integer>.ObjectIterator iterator = testSet.iterator();
        iterator.next();
        iterator.next();
        iterator.next();
    }
}
