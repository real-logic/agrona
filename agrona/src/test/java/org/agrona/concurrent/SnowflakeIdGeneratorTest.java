/*
 * Copyright 2014-2021 Real Logic Limited.
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

import org.agrona.collections.LongArrayList;
import org.agrona.collections.LongHashSet;
import org.agrona.collections.MutableLong;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.agrona.concurrent.SnowflakeIdGenerator.*;
import java.util.concurrent.CyclicBarrier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest
{
    private static final String NODE_ID_BITS_FIELD = "NODE_ID_BITS";
    private static final String SEQUENCE_BITS_FIELD = "SEQUENCE_BITS";
    private static final String NODE_ID_AND_SEQUENCE_BITS_FIELD = "NODE_ID_AND_SEQUENCE_BITS";
    private static URL[] urls;

    @BeforeAll
    static void beforeAll() throws MalformedURLException
    {
        final Path modulePath = new File("").toPath().toAbsolutePath();
        urls = new URL[]{ modulePath.resolve("build/classes/java/main").toUri().toURL() };
    }

    @Test
    void shouldInitializeNodeIdBitsFromSystemProperty() throws Exception
    {
        System.setProperty(NODE_ID_BITS_PROP_NAME, "7");
        try
        {
            final Class<?> clazz = loadSnowflakeClass();
            assertEquals(7, getFieldValue(clazz, NODE_ID_BITS_FIELD));
            assertEquals(10, NODE_ID_BITS); // default value
        }
        finally
        {
            System.clearProperty(NODE_ID_BITS_PROP_NAME);
        }
    }

    @Test
    void shouldInitializeSequenceBitsFromSystemProperty() throws Exception
    {
        System.setProperty(SEQUENCE_BITS_PROP_NAME, "11");
        try
        {
            final Class<?> clazz = loadSnowflakeClass();
            assertEquals(11, getFieldValue(clazz, SEQUENCE_BITS_FIELD));
            assertEquals(12, SEQUENCE_BITS); // default value
        }
        finally
        {
            System.clearProperty(SEQUENCE_BITS_PROP_NAME);
        }
    }

    @Test
    void shouldThrowExceptionIfNodeIdBitsIsSetToNegativeValue() throws Exception
    {
        System.setProperty(NODE_ID_BITS_PROP_NAME, "-3");
        try
        {
            final Class<?> clazz = loadSnowflakeClass();
            final ExceptionInInitializerError exception = assertThrows(
                ExceptionInInitializerError.class, () -> getFieldValue(clazz, NODE_ID_AND_SEQUENCE_BITS_FIELD));
            final Throwable cause = exception.getException();
            assertEquals(IllegalArgumentException.class, cause.getClass());
            assertEquals("must be >= 0: " + NODE_ID_BITS_PROP_NAME + "=-3", cause.getMessage());
        }
        finally
        {
            System.clearProperty(NODE_ID_BITS_PROP_NAME);
        }
    }

    @Test
    void shouldThrowExceptionIfSequenceBitsIsSetToNegativeValue() throws Exception
    {
        System.setProperty(SEQUENCE_BITS_PROP_NAME, "-1");
        try
        {
            final Class<?> clazz = loadSnowflakeClass();
            final ExceptionInInitializerError exception = assertThrows(
                ExceptionInInitializerError.class, () -> getFieldValue(clazz, NODE_ID_AND_SEQUENCE_BITS_FIELD));
            final Throwable cause = exception.getException();
            assertEquals(IllegalArgumentException.class, cause.getClass());
            assertEquals("must be >= 0: " + SEQUENCE_BITS_PROP_NAME + "=-1", cause.getMessage());
        }
        finally
        {
            System.clearProperty(SEQUENCE_BITS_PROP_NAME);
        }
    }

    static List<Arguments> exceedMaxNumberOfBits()
    {
        return Arrays.asList(
            Arguments.arguments(0, NODE_ID_AND_SEQUENCE_BITS + 1),
            Arguments.arguments(NODE_ID_AND_SEQUENCE_BITS + 1, 0),
            Arguments.arguments(10, 13),
            Arguments.arguments(13, 10));
    }

    @ParameterizedTest
    @MethodSource("exceedMaxNumberOfBits")
    void shouldThrowExceptionIfACombinationOfNodeIdBitsAndSequenceBitsExceedsMaxValue(
        final int nodeIdBits, final int sequenceBits) throws Exception
    {
        System.setProperty(NODE_ID_BITS_PROP_NAME, "" + nodeIdBits);
        System.setProperty(SEQUENCE_BITS_PROP_NAME, "" + sequenceBits);
        try
        {
            final Class<?> clazz = loadSnowflakeClass();
            final ExceptionInInitializerError exception = assertThrows(
                ExceptionInInitializerError.class, () -> getFieldValue(clazz, NODE_ID_AND_SEQUENCE_BITS_FIELD));
            final Throwable cause = exception.getException();
            assertEquals(IllegalArgumentException.class, cause.getClass());
            assertEquals("too many bits used, must not exceed " + NODE_ID_AND_SEQUENCE_BITS + ": " +
                NODE_ID_BITS_PROP_NAME + "=" + nodeIdBits + ", " + SEQUENCE_BITS_PROP_NAME + "=" + sequenceBits,
                cause.getMessage());
        }
        finally
        {
            System.clearProperty(NODE_ID_BITS_PROP_NAME);
            System.clearProperty(SEQUENCE_BITS_PROP_NAME);
        }
    }

    static List<Arguments> configureBoth()
    {
        return Arrays.asList(
            Arguments.arguments(0, NODE_ID_AND_SEQUENCE_BITS),
            Arguments.arguments(0, 1),
            Arguments.arguments(0, 0),
            Arguments.arguments(1, 0),
            Arguments.arguments(NODE_ID_AND_SEQUENCE_BITS, 0),
            Arguments.arguments(8, NODE_ID_AND_SEQUENCE_BITS - 8),
            Arguments.arguments(12, 10),
            Arguments.arguments(3, 5));
    }

    @ParameterizedTest
    @MethodSource("configureBoth")
    void shouldInitializeNodeIdAndSequenceBits(final int nodeIdBits, final int sequenceBits) throws Exception
    {
        System.setProperty(NODE_ID_BITS_PROP_NAME, "" + nodeIdBits);
        System.setProperty(SEQUENCE_BITS_PROP_NAME, "" + sequenceBits);
        try
        {
            final Class<?> clazz = loadSnowflakeClass();
            assertEquals(nodeIdBits, getFieldValue(clazz, NODE_ID_BITS_FIELD));
            assertEquals(sequenceBits, getFieldValue(clazz, SEQUENCE_BITS_FIELD));
        }
        finally
        {
            System.clearProperty(NODE_ID_BITS_PROP_NAME);
            System.clearProperty(SEQUENCE_BITS_PROP_NAME);
        }
    }

    @Test
    void shouldInitialiseGenerator()
    {
        final long nodeId = 7;
        final long timestampOffset = 19;
        final EpochClock clock = new SystemEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId, timestampOffset, clock);

        assertEquals(nodeId, idGenerator.nodeId());
        assertEquals(timestampOffset, idGenerator.timestampOffsetMs());
    }

    @Test
    void shouldCheckConstructorArgs()
    {
        assertThrows(IllegalArgumentException.class,
            () -> new SnowflakeIdGenerator(-1, 0, SystemEpochClock.INSTANCE));
        assertThrows(IllegalArgumentException.class,
            () -> new SnowflakeIdGenerator(1024, 0, SystemEpochClock.INSTANCE));

        assertThrows(IllegalArgumentException.class,
            () -> new SnowflakeIdGenerator(0, -1, SystemEpochClock.INSTANCE));

        assertThrows(NullPointerException.class,
            () -> new SnowflakeIdGenerator(0, 0, null));
    }

    @Test
    void shouldGetFirstId()
    {
        final long nodeId = 7;
        final long timestampOffset = 0;
        final CachedEpochClock clock = new CachedEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId, timestampOffset, clock);
        clock.advance(1);

        final long id = idGenerator.nextId();

        assertEquals(clock.time(), extractTimestamp(id));
        assertEquals(nodeId, extractNodeId(id));
        assertEquals(0L, extractSequence(id));
    }

    @Test
    void shouldIncrementSequence()
    {
        final long nodeId = 7;
        final long timestampOffset = 0;
        final CachedEpochClock clock = new CachedEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId, timestampOffset, clock);
        clock.advance(3);

        final long idOne = idGenerator.nextId();

        assertEquals(clock.time(), extractTimestamp(idOne));
        assertEquals(nodeId, extractNodeId(idOne));
        assertEquals(0L, extractSequence(idOne));

        final long idTwo = idGenerator.nextId();

        assertEquals(clock.time(), extractTimestamp(idTwo));
        assertEquals(nodeId, extractNodeId(idTwo));
        assertEquals(1L, extractSequence(idTwo));
    }

    @Test
    void shouldAdvanceTimestamp()
    {
        final long nodeId = 7;
        final long timestampOffset = 0;
        final CachedEpochClock clock = new CachedEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId, timestampOffset, clock);
        clock.advance(3);

        final long idOne = idGenerator.nextId();

        assertEquals(clock.time(), extractTimestamp(idOne));
        assertEquals(nodeId, extractNodeId(idOne));
        assertEquals(0L, extractSequence(idOne));

        clock.advance(3);
        final long idTwo = idGenerator.nextId();

        assertEquals(clock.time(), extractTimestamp(idTwo));
        assertEquals(nodeId, extractNodeId(idTwo));
        assertEquals(0L, extractSequence(idTwo));
    }

    @Test
    void shouldDetectClockGoingBackwards()
    {
        final long nodeId = 7;
        final long timestampOffset = 0;
        final CachedEpochClock clock = new CachedEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId, timestampOffset, clock);
        clock.update(7);

        idGenerator.nextId();

        clock.update(3);
        assertThrows(IllegalStateException.class, idGenerator::nextId);
    }

    @Test
    @Timeout(10)
    void shouldWaitOnSequenceRollover() throws InterruptedException
    {
        final long nodeId = 7;
        final long timestampOffset = 0;

        final MutableLong clockCounter = new MutableLong();
        final MutableLong generatedId = new MutableLong();
        final EpochClock clock = () -> clockCounter.getAndIncrement() <= MAX_SEQUENCE ? 1L : 2L;

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId, timestampOffset, clock);
        clockCounter.set(0);

        for (int i = 0; i <= MAX_SEQUENCE; i++)
        {
            final long id = idGenerator.nextId();

            assertEquals(1L, extractTimestamp(id));
            assertEquals(nodeId, extractNodeId(id));
            assertEquals(i, extractSequence(id));
        }

        final Thread thread = new Thread(() -> generatedId.set(idGenerator.nextId()));
        thread.setDaemon(true);
        thread.start();

        try
        {
            thread.join();

            assertEquals(2L, extractTimestamp(generatedId.get()));
            assertEquals(nodeId, extractNodeId(generatedId.get()));
            assertEquals(0L, extractSequence(generatedId.get()));
        }
        catch (final InterruptedException ex)
        {
            thread.interrupt();
            throw ex;
        }
    }

    @Test
    @Timeout(30)
    void shouldAllowConcurrentAccess() throws InterruptedException
    {
        HighResolutionTimer.enable();
        try
        {
            final int iterations = 10;
            for (int i = 0; i < iterations; i++)
            {
                testConcurrentAccess();
            }
        }
        finally
        {
            HighResolutionTimer.disable();
        }
    }

    private static void testConcurrentAccess() throws InterruptedException
    {
        final long nodeId = 16;
        final int idsPerThread = 50_000;
        final int numThreads = 2;

        final EpochClock clock = SystemEpochClock.INSTANCE;
        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId, 0, clock);
        final CyclicBarrier barrier = new CyclicBarrier(numThreads);

        class GetIdTask extends Thread
        {
            final LongArrayList ids = new LongArrayList(idsPerThread, Long.MIN_VALUE);

            public void run()
            {
                try
                {
                    barrier.await();
                }
                catch (final Exception ignore)
                {
                    fail();
                }

                long lastId = -1;
                for (int j = 0; j < idsPerThread; j++)
                {
                    final long id = idGenerator.nextId();
                    if (id <= lastId)
                    {
                        fail("id went backwards: lastId=" + lastId + ", newId=" + id);
                    }

                    ids.add(id);
                    lastId = id;
                }
            }
        }

        final GetIdTask[] tasks = new GetIdTask[numThreads];
        final long beginTimeMs = clock.time();

        for (int i = 0; i < numThreads; i++)
        {
            tasks[i] = new GetIdTask();
            tasks[i].start();
        }

        for (final GetIdTask task : tasks)
        {
            task.join();
        }

        final long endTimeMs = clock.time();
        final LongHashSet allIdsSet = new LongHashSet(numThreads * idsPerThread);

        for (final GetIdTask task : tasks)
        {
            final LongHashSet idsSet = new LongHashSet(task.ids.size());

            for (final long id : task.ids)
            {
                assertEquals(extractNodeId(id), nodeId);

                final long timestampMs = extractTimestamp(id);
                assertThat(timestampMs, greaterThanOrEqualTo(beginTimeMs));
                assertThat(timestampMs, lessThanOrEqualTo(endTimeMs));

                idsSet.add(id);
            }

            assertEquals(task.ids.size(), idsSet.size(), "non-unique ids within a thread");
            assertTrue(allIdsSet.addAll(idsSet));
        }

        assertEquals(numThreads * idsPerThread, allIdsSet.size(), "non-unique ids across threads");
    }

    private static long extractTimestamp(final long id)
    {
        return id >>> (NODE_ID_BITS + SEQUENCE_BITS);
    }

    private static long extractNodeId(final long id)
    {
        return (id >>> SEQUENCE_BITS) & (MAX_NODE_ID);
    }

    private static long extractSequence(final long id)
    {
        return id & MAX_SEQUENCE;
    }

    private static Class<?> loadSnowflakeClass() throws ClassNotFoundException
    {
        final URLClassLoader classLoader = new URLClassLoader(urls, null);
        final Class<?> clazz = classLoader.loadClass(SnowflakeIdGenerator.class.getName());
        assertNotEquals(SnowflakeIdGenerator.class, clazz);

        return clazz;
    }

    private static int getFieldValue(
        final Class<?> clazz, final String name) throws NoSuchFieldException, IllegalAccessException
    {
        final Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return (Integer)field.get(null);
    }
}
