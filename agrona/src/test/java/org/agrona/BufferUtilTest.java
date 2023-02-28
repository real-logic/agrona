/*
 * Copyright 2014-2023 Real Logic Limited.
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

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

import java.nio.ByteBuffer;

import static org.agrona.BitUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.JRE.JAVA_9;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BufferUtilTest
{
    @Test
    void shouldDetectNonPowerOfTwoAlignment()
    {
        assertThrows(IllegalArgumentException.class, () -> BufferUtil.allocateDirectAligned(1, 3));
    }

    @Test
    void shouldAlignToWordBoundary()
    {
        final int capacity = 128;
        final ByteBuffer byteBuffer = BufferUtil.allocateDirectAligned(capacity, SIZE_OF_LONG);

        final long address = BufferUtil.address(byteBuffer);
        assertTrue(isAligned(address, SIZE_OF_LONG));
        assertThat(byteBuffer.capacity(), is(capacity));
    }

    @Test
    void shouldAlignToCacheLineBoundary()
    {
        final int capacity = 128;
        final ByteBuffer byteBuffer = BufferUtil.allocateDirectAligned(capacity, CACHE_LINE_LENGTH);

        final long address = BufferUtil.address(byteBuffer);
        assertTrue(isAligned(address, CACHE_LINE_LENGTH));
        assertThat(byteBuffer.capacity(), is(capacity));
    }

    @Test
    void freeIsANoOpIfDirectBufferIsNull()
    {
        BufferUtil.free((DirectBuffer)null);
    }

    @Test
    void freeIsANoOpIfByteBufferIsNull()
    {
        BufferUtil.free((ByteBuffer)null);
    }

    @Test
    void freeIsANoOpIfByteBufferIsNotDirect()
    {
        final ByteBuffer buffer = ByteBuffer.allocate(4);

        BufferUtil.free(buffer);

        buffer.put(2, (byte)101);
        assertEquals(101, buffer.get(2));
    }

    @Test
    void freeIsANoOpIfDirectBufferContainsNonDirectByteBuffer()
    {
        final DirectBuffer buffer = mock(DirectBuffer.class);
        final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        when(buffer.byteBuffer()).thenReturn(byteBuffer);

        BufferUtil.free(buffer);

        byteBuffer.put(1, (byte)5);
        assertEquals(5, byteBuffer.get(1));
    }

    @Test
    void freeShouldReleaseByteBufferResources()
    {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(4);
        buffer.put((byte)1);
        buffer.put((byte)2);
        buffer.put((byte)3);
        buffer.position(0);

        BufferUtil.free(buffer);
    }

    @Test
    void freeShouldReleaseDirectBufferResources()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(4));
        buffer.setMemory(0, 4, (byte)111);

        BufferUtil.free(buffer);
    }

    @Test
    @EnabledForJreRange(min = JAVA_9)
    void freeThrowsIllegalArgumentExceptionIfByteBufferIsASlice()
    {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(4).slice();

        assertThrows(IllegalArgumentException.class, () -> BufferUtil.free(buffer));
    }

    @Test
    @EnabledForJreRange(min = JAVA_9)
    void freeThrowsIllegalArgumentExceptionIfByteBufferIsADuplicate()
    {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(4).duplicate();

        assertThrows(IllegalArgumentException.class, () -> BufferUtil.free(buffer));
    }
}
