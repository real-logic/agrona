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
package org.agrona.io;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.collections.MutableInteger;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.DataOutput;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DirectBufferDataInputAgronaTest extends DirectBufferDataInputTest
{
    UnsafeBuffer toUnsafeBuffer(final ThrowingConsumer<DataOutput> dataProvider) throws Throwable
    {
        final ExpandableArrayBuffer out = new ExpandableArrayBuffer();
        final MutableInteger index = new MutableInteger();
        dataProvider.accept(new DataOutputForTest(out, index));

        return new UnsafeBuffer(out.byteArray(), 0, index.get());
    }

    @Override
    ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }

    @Test
    void shouldReadUtf()
    {
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();

        buffer.putLong(0, 0);
        final int bytesWritten = buffer.putStringUtf8(8, "zażółć gęślą jaźń北查爾斯頓");
        buffer.putLong(8 + bytesWritten, 0);

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer, 8, bytesWritten);
        dataInput.byteOrder(ByteOrder.LITTLE_ENDIAN);

        assertEquals("zażółć gęślą jaźń北查爾斯頓", dataInput.readStringUTF8());
    }

    @Test
    void shouldThrowWhenCannotReadSizeOfUtfString() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out -> {});

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        assertThrows(IndexOutOfBoundsException.class, dataInput::readStringUTF8);
    }

    @Test
    void shouldThrowExceptionWhenCannotReadString() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out -> out.writeShort(42));

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        assertThrows(IndexOutOfBoundsException.class, dataInput::readStringUTF8);
    }

    @Test
    void shouldReadAsciiString()
    {
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();

        buffer.putLong(0, 0);
        final int bytesWritten = buffer.putStringAscii(8, "Cupcake ipsum dolor sit amet.");
        buffer.putLong(8 + bytesWritten, 0);

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer, 8, bytesWritten);
        dataInput.byteOrder(ByteOrder.LITTLE_ENDIAN);

        assertEquals("Cupcake ipsum dolor sit amet.", dataInput.readStringAscii());
    }

    @Test
    void shouldThrowWhenCannotReadSizeOfAsciiString() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer((out) -> {});

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        assertThrows(IndexOutOfBoundsException.class, dataInput::readStringAscii);
    }

    @Test
    void shouldThrowExceptionWhenCannotReadAsciiString() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer((out) -> out.writeShort(42));

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        assertThrows(IndexOutOfBoundsException.class, dataInput::readStringAscii);
    }

    @Test
    void shouldReadAsciiStringIntoAppendable()
    {
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();

        buffer.putLong(0, 0);
        final int bytesWritten = buffer.putStringAscii(8, "Cupcake ipsum dolor sit amet.");
        buffer.putLong(8 + bytesWritten, 0);

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer, 8, bytesWritten);
        dataInput.byteOrder(ByteOrder.LITTLE_ENDIAN);

        final StringBuilder actual = new StringBuilder();
        dataInput.readStringAscii(actual);

        assertEquals("Cupcake ipsum dolor sit amet.", actual.toString());
    }

    private static class DataOutputForTest implements DataOutput
    {
        private final ExpandableArrayBuffer out;
        private final MutableInteger index;

        DataOutputForTest(final ExpandableArrayBuffer out, final MutableInteger index)
        {
            this.out = out;
            this.index = index;
        }

        public void write(final int b)
        {
            out.putByte(index.get(), (byte)b);
            index.increment();
        }

        public void write(final byte[] b)
        {
            out.putBytes(index.get(), b);
            index.addAndGet(b.length);
        }

        public void write(final byte[] b, final int off, final int len)
        {
            out.putBytes(index.get(), b, off, len);
            index.addAndGet(len);
        }

        public void writeBoolean(final boolean v)
        {
            out.putByte(index.get(), (byte)(v ? 1 : 0));
            index.increment();
        }

        public void writeByte(final int v)
        {
            out.putByte(index.get(), (byte)v);
            index.increment();
        }

        public void writeShort(final int v)
        {
            out.putShort(index.get(), (short)v);
            index.addAndGet(2);
        }

        public void writeChar(final int v)
        {
            out.putChar(index.get(), (char)v);
            index.addAndGet(2);
        }

        public void writeInt(final int v)
        {
            out.putInt(index.get(), v);
            index.addAndGet(4);
        }

        public void writeLong(final long v)
        {
            out.putLong(index.get(), v);
            index.addAndGet(8);
        }

        public void writeFloat(final float v)
        {
            out.putFloat(index.get(), v);
            index.addAndGet(4);
        }

        public void writeDouble(final double v)
        {
            out.putDouble(index.get(), v);
            index.addAndGet(8);
        }

        public void writeBytes(final String s)
        {
            throw new UnsupportedOperationException();
        }

        public void writeChars(final String s)
        {
            for (int i = 0; i < s.length(); i++)
            {
                final char c = s.charAt(i);
                out.putChar(index.get(), c);
                index.addAndGet(2);
            }
        }

        public void writeUTF(final String s)
        {
            final int startingPosition = index.get();
            index.addAndGet(2);

            final int bytesWritten = out.putStringWithoutLengthUtf8(index.get(), s);
            index.addAndGet(bytesWritten);

            out.putShort(startingPosition, (short)bytesWritten);
        }
    }
}
