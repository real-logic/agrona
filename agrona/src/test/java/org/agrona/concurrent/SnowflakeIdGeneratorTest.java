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

import org.agrona.collections.MutableLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.agrona.concurrent.SnowflakeIdGenerator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnowflakeIdGeneratorTest
{
    @Test
    void shouldThrowExceptionIfNodeIdBitsIsNegative()
    {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new SnowflakeIdGenerator(-3, SEQUENCE_BITS_DEFAULT, 0, 0, SystemEpochClock.INSTANCE));
        assertEquals("must be >= 0: nodeIdBits=-3", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionIfSequenceBitsIsNegative()
    {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new SnowflakeIdGenerator(NODE_ID_BITS_DEFAULT, -1, 0, 0, SystemEpochClock.INSTANCE));
        assertEquals("must be >= 0: sequenceBits=-1", exception.getMessage());
    }

    static List<Arguments> invalidPayloadBits()
    {
        return Arrays.asList(
            Arguments.arguments(0, MAX_NODE_ID_AND_SEQUENCE_BITS + 1),
            Arguments.arguments(MAX_NODE_ID_AND_SEQUENCE_BITS + 1, 0),
            Arguments.arguments(10, 13),
            Arguments.arguments(13, 10));
    }

    @ParameterizedTest
    @MethodSource("invalidPayloadBits")
    void shouldThrowExceptionIfACombinationOfNodeIdBitsAndSequenceBitsExceedsMaxValue(
        final int nodeIdBits, final int sequenceBits)
    {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new SnowflakeIdGenerator(nodeIdBits, sequenceBits, 0, 0, SystemEpochClock.INSTANCE));
        assertEquals("too many bits used:" +
            " nodeIdBits=" + nodeIdBits + " + sequenceBits=" + sequenceBits + " > " + MAX_NODE_ID_AND_SEQUENCE_BITS,
            exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = { -4, 5 })
    void shouldThrowExceptionIfNodeIdIsOutOfRange(final int nodeId)
    {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new SnowflakeIdGenerator(2, SEQUENCE_BITS_DEFAULT, nodeId, 0, SystemEpochClock.INSTANCE));
        assertEquals("must be >= 0 && <= 3: nodeId=" + nodeId, exception.getMessage());
    }

    @Test
    void shouldThrowExceptionIfTimestampOffsetIsNegative()
    {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new SnowflakeIdGenerator(
            NODE_ID_BITS_DEFAULT, SEQUENCE_BITS_DEFAULT, 0, -6, SystemEpochClock.INSTANCE));
        assertEquals("must be >= 0: timestampOffsetMs=-6", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionIfTimestampOffsetIsGreaterThanCurrentTime()
    {
        final EpochClock clock = () -> 42;

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new SnowflakeIdGenerator(NODE_ID_BITS_DEFAULT, SEQUENCE_BITS_DEFAULT, 0, 256, clock));
        assertEquals("timestampOffsetMs=256 > nowMs=42", exception.getMessage());
    }

    static List<Arguments> configurePayloadBits()
    {
        return Arrays.asList(
            Arguments.arguments(0, MAX_NODE_ID_AND_SEQUENCE_BITS),
            Arguments.arguments(0, 1),
            Arguments.arguments(0, 0),
            Arguments.arguments(1, 0),
            Arguments.arguments(MAX_NODE_ID_AND_SEQUENCE_BITS, 0),
            Arguments.arguments(8, MAX_NODE_ID_AND_SEQUENCE_BITS - 8),
            Arguments.arguments(12, 10),
            Arguments.arguments(3, 5));
    }

    @ParameterizedTest
    @MethodSource("configurePayloadBits")
    void shouldInitializeNodeIdAndSequenceBits(final int nodeIdBits, final int sequenceBits)
    {
        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(
            nodeIdBits, sequenceBits, 0, 0, SystemEpochClock.INSTANCE);
        assertEquals((long)Math.pow(2, nodeIdBits) - 1, idGenerator.maxNodeId());
        assertEquals((long)Math.pow(2, sequenceBits) - 1, idGenerator.maxSequence());
    }

    @Test
    void shouldInitialiseGenerator()
    {
        final long nodeId = 7;
        final long timestampOffset = 19;
        final EpochClock clock = new SystemEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(
            NODE_ID_BITS_DEFAULT, SEQUENCE_BITS_DEFAULT, nodeId, timestampOffset, clock);

        assertEquals(nodeId, idGenerator.nodeId());
        assertEquals(timestampOffset, idGenerator.timestampOffsetMs());
    }

    @Test
    void shouldGetFirstId()
    {
        final long nodeId = 7;
        final long timestampOffset = 0;
        final CachedEpochClock clock = new CachedEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(
            NODE_ID_BITS_DEFAULT, SEQUENCE_BITS_DEFAULT, nodeId, timestampOffset, clock);
        clock.advance(1);

        final long id = idGenerator.nextId();

        assertEquals(clock.time(), idGenerator.extractTimestamp(id));
        assertEquals(nodeId, idGenerator.extractNodeId(id));
        assertEquals(0L, idGenerator.extractSequence(id));
    }

    @Test
    void shouldIncrementSequence()
    {
        final long nodeId = 7;
        final long timestampOffset = 0;
        final CachedEpochClock clock = new CachedEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(
            NODE_ID_BITS_DEFAULT, SEQUENCE_BITS_DEFAULT, nodeId, timestampOffset, clock);
        clock.advance(3);

        final long idOne = idGenerator.nextId();

        assertEquals(clock.time(), idGenerator.extractTimestamp(idOne));
        assertEquals(nodeId, idGenerator.extractNodeId(idOne));
        assertEquals(0L, idGenerator.extractSequence(idOne));

        final long idTwo = idGenerator.nextId();

        assertEquals(clock.time(), idGenerator.extractTimestamp(idTwo));
        assertEquals(nodeId, idGenerator.extractNodeId(idTwo));
        assertEquals(1L, idGenerator.extractSequence(idTwo));
    }

    @Test
    void shouldAdvanceTimestamp()
    {
        final long nodeId = 7;
        final long timestampOffset = 0;
        final CachedEpochClock clock = new CachedEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(
            NODE_ID_BITS_DEFAULT, SEQUENCE_BITS_DEFAULT, nodeId, timestampOffset, clock);
        clock.advance(3);

        final long idOne = idGenerator.nextId();

        assertEquals(clock.time(), idGenerator.extractTimestamp(idOne));
        assertEquals(nodeId, idGenerator.extractNodeId(idOne));
        assertEquals(0L, idGenerator.extractSequence(idOne));

        clock.advance(3);
        final long idTwo = idGenerator.nextId();

        assertEquals(clock.time(), idGenerator.extractTimestamp(idTwo));
        assertEquals(nodeId, idGenerator.extractNodeId(idTwo));
        assertEquals(0L, idGenerator.extractSequence(idTwo));
    }

    @Test
    void shouldDetectClockGoingBackwards()
    {
        final long nodeId = 7;
        final long timestampOffset = 0;
        final CachedEpochClock clock = new CachedEpochClock();

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(
            NODE_ID_BITS_DEFAULT, SEQUENCE_BITS_DEFAULT, nodeId, timestampOffset, clock);
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
        final int maxSequence = 1023;
        final EpochClock clock = () -> clockCounter.getAndIncrement() <= maxSequence ? 1L : 2L;

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(
            NODE_ID_BITS_DEFAULT, SEQUENCE_BITS_DEFAULT, nodeId, timestampOffset, clock);
        clockCounter.set(0);

        for (int i = 0; i <= maxSequence; i++)
        {
            final long id = idGenerator.nextId();

            assertEquals(1L, idGenerator.extractTimestamp(id));
            assertEquals(nodeId, idGenerator.extractNodeId(id));
            assertEquals(i, idGenerator.extractSequence(id));
        }

        final Thread thread = new Thread(() -> generatedId.set(idGenerator.nextId()));
        thread.setDaemon(true);
        thread.start();

        try
        {
            thread.join();

            assertEquals(2L, idGenerator.extractTimestamp(generatedId.get()));
            assertEquals(nodeId, idGenerator.extractNodeId(generatedId.get()));
            assertEquals(0L, idGenerator.extractSequence(generatedId.get()));
        }
        catch (final InterruptedException ex)
        {
            thread.interrupt();
            throw ex;
        }
    }
}
