/*
 * Copyright 2014-2019 Real Logic Ltd.
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
package org.agrona;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.agrona.BitUtil.CACHE_LINE_LENGTH;
import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.agrona.BitUtil.isAligned;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BufferUtilTest
{
    @Test(expected = IllegalArgumentException.class)
    public void shouldDetectNonPowerOfTwoAlignment()
    {
        BufferUtil.allocateDirectAligned(1, 3);
    }

    @Test
    public void shouldAlignToWordBoundary()
    {
        final int capacity = 128;
        final ByteBuffer byteBuffer = BufferUtil.allocateDirectAligned(capacity, SIZE_OF_LONG);

        final long address = BufferUtil.address(byteBuffer);
        assertTrue(isAligned(address, SIZE_OF_LONG));
        assertThat(byteBuffer.capacity(), is(capacity));
    }

    @Test
    public void shouldAlignToCacheLineBoundary()
    {
        final int capacity = 128;
        final ByteBuffer byteBuffer = BufferUtil.allocateDirectAligned(capacity, CACHE_LINE_LENGTH);

        final long address = BufferUtil.address(byteBuffer);
        assertTrue(isAligned(address, CACHE_LINE_LENGTH));
        assertThat(byteBuffer.capacity(), is(capacity));
    }
}