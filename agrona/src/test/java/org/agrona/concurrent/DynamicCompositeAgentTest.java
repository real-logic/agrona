/*
 * Copyright 2014-2022 Real Logic Limited.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DynamicCompositeAgentTest
{
    private static final String ROLE_NAME = "roleName";

    @Test
    public void shouldNotAllowAddAfterClose()
    {
        final DynamicCompositeAgent compositeAgent = new DynamicCompositeAgent(ROLE_NAME);
        final AgentInvoker invoker = new AgentInvoker(Throwable::printStackTrace, null, compositeAgent);

        invoker.close();

        assertThrows(IllegalStateException.class, () -> compositeAgent.tryAdd(mock(Agent.class)));
    }

    @Test
    public void shouldNotAllowRemoveAfterClose()
    {
        final DynamicCompositeAgent compositeAgent = new DynamicCompositeAgent(ROLE_NAME);
        final AgentInvoker invoker = new AgentInvoker(Throwable::printStackTrace, null, compositeAgent);

        invoker.close();

        assertThrows(IllegalStateException.class, () -> compositeAgent.tryRemove(mock(Agent.class)));
    }

    @Test
    public void shouldAddAgent() throws Exception
    {
        final Agent mockAgentOne = mock(Agent.class);

        final DynamicCompositeAgent compositeAgent = new DynamicCompositeAgent(ROLE_NAME, mockAgentOne);
        final AgentInvoker invoker = new AgentInvoker(Throwable::printStackTrace, null, compositeAgent);

        assertThat(compositeAgent.roleName(), is(ROLE_NAME));
        invoker.start();
        verify(mockAgentOne, times(1)).onStart();

        invoker.invoke();
        verify(mockAgentOne, times(1)).onStart();
        verify(mockAgentOne, times(1)).doWork();

        final Agent mockAgentTwo = mock(Agent.class);
        assertTrue(compositeAgent.tryAdd(mockAgentTwo));
        assertFalse(compositeAgent.hasAddAgentCompleted());

        invoker.invoke();
        assertTrue(compositeAgent.hasAddAgentCompleted());
        verify(mockAgentOne, times(1)).onStart();
        verify(mockAgentOne, times(2)).doWork();
        verify(mockAgentTwo, times(1)).onStart();
        verify(mockAgentTwo, times(1)).doWork();
    }

    @Test
    public void shouldRemoveAgent() throws Exception
    {
        final Agent mockAgentOne = mock(Agent.class);
        final Agent mockAgentTwo = mock(Agent.class);

        final DynamicCompositeAgent compositeAgent = new DynamicCompositeAgent(ROLE_NAME, mockAgentOne, mockAgentTwo);
        final AgentInvoker invoker = new AgentInvoker(Throwable::printStackTrace, null, compositeAgent);
        invoker.start();
        verify(mockAgentOne, times(1)).onStart();
        verify(mockAgentTwo, times(1)).onStart();

        invoker.invoke();
        verify(mockAgentOne, times(1)).doWork();
        verify(mockAgentTwo, times(1)).doWork();

        assertTrue(compositeAgent.tryRemove(mockAgentTwo));
        assertFalse(compositeAgent.hasRemoveAgentCompleted());

        invoker.invoke();
        assertTrue(compositeAgent.hasRemoveAgentCompleted());
        verify(mockAgentOne, times(2)).doWork();
        verify(mockAgentTwo, times(1)).doWork();
        verify(mockAgentOne, times(1)).onStart();
        verify(mockAgentTwo, times(1)).onStart();
        verify(mockAgentTwo, times(1)).onClose();
    }

    @Test
    public void shouldCloseAgents() throws Exception
    {
        final Agent mockAgentOne = mock(Agent.class);
        final Agent mockAgentTwo = mock(Agent.class);

        final DynamicCompositeAgent compositeAgent = new DynamicCompositeAgent(ROLE_NAME, mockAgentOne, mockAgentTwo);

        compositeAgent.onClose();
        verify(mockAgentOne, never()).doWork();
        verify(mockAgentTwo, never()).doWork();
        verify(mockAgentOne, never()).onStart();
        verify(mockAgentTwo, never()).onStart();
        verify(mockAgentOne, times(1)).onClose();
        verify(mockAgentTwo, times(1)).onClose();

        assertEquals(DynamicCompositeAgent.Status.CLOSED, compositeAgent.status());
    }

    @Test
    public void shouldDetectConcurrentAdd()
    {
        final Agent mockAgentOne = mock(Agent.class);
        final Agent mockAgentTwo = mock(Agent.class);

        final DynamicCompositeAgent compositeAgent = new DynamicCompositeAgent(ROLE_NAME, mockAgentOne, mockAgentTwo);
        final AgentInvoker invoker = new AgentInvoker(Throwable::printStackTrace, null, compositeAgent);
        invoker.start();

        assertTrue(compositeAgent.tryAdd(mockAgentOne));
        assertFalse(compositeAgent.tryAdd(mockAgentTwo));

        invoker.invoke();
        assertTrue(compositeAgent.tryAdd(mockAgentTwo));
    }

    @Test
    public void shouldDetectConcurrentRemove()
    {
        final Agent mockAgentOne = mock(Agent.class);
        final Agent mockAgentTwo = mock(Agent.class);

        final DynamicCompositeAgent compositeAgent = new DynamicCompositeAgent(ROLE_NAME, mockAgentOne, mockAgentTwo);
        final AgentInvoker invoker = new AgentInvoker(Throwable::printStackTrace, null, compositeAgent);
        invoker.start();

        assertTrue(compositeAgent.tryAdd(mockAgentOne));
        invoker.invoke();
        assertTrue(compositeAgent.tryAdd(mockAgentTwo));
        invoker.invoke();

        assertTrue(compositeAgent.tryRemove(mockAgentOne));
        assertFalse(compositeAgent.tryRemove(mockAgentTwo));
        invoker.invoke();

        assertTrue(compositeAgent.tryRemove(mockAgentTwo));
    }
}
