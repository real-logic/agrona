/*
 * Copyright 2014-2023 Real Logic Limited.
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

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressMeta;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.JJJ_Result;

import static java.nio.ByteBuffer.allocateDirect;
import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer.MIN_CAPACITY;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;

/**
 * Concurrent tests for {@link ManyToOneRingBuffer} class.
 */
public class ManyToOneRingBufferTests
{
    /**
     * Common set of annotation for write tests.
     */
    @Outcome(id = "0, 5", expect = Expect.ACCEPTABLE, desc = "reader -> writer2 -> writer1")
    @Outcome(id = "0, 16", expect = Expect.ACCEPTABLE, desc = "reader -> writer1 -> writer2")
    @Outcome(id = "5, 16", expect = Expect.ACCEPTABLE, desc = "writer1 -> reader -> writer2")
    @Outcome(id = "16, 5", expect = Expect.ACCEPTABLE, desc = "writer2 -> reader -> writer1")
    @Outcome(id = "16, 0", expect = Expect.ACCEPTABLE, desc = "writer1 -> writer2 -> reader")
    @Outcome(id = "5, 0", expect = Expect.ACCEPTABLE, desc = "writer2 -> writer1 -> reader")
    public static class WriteTest
    {
    }

    /**
     * Test for {@link ManyToOneRingBuffer#write(int, DirectBuffer, int, int)} method.
     */
    @JCStressTest
    @JCStressMeta(WriteTest.class)
    @State
    public static class Write
    {
        private static final int MSG_TYPE_ID = 7;
        private final ManyToOneRingBuffer ringBuffer = new ManyToOneRingBuffer(new UnsafeBuffer(allocateDirect(1024)));
        private final ExpandableArrayBuffer srcBuffer = new ExpandableArrayBuffer();

        /**
         * Initialize source data.
         */
        public Write()
        {
            srcBuffer.putInt(0, 5);
            srcBuffer.putInt(SIZE_OF_LONG, 16);
        }

        /**
         * First writer thread.
         */
        @Actor
        public void writer1()
        {
            ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, SIZE_OF_INT);
        }

        /**
         * Second writer thread.
         */
        @Actor
        public void writer2()
        {
            ringBuffer.write(MSG_TYPE_ID, srcBuffer, SIZE_OF_LONG, SIZE_OF_INT);
        }

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void reader(final II_Result result)
        {
            ringBuffer.read((msgTypeId, buffer, index, length) -> result.r1 = buffer.getInt(index));
        }

        /**
         * Arbiter thread to verify contents of the buffer.
         *
         * @param result object.
         */
        @Arbiter
        public void arbiter(final II_Result result)
        {
            ringBuffer.read((msgTypeId, buffer, index, length) -> result.r2 = buffer.getInt(index));
        }
    }

    /**
     * Test for {@link ManyToOneRingBuffer#tryClaim(int, int)} followed by {@link ManyToOneRingBuffer#commit(int)}.
     */
    @JCStressTest
    @JCStressMeta(WriteTest.class)
    @State
    public static class TryClaimCommit
    {
        private static final int MSG_TYPE_ID = 11;
        private final ManyToOneRingBuffer ringBuffer = new ManyToOneRingBuffer(new UnsafeBuffer(allocateDirect(1024)));

        /**
         * First writer thread.
         */
        @Actor
        public void writer1()
        {
            final int index = ringBuffer.tryClaim(MSG_TYPE_ID, SIZE_OF_INT);
            ringBuffer.buffer().putInt(index, 5);
            ringBuffer.commit(index);
        }

        /**
         * Second writer thread.
         */
        @Actor
        public void writer2()
        {
            final int index = ringBuffer.tryClaim(MSG_TYPE_ID, SIZE_OF_INT);
            ringBuffer.buffer().putInt(index, 16);
            ringBuffer.commit(index);
        }

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void reader(final II_Result result)
        {
            ringBuffer.read((msgTypeId, buffer, index, length) -> result.r1 = buffer.getInt(index));
        }

        /**
         * Arbiter thread to verify contents of the buffer.
         *
         * @param result object.
         */
        @Arbiter
        public void arbiter(final II_Result result)
        {
            ringBuffer.read((msgTypeId, buffer, index, length) -> result.r2 = buffer.getInt(index));
        }
    }

    /**
     * Test for {@link ManyToOneRingBuffer#tryClaim(int, int)} followed by {@link ManyToOneRingBuffer#abort(int)}.
     */
    @JCStressTest
    @JCStressMeta(WriteTest.class)
    @State
    public static class TryClaimAbort
    {
        private static final int MSG_TYPE_ID = 19;
        private final ManyToOneRingBuffer ringBuffer = new ManyToOneRingBuffer(new UnsafeBuffer(allocateDirect(1024)));

        /**
         * First writer thread.
         */
        @Actor
        public void writer1()
        {
            int index = ringBuffer.tryClaim(MSG_TYPE_ID, SIZE_OF_LONG);
            ringBuffer.buffer().putLong(index, -1);
            ringBuffer.abort(index);

            index = ringBuffer.tryClaim(MSG_TYPE_ID, SIZE_OF_INT);
            ringBuffer.buffer().putInt(index, 5);
            ringBuffer.commit(index);
        }

        /**
         * Second writer thread.
         */
        @Actor
        public void writer2()
        {
            int index = ringBuffer.tryClaim(MSG_TYPE_ID, 32);
            ringBuffer.buffer().putLong(index, Long.MAX_VALUE);
            ringBuffer.abort(index);

            index = ringBuffer.tryClaim(MSG_TYPE_ID, SIZE_OF_INT);
            ringBuffer.buffer().putInt(index, 16);
            ringBuffer.commit(index);
        }

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void reader(final II_Result result)
        {
            ringBuffer.read((msgTypeId, buffer, index, length) -> result.r1 = buffer.getInt(index));
        }

        /**
         * Arbiter thread to verify contents of the buffer.
         *
         * @param result object.
         */
        @Arbiter
        public void arbiter(final II_Result result)
        {
            ringBuffer.read((msgTypeId, buffer, index, length) -> result.r2 = buffer.getInt(index));
        }
    }

    /**
     * Test for {@link ManyToOneRingBuffer#nextCorrelationId()} method which must be thread safe.
     */
    @JCStressTest
    @Outcome(id = "0, 1, 2", expect = Expect.ACCEPTABLE, desc = "t1 -> t2")
    @Outcome(id = "1, 0, 2", expect = Expect.ACCEPTABLE, desc = "t2 -> t1")
    @State
    public static class CorrelationId
    {
        private final ManyToOneRingBuffer ringBuffer =
            new ManyToOneRingBuffer(new UnsafeBuffer(allocateDirect(MIN_CAPACITY + TRAILER_LENGTH)));

        /**
         * First thread.
         *
         * @param result object.
         */
        @Actor
        public void actor1(final JJJ_Result result)
        {
            result.r1 = ringBuffer.nextCorrelationId();
        }

        /**
         * Second thread.
         *
         * @param result object.
         */
        @Actor
        public void actor2(final JJJ_Result result)
        {
            result.r2 = ringBuffer.nextCorrelationId();
        }

        /**
         * Arbiter thread.
         *
         * @param result object.
         */
        @Arbiter
        public void arbiter(final JJJ_Result result)
        {
            result.r3 = ringBuffer.nextCorrelationId();
        }
    }
}
