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

import org.agrona.collections.MutableLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest
{
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
        final EpochClock clock = () -> clockCounter.getAndIncrement() <= SnowflakeIdGenerator.MAX_SEQUENCE ? 1L : 2L;

        final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(nodeId, timestampOffset, clock);
        clockCounter.set(0);

        for (int i = 0; i < SnowflakeIdGenerator.MAX_SEQUENCE; i++)
        {
            final long id = idGenerator.nextId();

            assertEquals(1L, extractTimestamp(id));
            assertEquals(nodeId, extractNodeId(id));
            assertEquals(i, extractSequence(id));
        }

        assertEquals(SnowflakeIdGenerator.MAX_SEQUENCE, extractSequence(idGenerator.nextId()));

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

    private static long extractTimestamp(final long id)
    {
        return id >>> (SnowflakeIdGenerator.NODE_ID_BITS + SnowflakeIdGenerator.SEQUENCE_BITS);
    }

    private static long extractNodeId(final long id)
    {
        return (id >>> SnowflakeIdGenerator.SEQUENCE_BITS) & (SnowflakeIdGenerator.MAX_NODE_ID);
    }

    private static long extractSequence(final long id)
    {
        return id & (SnowflakeIdGenerator.MAX_SEQUENCE);
    }
}
