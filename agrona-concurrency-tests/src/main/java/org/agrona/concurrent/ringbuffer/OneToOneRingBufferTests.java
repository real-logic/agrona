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
package org.agrona.concurrent.ringbuffer;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.IJ_Result;
import org.openjdk.jcstress.infra.results.JJJ_Result;

import static java.nio.ByteBuffer.allocateDirect;
import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.agrona.concurrent.ringbuffer.OneToOneRingBuffer.MIN_CAPACITY;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;

/**
 * Concurrent tests for {@link OneToOneRingBuffer} class.
 */
public class OneToOneRingBufferTests
{
    OneToOneRingBufferTests()
    {
    }

    /**
     * Test for the {@link OneToOneRingBuffer#write(int, DirectBuffer, int, int)} method.
     */
    @JCStressTest
    @Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Reader before writer")
    @Outcome(id = "42, -1", expect = Expect.ACCEPTABLE, desc = "Writer before reader")
    @State
    public static class Write
    {
        private static final int MSG_TYPE = 888;
        private final OneToOneRingBuffer ringBuffer =
            new OneToOneRingBuffer(new UnsafeBuffer(allocateDirect(TRAILER_LENGTH + 128)));

        private final ExpandableArrayBuffer srcBuffer = new ExpandableArrayBuffer();

        /**
         * Init.
         */
        public Write()
        {
            srcBuffer.putInt(0, -1);
            srcBuffer.putInt(SIZE_OF_INT, 42);
            srcBuffer.putLong(SIZE_OF_INT * 2, -1L);
        }

        /**
         * Producer thread.
         */
        @Actor
        public void producer()
        {
            ringBuffer.write(MSG_TYPE, srcBuffer, SIZE_OF_INT, SIZE_OF_INT + SIZE_OF_LONG);
        }

        /**
         * Consumer thread.
         *
         * @param result object.
         */
        @Actor
        public void consumer(final IJ_Result result)
        {
            ringBuffer.read((msgTypeId, buffer, index, length) ->
            {
                result.r1 = buffer.getInt(index);
                result.r2 = buffer.getLong(index + SIZE_OF_INT);
            });
        }
    }

    /**
     * Test for when a writing thread can only succeed if reader read an existing message
     */
    @JCStressTest
    @Outcome(id = "19, 0", expect = Expect.ACCEPTABLE, desc = "Write before read")
    @Outcome(id = "19, 42", expect = Expect.ACCEPTABLE, desc = "Read before write")
    @Outcome(id = "42, 0", expect = Expect.ACCEPTABLE, desc = "Write in the middle of the read")
    @State
    public static class WriteFullBuffer
    {
        private static final int MSG_TYPE = 7;
        private final OneToOneRingBuffer ringBuffer = new OneToOneRingBuffer(
            new UnsafeBuffer(allocateDirect(TRAILER_LENGTH + 32)));

        private final ExpandableArrayBuffer srcBuffer = new ExpandableArrayBuffer();

        /**
         * Init.
         */
        public WriteFullBuffer()
        {
            srcBuffer.putInt(0, 19);
            srcBuffer.putInt(SIZE_OF_INT, 42);
            ringBuffer.write(MSG_TYPE, srcBuffer, 0, SIZE_OF_INT);
        }

        /**
         * Producer thread.
         */
        @Actor
        public void producer()
        {
            ringBuffer.write(MSG_TYPE, srcBuffer, SIZE_OF_INT, SIZE_OF_INT);
        }

        /**
         * Consumer thread.
         *
         * @param result object.
         */
        @Actor
        public void consumer(final II_Result result)
        {
            ringBuffer.read((msgTypeId, buffer, index, length) -> result.r1 = buffer.getInt(index));
        }

        /**
         * Verify contents of the buffer.
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
     * Test using {@link OneToOneRingBuffer#tryClaim(int, int)} and {@link OneToOneRingBuffer#commit(int)} methods
     * to write messages.
     */
    @JCStressTest
    @Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Reader before writer")
    @Outcome(id = "1, 19", expect = Expect.ACCEPTABLE, desc = "Writer before reader")
    @State
    public static class TryClaimCommit
    {
        private static final int MSG_TYPE = 42;

        private final OneToOneRingBuffer ringBuffer =
            new OneToOneRingBuffer(new UnsafeBuffer(allocateDirect(1024)));

        TryClaimCommit()
        {
        }

        /**
         * Producer thread.
         */
        @Actor
        public void producer()
        {
            final int index = ringBuffer.tryClaim(MSG_TYPE, 32);
            ringBuffer.buffer().putInt(index + 28, 19);
            ringBuffer.commit(index);
        }

        /**
         * Consumer thread.
         *
         * @param result object.
         */
        @Actor
        public void consumer(final II_Result result)
        {
            result.r1 = ringBuffer.read((msgTypeId, buffer, index, length) -> result.r2 = buffer.getInt(index + 28));
        }
    }

    /**
     * Test using a combination of {@link OneToOneRingBuffer#tryClaim(int, int)}
     * and {@link OneToOneRingBuffer#abort(int)} methods followed by a call to
     * {@link OneToOneRingBuffer#write(int, DirectBuffer, int, int)}.
     */
    @JCStressTest
    @Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Reader before writer")
    @Outcome(id = "1, 42", expect = Expect.FORBIDDEN, desc = "Old value observed")
    @Outcome(id = "1, 111", expect = Expect.ACCEPTABLE, desc = "New value observed")
    @State
    public static class TryClaimAbort
    {
        private static final int MSG_TYPE = 19;

        private final OneToOneRingBuffer ringBuffer = new OneToOneRingBuffer(new UnsafeBuffer(allocateDirect(1024)));
        private final ExpandableArrayBuffer srcBuffer = new ExpandableArrayBuffer();

        TryClaimAbort()
        {
        }

        /**
         * Producer thread.
         */
        @Actor
        public void producer()
        {
            final int index = ringBuffer.tryClaim(Integer.MAX_VALUE, SIZE_OF_INT);
            ringBuffer.buffer().putInt(index, 42);
            ringBuffer.abort(index);

            srcBuffer.putInt(0, 111);
            ringBuffer.write(MSG_TYPE, srcBuffer, 0, SIZE_OF_INT);
        }

        /**
         * Consumer thread.
         *
         * @param result object.
         */
        @Actor
        public void consumer(final II_Result result)
        {
            result.r1 = ringBuffer.read((msgTypeId, buffer, index, length) -> result.r2 = buffer.getInt(index));
        }
    }

    /**
     * Test for {@link OneToOneRingBuffer#nextCorrelationId()} method which must be thread safe.
     */
    @JCStressTest
    @Outcome(id = "0, 1, 2", expect = Expect.ACCEPTABLE, desc = "t1 -> t2")
    @Outcome(id = "1, 0, 2", expect = Expect.ACCEPTABLE, desc = "t2 -> t1")
    @State
    public static class CorrelationId
    {
        private final OneToOneRingBuffer ringBuffer =
            new OneToOneRingBuffer(new UnsafeBuffer(allocateDirect(MIN_CAPACITY + TRAILER_LENGTH)));

        CorrelationId()
        {
        }

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
