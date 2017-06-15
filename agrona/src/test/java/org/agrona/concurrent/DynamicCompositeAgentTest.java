/*
 * Copyright 2017 Real Logic Ltd.
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

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class DynamicCompositeAgentTest
{
    private static final String ROLE_NAME = "roleName";

    @Test
    public void shouldAddAgent() throws Exception
    {
        final Agent mockAgentOne = mock(Agent.class);

        final DynamicCompositeAgent compositeAgent = new DynamicCompositeAgent(ROLE_NAME, mockAgentOne);
        assertThat(compositeAgent.roleName(), is(ROLE_NAME));
        compositeAgent.onStart();
        verify(mockAgentOne, times(1)).onStart();

        compositeAgent.doWork();
        verify(mockAgentOne, times(1)).onStart();
        verify(mockAgentOne, times(1)).doWork();

        final Agent mockAgentTwo = mock(Agent.class);
        compositeAgent.add(mockAgentTwo);

        compositeAgent.doWork();
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
        compositeAgent.onStart();
        verify(mockAgentOne, times(1)).onStart();
        verify(mockAgentTwo, times(1)).onStart();

        compositeAgent.doWork();
        verify(mockAgentOne, times(1)).doWork();
        verify(mockAgentTwo, times(1)).doWork();

        assertTrue(compositeAgent.remove(mockAgentTwo));

        compositeAgent.doWork();
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
    }
}