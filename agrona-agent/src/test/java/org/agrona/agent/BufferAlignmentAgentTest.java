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
package org.agrona.agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.UnsafeAccess;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static org.agrona.BitUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class BufferAlignmentAgentTest
{
    private static final String TEST_STRING = "BufferAlignmentTest";
    private static final CharSequence TEST_CHAR_SEQUENCE = new StringBuilder("BufferAlignmentTest");

    //on 32-bits JVMs, array content is not 8-byte aligned => need to add 4 bytes offset
    private static final int HEAP_BUFFER_ALIGNMENT_OFFSET = UnsafeAccess.ARRAY_BYTE_BASE_OFFSET % 8;
    private static final Pattern EXCEPTION_MESSAGE_PATTERN = Pattern.compile("-?\\d+");

    @BeforeAll
    static void installAgent()
    {
        BufferAlignmentAgent.agentmain("", ByteBuddyAgent.install());
    }

    @AfterAll
    static void removeAgent()
    {
        BufferAlignmentAgent.removeTransformer();
    }

    @Test
    void testUnsafeBufferFromByteArray()
    {
        testUnsafeBuffer(new UnsafeBuffer(new byte[256]), HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    void testUnsafeBufferFromByteArrayWithOffset()
    {
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[256], 1, 128);
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 7 + HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    void testUnsafeBufferFromHeapByteBuffer()
    {
        testUnsafeBuffer(new UnsafeBuffer(ByteBuffer.allocate(256)), HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    void testUnsafeBufferFromSlicedHeapByteBuffer()
    {
        final ByteBuffer nioBuffer = ByteBuffer.allocateDirect(256);
        nioBuffer.position(1);
        final UnsafeBuffer buffer = new UnsafeBuffer(nioBuffer.slice());
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 7 + HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    void testUnsafeBufferFromSlicedHeapByteBufferWithOffset()
    {
        final ByteBuffer nioBuffer = ByteBuffer.allocate(256);
        nioBuffer.position(1);
        final UnsafeBuffer buffer = new UnsafeBuffer(nioBuffer.slice(), 2, 128);
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 5 + HEAP_BUFFER_ALIGNMENT_OFFSET);
    }

    @Test
    void testUnsafeBufferFromDirectByteBuffer()
    {
        testUnsafeBuffer(new UnsafeBuffer(ByteBuffer.allocateDirect(256)), 0);
    }

    @Test
    void testUnsafeBufferFromSlicedDirectByteBuffer()
    {
        final ByteBuffer nioBuffer = ByteBuffer.allocateDirect(256);
        nioBuffer.position(1);
        final UnsafeBuffer buffer = new UnsafeBuffer(nioBuffer.slice());
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 7);
    }

    @Test
    void testUnsafeBufferFromSlicedDirectByteBufferWithOffset()
    {
        final ByteBuffer nioBuffer = ByteBuffer.allocateDirect(256);
        nioBuffer.position(1);
        final UnsafeBuffer buffer = new UnsafeBuffer(nioBuffer.slice(), 2, 128);
        assertTrue(buffer.addressOffset() % 4 != 0);
        testUnsafeBuffer(buffer, 5);
    }

    @Test
    void testExpandableBuffer()
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
        buffer.getLong(offset + SIZE_OF_LONG);
        buffer.getLong(offset + SIZE_OF_LONG, BIG_ENDIAN);
        buffer.getDouble(offset + SIZE_OF_DOUBLE);
        buffer.getDouble(offset + SIZE_OF_DOUBLE, BIG_ENDIAN);

        buffer.getInt(offset + SIZE_OF_INT);
        buffer.getInt(offset + SIZE_OF_INT, BIG_ENDIAN);
        buffer.getFloat(offset + SIZE_OF_FLOAT);
        buffer.getFloat(offset + SIZE_OF_FLOAT, BIG_ENDIAN);

        buffer.getShort(offset + SIZE_OF_SHORT);
        buffer.getShort(offset + SIZE_OF_SHORT, BIG_ENDIAN);
        buffer.getChar(offset + SIZE_OF_CHAR);
        buffer.getChar(offset + SIZE_OF_CHAR, BIG_ENDIAN);

        buffer.getByte(offset + SIZE_OF_BYTE);
        buffer.getByte(offset + SIZE_OF_BYTE);

        buffer.getStringUtf8(offset + SIZE_OF_INT);
        buffer.getStringUtf8(offset + SIZE_OF_INT, BIG_ENDIAN);
        buffer.getStringAscii(offset + SIZE_OF_INT);
        buffer.getStringAscii(offset + SIZE_OF_INT, BIG_ENDIAN);

        // string size is not read for these methods => no need for 4-bytes
        // alignment
        buffer.getStringUtf8(offset + SIZE_OF_BYTE, 7);
        buffer.getStringWithoutLengthUtf8(offset + SIZE_OF_BYTE, 7);
        buffer.getStringAscii(offset + SIZE_OF_BYTE, 7);
        buffer.getStringWithoutLengthAscii(offset + SIZE_OF_BYTE, 7);
    }

    private void testUnAlignedReadMethods(final DirectBuffer buffer, final int offset)
    {
        buffer.getLong(offset); // assert that buffer[offset] is 8-bytes aligned

        assertUnaligned(offset + SIZE_OF_INT, buffer::getLong);
        assertUnaligned(offset + SIZE_OF_INT, (i) -> buffer.getLong(i, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_FLOAT, buffer::getDouble);
        assertUnaligned(offset + SIZE_OF_FLOAT, (i) -> buffer.getDouble(i, BIG_ENDIAN));

        assertUnaligned(offset + SIZE_OF_SHORT, buffer::getInt);
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.getInt(i, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_SHORT, buffer::getFloat);
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.getFloat(i, BIG_ENDIAN));

        assertUnaligned(offset + SIZE_OF_BYTE, buffer::getShort);
        assertUnaligned(offset + SIZE_OF_BYTE, (i) -> buffer.getShort(i, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_BYTE, buffer::getChar);
        assertUnaligned(offset + SIZE_OF_BYTE, (i) -> buffer.getChar(i, BIG_ENDIAN));

        assertUnaligned(offset + SIZE_OF_SHORT, buffer::getStringUtf8);
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.getStringUtf8(i, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_SHORT, buffer::getStringAscii);
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.getStringAscii(i, BIG_ENDIAN));
    }

    private void testAlignedWriteMethods(final MutableDirectBuffer buffer, final int offset)
    {
        buffer.putLong(offset + SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.putLong(offset + SIZE_OF_LONG, Long.MAX_VALUE, BIG_ENDIAN);
        buffer.putDouble(offset + SIZE_OF_DOUBLE, Double.MAX_VALUE);
        buffer.putDouble(offset + SIZE_OF_DOUBLE, Double.MAX_VALUE, BIG_ENDIAN);

        buffer.putInt(offset + SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.putInt(offset + SIZE_OF_INT, Integer.MAX_VALUE, BIG_ENDIAN);
        buffer.putFloat(offset + SIZE_OF_FLOAT, Float.MAX_VALUE);
        buffer.putFloat(offset + SIZE_OF_FLOAT, Float.MAX_VALUE, BIG_ENDIAN);

        buffer.putShort(offset + SIZE_OF_SHORT, Short.MAX_VALUE);
        buffer.putShort(offset + SIZE_OF_SHORT, Short.MAX_VALUE, BIG_ENDIAN);
        buffer.putChar(offset + SIZE_OF_CHAR, Character.MAX_VALUE);
        buffer.putChar(offset + SIZE_OF_CHAR, Character.MAX_VALUE, BIG_ENDIAN);

        buffer.putByte(offset + SIZE_OF_BYTE, Byte.MAX_VALUE);
        buffer.putByte(offset + SIZE_OF_BYTE, Byte.MAX_VALUE);

        buffer.putStringUtf8(offset + SIZE_OF_INT, TEST_STRING);
        buffer.putStringUtf8(offset + SIZE_OF_INT, TEST_STRING, BIG_ENDIAN);
        buffer.putStringUtf8(offset + SIZE_OF_INT, TEST_STRING, Integer.MAX_VALUE);
        buffer.putStringUtf8(offset + SIZE_OF_INT, TEST_STRING, BIG_ENDIAN, Integer.MAX_VALUE);
        buffer.putStringAscii(offset + SIZE_OF_INT, TEST_STRING);
        buffer.putStringAscii(offset + SIZE_OF_INT, TEST_CHAR_SEQUENCE);
        buffer.putStringAscii(offset + SIZE_OF_INT, TEST_STRING, BIG_ENDIAN);
        buffer.putStringAscii(offset + SIZE_OF_INT, TEST_CHAR_SEQUENCE, BIG_ENDIAN);

        // string size is not read for these methods => no need for 4-bytes
        // alignment
        buffer.putStringWithoutLengthUtf8(offset + SIZE_OF_BYTE, TEST_STRING);
        buffer.putStringWithoutLengthAscii(offset + SIZE_OF_BYTE, TEST_STRING);
        buffer.putStringWithoutLengthAscii(offset + SIZE_OF_BYTE, TEST_CHAR_SEQUENCE);
    }

    private void testUnAlignedWriteMethods(final MutableDirectBuffer buffer, final int offset)
    {
        buffer.putLong(offset, Long.MAX_VALUE); // assert that buffer[offset] is
        // 8-bytes aligned

        assertUnaligned(offset + SIZE_OF_INT, (i) -> buffer.putLong(i, Long.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_INT, (i) -> buffer.putLong(i, Long.MAX_VALUE, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_FLOAT, (i) -> buffer.putDouble(i, Double.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_FLOAT, (i) -> buffer.putDouble(i, Double.MAX_VALUE, BIG_ENDIAN));

        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putInt(i, Integer.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putInt(i, Integer.MAX_VALUE, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putFloat(i, Float.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putFloat(i, Float.MAX_VALUE, BIG_ENDIAN));

        assertUnaligned(offset + SIZE_OF_BYTE, (i) -> buffer.putShort(i, Short.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_BYTE, (i) -> buffer.putShort(i, Short.MAX_VALUE, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_BYTE, (i) -> buffer.putChar(i, Character.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_BYTE, (i) -> buffer.putChar(i, Character.MAX_VALUE, BIG_ENDIAN));

        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putStringAscii(i, TEST_STRING));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putStringAscii(i, TEST_CHAR_SEQUENCE));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putStringAscii(i, TEST_STRING, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putStringAscii(i, TEST_CHAR_SEQUENCE, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putStringUtf8(i, TEST_STRING));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putStringUtf8(i, TEST_STRING, BIG_ENDIAN));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putStringUtf8(i, TEST_STRING, Integer.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_SHORT,
            (i) -> buffer.putStringUtf8(i, TEST_STRING, BIG_ENDIAN, Integer.MAX_VALUE));
    }

    private void testAlignedAtomicMethods(final AtomicBuffer buffer, final int offset)
    {
        buffer.getLongVolatile(offset + SIZE_OF_LONG);
        buffer.putLongVolatile(offset + SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.compareAndSetLong(offset + SIZE_OF_LONG, Long.MAX_VALUE, Long.MAX_VALUE);
        buffer.getAndAddLong(offset + SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.getAndSetLong(offset + SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.putLongOrdered(offset + SIZE_OF_LONG, Long.MAX_VALUE);
        buffer.addLongOrdered(offset + SIZE_OF_LONG, Long.MAX_VALUE);

        buffer.getIntVolatile(offset + SIZE_OF_INT);
        buffer.putIntVolatile(offset + SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.compareAndSetInt(offset + SIZE_OF_INT, Integer.MAX_VALUE, Integer.MAX_VALUE);
        buffer.getAndAddInt(offset + SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.getAndSetInt(offset + SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.putIntOrdered(offset + SIZE_OF_INT, Integer.MAX_VALUE);
        buffer.addIntOrdered(offset + SIZE_OF_INT, Integer.MAX_VALUE);

        buffer.getShortVolatile(offset + SIZE_OF_SHORT);
        buffer.putShortVolatile(offset + SIZE_OF_SHORT, Short.MAX_VALUE);
        buffer.getCharVolatile(offset + SIZE_OF_CHAR);
        buffer.putCharVolatile(offset + SIZE_OF_CHAR, Character.MAX_VALUE);
        buffer.getByteVolatile(offset + SIZE_OF_BYTE);
        buffer.putByteVolatile(offset + SIZE_OF_BYTE, Byte.MAX_VALUE);
    }

    private void testUnAlignedAtomicMethods(final AtomicBuffer buffer, final int offset)
    {
        buffer.getLongVolatile(offset); // assert that buffer[offset] is 8-bytes
        // aligned

        assertUnaligned(offset + SIZE_OF_INT, buffer::getLongVolatile);
        assertUnaligned(offset + SIZE_OF_INT, (i) -> buffer.putLongVolatile(i, Long.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_INT, (i) -> buffer.compareAndSetLong(i, Long.MAX_VALUE, Long.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_INT, (i) -> buffer.getAndAddLong(i, Long.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_INT, (i) -> buffer.getAndSetLong(i, Long.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_INT, (i) -> buffer.putLongOrdered(i, Long.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_INT, (i) -> buffer.addLongOrdered(i, Long.MAX_VALUE));

        assertUnaligned(offset + SIZE_OF_SHORT, buffer::getIntVolatile);
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putIntVolatile(i, Integer.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_SHORT,
            (i) -> buffer.compareAndSetInt(i, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.getAndAddInt(i, Integer.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.getAndSetInt(i, Integer.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.putIntOrdered(i, Integer.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_SHORT, (i) -> buffer.addIntOrdered(i, Integer.MAX_VALUE));

        assertUnaligned(offset + SIZE_OF_BYTE, buffer::getShortVolatile);
        assertUnaligned(offset + SIZE_OF_BYTE, (i) -> buffer.putShortVolatile(i, Short.MAX_VALUE));
        assertUnaligned(offset + SIZE_OF_BYTE, buffer::getCharVolatile);
        assertUnaligned(offset + SIZE_OF_BYTE, (i) -> buffer.putCharVolatile(i, Character.MAX_VALUE));
    }

    private void assertUnaligned(final int index, final IntConsumer methodUnderTest)
    {
        try
        {
            methodUnderTest.accept(index);

            fail("Should have thrown BufferAlignmentException");
        }
        catch (final BufferAlignmentException ex)
        {
            final Matcher matcher = EXCEPTION_MESSAGE_PATTERN.matcher(ex.getMessage());
            assertTrue(matcher.find());
            assertTrue(matcher.find());
            final int indexFound = Integer.parseInt(matcher.group());
            assertTrue(matcher.find());
            final int offsetFound = Integer.parseInt(matcher.group());

            assertEquals(index, indexFound, "BufferAlignmentException reported wrong index");
            assertNotEquals(0, offsetFound, "BufferAlignmentException reported wrong offset");
        }
    }
}
