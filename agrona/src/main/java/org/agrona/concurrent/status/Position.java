/*
 * Copyright 2014-2023 Real Logic Limited.
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
package org.agrona.concurrent.status;

/**
 * Reports on how far through a buffer some component has progressed.
 * <p>
 * Threadsafe to write to from a single writer.
 */
public abstract class Position extends ReadablePosition
{
    /**
     * Has this Position been closed?
     *
     * @return true if this position has already been closed.
     */
    public abstract boolean isClosed();

    /**
     * Get the current position of a component without memory ordering semantics.
     *
     * @return the current position of a component
     */
    public abstract long get();

    /**
     * Sets the current position of the component without memory ordering semantics.
     *
     * @param value the current position of the component.
     */
    public abstract void set(long value);

    /**
     * Sets the current position of the component with ordered memory semantics.
     *
     * @param value the current position of the component.
     */
    public abstract void setOrdered(long value);

    /**
     * Sets the current position of the component with volatile memory semantics.
     *
     * @param value the current position of the component.
     */
    public abstract void setVolatile(long value);

    /**
     * Set the position to a new proposedValue if greater than the current value without memory ordering semantics.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public abstract boolean proposeMax(long proposedValue);

    /**
     * Set the position to the new proposedValue if greater than the current value with memory ordering semantics.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public abstract boolean proposeMaxOrdered(long proposedValue);
}
