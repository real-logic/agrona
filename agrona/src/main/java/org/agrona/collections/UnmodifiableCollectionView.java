/*
 *  Copyright 2014-2017 Real Logic Ltd.
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

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

/**
 * An unmodifiable view of a collection that maps each element in an underlying
 * collection into a view.
 *
 * @param <V> The type of the view.
 * @param <E> The type of the underlying element.
 */
public class UnmodifiableCollectionView<V, E> extends AbstractCollection<V> implements Serializable
{
    private final ReusableIterator iterator = new ReusableIterator();
    private final Function<E, V> viewer;
    private final Collection<E> elements;

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
    public Iterator<V> iterator()
    {
        return iterator.reset();
    }

    private class ReusableIterator implements Iterator<V>, Serializable
    {
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
