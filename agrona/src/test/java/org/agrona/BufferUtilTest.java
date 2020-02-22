/*
 * Copyright 2014-2020 Real Logic Limited.
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
import org.mockito.InOrder;

import java.nio.ByteBuffer;

import static org.agrona.BitUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.JRE.JAVA_9;
import static org.mockito.Mockito.*;

public class BufferUtilTest
{
    @Test
    public void shouldDetectNonPowerOfTwoAlignment()
    {
        assertThrows(IllegalArgumentException.class, () -> BufferUtil.allocateDirectAligned(1, 3));
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

    @Test
    public void freeIsAnOpIfDirectBufferIsNull()
    {
        BufferUtil.free((DirectBuffer)null);
    }

    @Test
    public void freeIsAnOpIfByteBufferIsNull()
    {
        BufferUtil.free((ByteBuffer)null);
    }

    @Test
    public void freeIsAnOpIfByteBufferIsNotDirect()
    {
        final ByteBuffer buffer = mock(ByteBuffer.class);

        BufferUtil.free(buffer);

        verify(buffer).isDirect();
        verifyNoMoreInteractions(buffer);
    }

    @Test
    public void freeIsAnOpIfDirectBufferContainsNonDirectByteBuffer()
    {
        final DirectBuffer buffer = mock(DirectBuffer.class);
        final ByteBuffer byteBuffer = mock(ByteBuffer.class);
        when(buffer.byteBuffer()).thenReturn(byteBuffer);

        BufferUtil.free(buffer);

        final InOrder inOrder = inOrder(buffer, byteBuffer);
        inOrder.verify(buffer).byteBuffer();
        inOrder.verify(byteBuffer).isDirect();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void freeShouldReleaseByteBufferResources()
    {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(4);
        buffer.put((byte)1);
        buffer.put((byte)2);
        buffer.put((byte)3);
        buffer.position(0);

        BufferUtil.free(buffer);
    }

    @Test
    public void freeShouldReleaseDirectBufferResources()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(4));
        buffer.setMemory(0, 4, (byte)111);

        BufferUtil.free(buffer);
    }

    @Test
    @EnabledForJreRange(min = JAVA_9)
    public void freeThrowsIllegalArgumentExceptionIfByteBufferIsASlice()
    {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(4).slice();

        assertThrows(IllegalArgumentException.class, () -> BufferUtil.free(buffer));
    }

    @Test
    @EnabledForJreRange(min = JAVA_9)
    public void freeThrowsIllegalArgumentExceptionIfByteBufferIsADuplicate()
    {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(4).duplicate();

        assertThrows(IllegalArgumentException.class, () -> BufferUtil.free(buffer));
    }
}
