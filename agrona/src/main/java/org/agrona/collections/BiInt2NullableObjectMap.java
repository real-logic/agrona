/*
 * Copyright 2014-2024 Real Logic Limited.
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
 * Variation of {@link BiInt2ObjectMap} that allows {@code null} values.
 *
 * @param <V> type of values stored in the {@link java.util.Map}
 */
public class BiInt2NullableObjectMap<V> extends BiInt2ObjectMap<V>
{
    /**
     * Constructs map with default settings.
     */
    public BiInt2NullableObjectMap()
    {
        super();
    }

    /**
     * Constructs map with given initial capacity and load factory and enables caching of iterators.
     *
     * @param initialCapacity for the backing array.
     * @param loadFactor      limit for resizing on puts.
     */
    public BiInt2NullableObjectMap(final int initialCapacity, final float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    protected Object mapNullValue(final Object value)
    {
        return null == value ? NullReference.INSTANCE : value;
    }

    @SuppressWarnings("unchecked")
    protected V unmapNullValue(final Object value)
    {
        return NullReference.INSTANCE == value ? null : (V)value;
    }
}
