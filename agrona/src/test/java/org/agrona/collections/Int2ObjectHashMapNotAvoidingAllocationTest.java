package org.agrona.collections;

import org.junit.Test;

import java.util.Iterator;
import java.util.Map.Entry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class Int2ObjectHashMapNotAvoidingAllocationTest extends Int2ObjectHashMapTest
{
    @Override
    Int2ObjectHashMap<String> newMap(final float loadFactor, final int initialCapacity)
    {
        return new Int2ObjectHashMap<>(initialCapacity, loadFactor, false);
    }

    @Test
    public void valuesIteratorIsNotCached()
    {
        assertFalse(intToObjectMap.values().iterator() == intToObjectMap.values().iterator());
    }

    @Test
    public void keysIteratorIsNotCached()
    {
        assertFalse(intToObjectMap.keySet().iterator() == intToObjectMap.keySet().iterator());
    }

    @Test
    public void entryIteratorIsNotCached()
    {
        assertFalse(intToObjectMap.entrySet().iterator() == intToObjectMap.entrySet().iterator());
    }

    @Test
    public void entriesAreAllocatedByEntriesIterator()
    {
        intToObjectMap.put(1, "1");
        intToObjectMap.put(2, "2");
        final Iterator<Entry<Integer, String>> entryIterator = intToObjectMap.entrySet().iterator();
        final Entry<Integer, String> entry1 = entryIterator.next();
        final Entry<Integer, String> entry2 = entryIterator.next();
        assertNotEquals(entry1, entry2);
    }
}
