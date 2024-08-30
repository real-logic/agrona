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

import org.agrona.BufferUtil;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;
import org.openjdk.jcstress.infra.results.J_Result;

/**
 * Set of concurrency tests for {@link UnsafeBuffer}.
 */
public class UnsafeBufferTests
{
    UnsafeBufferTests()
    {
    }

    /**
     * Test that verifies the atomicity of the {@link UnsafeBuffer#putLongVolatile(int, long)},
     * {@link UnsafeBuffer#putLongOrdered(int, long)} and {@link UnsafeBuffer#getLongVolatile(int)}.
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE, desc = "read before writes")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "putLongVolatile before read")
    @Outcome(id = "9223372036854775806", expect = Expect.ACCEPTABLE, desc = "putLongOrdered before read")
    @State
    public static class DirectBufferLong
    {
        private final UnsafeBuffer buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(8, 8));

        DirectBufferLong()
        {
        }

        /**
         * Writer thread.
         */
        @Actor
        public void putLongVolatile()
        {
            buffer.putLongVolatile(0, -1);
        }

        /**
         * Writer thread.
         */
        @Actor
        public void putLongOrdered()
        {
            buffer.putLongOrdered(0, Long.MAX_VALUE - 1);
        }

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void actor2(final J_Result result)
        {
            result.r1 = buffer.getLongVolatile(0);
        }
    }

    /**
     * Test that verifies the atomicity of the {@link UnsafeBuffer#putIntVolatile(int, int)},
     * {@link UnsafeBuffer#putIntOrdered(int, int)} and {@link UnsafeBuffer#getIntVolatile(int)}.
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE, desc = "read before writes")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "putIntVolatile before read")
    @Outcome(id = "222222222", expect = Expect.ACCEPTABLE, desc = "putIntOrdered before read")
    @State
    public static class DirectBufferInt
    {
        private final UnsafeBuffer buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(8, 8));

        DirectBufferInt()
        {
        }

        /**
         * Writer thread.
         */
        @Actor
        public void putIntVolatile()
        {
            buffer.putIntVolatile(4, -1);
        }

        /**
         * Writer thread.
         */
        @Actor
        public void putIntOrdered()
        {
            buffer.putIntOrdered(4, 222222222);
        }

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void actor2(final I_Result result)
        {
            result.r1 = buffer.getIntVolatile(4);
        }
    }
}
