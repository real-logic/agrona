package org.agrona.collections;

import org.junit.Test;

import static org.agrona.collections.Int2IntHashMap.MIN_CAPACITY;
import static org.junit.Assert.assertFalse;

public class Int2IntHashMapNonCacheIteratorTest extends Int2IntHashMapTest
{
    public Int2IntHashMapNonCacheIteratorTest()
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
}
