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
package org.agrona.concurrent;

import org.agrona.CloseHelper;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.IoUtil;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappedResizeableBufferTest
{
    private static final long SIZE = 2 * (long)Integer.MAX_VALUE;
    private static final int VALUE = 4;
    private static final String PATH = IoUtil.tmpDirName() + "/eg-buffer";

    private static FileChannel channel;

    private MappedResizeableBuffer buffer;

    @BeforeAll
    public static void setUp() throws IOException
    {
        final RandomAccessFile file = new RandomAccessFile(PATH, "rw");
        file.setLength(SIZE);
        channel = file.getChannel();
    }

    @AfterEach
    void close()
    {
        CloseHelper.close(buffer);
    }

    @AfterAll
    public static void tearDown()
    {
        CloseHelper.close(channel);
        IoUtil.deleteIfExists(new File(PATH));
    }

    @Test
    void shouldWriteDataToBuffer()
    {
        buffer = new MappedResizeableBuffer(channel, 0, 100);

        exchangeDataAt(50L);
    }

    @Test
    void shouldResizeBufferToOver2GB()
    {
        buffer = new MappedResizeableBuffer(channel, 0, 100);

        buffer.resize(SIZE);

        exchangeDataAt(SIZE - 4);
    }

    @Test
    void shouldReadPreviousWrites()
    {
        buffer = new MappedResizeableBuffer(channel, 0, 100);

        exchangeDataAt(50L);

        buffer.resize(SIZE);

        assertEquals(VALUE, buffer.getInt(50L));
    }

    @Test
    void shouldReadBytesFromOtherBuffer()
    {
        buffer = new MappedResizeableBuffer(channel, 0, SIZE);

        exchangeDataAt(SIZE - 4);

        buffer.close();

        buffer = new MappedResizeableBuffer(channel, 0, SIZE);

        assertEquals(VALUE, buffer.getInt(SIZE - 4));
    }

    @Test
    void shouldNotCloseChannelUponBufferClose()
    {
        buffer = new MappedResizeableBuffer(channel, 0, SIZE);
        buffer.close();

        assertTrue(channel.isOpen());
        buffer = null;
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldPutBytesFromDirectBuffer(final MutableDirectBuffer src)
    {
        final long value = 0x5555555555555555L;
        final int srcIndex = 8;
        final int destIndex = 24;
        src.putLong(srcIndex, value);

        buffer = new MappedResizeableBuffer(channel, 0, 100);
        buffer.putBytes(destIndex, src, srcIndex, SIZE_OF_LONG);

        assertEquals(value, buffer.getLong(destIndex));
    }

    @ParameterizedTest
    @ValueSource(ints = { 11, 64, 1011 })
    void setMemory(final int length)
    {
        final int index = 2;
        final byte value = (byte)11;
        buffer = new MappedResizeableBuffer(channel, 0, 2 * index + length);

        buffer.setMemory(index, length, value);

        assertEquals(0, buffer.getByte(0));
        assertEquals(0, buffer.getByte(1));
        assertEquals(0, buffer.getByte(index + length));
        assertEquals(0, buffer.getByte(index + length + 1));
        for (int i = 0; i < length; i++)
        {
            assertEquals(value, buffer.getByte(index + i));
        }
    }

    private void exchangeDataAt(final long index)
    {
        buffer.putInt(index, VALUE);
        assertEquals(VALUE, buffer.getInt(index));
    }

    private static List<MutableDirectBuffer> buffers()
    {
        return Arrays.asList(
            new ExpandableArrayBuffer(16),
            new ExpandableDirectByteBuffer(16));
    }
}
