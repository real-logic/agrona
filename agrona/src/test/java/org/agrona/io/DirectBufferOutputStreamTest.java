/*
 * Copyright 2018 Real Logic Ltd.
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
package org.agrona.io;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DirectBufferOutputStreamTest
{

    @Test
    public void shouldCorrectlyConvertUInt8ToBytes()
    {
        final byte[] data = new byte[2];
        final MutableDirectBuffer buffer = new UnsafeBuffer(data);
        final DirectBufferOutputStream outputStream = new DirectBufferOutputStream(buffer);

        outputStream.write(255);
        outputStream.write(128);

        assertEquals(data[0], -1);
        assertEquals(data[1], -128);
    }

    @Test
    public void shouldWriteByteArray() throws IOException
    {
        final byte[] data = new byte[2];
        final MutableDirectBuffer buffer = new UnsafeBuffer(data);
        final DirectBufferOutputStream outputStream = new DirectBufferOutputStream(buffer);

        final byte[] source = { 1, 2 };
        outputStream.write(source);

        assertArrayEquals(source, data);
    }

}