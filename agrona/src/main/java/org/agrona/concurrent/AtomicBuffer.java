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
package org.agrona.concurrent;

import org.agrona.MutableDirectBuffer;
import org.agrona.SystemUtil;

import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * Abstraction over a range of buffer types that allows type to be accessed with various memory ordering semantics.
 * <p>
 * Before Java 9, there was no naming standard for stores with release semantics. On the AtomicLong there was the
 * {@link java.util.concurrent.atomic.AtomicLong#lazySet(long)}. Because there was no standard, the AtomicBuffer
 * has methods like {@link #putLongOrdered(int, long)}. With Java 9, the 'release' name has been introduced.
 * The AtomicBuffer also has methods with release methods which are identical to the ordered methods. All the
 * methods with 'ordered' name will call the equivalent method with release name. This introduces a small
 * performance penalty for the older methods and this should encourage users to switch to the newer methods.
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
     * to the size of the operand. For example a call to any of the following methods {@link #putIntRelease(int, int)},
     * {@link #putIntVolatile(int, int)}, {@link #addIntRelease(int, int)} (int, int)}, {@link #getIntVolatile(int)},
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
     * Atomically get the value at a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    long getLongVolatile(int index);

    /**
     * Atomically get the value at a given index with acquire semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     * @since 2.1.0
     */
    long getLongAcquire(int index);

    /**
     * Atomically put a value to a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putLongVolatile(int index, long value);

    /**
     * Atomically put a value to a given index with ordered store semantics.
     * <p>
     * Instead of using this method, use  {@link #putLongRelease(int, long)} instead. They
     * are identical and the putLongRelease is the preferred version.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putLongOrdered(int index, long value);

    /**
     * Atomically put a value to a given index with release semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     * @since 2.1.0
     */
    void putLongRelease(int index, long value);

    /**
     * Atomically adds a value to a given index with ordered store semantics. Use a negative increment to decrement.
     * <p>
     * The load has no ordering semantics. The store has release semantics.
     * <p>
     * Instead of using this method, use {@link #addLongRelease(int, long)} instead. They
     * are identical but the addLongRelease is the preferred version.
     *
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     */
    long addLongOrdered(int index, long increment);

    /**
     * Atomically put a value to a given index with opaque semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putLongOpaque(int index, long value);

    /**
     * Atomically get a value to a given index with opaque semantics.
     *
     * @param index in bytes for where to put.
     * @return the value for at a given index.
     */
    long getLongOpaque(int index);

    /**
     * Adds a value to a given index with opaque semantics. The read and write
     * will be atomic, but the combination is not atomic. So don't use this
     * method concurrently because you can run into lost updates due to
     * a race-condition.
     *
     * @param index in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     */
    long addLongOpaque(int index, long increment);

    /**
     * Atomically adds a value to a given index with ordered store semantics. Use a negative increment to decrement.
     * <p>
     * The load has no ordering semantics. The store has release semantics.
     *
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     * @since 2.1.0
     */
    long addLongRelease(int index, long increment);

    /**
     * Atomic compare and set of a long given an expected value.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index         in bytes for where to put.
     * @param expectedValue at to be compared.
     * @param updateValue   to be exchanged.
     * @return set successful or not.
     */
    boolean compareAndSetLong(int index, long expectedValue, long updateValue);

    /**
     * Atomically exchange a value at a location returning the previous contents.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     * @return previous value at the index.
     */
    long getAndSetLong(int index, long value);

    /**
     * Atomically add a delta to a value at a location returning the previous contents.
     * To decrement a negative delta can be provided.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes for where to put.
     * @param delta to be added to the value at the index.
     * @return previous value.
     */
    long getAndAddLong(int index, long delta);

    /**
     * Atomically get the value at a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    int getIntVolatile(int index);

    /**
     * Atomically put a value to a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putIntVolatile(int index, int value);

    /**
     * Atomically get the value at a given index with acquire semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     * @since 2.1.0
     */
    int getIntAcquire(int index);

    /**
     * Atomically put a value to a given index with ordered semantics.
     * <p>
     * Instead of using this method, use {@link #putIntRelease} instead. They
     * are identical but the putIntRelease is the preferred version.

     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putIntOrdered(int index, int value);

    /**
     * Atomically put a value to a given index with release semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     * @since 2.1.0
     */
    void putIntRelease(int index, int value);

    /**
     * Atomically add a value to a given index with ordered store semantics. Use a negative increment to decrement.
     * <p>
     * The load has no ordering semantics. The store has release semantics.
     * <p>
     * Instead of using this method, use {@link #addIntRelease(int, int)} instead. They
     * are identical but the addIntRelease is the preferred version.
     *
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     */
    int addIntOrdered(int index, int increment);

    /**
     * Atomically add a value to a given index with release semantics. Use a negative increment to decrement.
     * <p>
     * The load has no ordering semantics. The store has release semantics.
     *
     * @param index     in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     * @since 2.1.0
     */
    int addIntRelease(int index, int increment);

    /**
     * Atomically put a value to a given index with opaque semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putIntOpaque(int index, int value);

    /**
     * Atomically get a value to a given index with opaque semantics.
     *
     * @param index in bytes for where to put.
     * @return the value for at a given index.
     */
    int getIntOpaque(int index);

    /**
     * Adds a value to a given index with opaque semantics. The read and write
     * will be atomic, but the combination is not atomic. So don't use this
     * method concurrently because you can run into lost updates due to
     * a race-condition.
     *
     * @param index in bytes for where to put.
     * @param increment by which the value at the index will be adjusted.
     * @return the previous value at the index.
     */
    int addIntOpaque(int index, int increment);

    /**
     * Atomic compare and set of an int given an expected value.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index         in bytes for where to put.
     * @param expectedValue at to be compared.
     * @param updateValue   to be exchanged.
     * @return successful or not.
     */
    boolean compareAndSetInt(int index, int expectedValue, int updateValue);

    /**
     * Atomically exchange a value at a location returning the previous contents.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     * @return previous value.
     */
    int getAndSetInt(int index, int value);

    /**
     * Atomically add a delta to a value at a location returning the previous contents.
     * To decrement a negative delta can be provided.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes for where to put.
     * @param delta to be added to the value at the index.
     * @return previous value.
     */
    int getAndAddInt(int index, int delta);

    /**
     * Atomically get the value at a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    short getShortVolatile(int index);

    /**
     * Atomically put a value to a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putShortVolatile(int index, short value);

    /**
     * Atomically get the value at a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    char getCharVolatile(int index);

    /**
     * Atomically put a value to a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putCharVolatile(int index, char value);

    /**
     * Atomically get the value at a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    byte getByteVolatile(int index);

    /**
     * Atomically put a value to a given index with volatile semantics.
     * <p>
     * This call has sequential-consistent semantics.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putByteVolatile(int index, byte value);
}
