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

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

/**
 * An unmodifiable view of a collection that maps each element in an underlying collection into a view.
 *
 * @param <V> The type of the view.
 * @param <E> The type of the underlying element.
 */
public class UnmodifiableCollectionView<V, E> extends AbstractCollection<V>
{
    private final ReusableIterator iterator = new ReusableIterator();
    private final Function<E, V> viewer;
    private final Collection<E> elements;

    /**
     * Constructs an unmodifiable view over collection.
     *
     * @param viewer   function.
     * @param elements collection to create a view for.
     */
    public UnmodifiableCollectionView(final Function<E, V> viewer, final Collection<E> elements)
    {
        this.viewer = viewer;
        this.elements = elements;
    }

    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return elements.size();
    }

    /**
     * {@inheritDoc}
     */
    public ReusableIterator iterator()
    {
        return iterator.reset();
    }

    /**
     * A stateful reusable iterator.
     */
    public final class ReusableIterator implements Iterator<V>, Serializable
    {
        private static final long serialVersionUID = 9183617352140354854L;
        private Iterator<E> delegate;

        public boolean hasNext()
        {
            return delegate.hasNext();
        }

        public V next()
        {
            return viewer.apply(delegate.next());
        }

        private ReusableIterator reset()
        {
            delegate = elements.iterator();

            return this;
        }
    }
}
