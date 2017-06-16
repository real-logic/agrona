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

import org.agrona.collections.ArrayUtil;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Group several {@link Agent}s into one composite so they can be scheduled as a unit.
 * <p>
 * {@link Agent}s can be dynamically added and removed.
 * <p>
 * <b>Note:</b> This class is threadsafe for add and remove.
 */
public class DynamicCompositeAgent implements Agent
{
    private static final Agent TOMBSTONE = new Agent()
    {
        public int doWork() throws Exception
        {
            return 0;
        }

        public String roleName()
        {
            return "tombstone";
        }
    };

    private static final Agent[] EMPTY_AGENTS = new Agent[0];

    private Agent[] agents;
    private final String roleName;
    private final AtomicReference<Agent> addAgent = new AtomicReference<>();
    private final AtomicReference<Agent> removeAgent = new AtomicReference<>();

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
        this.roleName = roleName;
        this.agents = new Agent[agents.size()];

        int i = 0;
        for (final Agent agent : agents)
        {
            Objects.requireNonNull(agent, "Agent cannot be null");
            this.agents[i++] = agent;
        }
    }

    /**
     * @param roleName to be given for {@link Agent#roleName()}.
     * @param agents   the parts of this composite, at least one agent and no null agents allowed
     * @throws NullPointerException if the array or any element is null
     */
    public DynamicCompositeAgent(final String roleName, final Agent... agents)
    {
        this.roleName = roleName;
        this.agents = new Agent[agents.length];

        int i = 0;
        for (final Agent agent : agents)
        {
            Objects.requireNonNull(agent, "Agent cannot be null");
            this.agents[i++] = agent;
        }
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

        final Agent agentToAdd = addAgent.get();
        if (null != agentToAdd)
        {
            addAgent(agentToAdd);
        }

        final Agent agentToRemove = removeAgent.get();
        if (null != agentToRemove)
        {
            removeAgent(agentToRemove);
        }

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
        addAgent.lazySet(TOMBSTONE);
        removeAgent.lazySet(TOMBSTONE);

        for (final Agent agent : agents)
        {
            agent.onClose();
        }

        agents = EMPTY_AGENTS;
    }

    public String roleName()
    {
        return roleName;
    }

    /**
     * Add a new {@link Agent} to the composite.
     * <p>
     * The agent will be added during the next invocation of {@link #doWork()}. If the {@link Agent#onStart()}
     * method throws an exception then it will not be added and {@link Agent#onClose()} will be called.
     *
     * @param agent to be added to the composite.
     */
    public void add(final Agent agent)
    {
        Objects.requireNonNull(agent, "Agent cannot be null");

        while (!addAgent.compareAndSet(null, agent))
        {
            if (TOMBSTONE == addAgent.get())
            {
                throw new IllegalStateException("Add called after close");
            }

            Thread.yield();
        }
    }

    /**
     * Has the last {@link #add(Agent)} operation be processed in the {@link #doWork()} cycle?
     *
     * @return the last {@link #add(Agent)} operation be processed in the {@link #doWork()} cycle?
     */
    public boolean hasAddAgentCompleted()
    {
        return null == addAgent.get();
    }

    /**
     * Remove an {@link Agent} from the composite. The agent is removed during the next {@link #doWork()} duty cycle.
     * <p>
     * The {@link Agent} is removed by identity. Only the first found is removed.
     *
     * @param agent to be removed.
     */
    public void remove(final Agent agent)
    {
        Objects.requireNonNull(agent, "Agent cannot be null");

        while (!removeAgent.compareAndSet(null, agent))
        {
            if (TOMBSTONE == removeAgent.get())
            {
                throw new IllegalStateException("Remove called after close");
            }

            Thread.yield();
        }
    }

    /**
     * Has the last {@link #remove(Agent)} operation be processed in the {@link #doWork()} cycle?
     *
     * @return the last {@link #remove(Agent)} operation be processed in the {@link #doWork()} cycle?
     */
    public boolean hasRemoveAgentCompleted()
    {
        return null == removeAgent.get();
    }

    private void removeAgent(final Agent agent)
    {
        removeAgent.lazySet(null);

        final Agent[] newAgents = ArrayUtil.remove(agents, agent);

        try
        {
            if (newAgents != agents)
            {
                agent.onClose();
            }
        }
        finally
        {
            agents = newAgents;
        }
    }

    private void addAgent(final Agent agent)
    {
        addAgent.lazySet(null);

        try
        {
            agent.onStart();
        }
        catch (final RuntimeException ex)
        {
            agent.onClose();
            throw ex;
        }

        agents = ArrayUtil.add(agents, agent);
    }
}
