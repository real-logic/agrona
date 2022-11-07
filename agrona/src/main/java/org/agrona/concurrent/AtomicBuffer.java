/*
 * Copyright 2014-2022 Real Logic Limited.
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
package org.agrona.concurrent;

import org.agrona.MutableDirectBuffer;
import org.agrona.SystemUtil;

import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * Abstraction over a range of buffer types that allows type to be accessed with memory ordering semantics.
 */
public interface AtomicBuffer extends MutableDirectBuffer
{
    /**
     * Buffer alignment in bytes to ensure atomic word accesses.
     */
    int ALIGNMENT = SIZE_OF_LONG;

    /**
     * Name of the system property that specify if the alignment checks for atomic operations are strict. If the checks
     * are strict then the {@link #verifyAlignment()} method will throw an exception if the underlying buffer is a
     * {@code byte[]}.
     * <p>
     * The strictness of the check is platform-specific. For example on {@code x86} platform the check is disabled
     * whereas on {@code aarch64} it is enabled.
     */
    String STRICT_ALIGNMENT_CHECKS_PROP_NAME = "agrona.strict.alignment.checks";

    /**
     * Should alignment checks for atomic operations be done or not. Controlled by the
     * {@link #STRICT_ALIGNMENT_CHECKS_PROP_NAME} system property.
     *
     * @see AtomicBuffer#STRICT_ALIGNMENT_CHECKS_PROP_NAME
     */
    boolean STRICT_ALIGNMENT_CHECKS = "true".equals(
        SystemUtil.getProperty(STRICT_ALIGNMENT_CHECKS_PROP_NAME, SystemUtil.isX86Arch() ? "false" : "true"));

    /**
     * Verify that the underlying buffer is correctly aligned to prevent word tearing and other ordering issues.
     * <p>
     * Users are encouraged to call this method after constructing the {@link AtomicBuffer} instance in order to ensure
     * that the underlying buffer supports atomic access to {@code long} values.
     *
     * @throws IllegalStateException if the alignment is not correct.
     * @see #ALIGNMENT
     */
    void verifyAlignment();

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    long getLongVolatile(int index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putLongVolatile(int index, long value);

    /**
     * Put a value to a given index with ordered store semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putLongOrdered(int index, long value);

    /**
     * Add a value to a given index with ordered store semantics. Use a negative increment to decrement.
     *
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     */
    long addLongOrdered(int index, long increment);

    /**
     * Atomic compare and set of a long given an expected value.
     *
     * @param index         in bytes for where to put.
     * @param expectedValue at to be compared.
     * @param updateValue   to be exchanged.
     * @return set successful or not.
     */
    boolean compareAndSetLong(int index, long expectedValue, long updateValue);

    /**
     * Atomically exchange a value at a location returning the previous contents.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     * @return previous value at the index.
     */
    long getAndSetLong(int index, long value);

    /**
     * Atomically add a delta to a value at a location returning the previous contents.
     * To decrement a negative delta can be provided.
     *
     * @param index in bytes for where to put.
     * @param delta to be added to the value at the index.
     * @return previous value.
     */
    long getAndAddLong(int index, long delta);

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    int getIntVolatile(int index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putIntVolatile(int index, int value);

    /**
     * Put a value to a given index with ordered semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putIntOrdered(int index, int value);

    /**
     * Add a value to a given index with ordered store semantics. Use a negative increment to decrement.
     *
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     */
    int addIntOrdered(int index, int increment);

    /**
     * Atomic compare and set of an int given an expected value.
     *
     * @param index         in bytes for where to put.
     * @param expectedValue at to be compared.
     * @param updateValue   to be exchanged.
     * @return successful or not.
     */
    boolean compareAndSetInt(int index, int expectedValue, int updateValue);

    /**
     * Atomically exchange a value at a location returning the previous contents.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     * @return previous value.
     */
    int getAndSetInt(int index, int value);

    /**
     * Atomically add a delta to a value at a location returning the previous contents.
     * To decrement a negative delta can be provided.
     *
     * @param index in bytes for where to put.
     * @param delta to be added to the value at the index.
     * @return previous value.
     */
    int getAndAddInt(int index, int delta);

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    short getShortVolatile(int index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putShortVolatile(int index, short value);

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    char getCharVolatile(int index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putCharVolatile(int index, char value);

    /**
     * Get the value at a given index with volatile semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    byte getByteVolatile(int index);

    /**
     * Put a value to a given index with volatile semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putByteVolatile(int index, byte value);
}
