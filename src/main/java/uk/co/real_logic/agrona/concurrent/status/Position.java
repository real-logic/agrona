/*
 * Copyright 2014 Real Logic Ltd.
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
package uk.co.real_logic.agrona.concurrent.status;

/**
 * Reports on how far through a buffer some component has progressed..
 *
 * Threadsafe to write to.
 */
public interface Position extends ReadOnlyPosition
{
    /**
     * Sets the current position of the component
     *
     * @param value the current position of the component.
     */
    void set(long value);

    /**
     * Sets the current position of the component
     *
     * @param value the current position of the component.
     */
    void setOrdered(long value);

    /**
     * Set the position to the new proposedValue if it is greater than the current proposedValue.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    boolean proposeMax(long proposedValue);

    /**
     * Set the position to the new proposedValue if it is greater than the current proposedValue.
     *
     * @param proposedValue for the new max.
     * @return true if a new max as been set otherwise false.
     */
    boolean proposeMaxOrdered(long proposedValue);
}
