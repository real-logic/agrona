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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static org.agrona.collections.IntHashSet.MISSING_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntHashSetTest
{
    private static final int INITIAL_CAPACITY = 100;

    private final IntHashSet testSet = new IntHashSet(INITIAL_CAPACITY);

    @Test
    void initiallyContainsNoElements()
    {
        for (int i = 0; i < 10_000; i++)
        {
            assertFalse(testSet.contains(i));
        }
    }

    @Test
    void initiallyContainsNoBoxedElements()
    {
        for (int i = 0; i < 10_000; i++)
        {
            assertFalse(testSet.contains(Integer.valueOf(i)));
        }
    }

    @Test
    void containsAddedElement()
    {
        assertTrue(testSet.add(1));

        assertTrue(testSet.contains(1));
    }

    @Test
    void addingAnElementTwiceDoesNothing()
    {
        assertTrue(testSet.add(1));

        assertFalse(testSet.add(1));
    }

    @Test
    void containsAddedBoxedElements()
    {
        assertTrue(testSet.add(1));
        assertTrue(testSet.add(Integer.valueOf(2)));

        assertTrue(testSet.contains(Integer.valueOf(1)));
        assertTrue(testSet.contains(2));
    }

    @Test
    void removingAnElementFromAnEmptyListDoesNothing()
    {
        assertFalse(testSet.remove(0));
    }

    @Test
    void removingAPresentElementRemovesIt()
    {
        assertTrue(testSet.add(1));

        assertTrue(testSet.remove(1));

        assertFalse(testSet.contains(1));
    }

    @Test
    void sizeIsInitiallyZero()
    {
        assertEquals(0, testSet.size());
    }

    @Test
    void sizeIncrementsWithNumberOfAddedElements()
    {
        addTwoElements(testSet);

        assertEquals(2, testSet.size());
    }

    @Test
    @SuppressWarnings("OverwrittenKey")
    void sizeContainsNumberOfNewElements()
    {
        testSet.add(1);
        testSet.add(1);

        assertEquals(1, testSet.size());
    }

    @Test
    void iteratorsListElements()
    {
        addTwoElements(testSet);

        assertIteratorHasElements();
    }

    @Test
    void iteratorsStartFromTheBeginningEveryTime()
    {
        iteratorsListElements();

        assertIteratorHasElements();
    }

    @Test
    void iteratorsListElementsWithoutHasNext()
    {
        addTwoElements(testSet);

        assertIteratorHasElementsWithoutHasNext();
    }

    @Test
    void iteratorsStartFromTheBeginningEveryTimeWithoutHasNext()
    {
        iteratorsListElementsWithoutHasNext();

        assertIteratorHasElementsWithoutHasNext();
    }

    @Test
    void iteratorsThrowNoSuchElementException()
    {
        addTwoElements(testSet);

        assertThrows(NoSuchElementException.class, this::exhaustIterator);
    }

    @Test
    void iteratorsThrowNoSuchElementExceptionFromTheBeginningEveryTime()
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
    void iteratorHasNoElements()
    {
        assertFalse(testSet.iterator().hasNext());
    }

    @Test
    void iteratorThrowExceptionForEmptySet()
    {
        assertThrows(NoSuchElementException.class, () -> testSet.iterator().next());
    }

    @Test
    void clearRemovesAllElementsOfTheSet()
    {
        addTwoElements(testSet);

        testSet.clear();

        assertEquals(0, testSet.size());
        assertFalse(testSet.contains(1));
        assertFalse(testSet.contains(1001));
    }

    @Test
    void differenceReturnsNullIfBothSetsEqual()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100);
        addTwoElements(other);

        assertNull(testSet.difference(other));
    }

    @Test
    void differenceReturnsElementsOfTheOriginalSetThatNotContainedInTheOtherSet()
    {
        final IntHashSet other = new IntHashSet();
        other.add(10);
        other.add(1);
        other.add(5);
        other.add(42);
        final IntHashSet otherCopy = new IntHashSet();
        otherCopy.copy(other);
        testSet.add(1);
        testSet.add(3);
        testSet.add(5);
        testSet.add(21);

        final IntHashSet diff = testSet.difference(other);

        assertNotNull(diff);
        assertEquals(2, diff.size());
        assertTrue(diff.contains(3));
        assertTrue(diff.contains(21));

        assertEquals(otherCopy, other);
    }

    @Test
    void differenceReturnsASetWithOnlyAMissingValueIfItIsNotContainedInAnotherSet()
    {
        final IntHashSet one = new IntHashSet();
        one.add(1);
        one.add(5);
        final IntHashSet two = new IntHashSet();
        two.add(MISSING_VALUE);

        final IntHashSet diff = two.difference(one);

        assertNotNull(diff);
        assertEquals(1, diff.size());
        assertTrue(diff.contains(MISSING_VALUE));
        assertFalse(diff.contains(1));
        assertFalse(diff.contains(5));
    }

    @Test
    void differenceReturnsASetWithAMissingValueIfItIsNotContainedInAnotherSet()
    {
        final IntHashSet one = new IntHashSet();
        one.add(MISSING_VALUE);
        one.add(5);
        final IntHashSet two = new IntHashSet();
        two.add(2);

        final IntHashSet diff = one.difference(two);

        assertNotNull(diff);
        assertEquals(2, diff.size());
        assertTrue(diff.contains(MISSING_VALUE));
        assertTrue(diff.contains(5));
        assertFalse(diff.contains(2));
    }

    @Test
    void copiesOtherIntHashSet()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100);
        other.copy(testSet);

        assertContainsElements(other);
    }

    @Test
    void twoEmptySetsAreEqual()
    {
        final IntHashSet other = new IntHashSet(100);
        assertEquals(testSet, other);
    }

    @Test
    void setsWithTheSameValuesAreEqual()
    {
        final IntHashSet other = new IntHashSet(100);

        addTwoElements(testSet);
        addTwoElements(other);

        assertEquals(testSet, other);
    }

    @Test
    void setsWithTheDifferentSizesAreNotEqual()
    {
        final IntHashSet other = new IntHashSet(100);

        addTwoElements(testSet);

        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @Test
    void setsWithTheDifferentValuesAreNotEqual()
    {
        final IntHashSet other = new IntHashSet(100);

        addTwoElements(testSet);

        other.add(2);
        other.add(1001);

        assertNotEquals(testSet, other);
    }

    @Test
    void twoEmptySetsHaveTheSameHashcode()
    {
        assertEquals(testSet.hashCode(), new IntHashSet(100).hashCode());
    }

    @Test
    void setsWithTheSameValuesHaveTheSameHashcode()
    {
        final IntHashSet other = new IntHashSet(100);

        addTwoElements(testSet);

        addTwoElements(other);

        assertEquals(testSet.hashCode(), other.hashCode());
    }

    @Test
    void reducesSizeWhenElementRemoved()
    {
        addTwoElements(testSet);

        testSet.remove(1001);

        assertEquals(1, testSet.size());
    }

    @Test
    @SuppressWarnings("SuspiciousToArrayCall")
    void toArrayThrowsArrayStoreExceptionForWrongType()
    {
        assertThrows(ArrayStoreException.class, () -> testSet.toArray(new String[1]));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void toArrayThrowsNullPointerExceptionForNullArgument()
    {
        final Integer[] into = null;
        assertThrows(NullPointerException.class, () -> testSet.toArray(into));
    }

    @Test
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    void toArrayCopiesElementsIntoSufficientlySizedArray()
    {
        addTwoElements(testSet);

        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    void toArrayCopiesElementsIntoNewArray()
    {
        addTwoElements(testSet);

        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertArrayContainingElements(result);
    }

    @Test
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    void toArraySupportsEmptyCollection()
    {
        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertArrayEquals(result, new Integer[]{});
    }

    // Test case from usage bug.
    @Test
    void chainCompactionShouldNotCauseElementsToBeMovedBeforeTheirHash()
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
    void shouldResizeWhenItHitsCapacity()
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
    void containsEmptySet()
    {
        final IntHashSet other = new IntHashSet(100);

        assertTrue(testSet.containsAll(other));
        assertTrue(testSet.containsAll((Collection<?>)other));
    }

    @Test
    void containsSubset()
    {
        addTwoElements(testSet);

        final IntHashSet subset = new IntHashSet(100);

        subset.add(1);

        assertTrue(testSet.containsAll(subset));
        assertTrue(testSet.containsAll((Collection<?>)subset));
    }

    @Test
    void doesNotContainDisjointSet()
    {
        addTwoElements(testSet);

        final IntHashSet other = new IntHashSet(100);

        other.add(1);
        other.add(1002);

        assertFalse(testSet.containsAll(other));
        assertFalse(testSet.containsAll((Collection<?>)other));
    }

    @Test
    void doesNotContainSuperset()
    {
        addTwoElements(testSet);

        final IntHashSet superset = new IntHashSet(100);

        addTwoElements(superset);
        superset.add(15);

        assertFalse(testSet.containsAll(superset));
        assertFalse(testSet.containsAll((Collection<?>)superset));
    }

    @Test
    void addingEmptySetDoesNothing()
    {
        addTwoElements(testSet);

        assertFalse(testSet.addAll(new IntHashSet(100)));
        assertFalse(testSet.addAll(new HashSet<>()));
        assertContainsElements(testSet);
    }

    @Test
    void addingSubsetDoesNothing()
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
    void addingEqualSetDoesNothing()
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
    void containsValuesAddedFromDisjointSetPrimitive()
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
    void containsValuesAddedFromDisjointSet()
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
    void containsValuesAddedFromIntersectingSetPrimitive()
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
    void containsValuesAddedFromIntersectingSet()
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
    void removingEmptySetDoesNothing()
    {
        addTwoElements(testSet);

        assertFalse(testSet.removeAll(new IntHashSet(100)));
        assertFalse(testSet.removeAll(new HashSet<Integer>()));
        assertContainsElements(testSet);
    }

    @Test
    void removingDisjointSetDoesNothing()
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
    void doesNotContainRemovedIntersectingSetPrimitive()
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
    void doesNotContainRemovedIntersectingSet()
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
    void isEmptyAfterRemovingEqualSetPrimitive()
    {
        addTwoElements(testSet);

        final IntHashSet equal = new IntHashSet(100);

        addTwoElements(equal);

        assertTrue(testSet.removeAll(equal));
        assertTrue(testSet.isEmpty());
    }

    @Test
    void isEmptyAfterRemovingEqualSet()
    {
        addTwoElements(testSet);

        final HashSet<Integer> equal = new HashSet<>();

        addTwoElements(equal);

        assertTrue(testSet.removeAll(equal));
        assertTrue(testSet.isEmpty());
    }

    @Test
    void removeElementsFromIterator()
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
    void shouldNotContainMissingValueInitially()
    {
        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    void shouldAllowMissingValue()
    {
        assertTrue(testSet.add(MISSING_VALUE));

        assertTrue(testSet.contains(MISSING_VALUE));

        assertFalse(testSet.add(MISSING_VALUE));
    }

    @Test
    void shouldAllowRemovalOfMissingValue()
    {
        assertTrue(testSet.add(MISSING_VALUE));

        assertTrue(testSet.remove(MISSING_VALUE));

        assertFalse(testSet.contains(MISSING_VALUE));

        assertFalse(testSet.remove(MISSING_VALUE));
    }

    @Test
    void sizeAccountsForMissingValue()
    {
        testSet.add(1);
        testSet.add(MISSING_VALUE);

        assertEquals(2, testSet.size());
    }

    @Test
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    void toArrayCopiesElementsIntoNewArrayIncludingMissingValue()
    {
        addTwoElements(testSet);

        testSet.add(MISSING_VALUE);

        final Integer[] result = testSet.toArray(new Integer[testSet.size()]);

        assertThat(result, arrayContainingInAnyOrder(1, 1001, MISSING_VALUE));
    }

    @Test
    void toObjectArrayCopiesElementsIntoNewArrayIncludingMissingValue()
    {
        addTwoElements(testSet);

        testSet.add(MISSING_VALUE);

        final Object[] result = testSet.toArray();

        assertThat(result, arrayContainingInAnyOrder(1, 1001, MISSING_VALUE));
    }

    @Test
    void equalsAccountsForMissingValue()
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
    void consecutiveValuesShouldBeCorrectlyStored()
    {
        for (int i = 0; i < 10_000; i++)
        {
            testSet.add(i);
        }

        assertThat(testSet, hasSize(10_000));
    }

    @Test
    void hashCodeAccountsForMissingValue()
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
    void iteratorAccountsForMissingValue()
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
    void iteratorCanRemoveMissingValue()
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
    void shouldGenerateStringRepresentation()
    {
        final IntHashSet set = new IntHashSet(16);
        final int[] testEntries = { MISSING_VALUE, 3, 0, 1, -1, 19, 7, 11, 12, 7, -1, 3, 3, -1 };

        for (final int testEntry : testEntries)
        {
            set.add(testEntry);
        }

        final String mapAsAString = "{0, 12, 3, 7, 11, 1, 19, -1}";
        assertThat(set.toString(), equalTo(mapAsAString));
    }

    @Test
    void shouldRemoveMissingValueWhenCleared()
    {
        assertTrue(testSet.add(MISSING_VALUE));

        testSet.clear();

        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    void shouldHaveCompatibleEqualsAndHashcode()
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
        final IntHashSet set = new IntHashSet(1);
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
        final IntHashSet set = new IntHashSet(5);
        final IntConsumer consumer = mock(IntConsumer.class);

        set.add(MISSING_VALUE);
        set.add(44);

        set.forEachInt(consumer);

        final InOrder inOrder = inOrder(consumer);
        inOrder.verify(consumer).accept(44);
        inOrder.verify(consumer).accept(MISSING_VALUE);
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
        final IntHashSet coll = new IntHashSet(2);
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
        final IntHashSet coll = new IntHashSet(5);
        coll.addAll(Arrays.asList(42, 42, 42, 0, 500));
        testSet.add(MISSING_VALUE);
        testSet.add(42);
        testSet.add(500);

        assertTrue(testSet.retainAll(coll));

        assertEquals(2, testSet.size());
        assertTrue(testSet.contains(42));
        assertTrue(testSet.contains(500));
        assertFalse(testSet.contains(MISSING_VALUE));
    }

    @Test
    void removeAllIsANoOpIfTwoCollectionsHaveNoValuesInCommon()
    {
        final IntHashSet other = new IntHashSet(10);
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
        final IntHashSet other = new IntHashSet(10);
        other.add(4);
        other.add(5);
        other.add(MISSING_VALUE);
        testSet.add(1);
        testSet.add(2);
        testSet.add(MISSING_VALUE);

        assertTrue(testSet.removeAll(other));

        assertEquals(2, testSet.size());
        assertTrue(testSet.contains(1));
        assertTrue(testSet.contains(2));
        assertFalse(testSet.contains(MISSING_VALUE));
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
        final IntHashSet other = new IntHashSet(5);
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
        final IntHashSet other = new IntHashSet(5);
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
        final IntHashSet other = new IntHashSet(2);
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
        final IntHashSet other = new IntHashSet(2);
        other.add(9);
        testSet.add(9);

        assertTrue(testSet.containsAll(other));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 10, 100, 1000 })
    public void retainAllWithAnotherIntHashSet(final int capacity)
    {
        final IntHashSet set1 = new IntHashSet(capacity);
        set1.add(9);
        for (int i = 0; i < capacity; i++)
        {
            set1.add(ThreadLocalRandom.current().nextInt(10, Integer.MAX_VALUE));
        }
        set1.add(MISSING_VALUE);

        final IntHashSet set2 = new IntHashSet();
        set2.add(8);
        set2.add(9);

        assertTrue(set1.retainAll(set2));

        assertEquals(1, set1.size());
        assertTrue(set1.contains(9));
        assertFalse(set1.contains(MISSING_VALUE));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 10, 100, 1000 })
    public void retainAllWithCollection(final int capacity)
    {
        final IntHashSet set = new IntHashSet(capacity);
        set.add(9);
        for (int i = 0; i < capacity; i++)
        {
            set.add(ThreadLocalRandom.current().nextInt(10, Integer.MAX_VALUE));
        }
        set.add(MISSING_VALUE);

        final List<Integer> list = Arrays.asList(8, 9);

        assertTrue(set.retainAll(list));

        assertEquals(1, set.size());
        assertTrue(set.contains(9));
        assertFalse(set.contains(MISSING_VALUE));
    }

    @Test
    public void removeIfIntUsingDefaults()
    {
        final IntHashSet set = new IntHashSet();
        set.add(4);
        set.add(5);

        assertTrue(set.removeIfInt(i -> true));

        assertEquals(0, set.size());
    }

    @Test
    public void removeIfUsingDefaults()
    {
        final IntHashSet set = new IntHashSet();
        set.add(4);
        set.add(5);

        assertTrue(set.removeIf(i -> true));

        assertEquals(0, set.size());
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
