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
package org.agrona.agent;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.IntConsumer;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bytebuddy.agent.ByteBuddyAgent;
import sun.misc.Unsafe;

public class BufferAlignmentAgentTest
{
    private static final String TEST_STRING = "BufferAlignmentTest";
    //on 32-bits JVMs, array content are not 8-byte aligned => need to add a 4 bytes offset
    private static final int HEAP_BUFFER_ALIGNMENT_OFFSET = Unsafe.ARRAY_BYTE_BASE_OFFSET % 8;

    @BeforeClass
    public static void installAgent()
    {
        BufferAlignmentAgent.agentmain("", ByteBuddyAgent.install());
    }

    @AfterClass
    public static void removeAgent()
    {
        BufferAlignmentAgent.removeTransformer();
    }

    @Test
    public void testUnsafeBufferFromByteArray()
    {
        testUnsafeBuffer(new UnsafeBuffer(new byte[256]), HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    public void testUnsafeBufferFromByteArrayWithOffset()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[256], 1, 128);
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 7 + HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    public void testUnsafeBufferFromHeapByteBuffer()
    {
        testUnsafeBuffer(new UnsafeBuffer(ByteBuffer.allocate(256)), HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    public void testUnsafeBufferFromSlicedHeapByteBuffer()
    {
        final ByteBuffer nioBuffer = ByteBuffer.allocateDirect(256);
        nioBuffer.position(1);
        final UnsafeBuffer buffer = new UnsafeBuffer(nioBuffer.slice());
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 7 + HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    public void testUnsafeBufferFromSlicedHeapByteBufferWithOffset()
    {
        final ByteBuffer nioBuffer = ByteBuffer.allocate(256);
        nioBuffer.position(1);
        final UnsafeBuffer buffer = new UnsafeBuffer(nioBuffer.slice(), 2, 128);
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 5 + HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    public void testUnsafeBufferFromDirectByteBuffer()
    {
        testUnsafeBuffer(new UnsafeBuffer(ByteBuffer.allocateDirect(256)), 0);
    }

    @Test
    public void testUnsafeBufferFromSlicedDirectByteBuffer()
    {
        final ByteBuffer nioBuffer = ByteBuffer.allocateDirect(256);
        nioBuffer.position(1);
        final UnsafeBuffer buffer = new UnsafeBuffer(nioBuffer.slice());
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 7);
    }

    @Test
    public void testUnsafeBufferFromSlicedDirectByteBufferWithOffset()
    {
        final ByteBuffer nioBuffer = ByteBuffer.allocateDirect(256);
        nioBuffer.position(1);
        final UnsafeBuffer buffer = new UnsafeBuffer(nioBuffer.slice(), 2, 128);
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 5);
    }

    @Test
    public void testExpandableBuffer()
    {
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(256);
        testAlignedReadMethods(buffer, HEAP_BUFFER_ALIGNMENT_OFFSET);
        testUnAlignedReadMethods(buffer, HEAP_BUFFER_ALIGNMENT_OFFSET);
        testAlignedWriteMethods(buffer, HEAP_BUFFER_ALIGNMENT_OFFSET);
        testUnAlignedWriteMethods(buffer, HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    private void testUnsafeBuffer(final UnsafeBuffer buffer, final int offset)
    {
        testAlignedReadMethods(buffer, offset);
        testUnAlignedReadMethods(buffer, offset);
        testAlignedWriteMethods(buffer, offset);
        testUnAlignedWriteMethods(buffer, offset);
        testAlignedAtomicMethods(buffer, offset);
        testUnAlignedAtomicMethods(buffer, offset);
    }

    private void testAlignedReadMethods(final DirectBuffer buffer, final int offset)
    {
        buffer.getLong(offset + BitUtil.SIZE_OF_LONG);
        buffer.getLong(offset + BitUtil.SIZE_OF_LONG, ByteOrder.BIG_ENDIAN);
        buffer.getDouble(offset + BitUtil.SIZE_OF_DOUBLE);
        buffer.getDouble(offset + BitUtil.SIZE_OF_DOUBLE, ByteOrder.BIG_ENDIAN);

        buffer.getInt(offset + BitUtil.SIZE_OF_INT);
        buffer.getInt(offset + BitUtil.SIZE_OF_INT, ByteOrder.BIG_ENDIAN);
        buffer.getFloat(offset + BitUtil.SIZE_OF_FLOAT);
        buffer.getFloat(offset + BitUtil.SIZE_OF_FLOAT, ByteOrder.BIG_ENDIAN);

        buffer.getShort(offset + BitUtil.SIZE_OF_SHORT);
        buffer.getShort(offset + BitUtil.SIZE_OF_SHORT, ByteOrder.BIG_ENDIAN);
        buffer.getChar(offset + BitUtil.SIZE_OF_CHAR);
        buffer.getChar(offset + BitUtil.SIZE_OF_CHAR, ByteOrder.BIG_ENDIAN);

        buffer.getByte(offset + BitUtil.SIZE_OF_BYTE);
        buffer.getByte(offset + BitUtil.SIZE_OF_BYTE);

        buffer.getStringUtf8(offset + BitUtil.SIZE_OF_INT);
        buffer.getStringUtf8(offset + BitUtil.SIZE_OF_INT, ByteOrder.BIG_ENDIAN);
        buffer.getStringAscii(offset + BitUtil.SIZE_OF_INT);
        buffer.getStringAscii(offset + BitUtil.SIZE_OF_INT, ByteOrder.BIG_ENDIAN);

        // string size is not read for these method => no need for 4-bytes
        // alignment
        buffer.getStringUtf8(offset + BitUtil.SIZE_OF_BYTE, 7);
        buffer.getStringWithoutLengthUtf8(offset + BitUtil.SIZE_OF_BYTE, 7);
        buffer.getStringAscii(offset + BitUtil.SIZE_OF_BYTE, 7);
        buffer.getStringWithoutLengthAscii(offset + BitUtil.SIZE_OF_BYTE, 7);
    }

    private void testUnAlignedReadMethods(final DirectBuffer buffer, final int offset)
    {
        buffer.getLong(offset); // assert that buffer[offset] is 8-bytes aligned

        assertUnaligned(offset + BitUtil.SIZE_OF_INT, buffer::getLong);
        assertUnaligned(offset + BitUtil.SIZE_OF_INT, i -> buffer.getLong(i, ByteOrder.BIG_ENDIAN));
        assertUnaligned(offset + BitUtil.SIZE_OF_FLOAT, buffer::getDouble);
        assertUnaligned(offset + BitUtil.SIZE_OF_FLOAT, i -> buffer.getDouble(i, ByteOrder.BIG_ENDIAN));

        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, buffer::getInt);
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.getInt(i, ByteOrder.BIG_ENDIAN));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, buffer::getFloat);
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.getFloat(i, ByteOrder.BIG_ENDIAN));

        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, buffer::getShort);
        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, i -> buffer.getShort(i, ByteOrder.BIG_ENDIAN));
        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, buffer::getChar);
        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, i -> buffer.getChar(i, ByteOrder.BIG_ENDIAN));

        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, buffer::getStringUtf8);
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.getStringUtf8(i, ByteOrder.BIG_ENDIAN));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, buffer::getStringAscii);
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.getStringAscii(i, ByteOrder.BIG_ENDIAN));
    }

    private void testAlignedWriteMethods(final MutableDirectBuffer buffer, final int offset)
    {
        buffer.putLong(offset + BitUtil.SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.putLong(offset + BitUtil.SIZE_OF_LONG, Long.MAX_VALUE, ByteOrder.BIG_ENDIAN);
        buffer.putDouble(offset + BitUtil.SIZE_OF_DOUBLE, Double.MAX_VALUE);
        buffer.putDouble(offset + BitUtil.SIZE_OF_DOUBLE, Double.MAX_VALUE, ByteOrder.BIG_ENDIAN);

        buffer.putInt(offset + BitUtil.SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.putInt(offset + BitUtil.SIZE_OF_INT, Integer.MAX_VALUE, ByteOrder.BIG_ENDIAN);
        buffer.putFloat(offset + BitUtil.SIZE_OF_FLOAT, Float.MAX_VALUE);
        buffer.putFloat(offset + BitUtil.SIZE_OF_FLOAT, Float.MAX_VALUE, ByteOrder.BIG_ENDIAN);

        buffer.putShort(offset + BitUtil.SIZE_OF_SHORT, Short.MAX_VALUE);
        buffer.putShort(offset + BitUtil.SIZE_OF_SHORT, Short.MAX_VALUE, ByteOrder.BIG_ENDIAN);
        buffer.putChar(offset + BitUtil.SIZE_OF_CHAR, Character.MAX_VALUE);
        buffer.putChar(offset + BitUtil.SIZE_OF_CHAR, Character.MAX_VALUE, ByteOrder.BIG_ENDIAN);

        buffer.putByte(offset + BitUtil.SIZE_OF_BYTE, Byte.MAX_VALUE);
        buffer.putByte(offset + BitUtil.SIZE_OF_BYTE, Byte.MAX_VALUE);

        buffer.putStringUtf8(offset + BitUtil.SIZE_OF_INT, TEST_STRING);
        buffer.putStringUtf8(offset + BitUtil.SIZE_OF_INT, TEST_STRING, ByteOrder.BIG_ENDIAN);
        buffer.putStringUtf8(offset + BitUtil.SIZE_OF_INT, TEST_STRING, Integer.MAX_VALUE);
        buffer.putStringUtf8(offset + BitUtil.SIZE_OF_INT, TEST_STRING, ByteOrder.BIG_ENDIAN, Integer.MAX_VALUE);
        buffer.putStringAscii(offset + BitUtil.SIZE_OF_INT, TEST_STRING);
        buffer.putStringAscii(offset + BitUtil.SIZE_OF_INT, TEST_STRING, ByteOrder.BIG_ENDIAN);

        // string size is not read for these method => no need for 4-bytes
        // alignment
        buffer.putStringWithoutLengthUtf8(offset + BitUtil.SIZE_OF_BYTE, TEST_STRING);
        buffer.putStringWithoutLengthAscii(offset + BitUtil.SIZE_OF_BYTE, TEST_STRING);
    }

    private void testUnAlignedWriteMethods(final MutableDirectBuffer buffer, final int offset)
    {
        buffer.putLong(offset, Long.MAX_VALUE); // assert that buffer[offset] is
        // 8-bytes aligned

        assertUnaligned(offset + BitUtil.SIZE_OF_INT, i -> buffer.putLong(i, Long.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_INT, i -> buffer.putLong(i, Long.MAX_VALUE, ByteOrder.BIG_ENDIAN));
        assertUnaligned(offset + BitUtil.SIZE_OF_FLOAT, i -> buffer.putDouble(i, Double.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_FLOAT, i -> buffer.putDouble(i, Double.MAX_VALUE, ByteOrder.BIG_ENDIAN));

        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putInt(i, Integer.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putInt(i, Integer.MAX_VALUE, ByteOrder.BIG_ENDIAN));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putFloat(i, Float.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putFloat(i, Float.MAX_VALUE, ByteOrder.BIG_ENDIAN));

        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, i -> buffer.putShort(i, Short.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, i -> buffer.putShort(i, Short.MAX_VALUE, ByteOrder.BIG_ENDIAN));
        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, i -> buffer.putChar(i, Character.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, i -> buffer.putChar(i, Character.MAX_VALUE, ByteOrder.BIG_ENDIAN));

        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putStringAscii(i, TEST_STRING));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putStringAscii(i, TEST_STRING, ByteOrder.BIG_ENDIAN));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putStringUtf8(i, TEST_STRING));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putStringUtf8(i, TEST_STRING, ByteOrder.BIG_ENDIAN));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putStringUtf8(i, TEST_STRING, Integer.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT,
            (i) -> buffer.putStringUtf8(i, TEST_STRING, ByteOrder.BIG_ENDIAN, Integer.MAX_VALUE));
    }

    private void testAlignedAtomicMethods(final AtomicBuffer buffer, final int offset)
    {
        buffer.getLongVolatile(offset + BitUtil.SIZE_OF_LONG);
        buffer.putLongVolatile(offset + BitUtil.SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.compareAndSetLong(offset + BitUtil.SIZE_OF_LONG, Long.MAX_VALUE, Long.MAX_VALUE);
        buffer.getAndAddLong(offset + BitUtil.SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.getAndSetLong(offset + BitUtil.SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.putLongOrdered(offset + BitUtil.SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.addLongOrdered(offset + BitUtil.SIZE_OF_LONG, Long.MAX_VALUE);

        buffer.getIntVolatile(offset + BitUtil.SIZE_OF_INT);
        buffer.putIntVolatile(offset + BitUtil.SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.compareAndSetInt(offset + BitUtil.SIZE_OF_INT, Integer.MAX_VALUE, Integer.MAX_VALUE);
        buffer.getAndAddInt(offset + BitUtil.SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.getAndSetInt(offset + BitUtil.SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.putIntOrdered(offset + BitUtil.SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.addIntOrdered(offset + BitUtil.SIZE_OF_INT, Integer.MAX_VALUE);

        buffer.getShortVolatile(offset + BitUtil.SIZE_OF_SHORT);
        buffer.putShortVolatile(offset + BitUtil.SIZE_OF_SHORT, Short.MAX_VALUE);
        buffer.getCharVolatile(offset + BitUtil.SIZE_OF_CHAR);
        buffer.putCharVolatile(offset + BitUtil.SIZE_OF_CHAR, Character.MAX_VALUE);
        buffer.getByteVolatile(offset + BitUtil.SIZE_OF_BYTE);
        buffer.putByteVolatile(offset + BitUtil.SIZE_OF_BYTE, Byte.MAX_VALUE);
    }

    private void testUnAlignedAtomicMethods(final AtomicBuffer buffer, final int offset)
    {
        buffer.getLongVolatile(offset); // assert that buffer[offset] is 8-bytes
        // aligned

        assertUnaligned(offset + BitUtil.SIZE_OF_INT, buffer::getLongVolatile);
        assertUnaligned(offset + BitUtil.SIZE_OF_INT, i -> buffer.putLongVolatile(i, Long.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_INT, i -> buffer.compareAndSetLong(i, Long.MAX_VALUE, Long.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_INT, i -> buffer.getAndAddLong(i, Long.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_INT, i -> buffer.getAndSetLong(i, Long.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_INT, i -> buffer.putLongOrdered(i, Long.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_INT, i -> buffer.addLongOrdered(i, Long.MAX_VALUE));

        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, buffer::getIntVolatile);
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putIntVolatile(i, Integer.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.compareAndSetInt(i, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.getAndAddInt(i, Integer.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.getAndSetInt(i, Integer.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.putIntOrdered(i, Integer.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_SHORT, i -> buffer.addIntOrdered(i, Integer.MAX_VALUE));

        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, buffer::getShortVolatile);
        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, i -> buffer.putShortVolatile(i, Short.MAX_VALUE));
        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, buffer::getCharVolatile);
        assertUnaligned(offset + BitUtil.SIZE_OF_BYTE, i -> buffer.putCharVolatile(i, Character.MAX_VALUE));

    }

    private void assertUnaligned(final int index, final IntConsumer methodUnderTest)
    {
        try
        {
            methodUnderTest.accept(index);
        }
        catch (final BufferAlignmentException ignore)
        {
            return;
        }

        fail("Should have thrown BufferAlignmentException");
    }
}
