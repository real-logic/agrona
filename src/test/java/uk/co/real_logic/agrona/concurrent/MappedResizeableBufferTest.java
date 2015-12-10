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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.real_logic.agrona.IoUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import static org.junit.Assert.assertEquals;

public class MappedResizeableBufferTest
{

    private static final long SIZE = 2 * (long) Integer.MAX_VALUE;
    private static final String PATH = IoUtil.tmpDirName() +  "/eg-buffer";
    public static final int VALUE = 4;

    private static RandomAccessFile file;
    private static FileChannel channel;

    private MappedResizeableBuffer buffer;

    @BeforeClass
    public static void setUp() throws IOException
    {
        file = new RandomAccessFile(PATH, "rw");
        file.setLength(SIZE);
        channel = file.getChannel();
    }

    @Test
    public void shouldWriteDataToBuffer() throws IOException
    {
        buffer = new MappedResizeableBuffer(channel, 100);

        exchangeDataAt(50L);
    }

    @Test
    public void shouldResizeBufferToOver2GB() throws IOException
    {
        buffer = new MappedResizeableBuffer(channel, 100);

        buffer.resize(SIZE);

        exchangeDataAt(SIZE - 4);
    }

    @Test
    public void shouldReadPreviousWrites() throws IOException
    {
        buffer = new MappedResizeableBuffer(channel, 100);

        exchangeDataAt(50L);

        buffer.resize(SIZE);

        assertEquals(VALUE, buffer.getInt(50L));
    }

    @Test
    public void shouldReadBytesFromOtherBuffer() throws IOException
    {
        buffer = new MappedResizeableBuffer(channel, SIZE);

        exchangeDataAt(SIZE - 4);

        buffer.close();

        buffer = new MappedResizeableBuffer(channel, SIZE);

        assertEquals(VALUE, buffer.getInt(SIZE - 4));
    }

    private void exchangeDataAt(final long index) throws IOException
    {
        buffer.putInt(index, VALUE);
        assertEquals(VALUE, buffer.getInt(index));
    }

    @After
    public void close()
    {
        buffer.close();
    }

    @AfterClass
    public static void tearDown() throws IOException
    {
        channel.close();
        IoUtil.deleteIfExists(new File(PATH));
    }

}
