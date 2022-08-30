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
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IntHashSetTest
{
    private static final int INITIAL_CAPACITY = 100;
    private static final int MISSING_VALUE = -1;

    private final IntHashSet testSet = new IntHashSet(INITIAL_CAPACITY, MISSING_VALUE);

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
        final IntHashSet set = new IntHashSet(16, -9990999);
        final int[] testEntries = { -9990999, 3, 1, -1, 19, 7, 11, 12, 7, -1, 3, 3, -1 };

        for (final int testEntry : testEntries)
        {
            set.add(testEntry);
        }

        final String mapAsAString = "{-1, 12, 3, 7, 11, 1, 19, -9990999}";
        assertThat(set.toString(), equalTo(mapAsAString));
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

    @Test
    void forEachIsANoOpIfTheSetIsEmpty()
    {
        @SuppressWarnings("unchecked") final Consumer<Integer> consumer = mock(Consumer.class);

        testSet.forEach(consumer);

        verifyNoInteractions(consumer);
    }

    @Test
    void forEachShouldInvokeConsumerWithEveryValueAddedToTheSet()
    {
        @SuppressWarnings("unchecked") final Consumer<Integer> consumer = mock(Consumer.class);

        testSet.add(15);
        testSet.add(-2);
        testSet.add(0);
        testSet.add(100);
        testSet.add(-8);

        testSet.forEach(consumer);

        verify(consumer).accept(100);
        verify(consumer).accept(-8);
        verify(consumer).accept(0);
        verify(consumer).accept(15);
        verify(consumer).accept(-2);
        verifyNoMoreInteractions(consumer);
    }

    @Test
    void forEachShouldInvokeConsumerWithTheMissingValueAtTheIfOneWasAddedToTheSet()
    {
        @SuppressWarnings("unchecked") final Consumer<Integer> consumer = mock(Consumer.class);

        testSet.add(MISSING_VALUE);
        testSet.add(15);
        testSet.add(MISSING_VALUE);

        testSet.forEach(consumer);

        final InOrder inOrder = inOrder(consumer);
        inOrder.verify(consumer).accept(15);
        inOrder.verify(consumer).accept(MISSING_VALUE);
        verifyNoMoreInteractions(consumer);
    }

    @Test
    void forEachIntIsANoOpIfTheSetIsEmpty()
    {
        final IntConsumer consumer = mock(IntConsumer.class);

        testSet.forEachInt(consumer);

        verifyNoInteractions(consumer);
    }

    @Test
    void forEachIntShouldInvokeConsumerWithEveryValueAddedToTheSet()
    {
        final IntHashSet set = new IntHashSet(1, -1000);
        final IntConsumer consumer = mock(IntConsumer.class);

        set.add(-999);
        set.add(-2);
        set.add(0);
        set.add(1000);
        set.add(-8);

        set.forEachInt(consumer);

        verify(consumer).accept(-999);
        verify(consumer).accept(-8);
        verify(consumer).accept(0);
        verify(consumer).accept(-2);
        verify(consumer).accept(1000);
        verifyNoMoreInteractions(consumer);
    }

    @Test
    void forEachIntShouldInvokeConsumerWithTheMissingValueAtTheIfOneWasAddedToTheSet()
    {
        final int missingValue = 42;
        final IntHashSet set = new IntHashSet(5, missingValue);
        final IntConsumer consumer = mock(IntConsumer.class);

        set.add(missingValue);
        set.add(-1);
        set.add(missingValue);

        set.forEachInt(consumer);

        final InOrder inOrder = inOrder(consumer);
        inOrder.verify(consumer).accept(-1);
        inOrder.verify(consumer).accept(missingValue);
        verifyNoMoreInteractions(consumer);
    }

    @Test
    void retainAllCollectionIsANoOpIfCollectionHasAllOfTheElementsFromTheSet()
    {
        final List<Integer> coll = Arrays.asList(0, 5, -3, 42, 21, MISSING_VALUE);
        testSet.add(MISSING_VALUE);
        testSet.add(42);
        testSet.add(-3);

        assertFalse(testSet.retainAll(coll));

        assertEquals(3, testSet.size());
        assertTrue(testSet.contains(MISSING_VALUE));
        assertTrue(testSet.contains(42));
        assertTrue(testSet.contains(-3));
    }

    @Test
    void retainAllCollectionRemovesNonMissingValuesNotFoundInAGivenCollection()
    {
        final List<Integer> coll = Arrays.asList(100, 5, 21, 8);
        testSet.add(0);
        testSet.add(8);
        testSet.add(100);
        testSet.add(-999);

        assertTrue(testSet.retainAll(coll));

        assertEquals(2, testSet.size());
        assertTrue(testSet.contains(8));
        assertTrue(testSet.contains(100));
        assertFalse(testSet.contains(-999));
        assertFalse(testSet.contains(0));
    }

    @Test
    void retainAllCollectionRemovesMissingValueWhichWasAddedToTheSet()
    {
        final List<Integer> coll = Arrays.asList(42, 42, 42, 0, 500);
        testSet.add(MISSING_VALUE);
        testSet.add(42);

        assertTrue(testSet.retainAll(coll));

        assertEquals(1, testSet.size());
        assertTrue(testSet.contains(42));
        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    void retainAllIsANoOpIfCollectionHasAllOfTheElementsFromTheSet()
    {
        final IntHashSet coll = new IntHashSet(2, 42);
        coll.addAll(Arrays.asList(0, 5, -3, 42, 21, MISSING_VALUE));
        testSet.add(MISSING_VALUE);
        testSet.add(42);
        testSet.add(-3);

        assertFalse(testSet.retainAll(coll));

        assertEquals(3, testSet.size());
        assertTrue(testSet.contains(MISSING_VALUE));
        assertTrue(testSet.contains(42));
        assertTrue(testSet.contains(-3));
    }

    @Test
    void retainAllRemovesNonMissingValuesNotFoundInAGivenCollection()
    {
        final IntHashSet coll = new IntHashSet();
        coll.addAll(Arrays.asList(100, 5, 21, 8));
        testSet.add(0);
        testSet.add(8);
        testSet.add(100);
        testSet.add(-999);

        assertTrue(testSet.retainAll(coll));

        assertEquals(2, testSet.size());
        assertTrue(testSet.contains(8));
        assertTrue(testSet.contains(100));
        assertFalse(testSet.contains(-999));
        assertFalse(testSet.contains(0));
    }

    @Test
    void retainAllRemovesMissingValueWhichWasAddedToTheSet()
    {
        final IntHashSet coll = new IntHashSet(5, 0);
        coll.addAll(Arrays.asList(42, 42, 42, 0, 500));
        testSet.add(MISSING_VALUE);
        testSet.add(42);

        assertTrue(testSet.retainAll(coll));

        assertEquals(1, testSet.size());
        assertTrue(testSet.contains(42));
        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    void removeAllIsANoOpIfTwoCollectionsHaveNoValuesInCommon()
    {
        final IntHashSet other = new IntHashSet(10, 100);
        other.add(4);
        other.add(5);
        other.add(100);
        testSet.add(1);
        testSet.add(2);
        testSet.add(MISSING_VALUE);

        assertFalse(testSet.removeAll(other));

        assertEquals(3, testSet.size());
        assertTrue(testSet.contains(MISSING_VALUE));
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(2));
    }

    @Test
    void removeAllRemovesMissingValueOfTheOtherSet()
    {
        final IntHashSet other = new IntHashSet(10, 100);
        other.add(4);
        other.add(5);
        other.add(100);
        testSet.add(1);
        testSet.add(2);
        testSet.add(100);

        assertTrue(testSet.removeAll(other));

        assertEquals(2, testSet.size());
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(2));
    }

    @Test
    void removeIfIntIsANoOpIfNoValuesMatchFilter()
    {
        final IntPredicate filter = (v) -> v < 0;
        testSet.add(1);
        testSet.add(2);
        testSet.add(0);

        assertFalse(testSet.removeIfInt(filter));

        assertEquals(3, testSet.size());
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(2));
        assertTrue(testSet.contains(0));
    }

    @Test
    void removeIfIntDeletesAllMatchingValues()
    {
        final IntPredicate filter = (v) -> v < 0;
        testSet.add(1);
        testSet.add(-2);
        testSet.add(0);
        testSet.add(MISSING_VALUE);

        assertTrue(testSet.removeIfInt(filter));

        assertEquals(2, testSet.size());
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(0));
        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    void removeIfIsANoOpIfNoValuesMatchFilter()
    {
        final Predicate<Integer> filter = (v) -> v < 0;
        testSet.add(1);
        testSet.add(2);
        testSet.add(0);

        assertFalse(testSet.removeIf(filter));

        assertEquals(3, testSet.size());
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(2));
        assertTrue(testSet.contains(0));
    }

    @Test
    void removeIfDeletesAllMatchingValues()
    {
        final Predicate<Integer> filter = (v) -> v < 0;
        testSet.add(1);
        testSet.add(-2);
        testSet.add(0);
        testSet.add(MISSING_VALUE);

        assertTrue(testSet.removeIf(filter));

        assertEquals(2, testSet.size());
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(0));
        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    void addAllShouldAddOnlyNonMissingValuesIfTheSourceListDoesNotContainExplicitMissingValue()
    {
        final IntHashSet other = new IntHashSet(5, 888);
        other.add(1);
        other.add(2);
        other.add(3);
        other.add(4);
        other.add(5);
        testSet.add(1);
        testSet.add(5);
        testSet.add(MISSING_VALUE);

        assertTrue(testSet.addAll(other));

        assertEquals(6, testSet.size());
        for (int i = 1; i <= 5; i++)
        {
            assertTrue(testSet.contains(i));
        }
        assertTrue(testSet.contains(MISSING_VALUE));
    }

    @Test
    void addAllShouldAddMissingVaueFromAnotherSet()
    {
        final IntHashSet other = new IntHashSet(5, 3);
        other.add(1);
        other.add(2);
        other.add(3);
        testSet.add(0);

        assertTrue(testSet.addAll(other));

        assertEquals(4, testSet.size());
        for (int i = 0; i <= 3; i++)
        {
            assertTrue(testSet.contains(i));
        }
        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    void containsAllReturnsTrueIfTheSetContainsAllNonMissingValues()
    {
        final IntHashSet other = new IntHashSet(2, 9);
        other.add(0);
        other.add(2);
        other.add(4);
        other.add(6);
        testSet.add(MISSING_VALUE);
        for (int i = 0; i < 9; i++)
        {
            testSet.add(i);
        }

        assertTrue(testSet.containsAll(other));
    }

    @Test
    void containsAllReturnsTrueIfTheSetContainsTheMissingValueOfAnotherSet()
    {
        final IntHashSet other = new IntHashSet(2, 9);
        other.add(9);
        testSet.add(9);

        assertTrue(testSet.containsAll(other));
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
