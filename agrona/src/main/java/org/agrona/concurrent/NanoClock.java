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

/**
 * Functional interface for return the current time as system-wide monotonic tick of 1 nanosecond precision.
 */
@FunctionalInterface
public interface NanoClock
{
    /**
     * The number of ticks in nanoseconds the clock has advanced since starting.
     * <p>
     * This method can only be used to measure elapsed time and is not related to any other notion of system or
     * wall-clock time. The value returned represents nanoseconds since some fixed but arbitrary origin time
     * (perhaps in the future, so values may be negative). The same origin is used by all invocations of this method
     * in an instance of a Java virtual machine; other virtual machine instances are likely to use a different origin.
     *
     * @return number of ticks in nanoseconds the clock has advanced since starting.
     * @see System#nanoTime()
     */
    long nanoTime();
}
