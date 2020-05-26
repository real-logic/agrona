/*
 * Copyright 2014-2020 Real Logic Limited.
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

import org.junit.After;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayQueuePeekTest
{
    private static final int REPETITIONS = 10_000_000;
    private static final int QUEUE_CAPACITY = 128;

    private final AbstractConcurrentArrayQueue<Integer> queue = new ManyToManyConcurrentArrayQueue<>(QUEUE_CAPACITY);
    private final Producer producer = new Producer(queue);

    private Thread producerThread;

    @After
    public void after()
    {
        if (null != producerThread)
        {
            producerThread.interrupt();
        }
    }

    @Test
    @Timeout(10)
    public void shouldPeekThenPollSameElement()
    {
        producerThread = new Thread(producer);
        producerThread.start();

        for (int i = 0; i < REPETITIONS; i++)
        {
            Integer element;
            while (null == (element = queue.peek()))
            {
                Thread.yield();
                if (Thread.interrupted())
                {
                    return;
                }
            }

            assertEquals(element, queue.poll());
        }
    }

    static class Producer implements Runnable
    {
        private final Queue<Integer> queue;

        Producer(final Queue<Integer> queue)
        {
            this.queue = queue;
        }

        public void run()
        {
            for (int i = 0; i < REPETITIONS; i++)
            {
                while (!queue.offer(i))
                {
                    Thread.yield();
                    if (Thread.interrupted())
                    {
                        return;
                    }
                }
            }
        }
    }
}
