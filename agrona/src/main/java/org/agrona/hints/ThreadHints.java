/*
 * Copyright 2014-2024 Real Logic Limited.
 * Copyright 2016 Gil Tene
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
package org.agrona.hints;

import org.agrona.SystemUtil;

/**
 * This class captures possible hints that may be used by some
 * runtimes to improve code performance. It is intended to capture hinting
 * behaviours that are implemented in or anticipated to be spec'ed under the
 * {@link java.lang.Thread} class in some Java SE versions, but missing in prior
 * versions.
 */
public final class ThreadHints
{
    /**
     * Set this system property to true to disable {@link #onSpinWait()}.
     */
    public static final String DISABLE_ON_SPIN_WAIT_PROP_NAME = "org.agrona.hints.disable.onSpinWait";

    private static final boolean ON_SPIN_WAIT_ENABLED =
        !"true".equals(SystemUtil.getProperty(DISABLE_ON_SPIN_WAIT_PROP_NAME));

    private ThreadHints()
    {
    }

    /**
     * Indicates that the caller is momentarily unable to progress, until the
     * occurrence of one or more actions on the part of other activities.  By
     * invoking this method within each iteration of a spin-wait loop construct,
     * the calling thread indicates to the runtime that it is busy-waiting. The runtime
     * may take action to improve the performance of invoking spin-wait loop constructions.
     */
    public static void onSpinWait()
    {
        if (ON_SPIN_WAIT_ENABLED)
        {
            Thread.onSpinWait();
        }
    }
}
