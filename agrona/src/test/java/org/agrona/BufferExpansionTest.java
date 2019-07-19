/*
 * Copyright 2014-2019 Real Logic Ltd.
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

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(Theories.class)
public class BufferExpansionTest
{
    @DataPoint
    public static final MutableDirectBuffer EXPANDABLE_ARRAY_BUFFER = new ExpandableArrayBuffer();

    @DataPoint
    public static final MutableDirectBuffer EXPANDABLE_DIRECT_BYTE_BUFFER = new ExpandableDirectByteBuffer();

    @Theory
    public void shouldExpand(final MutableDirectBuffer buffer)
    {
        final int capacity = buffer.capacity();

        final int index = capacity + 50;
        final int value = 777;
        buffer.putInt(index, value);

        assertThat(buffer.capacity(), greaterThan(capacity));
        assertThat(buffer.getInt(index), is(value));
    }
}
