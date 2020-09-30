/*
 * Copyright 2014-2020 Real Logic Limited.
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

import org.agrona.generation.DoNotSub;

/**
 * Variation of {@link Int2ObjectHashMap} that allows {@code null} values.
 *
 * @param <V> type of values stored in the {@link java.util.Map}
 */
public class Int2NullableObjectHashMap<V> extends Int2ObjectHashMap<V>
{
    /**
     * Constructs map with default settings.
     */
    public Int2NullableObjectHashMap()
    {
    }

    /**
     * Constructs map with given initial capacity and load factory and enables caching of iterators.
     *
     * @param initialCapacity for the backing array.
     * @param loadFactor      limit for resizing on puts.
     */
    public Int2NullableObjectHashMap(
        @DoNotSub final int initialCapacity, final float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    /**
     * Construct a new map allowing a configuration for initial capacity and load factor.
     *
     * @param initialCapacity       for the backing array
     * @param loadFactor            limit for resizing on puts
     * @param shouldAvoidAllocation should allocation be avoided by caching iterators and map entries.
     */
    public Int2NullableObjectHashMap(
        @DoNotSub final int initialCapacity, final float loadFactor, final boolean shouldAvoidAllocation)
    {
        super(initialCapacity, loadFactor, shouldAvoidAllocation);
    }

    /**
     * Copy construct a new map from an existing one.
     *
     * @param mapToCopy for construction.
     */
    public Int2NullableObjectHashMap(final Int2ObjectHashMap<V> mapToCopy)
    {
        super(mapToCopy);
    }

    protected Object mapNullValue(final Object value)
    {
        return value == null ? NullReference.INSTANCE : value;
    }

    @SuppressWarnings("unchecked")
    protected V unmapNullValue(final Object value)
    {
        return value == NullReference.INSTANCE ? null : (V)value;
    }
}
