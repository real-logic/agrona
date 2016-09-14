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

    private final IntHashSet obj = new IntHashSet(INITIAL_CAPACITY, -1);

    @Test
    public void initiallyContainsNoElements() throws Exception
    {
        for (int i = 0; i < 10_000; i++)
        {
            assertFalse(obj.contains(i));
        }
    }

    @Test
    public void initiallyContainsNoBoxedElements()
    {
        for (int i = 0; i < 10_000; i++)
        {
            assertFalse(obj.contains(Integer.valueOf(i)));
        }
    }

    @Test
    public void containsAddedElement()
    {
        assertTrue(obj.add(1));

        assertTrue(obj.contains(1));
    }

    @Test
    public void addingAnElementTwiceDoesNothing()
    {
        assertTrue(obj.add(1));

        assertFalse(obj.add(1));
    }

    @Test
    public void containsAddedBoxedElements()
    {
        assertTrue(obj.add(1));
        assertTrue(obj.add(Integer.valueOf(2)));

        assertTrue(obj.contains(Integer.valueOf(1)));
        assertTrue(obj.contains(2));
    }

    @Test
    public void doesNotContainMissingValue()
    {
        assertFalse(obj.contains(obj.missingValue()));
    }

    @Test
    public void removingAnElementFromAnEmptyListDoesNothing()
    {
        assertFalse(obj.remove(0));
    }

    @Test
    public void removingAPresentElementRemovesIt()
    {
        assertTrue(obj.add(1));

        assertTrue(obj.remove(1));

        assertFalse(obj.contains(1));
    }

    @Test
    public void sizeIsInitiallyZero()
    {
        assertEquals(0, obj.size());
    }

    @Test
    public void sizeIncrementsWithNumberOfAddedElements()
    {
        addTwoElements(obj);

        assertEquals(2, obj.size());
    }

    @Test
    public void sizeContainsNumberOfNewElements()
    {
        obj.add(1);
        obj.add(1);

        assertEquals(1, obj.size());
    }

    @Test
    public void iteratorsListElements()
    {
        addTwoElements(obj);

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
        addTwoElements(obj);

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
        addTwoElements(obj);

        exhaustIterator();
    }

    @Test(expected = NoSuchElementException.class)
    public void iteratorsThrowNoSuchElementExceptionFromTheBeginningEveryTime()
    {
        addTwoElements(obj);

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
        assertFalse(obj.iterator().hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void iteratorThrowExceptionForEmptySet()
    {
        obj.iterator().next();
    }

    @Test
    public void clearRemovesAllElementsOfTheSet()
    {
        addTwoElements(obj);

        obj.clear();

        assertEquals(0, obj.size());
        assertFalse(obj.contains(1));
        assertFalse(obj.contains(1001));
    }

    @Test
    public void differenceReturnsNullIfBothSetsEqual()
    {
        addTwoElements(obj);

        final IntHashSet other = new IntHashSet(100, -1);
        addTwoElements(other);

        assertNull(obj.difference(other));
    }

    @Test
    public void differenceReturnsSetDifference()
    {
        addTwoElements(obj);

        final IntHashSet other = new IntHashSet(100, -1);
        other.add(1);

        final IntHashSet diff = obj.difference(other);
        assertThat(diff, containsInAnyOrder(1001));
    }

    @Test
    public void copiesOtherIntHashSet()
    {
        addTwoElements(obj);

        final IntHashSet other = new IntHashSet(100, -1);
        other.copy(obj);

        assertContainsElements(other);
    }

    @Test
    public void twoEmptySetsAreEqual()
    {
        final IntHashSet other = new IntHashSet(100, -1);
        assertEquals(obj, other);
    }

    @Test
    public void equalityRequiresTheSameMissingValue()
    {
        final IntHashSet other = new IntHashSet(100, 1);
        assertNotEquals(obj, other);
    }

    @Test
    public void setsWithTheSameValuesAreEqual()
    {
        final IntHashSet other = new IntHashSet(100, -1);

        addTwoElements(obj);
        addTwoElements(other);

        assertEquals(obj, other);
    }

    @Test
    public void setsWithTheDifferentSizesAreNotEqual()
    {
        final IntHashSet other = new IntHashSet(100, -1);

        addTwoElements(obj);

        other.add(1001);

        assertNotEquals(obj, other);
    }

    @Test
    public void setsWithTheDifferentValuesAreNotEqual()
    {
        final IntHashSet other = new IntHashSet(100, -1);

        addTwoElements(obj);

        other.add(2);
        other.add(1001);

        assertNotEquals(obj, other);
    }

    @Test
    public void twoEmptySetsHaveTheSameHashcode()
    {
        final IntHashSet other = new IntHashSet(100, -1);
        assertEquals(obj.hashCode(), other.hashCode());
    }

    @Test
    public void setsWithTheSameValuesHaveTheSameHashcode()
    {
        final IntHashSet other = new IntHashSet(100, -1);

        addTwoElements(obj);

        addTwoElements(other);

        assertEquals(obj.hashCode(), other.hashCode());
    }

    @Test
    public void reducesSizeWhenElementRemoved()
    {
        addTwoElements(obj);

        obj.remove(1001);

        assertEquals(1, obj.size());
    }

    @Test(expected = ArrayStoreException.class)
    public void toArrayThrowsArrayStoreExceptionForWrongType()
    {
        obj.toArray(new String[1]);
    }

    @Test(expected = NullPointerException.class)
    public void toArrayThrowsNullPointerExceptionForNullArgument()
    {
        obj.toArray(null);
    }

    @Test
    public void toArrayCopiesElementsIntoSufficientlySizedArray()
    {
        addTwoElements(obj);

        final Integer[] result = obj.toArray(new Integer[obj.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    public void toArrayCopiesElementsIntoNewArray()
    {
        addTwoElements(obj);

        final Integer[] result = obj.toArray(new Integer[obj.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    public void toArraySupportsEmptyCollection()
    {
        final Integer[] result = obj.toArray(new Integer[obj.size()]);

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
            assertTrue(obj.add(i));
        }

        for (int i = 0; i < 2 * INITIAL_CAPACITY; i++)
        {
            assertTrue(obj.contains(i));
        }
    }

     @Test
    public void containsEmptySet()
    {
        final IntHashSet other = new IntHashSet(100, -1);

        assertTrue(obj.containsAll(other));
        assertTrue(obj.containsAll((Collection<?>) other));
    }

    @Test
    public void containsSubset()
    {
        addTwoElements(obj);

        final IntHashSet subset = new IntHashSet(100, -1);

        subset.add(1);

        assertTrue(obj.containsAll(subset));
        assertTrue(obj.containsAll((Collection<?>) subset));
    }

    @Test
    public void doesNotContainDisjointSet()
    {
        addTwoElements(obj);

        final IntHashSet other = new IntHashSet(100, -1);

        other.add(1);
        other.add(1002);

        assertFalse(obj.containsAll(other));
        assertFalse(obj.containsAll((Collection<?>) other));
    }

    @Test
    public void doesNotContainSuperset()
    {
        addTwoElements(obj);

        final IntHashSet superset = new IntHashSet(100, -1);

        addTwoElements(superset);
        superset.add(15);

        assertFalse(obj.containsAll(superset));
        assertFalse(obj.containsAll((Collection<?>) superset));
    }

    @Test
    public void addingEmptySetDoesNothing()
    {
        addTwoElements(obj);

        assertFalse(obj.addAll(new IntHashSet(100, -1)));
        assertContainsElements(obj);
    }

    @Test
    public void addingSubsetDoesNothing()
    {
        addTwoElements(obj);

        final IntHashSet subset = new IntHashSet(100, -1);

        subset.add(1);

        assertFalse(obj.addAll(subset));
        assertContainsElements(obj);
    }

    @Test
    public void addingEqualSetDoesNothing()
    {
        addTwoElements(obj);

        final IntHashSet equal = new IntHashSet(100, -1);

        addTwoElements(equal);

        assertFalse(obj.addAll(equal));
        assertContainsElements(obj);
    }

    @Test
    public void containsValuesAddedFromDisjointSet()
    {
        addTwoElements(obj);

        final IntHashSet disjoint = new IntHashSet(100, -1);

        disjoint.add(2);
        disjoint.add(1002);

        assertTrue(obj.addAll(disjoint));
        assertTrue(obj.contains(1));
        assertTrue(obj.contains(1001));
        assertTrue(obj.containsAll(disjoint));
    }

    @Test
    public void containsValuesAddedFromIntersectingSet()
    {
        addTwoElements(obj);

        final IntHashSet intersecting = new IntHashSet(100, -1);

        intersecting.add(1);
        intersecting.add(1002);

        assertTrue(obj.addAll(intersecting));
        assertTrue(obj.contains(1));
        assertTrue(obj.contains(1001));
        assertTrue(obj.containsAll(intersecting));
    }

    @Test
    public void removingEmptySetDoesNothing()
    {
        addTwoElements(obj);

        assertFalse(obj.removeAll(new IntHashSet(100, -1)));
        assertContainsElements(obj);
    }

    @Test
    public void removingDisjointSetDoesNothing()
    {
        addTwoElements(obj);

        final IntHashSet disjoint = new IntHashSet(100, -1);

        disjoint.add(2);
        disjoint.add(1002);

        assertFalse(obj.removeAll(disjoint));
        assertContainsElements(obj);
    }

    @Test
    public void doesNotContainRemovedIntersectingSet()
    {
        addTwoElements(obj);

        final IntHashSet intersecting = new IntHashSet(100, -1);

        intersecting.add(1);
        intersecting.add(1002);

        assertTrue(obj.removeAll(intersecting));
        assertTrue(obj.contains(1001));
        assertFalse(obj.containsAll(intersecting));
    }

    @Test
    public void isEmptyAfterRemovingEqualSet()
    {
        addTwoElements(obj);

        final IntHashSet equal = new IntHashSet(100, -1);

        addTwoElements(equal);

        assertTrue(obj.removeAll(equal));
        assertTrue(obj.isEmpty());
    }

    private static void addTwoElements(final IntHashSet obj)
    {
        obj.add(1);
        obj.add(1001);
    }

    private void assertIteratorHasElements()
    {
        final Iterator<Integer> iter = obj.iterator();

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
        final Iterator<Integer> iter = obj.iterator();

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
        final IntIterator iterator = obj.iterator();
        iterator.next();
        iterator.next();
        iterator.next();
    }
}
