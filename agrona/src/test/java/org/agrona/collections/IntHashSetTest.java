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

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.agrona.collections.IntHashSet.MISSING_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;

public class IntHashSetTest
{
    private static final int INITIAL_CAPACITY = 100;

    private final IntHashSet testSet = new IntHashSet(INITIAL_CAPACITY);

    @Test
    public void initiallyContainsNoElements()
    {
        for (int i = 0; i < 10_000; i++)
        {
            assertFalse(testSet.contains(i));
        }
    }

    @Test
    public void initiallyContainsNoBoxedElements()
    {
        for (int i = 0; i < 10_000; i++)
        {
            assertFalse(testSet.contains(Integer.valueOf(i)));
        }
    }

    @Test
    public void containsAddedElement()
    {
        assertTrue(testSet.add(1));

        assertTrue(testSet.contains(1));
    }

    @Test
    public void addingAnElementTwiceDoesNothing()
    {
        assertTrue(testSet.add(1));

        assertFalse(testSet.add(1));
    }

    @Test
    public void containsAddedBoxedElements()
    {
        assertTrue(testSet.add(1));
        assertTrue(testSet.add(Integer.valueOf(2)));

        assertTrue(testSet.contains(Integer.valueOf(1)));
        assertTrue(testSet.contains(2));
    }

    @Test
    public void removingAnElementFromAnEmptyListDoesNothing()
    {
        assertFalse(testSet.remove(0));
    }

    @Test
    public void removingAPresentElementRemovesIt()
    {
        assertTrue(testSet.add(1));

        assertTrue(testSet.remove(1));

        assertFalse(testSet.contains(1));
    }

    @Test
    public void sizeIsInitiallyZero()
    {
        assertEquals(0, testSet.size());
    }

    @Test
    public void sizeIncrementsWithNumberOfAddedElements()
    {
        addTwoElements(testSet);

        assertEquals(2, testSet.size());
    }

    @Test
    @SuppressWarnings("OverwrittenKey")
    public void sizeContainsNumberOfNewElements()
    {
        testSet.add(1);
        testSet.add(1);

        assertEquals(1, testSet.size());
    }

    @Test
    public void iteratorsListElements()
    {
        addTwoElements(testSet);

        assertIteratorHasElements();
    }

    @Test
    public void iteratorsStartFromTheBeginningEveryTime()
    {
        iteratorsListElements();

        assertIteratorHasElements();
    }

    @Test
    public void iteratorsListElementsWithoutHasNext()
    {
        addTwoElements(testSet);

        assertIteratorHasElementsWithoutHasNext();
    }

    @Test
    public void iteratorsStartFromTheBeginningEveryTimeWithoutHasNext()
    {
        iteratorsListElementsWithoutHasNext();

        assertIteratorHasElementsWithoutHasNext();
    }

    @Test
    public void iteratorsThrowNoSuchElementException()
    {
        addTwoElements(testSet);

        assertThrows(NoSuchElementException.class, this::exhaustIterator);
    }

    @Test
    public void iteratorsThrowNoSuchElementExceptionFromTheBeginningEveryTime()
    {
        addTwoElements(testSet);

        try
        {
            exhaustIterator();
        }
        catch (final NoSuchElementException ignore)
        {
        }

        assertThrows(NoSuchElementException.class, this::exhaustIterator);
    }

    @Test
    public void iteratorHasNoElements()
    {
        assertFalse(testSet.iterator().hasNext());
    }

    @Test
    public void iteratorThrowExceptionForEmptySet()
    {
        assertThrows(NoSuchElementException.class, () -> testSet.iterator().next());
    }

    @Test
    public void clearRemovesAllElementsOfTheSet()
    {
        addTwoElements(testSet);

        testSet.clear();

        assertEquals(0, testSet.size());
        assertFalse(testSet.contains(1));
        assertFalse(testSet.contains(1001));
    }

