/*
 * Copyright 2014 - 2015 Real Logic Ltd.
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
package uk.co.real_logic.agrona.concurrent;

import java.util.Arrays;

/**
 * Compose several agents into one so a thread can be shared.
 */
public class CompositeAgent implements Agent
{
    private final Agent[] agents;
    private final String roleName;

    /**
     * @param agents the parts of this composite, at least one agent and no null agents allowed
     * @throws IllegalArgumentException if an empty array of agents is provided
     * @throws NullPointerException if the array or any element is null
     */
    public CompositeAgent(Agent... agents)
    {
        if (agents == null)
        {
            throw new NullPointerException("Expecting at least one agent");
        }
        if (agents.length == 0)
        {
            throw new IllegalArgumentException("Expecting at least one agent");
        }

        final StringBuffer buff = new StringBuffer(agents.length * 16);
        buff.append('[');
        for (Agent smith: agents)
        {
            if (smith == null)
            {
                throw new NullPointerException("Agents list contains a null");
            }
            buff.append(smith.roleName());
            buff.append(',');
        }
        buff.setCharAt(buff.length() - 1, ']'); // overwrite the last ','
        roleName = buff.toString();

        this.agents = Arrays.copyOf(agents, agents.length);
    }

    public int doWork() throws Exception
    {
        int sum = 0;
        for (Agent a : agents)
        {
            sum += a.doWork();
        }

        return sum;
    }

    public void onClose()
    {
        for (Agent a : agents)
        {
            a.onClose();
        }
    }

    public String roleName()
    {
        return roleName;
    }
}
