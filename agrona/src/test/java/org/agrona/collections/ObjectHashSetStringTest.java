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

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ObjectHashSetStringTest
{
    private static final int INITIAL_CAPACITY = 100;

    private final ObjectHashSet<String> testSet = new ObjectHashSet<>(INITIAL_CAPACITY);

    @Test
    public void containsAddedElement()
    {
        assertTrue(testSet.add("1"));

        assertTrue(testSet.contains("1"));
    }

    @Test
    public void addingAnElementTwiceDoesNothing()
    {
        assertTrue(testSet.add("1"));

        assertFalse(testSet.add("1"));
    }

    @Test
    public void removingAnElementFromAnEmptyListDoesNothing()
    {
        assertFalse(testSet.remove("0"));
    }

    @Test
    public void removingAPresentElementRemovesIt()
    {
        assertTrue(testSet.add("1"));

        assertTrue(testSet.remove("1"));

        assertFalse(testSet.contains("1"));
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
        testSet.add("1");
        testSet.add("1");

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
        assertFalse(testSet.contains("1"));
        assertFalse(testSet.contains("1001"));
    }

    @Test
    public void differenceReturnsNullIfBothSetsEqual()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> other = new ObjectHashSet<>(100);
        addTwoElements(other);

        assertNull(testSet.difference(other));
    }

    @Test
    public void differenceReturnsSetDifference()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> other = new ObjectHashSet<>(100);
        other.add("1");

        final ObjectHashSet<String> diff = testSet.difference(other);
        assertThat(diff, containsInAnyOrder("1001"));
    }

    @Test
    public void twoEmptySetsAreEqual()
    {
        final ObjectHashSet other = new ObjectHashSet(100);
        assertEquals(testSet, other);
    }

    @Test
    public void setsWithTheSameValuesAreEqual()
    {
        final ObjectHashSet<String> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);
        addTwoElements(other);

        assertEquals(testSet, other);
    }

    @Test
    public void setsWithTheDifferentSizesAreNotEqual()
    {
        final ObjectHashSet<String> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);

        other.add("1001");

        assertNotEquals(testSet, other);
    }

    @Test
    public void setsWithTheDifferentValuesAreNotEqual()
    {
        final ObjectHashSet<String> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);

        other.add("2");
        other.add("1001");

        assertNotEquals(testSet, other);
    }

    @Test
    public void twoEmptySetsHaveTheSameHashcode()
    {
        assertEquals(testSet.hashCode(), new ObjectHashSet<String>(100).hashCode());
    }

    @Test
    public void reducesSizeWhenElementRemoved()
    {
        addTwoElements(testSet);

        testSet.remove("1001");

        assertEquals(1, testSet.size());
    }

    @Test
    public void toArrayCopiesElementsIntoSufficientlySizedArray()
    {
        addTwoElements(testSet);

        final String[] result = testSet.toArray(new String[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    public void toArrayCopiesElementsIntoNewArray()
    {
        addTwoElements(testSet);

        final String[] result = testSet.toArray(new String[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    public void toArraySupportsEmptyCollection()
    {
        final String[] result = testSet.toArray(new String[testSet.size()]);

        assertArrayEquals(result, new String[]{});
    }

    // Test case from usage bug.
    @Test
    public void chainCompactionShouldNotCauseElementsToBeMovedBeforeTheirHash()
    {
        final ObjectHashSet<String> requiredFields = new ObjectHashSet<>(14);

        requiredFields.add("8");
        requiredFields.add("9");
        requiredFields.add("35");
        requiredFields.add("49");
        requiredFields.add("56");

        assertTrue("Failed to remove 8", requiredFields.remove("8"));
        assertTrue("Failed to remove 9", requiredFields.remove("9"));

        assertThat(requiredFields, containsInAnyOrder("35", "49", "56"));
    }

    @Test
    public void shouldResizeWhenItHitsCapacity()
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

    @Test
    public void containsSubset()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> subset = new ObjectHashSet<>(100);

        subset.add("1");

        assertTrue(testSet.containsAll(subset));
    }

    @Test
    public void doesNotContainDisjointSet()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> other = new ObjectHashSet<>(100);

        other.add("1");
        other.add("1002");

        assertFalse(testSet.containsAll(other));
    }

    @Test
    public void doesNotContainSuperset()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> superset = new ObjectHashSet<>(100);

        addTwoElements(superset);
        superset.add("15");

        assertFalse(testSet.containsAll(superset));
    }

    @Test
    public void addingEmptySetDoesNothing()
    {
        addTwoElements(testSet);

        assertFalse(testSet.addAll(new ObjectHashSet<>(100)));
        assertContainsElements(testSet);
    }

    @Test
    public void addingSubsetDoesNothing()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> subset = new ObjectHashSet<>(100);

        subset.add("1");

        assertFalse(testSet.addAll(subset));
        assertContainsElements(testSet);
    }

    @Test
    public void addingEqualSetDoesNothing()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> equal = new ObjectHashSet<>(100);

        addTwoElements(equal);

        assertFalse(testSet.addAll(equal));
        assertContainsElements(testSet);
    }

    @Test
    public void containsValuesAddedFromDisjointSet()
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

    @Test
    public void containsValuesAddedFromIntersectingSet()
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

    @Test
    public void removingEmptySetDoesNothing()
    {
        addTwoElements(testSet);

        assertFalse(testSet.removeAll(new ObjectHashSet<String>(100)));
        assertContainsElements(testSet);
    }

    @Test
    public void removingDisjointSetDoesNothing()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> disjoint = new ObjectHashSet<>(100);

        disjoint.add("2");
        disjoint.add("1002");

        assertFalse(testSet.removeAll(disjoint));
        assertContainsElements(testSet);
    }

    @Test
    public void doesNotContainRemovedIntersectingSet()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> intersecting = new ObjectHashSet<>(100);

        intersecting.add("1");
        intersecting.add("1002");

        assertTrue(testSet.removeAll(intersecting));
        assertTrue(testSet.contains("1001"));
        assertFalse(testSet.containsAll(intersecting));
    }

    @Test
    public void isEmptyAfterRemovingEqualSet()
    {
        addTwoElements(testSet);

        final ObjectHashSet<String> equal = new ObjectHashSet<>(100);

        addTwoElements(equal);

        assertTrue(testSet.removeAll(equal));
        assertTrue(testSet.isEmpty());
    }

    @Test
    public void removeElementsFromIterator()
    {
        addTwoElements(testSet);

        final ObjectIterator<String> intIterator = testSet.iterator();
        while (intIterator.hasNext())
        {
            if (intIterator.nextValue().equals("1"))
            {
                intIterator.remove();
            }
        }

        assertThat(testSet, contains("1001"));
        assertThat(testSet, hasSize(1));
    }

    private static void addTwoElements(final ObjectHashSet<String> obj)
    {
        obj.add("1");
        obj.add("1001");
    }

    private void assertIteratorHasElements()
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

    private void assertIteratorHasElementsWithoutHasNext()
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

    private void exhaustIterator()
    {
        final ObjectIterator iterator = testSet.iterator();
        iterator.next();
        iterator.next();
        iterator.next();
    }
}