    @Test
    public void differenceReturnsNullIfBothSetsEqual()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100);
        addTwoElements(other);

        assertNull(testSet.difference(other));
    }

    @Test
    public void differenceReturnsSetDifference()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100);
        other.add(1);

        final IntHashSet diff = testSet.difference(other);
        assertThat(diff, containsInAnyOrder(1001));
    }

    @Test
    public void copiesOtherIntHashSet()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100);
        other.copy(testSet);

        assertContainsElements(other);
    }

    @Test
    public void twoEmptySetsAreEqual()
    {
        final IntHashSet other = new IntHashSet(100);
        assertEquals(testSet, other);
    }

    @Test
    public void setsWithTheSameValuesAreEqual()
    {
        final IntHashSet other = new IntHashSet(100);

        addTwoElements(testSet);
        addTwoElements(other);

        assertEquals(testSet, other);
    }

    @Test
    public void setsWithTheDifferentSizesAreNotEqual()
    {
        final IntHashSet other = new IntHashSet(100);

        addTwoElements(testSet);

        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @Test
    public void setsWithTheDifferentValuesAreNotEqual()
    {
        final IntHashSet other = new IntHashSet(100);

        addTwoElements(testSet);

        other.add(2);
        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @Test
    public void twoEmptySetsHaveTheSameHashcode()
    {
        assertEquals(testSet.hashCode(), new IntHashSet(100).hashCode());
    }

    @Test
    public void setsWithTheSameValuesHaveTheSameHashcode()
    {
        final IntHashSet other = new IntHashSet(100);

        addTwoElements(testSet);

        addTwoElements(other);

        assertEquals(testSet.hashCode(), other.hashCode());
    }

    @Test
    public void reducesSizeWhenElementRemoved()
    {
        addTwoElements(testSet);

        testSet.remove(1001);

        assertEquals(1, testSet.size());
    }

    @Test
    @SuppressWarnings("SuspiciousToArrayCall")
    public void toArrayThrowsArrayStoreExceptionForWrongType()
    {
        assertThrows(ArrayStoreException.class, () -> testSet.toArray(new String[1]));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void toArrayThrowsNullPointerExceptionForNullArgument()
    {
        final Integer[] into = null;
        assertThrows(NullPointerException.class, () -> testSet.toArray(into));
    }

    @Test
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    public void toArrayCopiesElementsIntoSufficientlySizedArray()
    {
        addTwoElements(testSet);

        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    public void toArrayCopiesElementsIntoNewArray()
    {
        addTwoElements(testSet);

        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    public void toArraySupportsEmptyCollection()
    {
        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertArrayEquals(result, new Integer[]{});
    }

    // Test case from usage bug.
    @Test
    public void chainCompactionShouldNotCauseElementsToBeMovedBeforeTheirHash()
    {
        final IntHashSet requiredFields = new IntHashSet(14);

        requiredFields.add(8);
        requiredFields.add(9);
        requiredFields.add(35);
        requiredFields.add(49);
        requiredFields.add(56);

        assertTrue(requiredFields.remove(8), "Failed to remove 8");
        assertTrue(requiredFields.remove(9), "Failed to remove 9");

        assertThat(requiredFields, containsInAnyOrder(35, 49, 56));
    }

    @Test
    public void shouldResizeWhenItHitsCapacity()
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

    @Test
    public void containsEmptySet()
    {
        final IntHashSet other = new IntHashSet(100);

        assertTrue(testSet.containsAll(other));
        assertTrue(testSet.containsAll((Collection<?>)other));
    }

    @Test
    public void containsSubset()
    {
        addTwoElements(testSet);

        final IntHashSet subset = new IntHashSet(100);

        subset.add(1);

        assertTrue(testSet.containsAll(subset));
        assertTrue(testSet.containsAll((Collection<?>)subset));
    }

    @Test
    public void doesNotContainDisjointSet()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100);

        other.add(1);
        other.add(1002);

        assertFalse(testSet.containsAll(other));
        assertFalse(testSet.containsAll((Collection<?>)other));
    }

    @Test
    public void doesNotContainSuperset()
    {
        addTwoElements(testSet);

        final IntHashSet superset = new IntHashSet(100);

        addTwoElements(superset);
        superset.add(15);

        assertFalse(testSet.containsAll(superset));
        assertFalse(testSet.containsAll((Collection<?>)superset));
    }

    @Test
    public void addingEmptySetDoesNothing()
    {
        addTwoElements(testSet);

        assertFalse(testSet.addAll(new IntHashSet(100)));
        assertFalse(testSet.addAll(new HashSet<>()));
        assertContainsElements(testSet);
    }

    @Test
    public void addingSubsetDoesNothing()
    {
        addTwoElements(testSet);

        final IntHashSet subset = new IntHashSet(100);

        subset.add(1);

        final HashSet<Integer> subSetCollection = new HashSet<>(subset);

        assertFalse(testSet.addAll(subset));
        assertFalse(testSet.addAll(subSetCollection));
        assertContainsElements(testSet);
    }

    @Test
    public void addingEqualSetDoesNothing()
    {
        addTwoElements(testSet);

        final IntHashSet equal = new IntHashSet(100);

        addTwoElements(equal);

        final HashSet<Integer> equalCollection = new HashSet<>(equal);

        assertFalse(testSet.addAll(equal));
        assertFalse(testSet.addAll(equalCollection));
        assertContainsElements(testSet);
    }

    @Test
    public void containsValuesAddedFromDisjointSetPrimitive()
    {
        addTwoElements(testSet);

        final IntHashSet disjoint = new IntHashSet(100);

        disjoint.add(2);
        disjoint.add(1002);

        assertTrue(testSet.addAll(disjoint));
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(1001));
        assertTrue(testSet.containsAll(disjoint));
    }

    @Test
    public void containsValuesAddedFromDisjointSet()
    {
        addTwoElements(testSet);

        final HashSet<Integer> disjoint = new HashSet<>();

        disjoint.add(2);
        disjoint.add(1002);

        assertTrue(testSet.addAll(disjoint));
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(1001));
        assertTrue(testSet.containsAll(disjoint));
    }

    @Test
    public void containsValuesAddedFromIntersectingSetPrimitive()
    {
        addTwoElements(testSet);

        final IntHashSet intersecting = new IntHashSet(100);

        intersecting.add(1);
        intersecting.add(1002);

        assertTrue(testSet.addAll(intersecting));
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(1001));
        assertTrue(testSet.containsAll(intersecting));
    }

    @Test
    public void containsValuesAddedFromIntersectingSet()
    {
        addTwoElements(testSet);

        final HashSet<Integer> intersecting = new HashSet<>();

        intersecting.add(1);
        intersecting.add(1002);

        assertTrue(testSet.addAll(intersecting));
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(1001));
        assertTrue(testSet.containsAll(intersecting));
    }

    @Test
    public void removingEmptySetDoesNothing()
    {
        addTwoElements(testSet);

        assertFalse(testSet.removeAll(new IntHashSet(100)));
        assertFalse(testSet.removeAll(new HashSet<Integer>()));
        assertContainsElements(testSet);
    }

    @Test
    public void removingDisjointSetDoesNothing()
    {
        addTwoElements(testSet);

        final IntHashSet disjoint = new IntHashSet(100);

        disjoint.add(2);
        disjoint.add(1002);

        assertFalse(testSet.removeAll(disjoint));
        assertFalse(testSet.removeAll(new HashSet<Integer>()));
        assertContainsElements(testSet);
    }

    @Test
    public void doesNotContainRemovedIntersectingSetPrimitive()
    {
        addTwoElements(testSet);

        final IntHashSet intersecting = new IntHashSet(100);

        intersecting.add(1);
        intersecting.add(1002);

        assertTrue(testSet.removeAll(intersecting));
        assertTrue(testSet.contains(1001));
        assertFalse(testSet.containsAll(intersecting));
    }

    @Test
    public void doesNotContainRemovedIntersectingSet()
    {
        addTwoElements(testSet);

        final HashSet<Integer> intersecting = new HashSet<>();

        intersecting.add(1);
        intersecting.add(1002);

        assertTrue(testSet.removeAll(intersecting));
        assertTrue(testSet.contains(1001));
        assertFalse(testSet.containsAll(intersecting));
    }

    @Test
    public void isEmptyAfterRemovingEqualSetPrimitive()
    {
        addTwoElements(testSet);

        final IntHashSet equal = new IntHashSet(100);

        addTwoElements(equal);

        assertTrue(testSet.removeAll(equal));
        assertTrue(testSet.isEmpty());
    }

    @Test
    public void isEmptyAfterRemovingEqualSet()
    {
        addTwoElements(testSet);

        final HashSet<Integer> equal = new HashSet<>();

        addTwoElements(equal);

        assertTrue(testSet.removeAll(equal));
        assertTrue(testSet.isEmpty());
    }

    @Test
    public void removeElementsFromIterator()
    {
        addTwoElements(testSet);

        final IntHashSet.IntIterator iterator = testSet.iterator();
        while (iterator.hasNext())
        {
            if (iterator.nextValue() == 1)
            {
                iterator.remove();
            }
        }

        assertThat(testSet, contains(1001));
        assertThat(testSet, hasSize(1));
    }

    @Test
    public void shouldNotContainMissingValueInitially()
    {
        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    public void shouldAllowMissingValue()
    {
        assertTrue(testSet.add(MISSING_VALUE));

        assertTrue(testSet.contains(MISSING_VALUE));

        assertFalse(testSet.add(MISSING_VALUE));
    }

    @Test
    public void shouldAllowRemovalOfMissingValue()
    {
        assertTrue(testSet.add(MISSING_VALUE));

        assertTrue(testSet.remove(MISSING_VALUE));

        assertFalse(testSet.contains(MISSING_VALUE));

        assertFalse(testSet.remove(MISSING_VALUE));
    }

    @Test
    public void sizeAccountsForMissingValue()
    {
        testSet.add(1);
        testSet.add(MISSING_VALUE);

        assertEquals(2, testSet.size());
    }

    @Test
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    public void toArrayCopiesElementsIntoNewArrayIncludingMissingValue()
    {
        addTwoElements(testSet);

        testSet.add(MISSING_VALUE);

        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertThat(result, arrayContainingInAnyOrder(1, 1001, MISSING_VALUE));
    }

    @Test
    public void toObjectArrayCopiesElementsIntoNewArrayIncludingMissingValue()
    {
        addTwoElements(testSet);

        testSet.add(MISSING_VALUE);

        final Object[] result = testSet.toArray();

        assertThat(result, arrayContainingInAnyOrder(1, 1001, MISSING_VALUE));
    }

    @Test
    public void equalsAccountsForMissingValue()
    {
        addTwoElements(testSet);
        testSet.add(MISSING_VALUE);

        final IntHashSet other = new IntHashSet(100);
        addTwoElements(other);

        assertNotEquals(testSet, other);

        other.add(MISSING_VALUE);
        assertEquals(testSet, other);

        testSet.remove(MISSING_VALUE);

        assertNotEquals(testSet, other);
    }

    @Test
    public void consecutiveValuesShouldBeCorrectlyStored()
    {
        for (int i = 0; i < 10_000; i++)
        {
            testSet.add(i);
        }

        assertThat(testSet, hasSize(10_000));

        int distinctElements = 0;
        for (final int ignore : testSet)
        {
            distinctElements++;
        }

        assertThat(distinctElements, is(10_000));
    }

    @Test
    public void hashCodeAccountsForMissingValue()
    {
        addTwoElements(testSet);
        testSet.add(MISSING_VALUE);

        final IntHashSet other = new IntHashSet(100);
        addTwoElements(other);

        assertNotEquals(testSet.hashCode(), other.hashCode());

        other.add(MISSING_VALUE);
        assertEquals(testSet.hashCode(), other.hashCode());

        testSet.remove(MISSING_VALUE);

        assertNotEquals(testSet.hashCode(), other.hashCode());
    }

    @Test
    public void iteratorAccountsForMissingValue()
    {
        addTwoElements(testSet);
        testSet.add(MISSING_VALUE);

        int missingValueCount = 0;
        final IntHashSet.IntIterator iterator = testSet.iterator();
        while (iterator.hasNext())
        {
            if (iterator.nextValue() == MISSING_VALUE)
            {
                missingValueCount++;
            }
        }

        assertEquals(1, missingValueCount);
    }

    @Test
    public void iteratorCanRemoveMissingValue()
    {
        addTwoElements(testSet);
        testSet.add(MISSING_VALUE);

        final IntHashSet.IntIterator iterator = testSet.iterator();
        while (iterator.hasNext())
        {
            if (iterator.nextValue() == MISSING_VALUE)
            {
                iterator.remove();
            }
        }

        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    public void shouldGenerateStringRepresentation()
    {
        final int[] testEntries = { 3, 1, -1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            testSet.add(testEntry);
        }

        final String mapAsAString = "{1, 19, 11, 7, 3, 12, -1}";
        assertThat(testSet.toString(), equalTo(mapAsAString));
    }

    @Test
    public void shouldRemoveMissingValueWhenCleared()
    {
        assertTrue(testSet.add(MISSING_VALUE));

        testSet.clear();

        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    public void shouldHaveCompatibleEqualsAndHashcode()
    {
        final HashSet<Integer> compatibleSet = new HashSet<>();
        final long seed = System.nanoTime();
        final Random random = new Random(seed);
        for (int i = 0; i < 1024; i++)
        {
            final int value = random.nextInt();
            compatibleSet.add(value);
            testSet.add(value);
        }

        if (random.nextBoolean())
        {
            compatibleSet.add(MISSING_VALUE);
            testSet.add(MISSING_VALUE);
        }

        assertEquals(testSet, compatibleSet, "Fail with seed:" + seed);
        assertEquals(compatibleSet, testSet, "Fail with seed:" + seed);
        assertEquals(compatibleSet.hashCode(), testSet.hashCode(), "Fail with seed:" + seed);
    }

    private static void addTwoElements(final IntHashSet obj)
    {
        obj.add(1);
        obj.add(1001);
    }

    private static void addTwoElements(final HashSet<Integer> obj)
    {
        obj.add(1);
        obj.add(1001);
    }

    private void assertIteratorHasElements()
    {
        final Iterator<Integer> iter = testSet.iterator();

        final Set<Integer> values = new HashSet<>();

        assertTrue(iter.hasNext());
        values.add(iter.next());
        assertTrue(iter.hasNext());
        values.add(iter.next());
        assertFalse(iter.hasNext());

        assertContainsElements(values);
    }

    private void assertIteratorHasElementsWithoutHasNext()
    {
        final Iterator<Integer> iter = testSet.iterator();

        final Set<Integer> values = new HashSet<>();

        values.add(iter.next());
        values.add(iter.next());

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

    private void exhaustIterator()
    {
        final Iterator<Integer> iterator = testSet.iterator();
        iterator.next();
        iterator.next();
        iterator.next();
    }
}
