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
package uk.co.real_logic.agrona.concurrent;

import uk.co.real_logic.agrona.MutableDirectBuffer;

/**
 * Abstraction over a range of buffer types that allows type to be accessed with memory ordering semantics.
 */
public interface AtomicBuffer extends MutableDirectBuffer
{
    /**
     * Verify that the underlying buffer is correctly aligned to prevent word tearing and other ordering issues.
     *
     * @throws IllegalStateException if the alignment is not correct.
     */
    void verifyAlignment();

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index
     */
    long getLongVolatile(long index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index
     */
    void putLongVolatile(long index, long value);

    /**
     * Put a value to a given index with ordered store semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index
     */
    void putLongOrdered(long index, long value);

    /**
     * Add a value to a given index with ordered store semantics. Use a negative increment to decrement.
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index
     */
    long addLongOrdered(long index, long increment);

    /**
     * Atomic compare and set of a long given an expected value.
     *
     * @param index         in bytes for where to put.
     * @param expectedValue at to be compared
     * @param updateValue   to be exchanged
     * @return set successful or not
     */
    boolean compareAndSetLong(long index, long expectedValue, long updateValue);

    /**
     * Atomically exchange a value at a location returning the previous contents.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index
     * @return previous value at the index
     */
    long getAndSetLong(long index, long value);

    /**
     * Atomically add a delta to a value at a location returning the previous contents.
     * To decrement a negative delta can be provided.
     *
     * @param index in bytes for where to put.
     * @param delta to be added to the value at the index
     * @return previous value
     */
    long getAndAddLong(long index, long delta);

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index
     */
    int getIntVolatile(long index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index
     */
    void putIntVolatile(long index, int value);

    /**
     * Put a value to a given index with ordered semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index
     */
    void putIntOrdered(long index, int value);

    /**
     * Add a value to a given index with ordered store semantics. Use a negative increment to decrement.
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index
     */
    int addIntOrdered(long index, int increment);

    /**
     * Atomic compare and set of a int given an expected value.
     *
     * @param index         in bytes for where to put.
     * @param expectedValue at to be compared
     * @param updateValue   to be exchanged
     * @return successful or not
     */
    boolean compareAndSetInt(long index, int expectedValue, int updateValue);

    /**
     * Atomically exchange a value at a location returning the previous contents.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index
     * @return previous value
     */
    int getAndSetInt(long index, int value);

    /**
     * Atomically add a delta to a value at a location returning the previous contents.
     * To decrement a negative delta can be provided.
     *
     * @param index in bytes for where to put.
     * @param delta to be added to the value at the index
     * @return previous value
     */
    int getAndAddInt(long index, int delta);

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index
     */
    short getShortVolatile(long index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index
     */
    void putShortVolatile(long index, short value);

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index
     */
    char getCharVolatile(long index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index
     */
    void putCharVolatile(long index, char value);

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index
     */
    byte getByteVolatile(long index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index
     */
    void putByteVolatile(long index, byte value);
}
