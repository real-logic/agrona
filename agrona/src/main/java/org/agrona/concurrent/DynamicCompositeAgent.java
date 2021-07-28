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
package org.agrona.concurrent;

import org.agrona.collections.ArrayUtil;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Group several {@link Agent}s into one composite, so they can be scheduled as a unit.
 * <p>
 * {@link Agent}s can be dynamically added and removed.
 * <p>
 * <b>Note:</b> This class is threadsafe for add and remove.
 */
public class DynamicCompositeAgent implements Agent
{
    /**
     * {@link Enum} to indicate the current status of a {@link DynamicCompositeAgent}.
     */
    public enum Status
    {
        /**
         * Agent is being initialised and has not yet been started.
         */
        INIT,

        /**
         * Agent is not active after a successful {@link #onStart()}
         */
        ACTIVE,

        /**
         * Agent has been closed.
         */
        CLOSED
    }

    private static final Agent[] EMPTY_AGENTS = new Agent[0];

    private int agentIndex = 0;
    private volatile Status status = Status.INIT;
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
            Objects.requireNonNull(agent, "agent cannot be null");
            this.agents[i++] = agent;
        }
    }

    /**
     * Get the {@link Status} for the Agent.
     *
     * @return the {@link Status} for the Agent.
     */
    public Status status()
    {
        return status;
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
            Objects.requireNonNull(agent, "agent cannot be null");
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

        status = Status.ACTIVE;
    }

    /**
     *  {@inheritDoc}
     */
    public int doWork() throws Exception
    {
        int workCount = 0;

        final Agent agentToAdd = addAgent.get();
        if (null != agentToAdd)
        {
            add(agentToAdd);
        }

        final Agent agentToRemove = removeAgent.get();
        if (null != agentToRemove)
        {
            remove(agentToRemove);
        }

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
     * suppressed exceptions in the thrown exception.
     */
    public void onClose()
    {
        status = Status.CLOSED;

        RuntimeException ce = null;
        for (final Agent agent : agents)
        {
            try
            {
                agent.onClose();
            }
            catch (final Throwable ex)
            {
                if (ce == null)
                {
                    ce = new RuntimeException(getClass().getName() + ": underlying agent error on close");
                }

                ce.addSuppressed(ex);
            }
        }

        agents = EMPTY_AGENTS;

        if (ce != null)
        {
            throw ce;
        }
    }

    /**
     *  {@inheritDoc}
     */
    public String roleName()
    {
        return roleName;
    }

    /**
     * Try and add a new {@link Agent} to the composite. This method does not block and will return false if another
     * concurrent attempt to add is in progress.
     * <p>
     * The agent will be added during the next invocation of {@link #doWork()} if this operation is successful.
     * If the {@link Agent#onStart()} method throws an exception then it will not be added and {@link Agent#onClose()}
     * will be called.
     *
     * @param agent to be added to the composite.
     * @return true is a successful add request is pending otherwise false if another concurrent add request is in
     * progress.
     * @see #hasAddAgentCompleted()
     */
    public boolean tryAdd(final Agent agent)
    {
        Objects.requireNonNull(agent, "agent cannot be null");

        if (Status.ACTIVE != status)
        {
            throw new IllegalStateException("add called when not active");
        }

        return addAgent.compareAndSet(null, agent);
    }

    /**
     * Has the last successful {@link #tryAdd(Agent)} operation been processed in the {@link #doWork()} cycle?
     *
     * @return the last successful {@link #tryAdd(Agent)} operation been processed in the {@link #doWork()} cycle?
     * @see #tryAdd(Agent)
     */
    public boolean hasAddAgentCompleted()
    {
        if (Status.ACTIVE != status)
        {
            throw new IllegalStateException("agent is not active");
        }

        return null == addAgent.get();
    }

    /**
     * Try and remove an {@link Agent} from the composite. The agent is removed during the next {@link #doWork()}
     * duty cycle if this operation is successful. This method does not block and will return false if another
     * concurrent attempt to remove is in progress.
     * <p>
     * The {@link Agent} is removed by identity. Only the first found is removed.
     *
     * @param agent to be removed.
     * @return true is a successful remove request is pending otherwise false if another concurrent remove request
     * is in progress.
     * @see #hasRemoveAgentCompleted()
     */
    public boolean tryRemove(final Agent agent)
    {
        Objects.requireNonNull(agent, "agent cannot be null");

        if (Status.ACTIVE != status)
        {
            throw new IllegalStateException("remove called when not active");
        }

        return removeAgent.compareAndSet(null, agent);
    }

    /**
     * Has the last {@link #tryRemove(Agent)} operation been processed in the {@link #doWork()} cycle?
     *
     * @return the last {@link #tryRemove(Agent)} operation been processed in the {@link #doWork()} cycle?
     * @see #tryRemove(Agent)
     */
    public boolean hasRemoveAgentCompleted()
    {
        if (Status.ACTIVE != status)
        {
            throw new IllegalStateException("agent is not active");
        }

        return null == removeAgent.get();
    }

    private void add(final Agent agent)
    {
        addAgent.lazySet(null);

        try
        {
            agent.onStart();
        }
        catch (final Throwable ex)
        {
            agent.onClose();
            throw ex;
        }

        agents = ArrayUtil.add(agents, agent);
    }

    private void remove(final Agent agent)
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
}
