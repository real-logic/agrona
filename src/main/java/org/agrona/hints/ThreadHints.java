/*
 *  Copyright 2016 Gil Tene
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

package org.agrona.hints;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * This class captures possible hints that may be used by some
 * runtimes to improve code performance. It is intended to capture hinting
 * behaviors that are implemented in or anticipated to be spec'ed under the
 * java.lang.Thread class in some Java SE versions, but missing in prior
 * versions.
 */
public final class ThreadHints {

    private static final MethodHandle onSpinWaitMH;

    static {
        MethodHandle mh;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            mh = lookup.findStatic(java.lang.Thread.class, "onSpinWait", MethodType.methodType(void.class));
        } catch (Exception e) {
            mh = null;
        }
        onSpinWaitMH = mh;
    }

    // prevent construction...
    private ThreadHints() {
    }

    /** Indicates that the caller is momentarily unable to progress, until the
     * occurrence of one or more actions on the part of other activities.  By
     * invoking this method within each iteration of a spin-wait loop construct,
     * the calling thread indicates to the runtime that it is busy-waiting. The runtime
     * may take action to improve the performance of invoking spin-wait loop
     * constructions.
     */
    public static void onSpinWait() {
        // Call java.lang.Runtime.onSpinWait() on Java SE versions that support it. Do nothing otherwise.
        // This should optimize away to either nothing or to an inlining of java.lang.Runtime.onSpinWait()
        if (onSpinWaitMH != null) {
            try {
                onSpinWaitMH.invokeExact();
            } catch (Throwable throwable) {
                // Nothing to do here...
            }
        }
    }
}