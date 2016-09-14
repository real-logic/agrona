/*
 *  Copyright 2014 - 2016 Real Logic Ltd.
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

import java.util.*;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

public class IntHashSetTest
{
    private static final int INITIAL_CAPACITY = 100;

    private final IntHashSet testSet = new IntHashSet(INITIAL_CAPACITY, -1);

    @Test
    public void initiallyContainsNoElements() throws Exception
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
    public void doesNotContainMissingValue()
    {
        assertFalse(testSet.contains(testSet.missingValue()));
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

    @Test(expected = NoSuchElementException.class)
    public void iteratorsThrowNoSuchElementException()
    {
        addTwoElements(testSet);

        exhaustIterator();
    }

    @Test(expected = NoSuchElementException.class)
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

        exhaustIterator();
    }

    @Test
    public void iteratorHasNoElements()
    {
        assertFalse(testSet.iterator().hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void iteratorThrowExceptionForEmptySet()
    {
        testSet.iterator().next();
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

        final IntHashSet other = new IntHashSet(100, -1);
        addTwoElements(other);

        assertNull(testSet.difference(other));
    }

    @Test
    public void differenceReturnsSetDifference()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100, -1);
        other.add(1);

        final IntHashSet diff = testSet.difference(other);
        assertThat(diff, containsInAnyOrder(1001));
    }

    @Test
    public void copiesOtherIntHashSet()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100, -1);
        other.copy(testSet);

        assertContainsElements(other);
    }

    @Test
    public void twoEmptySetsAreEqual()
    {
        final IntHashSet other = new IntHashSet(100, -1);
        assertEquals(testSet, other);
    }

    @Test
    public void equalityRequiresTheSameMissingValue()
    {
        final IntHashSet other = new IntHashSet(100, 1);
        assertNotEquals(testSet, other);
    }

    @Test
    public void setsWithTheSameValuesAreEqual()
    {
        final IntHashSet other = new IntHashSet(100, -1);

        addTwoElements(testSet);
        addTwoElements(other);

        assertEquals(testSet, other);
    }

    @Test
    public void setsWithTheDifferentSizesAreNotEqual()
    {
        final IntHashSet other = new IntHashSet(100, -1);

        addTwoElements(testSet);

        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @Test
    public void setsWithTheDifferentValuesAreNotEqual()
    {
        final IntHashSet other = new IntHashSet(100, -1);

        addTwoElements(testSet);

        other.add(2);
        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @Test
    public void twoEmptySetsHaveTheSameHashcode()
    {
        final IntHashSet other = new IntHashSet(100, -1);
        assertEquals(testSet.hashCode(), other.hashCode());
    }

    @Test
    public void setsWithTheSameValuesHaveTheSameHashcode()
    {
        final IntHashSet other = new IntHashSet(100, -1);

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

    @Test(expected = ArrayStoreException.class)
    public void toArrayThrowsArrayStoreExceptionForWrongType()
    {
        testSet.toArray(new String[1]);
    }

    @Test(expected = NullPointerException.class)
    public void toArrayThrowsNullPointerExceptionForNullArgument()
    {
        testSet.toArray(null);
    }

    @Test
    public void toArrayCopiesElementsIntoSufficientlySizedArray()
    {
        addTwoElements(testSet);

        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    public void toArrayCopiesElementsIntoNewArray()
    {
        addTwoElements(testSet);

        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    public void toArraySupportsEmptyCollection()
    {
        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertArrayEquals(result, new Integer[]{});
    }

    // Test case from usage bug.
    @Test
    public void chainCompactionShouldNotCauseElementsToBeMovedBeforeTheirHash()
    {
        final IntHashSet requiredFields = new IntHashSet(14, -1);

        requiredFields.add(8);
        requiredFields.add(9);
        requiredFields.add(35);
        requiredFields.add(49);
        requiredFields.add(56);

        assertTrue("Failed to remove 8", requiredFields.remove(8));
        assertTrue("Failed to remove 9", requiredFields.remove(9));

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
        final IntHashSet other = new IntHashSet(100, -1);

        assertTrue(testSet.containsAll(other));
        assertTrue(testSet.containsAll((Collection<?>)other));
    }

    @Test
    public void containsSubset()
    {
        addTwoElements(testSet);

        final IntHashSet subset = new IntHashSet(100, -1);

        subset.add(1);

        assertTrue(testSet.containsAll(subset));
        assertTrue(testSet.containsAll((Collection<?>)subset));
    }

    @Test
    public void doesNotContainDisjointSet()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100, -1);

        other.add(1);
        other.add(1002);

        assertFalse(testSet.containsAll(other));
        assertFalse(testSet.containsAll((Collection<?>)other));
    }

    @Test
    public void doesNotContainSuperset()
    {
        addTwoElements(testSet);

        final IntHashSet superset = new IntHashSet(100, -1);

        addTwoElements(superset);
        superset.add(15);

        assertFalse(testSet.containsAll(superset));
        assertFalse(testSet.containsAll((Collection<?>)superset));
    }

    @Test
    public void addingEmptySetDoesNothing()
    {
        addTwoElements(testSet);

        assertFalse(testSet.addAll(new IntHashSet(100, -1)));
        assertContainsElements(testSet);
    }

    @Test
    public void addingSubsetDoesNothing()
    {
        addTwoElements(testSet);

        final IntHashSet subset = new IntHashSet(100, -1);

        subset.add(1);

        assertFalse(testSet.addAll(subset));
        assertContainsElements(testSet);
    }

    @Test
    public void addingEqualSetDoesNothing()
    {
        addTwoElements(testSet);

        final IntHashSet equal = new IntHashSet(100, -1);

        addTwoElements(equal);

        assertFalse(testSet.addAll(equal));
        assertContainsElements(testSet);
    }

    @Test
    public void containsValuesAddedFromDisjointSet()
    {
        addTwoElements(testSet);

        final IntHashSet disjoint = new IntHashSet(100, -1);

        disjoint.add(2);
        disjoint.add(1002);

        assertTrue(testSet.addAll(disjoint));
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(1001));
        assertTrue(testSet.containsAll(disjoint));
    }

    @Test
    public void containsValuesAddedFromIntersectingSet()
    {
        addTwoElements(testSet);

        final IntHashSet intersecting = new IntHashSet(100, -1);

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

        assertFalse(testSet.removeAll(new IntHashSet(100, -1)));
        assertContainsElements(testSet);
    }

    @Test
    public void removingDisjointSetDoesNothing()
    {
        addTwoElements(testSet);

        final IntHashSet disjoint = new IntHashSet(100, -1);

        disjoint.add(2);
        disjoint.add(1002);

        assertFalse(testSet.removeAll(disjoint));
        assertContainsElements(testSet);
    }

    @Test
    public void doesNotContainRemovedIntersectingSet()
    {
        addTwoElements(testSet);

        final IntHashSet intersecting = new IntHashSet(100, -1);

        intersecting.add(1);
        intersecting.add(1002);

        assertTrue(testSet.removeAll(intersecting));
        assertTrue(testSet.contains(1001));
        assertFalse(testSet.containsAll(intersecting));
    }

    @Test
    public void isEmptyAfterRemovingEqualSet()
    {
        addTwoElements(testSet);

        final IntHashSet equal = new IntHashSet(100, -1);

        addTwoElements(equal);

        assertTrue(testSet.removeAll(equal));
        assertTrue(testSet.isEmpty());
    }

    private static void addTwoElements(final IntHashSet obj)
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
        final IntIterator iterator = testSet.iterator();
        iterator.next();
        iterator.next();
        iterator.next();
    }
}
