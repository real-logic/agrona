/*
 * Copyright 2014-2015 Real Logic Ltd.
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

import org.junit.Before;
import org.junit.Test;

import java.util.function.IntFunction;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CompositeBufferTest
{
    private static UnsafeBuffer[] buffers =
        {
            new UnsafeBuffer(new byte[CompositeBuffer.SIZE]),
            new UnsafeBuffer(new byte[CompositeBuffer.SIZE]),
        };

    private IntFunction<UnsafeBuffer> factory = mock(IntFunction.class);
    private CompositeBuffer compositeBuffer = new CompositeBuffer(factory);

    @Before
    public void setUp()
    {
        when(factory.apply(0)).thenReturn(buffers[0]);
        when(factory.apply(1)).thenReturn(buffers[1]);
    }

    @Test
    public void shouldUseInitialBuffer()
    {
        final int index = 100;
        final int value = 5;

        compositeBuffer.putInt(index, value);

        assertEquals(value, buffers[0].getInt(index));
    }

}
