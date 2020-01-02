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
package org.agrona.concurrent.errors;

/**
 * Callback handler for consuming errors encountered in a {@link DistinctErrorLog}.
 */
@FunctionalInterface
public interface ErrorConsumer
{
    /**
     * Callback for accepting errors encountered in the log.
     *
     * @param observationCount          the number of times this distinct exception has been recorded.
     * @param firstObservationTimestamp time the first observation was recorded.
     * @param lastObservationTimestamp  time the last observation was recorded.
     * @param encodedException          String encoding of the exception and stack trace in UTF-8 format.
     */
    void accept(
        int observationCount, long firstObservationTimestamp, long lastObservationTimestamp, String encodedException);
}
