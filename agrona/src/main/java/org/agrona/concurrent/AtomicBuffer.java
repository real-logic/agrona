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
     */
    String STRICT_ALIGNMENT_CHECKS_PROP_NAME = "agrona.strict.alignment.checks";

    /**
     * Should alignment checks for atomic operations be done or not. The value is platform-dependent:
     * <ul>
     *     <li>On x64 it is controlled by the {@link #STRICT_ALIGNMENT_CHECKS_PROP_NAME} system property.</li>
     *     <li>On other platforms it is always {@code true}.</li>
     * </ul>
     *
     * @see AtomicBuffer#STRICT_ALIGNMENT_CHECKS_PROP_NAME
     */
    boolean STRICT_ALIGNMENT_CHECKS = !SystemUtil.isX64Arch() ||
        "true".equals(SystemUtil.getProperty(STRICT_ALIGNMENT_CHECKS_PROP_NAME, "false"));

    /**
     * Verify that the underlying buffer is correctly aligned to prevent word tearing, other ordering issues and
     * the JVM crashes. In particular this method verifies that the starting offset of the underlying buffer is properly
     * aligned. However, the actual atomic call must ensure that the index is properly aligned, i.e. it must be aligned
     * to the size of the operand. For example a call to any of the following methods {@link #putIntOrdered(int, int)},
     * {@link #putIntVolatile(int, int)}, {@link #addIntOrdered(int, int)}, {@link #getIntVolatile(int)},
     * {@link #getAndAddInt(int, int)} or {@link #getAndSetInt(int, int)}, must have the index aligned by four bytes
     * (e.g. {@code 0, 4, 8, 12, 60 etc.}).
     * <p>
     * Users are encouraged to call this method after constructing the {@link AtomicBuffer} instance in order to ensure
     * that the underlying buffer supports atomic access to {@code long} values.
     * <p>
     * Agrona provides an agent ({@code org.agrona.agent.BufferAlignmentAgent}) that checks the alignment of indexes
     * for all operations at runtime. The agent throws an exception if the unaligned access is detected.
     * <p>
     * Note: on some platforms unaligned atomic access can lead to the JVM crashes, e.g.:
     * <pre>
     * {@code
     * # Java VM: OpenJDK 64-Bit Server VM (25.352-b08 mixed mode bsd-aarch64 compressed oops)
     * #
     * # siginfo: si_signo: 10 (SIGBUS), si_code: 1 (BUS_ADRALN)
     * }
     * </pre>
     *
     * @throws IllegalStateException if the starting offset into the buffer is not properly aligned.
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
