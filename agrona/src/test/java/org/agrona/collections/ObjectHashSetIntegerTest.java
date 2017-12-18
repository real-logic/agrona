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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ObjectHashSetIntegerTest
{
    @Parameterized.Parameters
    public static Iterable<ObjectHashSet<Integer>> data()
    {
        return Arrays.asList(
            new ObjectHashSet<Integer>(INITIAL_CAPACITY),
            new ObjectHashSet<Integer>(INITIAL_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, false));
    }

    private static final int INITIAL_CAPACITY = 100;

    private final ObjectHashSet<Integer> testSet;

    public ObjectHashSetIntegerTest(final ObjectHashSet<Integer> testSet)
    {
        this.testSet = testSet;
    }

    @Before
    public void clear()
    {
        testSet.clear();
    }

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
            //noinspection UnnecessaryBoxing
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
        //noinspection UnnecessaryBoxing
        assertTrue(testSet.add(Integer.valueOf(2)));

        //noinspection UnnecessaryBoxing
        assertTrue(testSet.contains(Integer.valueOf(1)));
        assertTrue(testSet.contains(2));
    }

    @Test
    public void doesNotContainMissingValue()
    {
        assertFalse(testSet.contains(2048));
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

        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);
        addTwoElements(other);

        assertNull(testSet.difference(other));
    }

    @Test
    public void differenceReturnsSetDifference()
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);
        other.add(1);

        final ObjectHashSet<Integer> diff = testSet.difference(other);
        assertThat(diff, containsInAnyOrder(1001));
    }

    @Test
    public void copiesOtherIntHashSet()
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);
        other.copy(testSet);

        assertContainsElements(other);
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
        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);
        addTwoElements(other);

        assertEquals(testSet, other);
    }

    @Test
    public void setsWithTheDifferentSizesAreNotEqual()
    {
        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);

        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @Test
    public void setsWithTheDifferentValuesAreNotEqual()
    {
        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        addTwoElements(testSet);

        other.add(2);
        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @Test
    public void twoEmptySetsHaveTheSameHashcode()
    {
        assertEquals(testSet.hashCode(), new ObjectHashSet<Integer>(100).hashCode());
    }

    @Test
    public void setsWithTheSameValuesHaveTheSameHashcode()
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> secondSet = new ObjectHashSet<>(100);
        addTwoElements(secondSet);

        assertEquals(testSet.hashCode(), secondSet.hashCode());
    }

    @Test
    public void reducesSizeWhenElementRemoved()
    {
        addTwoElements(testSet);

        testSet.remove(1001);

        assertEquals(1, testSet.size());
    }

    @Test(expected = NullPointerException.class)
    public void toArrayThrowsNullPointerExceptionForNullArgument()
    {
        //noinspection ConstantConditions
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
        final ObjectHashSet<Integer> requiredFields = new ObjectHashSet<>(14);

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
        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        assertTrue(testSet.containsAll(other));
    }

    @Test
    public void containsSubset()
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> subset = new ObjectHashSet<>(100);

        subset.add(1);

        assertTrue(testSet.containsAll(subset));
    }

    @Test
    public void doesNotContainDisjointSet()
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> other = new ObjectHashSet<>(100);

        other.add(1);
        other.add(1002);

        assertFalse(testSet.containsAll(other));
    }

    @Test
    public void doesNotContainSuperset()
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> superset = new ObjectHashSet<>(100);

        addTwoElements(superset);
        superset.add(15);

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

        final ObjectHashSet<Integer> subset = new ObjectHashSet<>(100);

        subset.add(1);

        assertFalse(testSet.addAll(subset));
        assertContainsElements(testSet);
    }

    @Test
    public void addingEqualSetDoesNothing()
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> equal = new ObjectHashSet<>(100);

        addTwoElements(equal);

        assertFalse(testSet.addAll(equal));
        assertContainsElements(testSet);
    }

    @Test
    public void containsValuesAddedFromDisjointSet()
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

    @Test
    public void containsValuesAddedFromIntersectingSet()
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

    @Test
    public void removingEmptySetDoesNothing()
    {
        addTwoElements(testSet);

        assertFalse(testSet.removeAll(new ObjectHashSet<>(100)));
        assertContainsElements(testSet);
    }

    @Test
    public void removingDisjointSetDoesNothing()
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> disjoint = new ObjectHashSet<>(100);

        disjoint.add(2);
        disjoint.add(1002);

        assertFalse(testSet.removeAll(disjoint));
        assertContainsElements(testSet);
    }

    @Test
    public void doesNotContainRemovedIntersectingSet()
    {
        addTwoElements(testSet);

        final ObjectHashSet<Integer> intersecting = new ObjectHashSet<>(100);

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

        final ObjectHashSet<Integer> equal = new ObjectHashSet<>(100);

        addTwoElements(equal);

        assertTrue(testSet.removeAll(equal));
        assertTrue(testSet.isEmpty());
    }

    @Test
    public void removeElementsFromIterator()
    {
        addTwoElements(testSet);

        final ObjectIterator intIterator = testSet.iterator();
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


    @Test
    public void shouldGenerateStringRepresentation()
    {
        final int[] testEntries = { 3, 1, -1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            testSet.add(testEntry);
        }

        final String mapAsAString = "{1, 3, 7, 11, 12, 19, -1}";
        assertThat(testSet.toString(), equalTo(mapAsAString));
    }

    @Test
    public void shouldIterateOverExpandedSet()
    {
        final HashSet<Integer> refSet = new HashSet<>(5);
        final ObjectHashSet<Integer> testSet = new ObjectHashSet<>(5);

        for (int i = 0; i < 20; i++)
        {
            refSet.add(i);
            testSet.add(i);
        }

        final ObjectIterator<Integer> iter = testSet.iterator();
        for (int i = 0; i < 20; i++)
        {
            assertTrue(iter.hasNext());
            assertTrue(refSet.contains(iter.next()));
        }

        assertFalse(iter.hasNext());
    }

    @Test
    public void shouldHaveCompatibleEqualsAndHashcode()
    {
        final HashSet compatibleSet = new HashSet();
        final long seed = System.nanoTime();
        final Random r = new Random(seed);
        for (int i = 0; i < 1024; i++)
        {
            final int value = r.nextInt();
            compatibleSet.add(value);
            testSet.add(value);
        }

        assertEquals("Fail with seed:" + seed, testSet, compatibleSet);
        assertEquals("Fail with seed:" + seed, compatibleSet, testSet);
        assertEquals("Fail with seed:" + seed, compatibleSet.hashCode(), testSet.hashCode());
    }

    private static void addTwoElements(final ObjectHashSet<Integer> obj)
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
        final ObjectIterator iterator = testSet.iterator();
        iterator.next();
        iterator.next();
        iterator.next();
    }
}
