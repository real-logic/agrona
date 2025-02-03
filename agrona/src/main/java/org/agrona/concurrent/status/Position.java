/*
 * Copyright 2014-2025 Real Logic Limited.
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
 * Threadsafe to write to from a single writer unless methods with plain memory semantics are used.
 */
public abstract class Position extends ReadablePosition
{
    /**
     * Default constructor.
     */
    public Position()
    {
    }

    /**
     * Has this Position been closed?
     *
     * @return true if this position has already been closed.
     */
    public abstract boolean isClosed();

    /**
     * Sets the current position of the component with volatile memory semantics.
     *
     * @param value the current position of the component.
     */
    public abstract void setVolatile(long value);

    /**
     * Sets the current position of the component with ordered memory semantics.
     * <p>
     * This method is identical to {@link #setRelease(long)} and that method should be used instead.
     *
     * @param value the current position of the component.
     */
    public abstract void setOrdered(long value);

    /**
     * Sets the current position of the component with release memory semantics.
     *
     * @param value the current position of the component.
     * @since 2.1.0
     */
    public abstract void setRelease(long value);

    /**
     * Sets the current position of the component with opaque memory semantics.
     *
     * @param value the current position of the component.
     * @since 2.1.0
     */
    public abstract void setOpaque(long value);

    /**
     * Sets the current position of the component plain memory semantics.
     *
     * @param value the current position of the component.
     */
    public abstract void set(long value);

    /**
     * Set the position to a new proposedValue if greater than the current value with plain memory semantics.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public abstract boolean proposeMax(long proposedValue);

    /**
     * Set the position to the new proposedValue if greater than the current value with release memory semantics.
     * <p>
     * This method is identical to {@link #proposeMaxRelease(long)} and that method should be preferred instead.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public abstract boolean proposeMaxOrdered(long proposedValue);

    /**
     * Set the position to the new proposedValue if greater than the current value with release memory
     * semantics.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public abstract boolean proposeMaxRelease(long proposedValue);

    /**
     * Set the position to the new proposedValue if greater than the current value with opaque memory
     * semantics.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    public abstract boolean proposeMaxOpaque(long proposedValue);
}
