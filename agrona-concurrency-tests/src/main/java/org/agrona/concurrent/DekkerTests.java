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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressMeta;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.JJJJ_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Set of concurrency tests for the Dekker's algorithm.
 */
public class DekkerTests
{

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    DekkerTests()
    {
    }

    /**
     * Expected states.
     */
    @Outcome(id = "0, 0, 0, 0", expect = Expect.ACCEPTABLE, desc = "read before write")
    @Outcome(id = "1, 8, 7, 1", expect = Expect.ACCEPTABLE, desc = "write before read")
    @Outcome(id = "1, -1, -1, 0", expect = Expect.ACCEPTABLE, desc = "aborted read")
    public static class ExpectedStates
    {
        ExpectedStates()
        {
        }
    }

    /**
     * Implementation based on volatile access.
     */
    @JCStressTest
    @JCStressMeta(ExpectedStates.class)
    @State
    public static class Volatile
    {
        private volatile long before;
        private volatile long after;
        private int x;
        private int y;

        Volatile()
        {
        }

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void reader(final JJJJ_Result result)
        {
            result.r4 = after;

            result.r2 = x;
            result.r3 = y;
            VarHandle.loadLoadFence();

            result.r1 = before;
            if (result.r1 != result.r4)
            {
                result.r2 = -1;
                result.r3 = -1;
            }
        }

        /**
         * Write thread.
         */
        @Actor
        public void writer()
        {
            final long changeNr = before + 1;
            before = changeNr;

            x = 8;
            y = 7;

            after = changeNr;
        }
    }

    /**
     * An implementation using {@link VarHandle} API and acquire/release semantics.
     */
    @JCStressTest
    @JCStressMeta(ExpectedStates.class)
    @State
    public static class VarHandlesAcquireRelease
    {
        private static final VarHandle BEFORE;
        private static final VarHandle AFTER;

        static
        {
            try
            {
                BEFORE = LOOKUP.findVarHandle(VarHandlesAcquireRelease.class, "before", long.class);
                AFTER = LOOKUP.findVarHandle(VarHandlesAcquireRelease.class, "after", long.class);
            }
            catch (final NoSuchFieldException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }

        private volatile long before;
        private volatile long after;
        private int x;
        private int y;

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void reader(final JJJJ_Result result)
        {
            result.r4 = (long)AFTER.getAcquire(this);

            result.r2 = x;
            result.r3 = y;
            VarHandle.loadLoadFence();

            result.r1 = (long)BEFORE.getAcquire(this);
            if (result.r1 != result.r4)
            {
                result.r2 = -1;
                result.r3 = -1;
            }
        }

        /**
         * Write thread.
         */
        @Actor
        public void writer()
        {
            final long changeNr = (long)BEFORE.get(this) + 1;
            BEFORE.setRelease(this, changeNr);
            VarHandle.storeStoreFence();

            x = 8;
            y = 7;

            AFTER.setRelease(this, changeNr);
        }
    }

    /**
     * An implementation using {@link VarHandle} API and acquire/release semantics with relaxed write barriers.
     */
    @JCStressTest
    @JCStressMeta(ExpectedStates.class)
    @State
    public static class VarHandlesAcquireReleasePlainWrite
    {
        private static final VarHandle BEFORE;
        private static final VarHandle AFTER;

        static
        {
            try
            {
                BEFORE = LOOKUP.findVarHandle(VarHandlesAcquireReleasePlainWrite.class, "before", long.class);
                AFTER = LOOKUP.findVarHandle(VarHandlesAcquireReleasePlainWrite.class, "after", long.class);
            }
            catch (final NoSuchFieldException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }

        private volatile long before;
        private volatile long after;
        private int x;
        private int y;

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void reader(final JJJJ_Result result)
        {
            result.r4 = (long)AFTER.getAcquire(this);

            result.r2 = x;
            result.r3 = y;
            VarHandle.loadLoadFence();

            result.r1 = (long)BEFORE.getAcquire(this);
            if (result.r1 != result.r4)
            {
                result.r2 = -1;
                result.r3 = -1;
            }
        }

        /**
         * Write thread.
         */
        @Actor
        public void writer()
        {
            final long changeNr = (long)BEFORE.get(this) + 1;
            BEFORE.set(this, changeNr);
            VarHandle.storeStoreFence();

            x = 8;
            y = 7;

            AFTER.setRelease(this, changeNr);
        }
    }

    /**
     * An implementation using {@link VarHandle} API and acquire/release semantics with relaxed write/read barriers.
     */
    @JCStressTest
    @JCStressMeta(ExpectedStates.class)
    @State
    public static class VarHandlesAcquireReleasePlainReadAndWrite
    {
        private static final VarHandle BEFORE;
        private static final VarHandle AFTER;

        static
        {
            try
            {
                BEFORE = LOOKUP.findVarHandle(VarHandlesAcquireReleasePlainReadAndWrite.class, "before", long.class);
                AFTER = LOOKUP.findVarHandle(VarHandlesAcquireReleasePlainReadAndWrite.class, "after", long.class);
            }
            catch (final NoSuchFieldException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }

        private volatile long before;
        private volatile long after;
        private int x;
        private int y;

        /**
         * Reader thread.
         *
         * @param result object.
         */
        @Actor
        public void reader(final JJJJ_Result result)
        {
            result.r4 = (long)AFTER.getAcquire(this);

            result.r2 = x;
            result.r3 = y;
            VarHandle.loadLoadFence();

            result.r1 = (long)BEFORE.get(this);
            if (result.r1 != result.r4)
            {
                result.r2 = -1;
                result.r3 = -1;
            }
        }

        /**
         * Write thread.
         */
        @Actor
        public void writer()
        {
            final long changeNr = (long)BEFORE.get(this) + 1;
            BEFORE.set(this, changeNr);
            VarHandle.storeStoreFence();

            x = 8;
            y = 7;

            AFTER.setRelease(this, changeNr);
        }
    }
}
