/*
 * Copyright 2015 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.agrona.concurrent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Busy spin strategy targeted at lowest possible latency. This strategy will monopolise a thread to achieve the lowest
 * possible latency. Useful for creating bubbles in the execution pipeline of tight busy spin loops with no other logic than
 * status checks on progress.
 */
@SuppressWarnings("unused")
abstract class BusySpinIdleStrategyPrePad
{
    long pad01, pad02, pad03, pad04, pad05, pad06, pad07, pad08;
}

abstract class BusySpinIdleStrategyData extends BusySpinIdleStrategyPrePad
{
    protected int dummyCounter;
}

@SuppressWarnings("unused")
public final class BusySpinIdleStrategy extends BusySpinIdleStrategyData implements IdleStrategy
{
    long pad01, pad02, pad03, pad04, pad05, pad06, pad07, pad08;

    /**
     * <b>Note</b>: this implementation will result in no safepoint poll once inlined.
     *
     * @see uk.co.real_logic.agrona.concurrent.IdleStrategy#idle(int)
     */
    public void idle(final int workCount)
    {
        if (workCount > 0)
        {
            return;
        }
        idle();
    }

    public void idle()
    {
        // Trick speculative execution into not progressing
        if (dummyCounter > 0)
        {
            if (ThreadLocalRandom.current().nextInt() > 0)
            {
                --dummyCounter;
            }
        }
        else
        {
            dummyCounter = 64;
        }
    }

    public void reset()
    {
    }
}
