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
package org.agrona;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BufferExpansionTest
{
    private static Stream<MutableDirectBuffer> buffers()
    {
        return Stream.of(
            new ExpandableArrayBuffer(),
            new ExpandableDirectByteBuffer()
        );
    }

    @ParameterizedTest
    @MethodSource("buffers")
    public void shouldExpand(final MutableDirectBuffer buffer)
    {
        final int capacity = buffer.capacity();

        final int index = capacity + 50;
        final int value = 777;
        buffer.putInt(index, value);

        assertThat(buffer.capacity(), greaterThan(capacity));
        assertEquals(buffer.getInt(index), value);
    }

    @Test
    public void shouldExpandArrayBufferFromZeroCapacity()
    {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(0);
        buffer.putByte(0, (byte)4);

        assertThat(buffer.capacity(), greaterThan(0));
    }

    @Test
    public void shouldExpandArrayBufferFromOneCapacity()
    {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(1);
        buffer.putByte(0, (byte)4);
        buffer.putByte(1, (byte)2);
    }

    @Test
    public void shouldExpandDirectBufferFromZeroCapacity()
    {
        final MutableDirectBuffer buffer = new ExpandableDirectByteBuffer(0);
        buffer.putByte(0, (byte)4);

        assertThat(buffer.capacity(), greaterThan(0));
    }

    @Test
    public void shouldExpandDirectBufferFromOneCapacity()
    {
        final MutableDirectBuffer buffer = new ExpandableDirectByteBuffer(1);
        buffer.putByte(0, (byte)4);
        buffer.putByte(1, (byte)2);
    }
}
