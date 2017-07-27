package org.agrona.concurrent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class CompositeAgentTest
{
    static class AgentException extends RuntimeException
    {
        final int index;

        AgentException(final int index)
        {
            this.index = index;
        }
    }

    private final Agent[] agents = new Agent[]{mock(Agent.class), mock(Agent.class), mock(Agent.class)};

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptEmptyList()
    {
        final CompositeAgent ignore = new CompositeAgent();
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullAgents()
    {
        final CompositeAgent ignore = new CompositeAgent(agents[0], null, agents[1]);
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