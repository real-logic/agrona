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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CompositeAgentTest
{
    static class AgentException extends RuntimeException
    {
        private static final long serialVersionUID = 104952591506361027L;

        final int index;

        AgentException(final int index)
        {
            this.index = index;
        }
    }

    private final Agent[] agents = new Agent[]{ mock(Agent.class), mock(Agent.class), mock(Agent.class) };

    @Test
    public void shouldNotAcceptEmptyList()
    {
        assertThrows(IllegalArgumentException.class, CompositeAgent::new);
    }

    @Test
    public void shouldNotAcceptNullAgents()
    {
        assertThrows(NullPointerException.class, () -> new CompositeAgent(agents[0], null, agents[1]));
    }

    @Test
    public void shouldApplyLifecycleToAll() throws Exception
    {
        final CompositeAgent compositeAgent = new CompositeAgent(agents[0], agents[1], agents[2]);

        compositeAgent.onStart();
        for (final Agent agent : agents)
        {
            verify(agent).onStart();
        }

        compositeAgent.doWork();
        for (final Agent agent : agents)
        {
            verify(agent).doWork();
        }

        compositeAgent.onClose();
        for (final Agent agent : agents)
        {
            verify(agent).onClose();
        }
    }

    @Test
    public void shouldApplyLifecycleToAllDespiteExceptions() throws Exception
    {
        final CompositeAgent compositeAgent = new CompositeAgent(agents[0], agents[1], agents[2]);

        for (int i = 0; i < agents.length; i++)
        {
            final int index = i;
            final Agent agent = agents[index];
            doThrow(new AgentException(index)).when(agent).onStart();
        }

        try
        {
            compositeAgent.onStart();
        }
        catch (final Exception e)
        {
            for (final Throwable suppressed : e.getSuppressed())
            {
                assertTrue(suppressed instanceof AgentException);
            }
        }

        for (final Agent agent : agents)
        {
            verify(agent).onStart();
        }

        for (int i = 0; i < agents.length; i++)
        {
            final int index = i;
            final Agent agent = agents[index];
            doThrow(new AgentException(index)).when(agent).doWork();
        }

        for (int i = 0; i < agents.length; i++)
        {
            try
            {
                compositeAgent.doWork();
            }
            catch (final AgentException e)
            {
                assertEquals(i, e.index);
            }
        }

        for (final Agent agent : agents)
        {
            verify(agent).doWork();
        }

        for (int i = 0; i < agents.length; i++)
        {
            final int index = i;
            final Agent agent = agents[index];
            doThrow(new AgentException(index)).when(agent).onClose();
        }

        try
        {
            compositeAgent.onClose();
        }
        catch (final Exception e)
        {
            for (final Throwable suppressed : e.getSuppressed())
            {
                assertTrue(suppressed instanceof AgentException);
            }
        }

        for (final Agent agent : agents)
        {
            verify(agent).onClose();
        }
    }
}
