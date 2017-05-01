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

import java.util.Arrays;
import java.util.List;

/**
 * Group several {@link Agent}s into one composite so it can be scheduled as a unit.
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
        if (agents == null)
        {
            throw new NullPointerException("Expecting at least one Agent");
        }
        if (agents.length == 0)
        {
            throw new IllegalArgumentException("Expecting at least one Agent");
        }

        final StringBuilder sb = new StringBuilder(agents.length * 16);
        sb.append('[');
        for (final Agent agent : agents)
        {
            if (agent == null)
            {
                throw new NullPointerException("Agents list contains a null");
            }

            sb.append(agent.roleName());
            sb.append(',');
        }

        sb.setCharAt(sb.length() - 1, ']');
        roleName = sb.toString();

        this.agents = Arrays.copyOf(agents, agents.length);
    }

    public int doWork() throws Exception
    {
        int sum = 0;
        for (final Agent agent : agents)
        {
            sum += agent.doWork();
        }

        return sum;
    }

    public void onClose()
    {
        for (final Agent agent : agents)
        {
            agent.onClose();
        }
    }

    public String roleName()
    {
        return roleName;
    }
}
