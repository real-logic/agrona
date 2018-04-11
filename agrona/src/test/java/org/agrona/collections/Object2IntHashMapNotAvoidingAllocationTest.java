package org.agrona.collections;

import org.junit.Test;

import java.util.Iterator;
import java.util.Map.Entry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class Object2IntHashMapNotAvoidingAllocationTest extends Object2IntHashMapTest
{
    <T> Object2IntHashMap<T> newMap(final float loadFactor, final int initialCapacity)
    {
        return new Object2IntHashMap<T>(initialCapacity, loadFactor, MISSING_VALUE, false);
    }

    @Test
    public void valuesIteratorIsNotCached()
    {
        assertFalse(objectToIntMap.values().iterator() == objectToIntMap.values().iterator());
    }

    @Test
    public void keysIteratorIsNotCached()
    {
        assertFalse(objectToIntMap.keySet().iterator() == objectToIntMap.keySet().iterator());
    }

    @Test
    public void entryIteratorIsNotCached()
    {
        assertFalse(objectToIntMap.entrySet().iterator() == objectToIntMap.entrySet().iterator());
    }

    @Test
    public void entriesAreAllocatedByEntriesIterator()
    {
        objectToIntMap.put("1", 1);
        objectToIntMap.put("2", 2);
        final Iterator<Entry<String, Integer>> entryIterator = objectToIntMap.entrySet().iterator();
        final Entry<String, Integer> entry1 = entryIterator.next();
        final Entry<String, Integer> entry2 = entryIterator.next();
        assertNotEquals(entry1, entry2);
    }
}
