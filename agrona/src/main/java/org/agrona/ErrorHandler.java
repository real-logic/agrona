/*
 * Copyright 2014-2021 Real Logic Limited.
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
package org.agrona;

/**
 * Callback interface for handling an error/exception that has occurred when processing an operation or event.
 */
@FunctionalInterface
public interface ErrorHandler
{
    /**
     * Callback to notify of an error that has occurred when processing an operation or event.
     * <p>
     * This method is assumed non-throwing, so rethrowing the exception or triggering further exceptions would be a bug.
     *
     * @param throwable that occurred while processing an operation or event.
     */
    void onError(Throwable throwable);
}
