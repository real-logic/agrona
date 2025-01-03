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

import java.util.List;
import java.util.Objects;

/**
 * Group several {@link Agent}s into one composite, so they can be scheduled as a unit.
 */
public class CompositeAgent implements Agent
{
    private final Agent[] agents;
    private final String roleName;
    private int agentIndex = 0;

    /**
     * Construct a new composite with a given list {@link Agent}s to begin with.
     *
     * @param agents the parts of this composite, at least one agent and no null agents allowed
     * @throws IllegalArgumentException if an empty array of agents is provided
     * @throws NullPointerException     if the array or any element is null
     */
    public CompositeAgent(final List<? extends Agent> agents)
    {
        this(agents.toArray(new Agent[0]));
    }

    /**
     * Construct a new composite with a given list {@link Agent}s to begin with.
     *
     * @param agents the parts of this composite, at least one agent and no null agents allowed
     * @throws IllegalArgumentException if an empty array of agents is provided, or single agent provided
     * @throws NullPointerException     if the array or any element is null
     */
    public CompositeAgent(final Agent... agents)
    {
        if (agents.length == 0)
        {
            throw new IllegalArgumentException("requires at least one sub-agent");
        }

        this.agents = new Agent[agents.length];

        final StringBuilder sb = new StringBuilder(agents.length * 16);
        sb.append('[');
        int i = 0;
        for (final Agent agent : agents)
        {
            Objects.requireNonNull(agent, "agent cannot be null");
            sb.append(agent.roleName()).append(',');
            this.agents[i++] = agent;
        }

        sb.setCharAt(sb.length() - 1, ']');
        roleName = sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String roleName()
    {
        return roleName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that one agent throwing an exception on start will not prevent other agents from being started.
     *
     * @throws RuntimeException if any sub-agent throws an exception onStart. The agents exceptions are collected as
     *                          suppressed exceptions in the thrown exception.
     */
    public void onStart()
    {
        RuntimeException ce = null;
        for (final Agent agent : agents)
        {
            try
            {
                agent.onStart();
            }
            catch (final Exception ex)
            {
                if (ce == null)
                {
                    ce = new RuntimeException(getClass().getName() + ": underlying agent error on start");
                }
                ce.addSuppressed(ex);
            }
        }

        if (null != ce)
        {
            throw ce;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int doWork() throws Exception
    {
        int workCount = 0;

        final Agent[] agents = this.agents;
        while (agentIndex < agents.length)
        {
            final Agent agent = agents[agentIndex++];
            workCount += agent.doWork();
        }

        agentIndex = 0;

        return workCount;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that one agent throwing an exception on close will not prevent other agents from being closed.
     *
     * @throws RuntimeException if any sub-agent throws an exception onClose. The agents exceptions are collected as
     *                          suppressed exceptions in the thrown exception.
     */
    public void onClose()
    {
        RuntimeException ce = null;
        for (final Agent agent : agents)
        {
            try
            {
                agent.onClose();
            }
            catch (final Exception ex)
            {
                if (ce == null)
                {
                    ce = new RuntimeException(getClass().getName() + ": underlying agent error on close");
                }

                ce.addSuppressed(ex);
            }
        }

        if (null != ce)
        {
            throw ce;
        }
    }
}
