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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Group several {@link Agent}s into one composite so they can be scheduled as a unit.
 *
 * {@link Agent}s can be dynamically added and removed.
 *
 * <b>Note:</b> This class is threadsafe for add and remove.
 */
public class DynamicCompositeAgent implements Agent
{
    private static final Agent[] EMPTY_AGENTS = new Agent[0];
    private static final AtomicReferenceFieldUpdater<DynamicCompositeAgent, Agent[]> AGENTS_UPDATER =
        AtomicReferenceFieldUpdater.newUpdater(DynamicCompositeAgent.class, Agent[].class, "agents");

    private final String roleName;
    private volatile Agent[] agents;

    /**
     * Construct a new composite that has no {@link Agent}s to begin with.
     *
     * @param roleName to be given for {@link Agent#roleName()}.
     */
    public DynamicCompositeAgent(final String roleName)
    {
        this.roleName = roleName;
        agents = EMPTY_AGENTS;
    }

    /**
     * @param roleName to be given for {@link Agent#roleName()}.
     * @param agents   the parts of this composite, at least one agent and no null agents allowed
     * @throws NullPointerException if the array or any element is null
     */
    public DynamicCompositeAgent(final String roleName, final List<? extends Agent> agents)
    {
        this(roleName, agents.toArray(new Agent[agents.size()]));
    }

    /**
     * @param roleName to be given for {@link Agent#roleName()}.
     * @param agents   the parts of this composite, at least one agent and no null agents allowed
     * @throws NullPointerException if the array or any element is null
     */
    public DynamicCompositeAgent(final String roleName, final Agent... agents)
    {
        this.roleName = roleName;

        if (agents == null)
        {
            throw new NullPointerException("Agents cannot be null");
        }

        for (final Agent agent : agents)
        {
            if (null == agent)
            {
                throw new NullPointerException("Nulls are not supported");
            }
        }

        this.agents = Arrays.copyOf(agents, agents.length);
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

    public void onClose()
    {
        for (final Agent agent : AGENTS_UPDATER.getAndSet(this, EMPTY_AGENTS))
        {
            agent.onClose();
        }
    }

    public String roleName()
    {
        return roleName;
    }

    /**
     * Add a new {@link Agent} to the composite. This method is lock-free.
     *
     * @param agent to be added to the composite.
     */
    public void add(final Agent agent)
    {
        if (agent == null)
        {
            throw new NullPointerException("Null agents is not supported");
        }

        Agent[] newAgents;
        Agent[] oldAgents;

        do
        {
            oldAgents = agents;
            final int oldLength = oldAgents.length;
            newAgents = new Agent[oldLength + 1];
            System.arraycopy(oldAgents, 0, newAgents, 0, oldLength);
            newAgents[oldLength] = agent;
        }
        while (!AGENTS_UPDATER.compareAndSet(this, oldAgents, newAgents));
    }

    /**
     * Remove an {@link Agent} from the composite. This method is lock-free.
     *
     * The {@link Agent} is removed by identity. Only the first found is removed.
     *
     * @param agent to be removed.
     * @return true if the agent was removed otherwise false.
     */
    public boolean remove(final Agent agent)
    {
        if (null == agent)
        {
            throw new NullPointerException("Null agents is not supported");
        }

        Agent[] newAgents;
        Agent[] oldAgents;

        do
        {
            oldAgents = agents;

            final int index = find(oldAgents, agent);
            if (-1 == index)
            {
                return false;
            }

            final int newLength = oldAgents.length - 1;
            newAgents = new Agent[newLength];

            System.arraycopy(oldAgents, 0, newAgents, 0, index);
            System.arraycopy(oldAgents, index + 1, newAgents, index, newLength - index);
        }
        while (!AGENTS_UPDATER.compareAndSet(this, oldAgents, newAgents));

        agent.onClose();

        return true;
    }

    private static int find(final Agent[] agents, final Agent agent)
    {
        for (int i = 0; i < agents.length; i++)
        {
            if (agent == agents[i])
            {
                return i;
            }
        }

        return -1;
    }
}
