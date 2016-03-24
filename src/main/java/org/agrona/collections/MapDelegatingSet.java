/*
 * Copyright 2014 - 2016 Real Logic Ltd.
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

import java.util.AbstractSet;
import java.util.Map;

/**
 * Read-only collection which delegates its operations to an underlying map and a couple of functions. Designed
 * to easily implement keyset() and values() methods in a map.
 *
 * @param <V> The generic type of the set.
 */
abstract class MapDelegatingSet<V> extends AbstractSet<V>
{
    private final Map<?, ?> delegate;

    protected MapDelegatingSet(final Map<?, ?> delegate)
    {
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return delegate.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        delegate.clear();
    }

    /**
     * {@inheritDoc}
     */
    public abstract boolean contains(final Object o);
}
