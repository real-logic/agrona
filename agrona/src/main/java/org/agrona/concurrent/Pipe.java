/*
 * Copyright 2014-2019 Real Logic Ltd.
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
package org.agrona.concurrent;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * A container for items exchanged from producers to consumers.
 */
public interface Pipe<E>
{
    /**
     * The number of items added to this container since creation.
     *
     * @return the number of items added.
     */
    long addedCount();

    /**
     * The number of items removed from this container since creation.
     *
     * @return the number of items removed.
     */
    long removedCount();

    /**
     * The maximum capacity of this container to hold items.
     *
     * @return the capacity of the container.
     */
    int capacity();

    /**
     * The number of items currently in the container.
     *
     * @return number of items currently in the container.
     */
    int size();

    /**
     * Get the remaining capacity for elements in the container given the current size.
     *
     * @return remaining capacity of the container.
     */
    int remainingCapacity();

    /**
     * Drain the number of elements present in a collection at the time the operation starts.
     * <p>
     * If possible, implementations should use smart batching to best handle burst traffic.
     *
     * @param elementConsumer {@link Consumer} for processing elements.
     * @return the number of elements drained.
     */
    int drain(Consumer<E> elementConsumer);

    /**
     * Drain the minimum of a limit and the number of elements present in a collection at the time the operation starts.
     * <p>
     * If possible, implementations should use smart batching to best handle burst traffic.
     *
     * @param elementConsumer {@link Consumer} for processing elements.
     * @param limit           maximum number of elements to be drained in a drain operation.
     * @return the number of elements drained.
     */
    int drain(Consumer<E> elementConsumer, int limit);

    /**
     * Drain available elements into the provided {@link java.util.Collection} up to a provided maximum limit of
     * elements.
     * <p>
     * If possible, implementations should use smart batching to best handle burst traffic.
     *
     * @param target in to which elements are drained.
     * @param limit  maximum number of elements to be drained in a drain operation.
     * @return the number of elements actually drained.
     */
    int drainTo(Collection<? super E> target, int limit);
}
