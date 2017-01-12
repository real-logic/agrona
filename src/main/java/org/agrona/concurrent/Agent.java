/*
 * Copyright 2014 - 2015 Real Logic Ltd.
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
package org.agrona.concurrent;

/**
 * An Agent is scheduled to do work on a thread on a duty cycle. Each Agent should have a defined role in a system.
 */
public interface Agent
{
    /**
     * An agent should implement this method to do its work.
     *
     * The return value is used for implementing a backoff strategy that can be employed when no work is
     * currently available for the agent to process.
     *
     * @throws java.lang.Exception if an error has occurred
     * @return 0 to indicate no work was currently available, a positive value otherwise.
     */
    int doWork() throws Exception;

    /**
     * To be overridden by Agents that need to do resource cleanup on close.
     *
     * This method will be called after the agent thread has terminated. It will only be called once by a single thread.
     *
     * <b>Note:</b> Implementations of this method must be idempotent.
     */
    default void onClose()
    {
        // default to do nothing unless you want to handle the notification.
    }

    /**
     * Get the name of this agent's role.
     *
     * @return the name of this agent's role.
     */
    String roleName();
}
