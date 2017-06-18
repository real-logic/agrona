/*
 * Copyright 2014-2017 Real Logic Ltd.
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

import java.util.List;
import java.util.Objects;

/**
 * Group several {@link Agent}s into one composite so they can be scheduled as a unit.
 */
public class CompositeAgent implements Agent
{
    private final Agent[] agents;
    private final String roleName;

    /**
     * @param agents the parts of this composite, at least one agent and no null agents allowed
     * @throws IllegalArgumentException if an empty array of agents is provided
     * @throws NullPointerException     if the array or any element is null
     */
    public CompositeAgent(final List<? extends Agent> agents)
    {
        this(agents.toArray(new Agent[agents.size()]));
    }

    /**
     * @param agents the parts of this composite, at least one agent and no null agents allowed
     * @throws IllegalArgumentException if an empty array of agents is provided
     * @throws NullPointerException     if the array or any element is null
     */
    public CompositeAgent(final Agent... agents)
    {
        this.agents = new Agent[agents.length];

        final StringBuilder sb = new StringBuilder(agents.length * 16);
        sb.append('[');
        int i = 0;
        for (final Agent agent : agents)
        {
            Objects.requireNonNull(agent, "Agent cannot be null");
            sb.append(agent.roleName()).append(',');
            this.agents[i++] = agent;
        }

        sb.setCharAt(sb.length() - 1, ']');
        roleName = sb.toString();
    }

    public String roleName()
    {
        return roleName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that one agent throwing an exception on start may result in other agents not being started.
     */
    public void onStart()
    {
        for (final Agent agent : agents)
        {
            agent.onStart();
        }
    }

    public int doWork() throws Exception
    {
        int workCount = 0;

        for (final Agent agent : agents)
        {
            workCount += agent.doWork();
        }

        return workCount;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that one agent throwing an exception on close may result in other agents not being closed.
     */
    public void onClose()
    {
        for (final Agent agent : agents)
        {
            agent.onClose();
        }
    }
}
