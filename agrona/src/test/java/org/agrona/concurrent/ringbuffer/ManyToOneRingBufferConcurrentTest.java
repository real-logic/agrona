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
package org.agrona.concurrent.ringbuffer;

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CyclicBarrier;

import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.concurrent.ringbuffer.RingBuffer.INSUFFICIENT_CAPACITY;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ManyToOneRingBufferConcurrentTest
{
    private static final int MSG_TYPE_ID = 7;

    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect((16 * 1024) + TRAILER_LENGTH);
    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(byteBuffer);
    private final RingBuffer ringBuffer = new ManyToOneRingBuffer(unsafeBuffer);

    @Test
    public void shouldProvideCorrelationIds() throws Exception
    {
        final int reps = 10_000_000;
        final int numThreads = 2;
        final CyclicBarrier barrier = new CyclicBarrier(numThreads);
        final Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++)
        {
            threads[i] = new Thread(
                () ->
                {
                    try
                    {
                        barrier.await();
                    }
                    catch (final Exception ignore)
                    {
                    }

                    for (int r = 0; r < reps; r++)
                    {
                        ringBuffer.nextCorrelationId();
                    }
                });

            threads[i].start();
        }

        for (final Thread t : threads)
        {
            t.join();
        }

        assertEquals(reps * numThreads, ringBuffer.nextCorrelationId());
    }

    @Test
    public void shouldExchangeMessages() throws Exception
    {
        final int reps = 10_000_000;
        final int numProducers = 2;
        final CyclicBarrier barrier = new CyclicBarrier(numProducers + 1);
        final Thread[] threads = new Thread[numProducers];

        for (int i = 0; i < numProducers; i++)
        {
            threads[i] = new Thread(new Producer(i, barrier, reps));
            threads[i].start();
        }

        final int[] counts = new int[numProducers];

        final MessageHandler handler =
            (msgTypeId, buffer, index, length) ->
            {
                final int producerId = buffer.getInt(index);
                final int iteration = buffer.getInt(index + SIZE_OF_INT);

                final int count = counts[producerId];
                assertEquals(count, iteration);

                counts[producerId]++;
            };

        barrier.await();

        int msgCount = 0;
        while (msgCount < (reps * numProducers))
        {
            final int readCount = ringBuffer.read(handler);
            if (0 == readCount)
            {
                Thread.yield();
            }

            msgCount += readCount;
        }

        assertEquals(reps * numProducers, msgCount);

        for (final Thread t : threads)
        {
            t.join();
        }
    }

    @Test
    public void shouldExchangeMessagesViaTryClaimCommit() throws Exception
    {
        final int reps = 10_000_000;
        final int numProducers = 2;
        final CyclicBarrier barrier = new CyclicBarrier(numProducers + 1);
        final Thread[] threads = new Thread[numProducers];

        for (int i = 0; i < numProducers; i++)
        {
            threads[i] = new Thread(new ClaimCommit(i, barrier, reps));
            threads[i].start();
        }

        final int[] counts = new int[numProducers];

        final MessageHandler handler =
            (msgTypeId, buffer, index, length) ->
            {
                final int producerId = buffer.getInt(index);
                final int iteration = buffer.getInt(index + SIZE_OF_INT);

                final int count = counts[producerId];
                assertEquals(count, iteration);

                counts[producerId]++;
            };

        barrier.await();

        int msgCount = 0;
        while (msgCount < (reps * numProducers))
        {
            final int readCount = ringBuffer.read(handler);
            if (0 == readCount)
            {
                Thread.yield();
            }

            msgCount += readCount;
        }

        assertEquals(reps * numProducers, msgCount);

        for (final Thread t : threads)
        {
            t.join();
        }
    }

    @Test
    public void shouldExchangeMessagesViaTryClaimAbort() throws Exception
    {
        final int reps = 10_000_000;
        final int numProducers = 2;
        final CyclicBarrier barrier = new CyclicBarrier(numProducers + 1);
        final Thread[] threads = new Thread[numProducers];

        for (int i = 0; i < numProducers; i++)
        {
            threads[i] = new Thread(new ClaimAbort(i, barrier, reps));
            threads[i].start();
        }

        final int[] counts = new int[numProducers];

        final MessageHandler handler =
            (msgTypeId, buffer, index, length) ->
            {
                final int producerId = buffer.getInt(index);
                final int iteration = buffer.getInt(index + SIZE_OF_INT);

                final int count = counts[producerId];
                assertEquals(count, iteration);

                counts[producerId]++;
            };

        barrier.await();

        int msgCount = 0;
        while (msgCount < (reps * numProducers))
        {
            final int readCount = ringBuffer.read(handler);
            if (0 == readCount)
            {
                Thread.yield();
            }

            msgCount += readCount;
        }

        assertEquals(reps * numProducers, msgCount);

        for (final Thread t : threads)
        {
            t.join();
        }
    }

    class Producer implements Runnable
    {
        private final int producerId;
        private final CyclicBarrier barrier;
        private final int reps;

        Producer(final int producerId, final CyclicBarrier barrier, final int reps)
        {
            this.producerId = producerId;
            this.barrier = barrier;
            this.reps = reps;
        }

        public void run()
        {
            try
            {
                barrier.await();
            }
            catch (final Exception ignore)
            {
            }

            final int length = SIZE_OF_INT * 2;
            final int repsValueOffset = SIZE_OF_INT;
            final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

            srcBuffer.putInt(0, producerId);

            for (int i = 0; i < reps; i++)
            {
                srcBuffer.putInt(repsValueOffset, i);

                while (!ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, length))
                {
                    Thread.yield();
                }
            }
        }
    }

    class ClaimCommit implements Runnable
    {
        private final int producerId;
        private final CyclicBarrier barrier;
        private final int reps;

        ClaimCommit(final int producerId, final CyclicBarrier barrier, final int reps)
        {
            this.producerId = producerId;
            this.barrier = barrier;
            this.reps = reps;
        }

        public void run()
        {
            try
            {
                barrier.await();
            }
            catch (final Exception ignore)
            {
            }

            final int length = SIZE_OF_INT * 2;
            for (int i = 0; i < reps; i++)
            {
                int index = -1;
                try
                {
                    while (INSUFFICIENT_CAPACITY == (index = ringBuffer.tryClaim(MSG_TYPE_ID, length)))
                    {
                        Thread.yield();
                    }

                    final AtomicBuffer buffer = ringBuffer.buffer();
                    buffer.putInt(index, producerId);
                    buffer.putInt(index + SIZE_OF_INT, i);
                }
                finally
                {
                    ringBuffer.commit(index);
                }
            }
        }
    }

    class ClaimAbort implements Runnable
    {
        private final int producerId;
        private final CyclicBarrier barrier;
        private final int reps;

        ClaimAbort(final int producerId, final CyclicBarrier barrier, final int reps)
        {
            this.producerId = producerId;
            this.barrier = barrier;
            this.reps = reps;
        }

        public void run()
        {
            try
            {
                barrier.await();
            }
            catch (final Exception ignore)
            {
            }

            final int length = SIZE_OF_INT * 2;
            final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
            srcBuffer.putInt(0, producerId);

            for (int i = 0; i < reps; i++)
            {
                int claimIndex = -1;
                try
                {
                    while (INSUFFICIENT_CAPACITY == (claimIndex = ringBuffer.tryClaim(MSG_TYPE_ID, SIZE_OF_INT)))
                    {
                        Thread.yield();
                    }
                    ringBuffer.buffer().putInt(claimIndex, -i); // should be skipped
                }
                finally
                {
                    ringBuffer.abort(claimIndex);
                }

                srcBuffer.putInt(SIZE_OF_INT, i);
                while (!ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, length))
                {
                    Thread.yield();
                }
            }
        }
    }
}
