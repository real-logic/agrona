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

import org.agrona.collections.ArrayListUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Group several {@link Agent}s into one composite so they can be scheduled as a unit.
 * <p>
 * {@link Agent}s can be dynamically added and removed.
 * <p>
 * <b>Note:</b> This class is threadsafe for add and remove.
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class DynamicCompositeAgent implements Agent
{
    private final String roleName;
    private final ArrayList<Agent> agents = new ArrayList<>();
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
    }

    /**
     * @param roleName to be given for {@link Agent#roleName()}.
     * @param agents   the parts of this composite, at least one agent and no null agents allowed
     * @throws NullPointerException if the array or any element is null
     */
    public DynamicCompositeAgent(final String roleName, final List<? extends Agent> agents)
    {
        this.roleName = roleName;
        this.agents.addAll(agents);
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

            this.agents.add(agent);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that one agent throwing an exception on start may result in other agents not being started.
     */
    public void onStart()
    {
        for (int i = 0, size = agents.size(); i < size; i++)
        {
            agents.get(i).onStart();
        }
    }

    public int doWork() throws Exception
    {
        int workCount = 0;

        final Agent toBeAddedAgent = addAgent.get();
        if (null != toBeAddedAgent)
        {
            addAgent.set(null);
            toBeAddedAgent.onStart();
            agents.add(toBeAddedAgent);
        }

        final Agent toBeRemovedAgent = removeAgent.get();
        if (null != toBeRemovedAgent)
        {
            removeAgent.set(null);
            for (int i = 0, size = agents.size(); i < size; i++)
            {
                if (agents.get(i) == toBeRemovedAgent)
                {
                    ArrayListUtil.fastUnorderedRemove(agents, i);
                    toBeRemovedAgent.onClose();
                    break;
                }
            }
        }

        for (int i = 0, size = agents.size(); i < size; i++)
        {
            workCount += agents.get(i).doWork();
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
        for (int i = 0, size = agents.size(); i < size; i++)
        {
            agents.get(i).onClose();
        }

        agents.clear();
    }

    public String roleName()
    {
        return roleName;
    }

    /**
     * Add a new {@link Agent} to the composite. This method is lock-free.
     * <p>
     * The agent will be added during the next invocation of {@link #doWork()}.
     *
     * @param agent to be added to the composite.
     */
    public void add(final Agent agent)
    {
        if (agent == null)
        {
            throw new NullPointerException("Null agents is not supported");
        }

        while (!addAgent.compareAndSet(null, agent))
        {
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
     * Remove an {@link Agent} from the composite. This method blocks until the agent is added during the next
     * {@link #doWork()} duty cycle.
     * <p>
     * The {@link Agent} is removed by identity. Only the first found is removed.
     *
     * @param agent to be removed.
     */
    public void remove(final Agent agent)
    {
        if (null == agent)
        {
            throw new NullPointerException("Null agents is not supported");
        }

        while (!removeAgent.compareAndSet(null, agent))
        {
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
}
