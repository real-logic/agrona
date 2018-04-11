package org.agrona.collections;

import org.junit.Test;

import java.util.Iterator;
import java.util.Map.Entry;

import static org.agrona.collections.Int2IntHashMap.MIN_CAPACITY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class Int2IntHashMapNotAvoidingAllocationTest extends Int2IntHashMapTest
{
    public Int2IntHashMapNotAvoidingAllocationTest()
    {
        super(new Int2IntHashMap(MIN_CAPACITY, Hashing.DEFAULT_LOAD_FACTOR, MISSING_VALUE, false));
    }

    @Test
    public void valuesIteratorIsNotCached()
    {
        assertFalse(map.values().iterator() == map.values().iterator());
    }

    @Test
    public void keysIteratorIsNotCached()
    {
        assertFalse(map.keySet().iterator() == map.keySet().iterator());
    }

    @Test
    public void entryIteratorIsNotCached()
    {
        assertFalse(map.entrySet().iterator() == map.entrySet().iterator());
    }

    @Test
    public void entriesAreAllocatedByEntriesIterator()
    {
        map.put(1, 1);
        map.put(2, 2);
        final Iterator<Entry<Integer, Integer>> entryIterator = map.entrySet().iterator();
        final Entry<Integer, Integer> entry1 = entryIterator.next();
        final Entry<Integer, Integer> entry2 = entryIterator.next();
        assertNotEquals(entry1, entry2);
    }
}
