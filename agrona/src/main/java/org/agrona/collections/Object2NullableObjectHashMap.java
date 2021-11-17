/*
 * Copyright 2014-2021 Real Logic Limited.
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

/**
 * Variation of {@link Object2ObjectHashMap} that allows {@code null} values.
 */
public class Object2NullableObjectHashMap<K, V> extends Object2ObjectHashMap<K, V>
{
    /**
     * Default constructor.
     */
    public Object2NullableObjectHashMap()
    {
    }

    /**
     * Defaults to avoiding allocation.
     *
     * @param initialCapacity for the map to override {@link #MIN_CAPACITY}
     * @param loadFactor      for the map to override {@link Hashing#DEFAULT_LOAD_FACTOR}.
     */
    public Object2NullableObjectHashMap(final int initialCapacity, final float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    /**
     * @param initialCapacity       for the map to override {@link #MIN_CAPACITY}
     * @param loadFactor            for the map to override {@link Hashing#DEFAULT_LOAD_FACTOR}.
     * @param shouldAvoidAllocation should allocation be avoided by caching iterators and map entries.
     */
    public Object2NullableObjectHashMap(
        final int initialCapacity, final float loadFactor, final boolean shouldAvoidAllocation)
    {
        super(initialCapacity, loadFactor, shouldAvoidAllocation);
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
