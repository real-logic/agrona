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

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.DataOutput;
import java.io.EOFException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

abstract class DirectBufferDataInputTest
{

    abstract UnsafeBuffer toUnsafeBuffer(ThrowingConsumer<DataOutput> dataProvider) throws Throwable;

    abstract ByteOrder byteOrder();

    @Test
    void shouldWrapBufferUsingConstructor() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        assertEquals(124325, dataInput.readInt());
        assertEquals(2353242342L, dataInput.readLong());
        assertEquals(31415926535L, dataInput.readLong());
        assertEquals(0, dataInput.remaining());
    }

    @Test
    void shouldWrapBuffer() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        assertEquals(124325, dataInput.readInt());
        assertEquals(2353242342L, dataInput.readLong());
        assertEquals(31415926535L, dataInput.readLong());
        assertEquals(0, dataInput.remaining());
    }

    @Test
    void shouldWrapBufferWithOffsetUsingConstructor() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            16
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(2353242342L, dataInput.readLong());
        assertEquals(31415926535L, dataInput.readLong());
        assertEquals(0, dataInput.remaining());
    }

    @Test
    void shouldWrapBufferWithOffsetAndLengthUsingConstructor() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            8
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(2353242342L, dataInput.readLong());
        assertEquals(0, dataInput.remaining());
    }

    @Test
    void shouldWrapBufferWithOffsetAndLength() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(new UnsafeBuffer(new byte[0]));
        dataInput.byteOrder(byteOrder());

        dataInput.wrap(
            buffer,
            4,
            8
        );

        assertEquals(2353242342L, dataInput.readLong());
        assertEquals(0, dataInput.remaining());
    }

    @Test
    void shouldThrowExceptionOnNegativeOffset()
    {
        assertThrows(IllegalArgumentException.class, () ->
        new DirectBufferDataInput(new UnsafeBuffer(new byte[11]), -1, 10));
    }

    @Test
    void shouldThrowExceptionOnNegativeLength()
    {
        assertThrows(IllegalArgumentException.class, () ->
        new DirectBufferDataInput(new UnsafeBuffer(new byte[11]), 1, -1));
    }

    @Test
    void shouldThrowExceptionOnInsufficientCapacity()
    {
        assertThrows(IllegalArgumentException.class, () ->
        new DirectBufferDataInput(new UnsafeBuffer(new byte[11]), 2, 10));
    }

    @Test
    void shouldReadFully() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            16
        );
        dataInput.byteOrder(byteOrder());

        final byte[] destination = new byte[16];
        ThreadLocalRandom.current().nextBytes(destination);
        dataInput.readFully(destination);

        final UnsafeBuffer actual = new UnsafeBuffer(destination);
        assertEquals(2353242342L, actual.getLong(0, byteOrder()));
        assertEquals(31415926535L, actual.getLong(8, byteOrder()));
    }

    @Test
    void shouldReadFullyWithDestinationAndOffset() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            16
        );
        dataInput.byteOrder(byteOrder());

        final byte[] destination = new byte[64];
        dataInput.readFully(destination, 20, 8);

        final UnsafeBuffer actual = new UnsafeBuffer(destination);
        assertEquals(0L, actual.getLong(0));
        assertEquals(0L, actual.getLong(8));
        assertEquals(0L, actual.getInt(16));
        assertEquals(2353242342L, actual.getLong(20, byteOrder()));
        assertEquals(0L, actual.getLong(28));
    }

    @Test
    void shouldThrowNPEIfDestinationIsNull() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);

        assertThrows(NullPointerException.class, () -> dataInput.readFully(null));
        assertThrows(NullPointerException.class, () -> dataInput.readFully(null, 0, 0));
    }

    @Test
    void shouldThrowIOOBEIfOffsetIsNegative() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            16
        );
        dataInput.byteOrder(byteOrder());

        assertThrows(IndexOutOfBoundsException.class,
            () -> dataInput.readFully(new byte[0], -1, 10)
        );
    }

    @Test
    void shouldThrowIOOBEIfLengthIsNegative() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            16
        );
        dataInput.byteOrder(byteOrder());

        assertThrows(IndexOutOfBoundsException.class,
            () -> dataInput.readFully(new byte[0], 10, -1)
        );
    }

    @Test
    void shouldThrowIOOBEIfOffsetPlusLengthIsGreaterThanArraySize() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            16
        );
        dataInput.byteOrder(byteOrder());

        assertThrows(IndexOutOfBoundsException.class,
            () -> dataInput.readFully(new byte[223], 11, 213)
        );
    }

    @Test
    void shouldSkipBytes() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            16
        );
        dataInput.byteOrder(byteOrder());

        dataInput.skipBytes(3);
        assertEquals(13, dataInput.remaining());

        dataInput.skipBytes(2);
        assertEquals(11, dataInput.remaining());

        dataInput.skipBytes(1);
        assertEquals(10, dataInput.remaining());

        dataInput.skipBytes(-1);
        assertEquals(11, dataInput.remaining());
    }

    @Test
    void shouldSkipBytesButNotPastBufferSize() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(124325);
            out.writeLong(2353242342L);
            out.writeLong(31415926535L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            16
        );
        dataInput.byteOrder(byteOrder());

        dataInput.skipBytes(500);
        assertEquals(0, dataInput.remaining());
    }

    @Test
    void shouldReadBoolean() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeBoolean(false);
            out.writeBoolean(false);
            out.writeBoolean(true);
            out.writeBoolean(false);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            2,
            2
        );
        dataInput.byteOrder(byteOrder());

        assertTrue(dataInput.readBoolean());
        assertFalse(dataInput.readBoolean());
    }

    @Test
    void shouldReadByte() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(0);
            out.writeByte((byte)-44);
            out.writeByte((byte)223);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            2
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(-44, dataInput.readByte());
        assertEquals(-33, dataInput.readByte());
    }

    @Test
    void shouldReadUnsignedByte() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(0);
            out.writeByte((byte)-44);
            out.writeByte((byte)223);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            2
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(212, dataInput.readUnsignedByte());
        assertEquals(223, dataInput.readUnsignedByte());
    }

    @Test
    void shouldReadShort() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(0);
            out.writeShort((short)13244);
            out.writeShort((short)22321);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            4
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(13244, dataInput.readShort());
        assertEquals(22321, dataInput.readShort());
    }

    @Test
    void shouldReadUnsignedShort() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeInt(0);
            out.writeShort((short)-13244);
            out.writeShort((short)22321);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            4,
            4
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(52292, dataInput.readUnsignedShort());
        assertEquals(22321, dataInput.readUnsignedShort());
    }

    @Test
    void shouldReadChar() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out -> out.writeChars("zażółć gęślą jaźń北查爾斯頓"));
        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            30,
            14
        );
        dataInput.byteOrder(byteOrder());

        assertEquals('ź', dataInput.readChar());
        assertEquals('ń', dataInput.readChar());
        assertEquals('北', dataInput.readChar());
        assertEquals('查', dataInput.readChar());
    }

    @Test
    void shouldReadInt() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeByte(0);
            out.writeInt(352345324);
            out.writeInt(314159265);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            1,
            8
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(352345324, dataInput.readInt());
        assertEquals(314159265, dataInput.readInt());
    }

    @Test
    void shouldReadLong() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeByte(0);
            out.writeLong(3523453241L);
            out.writeLong(1231415239265L);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            1,
            16
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(3523453241L, dataInput.readLong());
        assertEquals(1231415239265L, dataInput.readLong());
    }

    @Test
    void shouldReadFloat() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeByte(0);
            out.writeFloat(0.13f);
            out.writeFloat(123141523926.0f);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            1,
            8
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(0.13f, dataInput.readFloat());
        assertEquals(123141523926.0f, dataInput.readFloat());
    }

    @Test
    void shouldReadDouble() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeByte(0);
            out.writeDouble(0.13f);
            out.writeDouble(123141523926.0f);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            1,
            16
        );
        dataInput.byteOrder(byteOrder());

        assertEquals(0.13f, dataInput.readDouble());
        assertEquals(123141523926.0f, dataInput.readDouble());
    }

    @Test
    void shouldReadLines() throws Throwable
    {
        final String text = "Cupcake ipsum dolor sit amet. Souffle chocolate bar fruitcake cookie toffee. Candy\n" +
            "gummies cookie shortbread sweet cake topping wafer.\r\n" +
            "Bear claw apple pie danish carrot cake carrot cake halvah danish carrot cake. Brownie\r" +
            "danish toffee topping toffee. Sweet sesame snaps chocolate bar jujubes muffin shortbread.";

        final UnsafeBuffer buffer = toUnsafeBuffer(out -> out.write(text.getBytes(StandardCharsets.US_ASCII)));

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            8,
            241
        );
        dataInput.byteOrder(byteOrder());

        assertEquals("ipsum dolor sit amet. Souffle chocolate bar fruitcake cookie toffee. Candy",
            dataInput.readLine());
        assertEquals("gummies cookie shortbread sweet cake topping wafer.", dataInput.readLine());
        assertEquals("Bear claw apple pie danish carrot cake carrot cake halvah danish carrot cake. Brownie",
            dataInput.readLine());
        assertEquals("danish toffee topping toffe", dataInput.readLine());
    }

    @Test
    void shouldReturnNullIfThereIsNothingToRead() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out -> out.write("Test".getBytes(StandardCharsets.US_ASCII)));

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        assertEquals("Test", dataInput.readLine());
        assertNull(dataInput.readLine());
    }

    @Test
    void shouldReadLinesIntoAppendable() throws Throwable
    {
        final String text = "Cupcake ipsum dolor sit amet. Souffle chocolate bar fruitcake cookie toffee. Candy\n" +
            "gummies cookie shortbread sweet cake topping wafer.\r\n" +
            "Bear claw apple pie danish carrot cake carrot cake halvah danish carrot cake. Brownie\r" +
            "danish toffee topping toffee. Sweet sesame snaps chocolate bar jujubes muffin shortbread.";

        final UnsafeBuffer buffer = toUnsafeBuffer(out -> out.write(text.getBytes(StandardCharsets.US_ASCII)));

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        final StringBuilder actual = new StringBuilder();
        final int bytesRead = dataInput.readLine(actual);

        final String expected = "Cupcake ipsum dolor sit amet. Souffle chocolate bar fruitcake cookie toffee. Candy";
        assertEquals(expected, actual.toString());
        assertEquals(expected.length() + 1, bytesRead);
    }

    @Test
    void shouldReadLinesIntoAppendableWhenEmpty() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        final StringBuilder actual = new StringBuilder();
        final int bytesRead = dataInput.readLine(actual);

        assertTrue(actual.toString().isEmpty());
        assertEquals(0, bytesRead);
    }

    @Test
    void shouldReadUtf() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
            out.writeLong(0);
            out.writeUTF("zażółć gęślą jaźń北查爾斯頓");
            out.writeLong(0);
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(
            buffer,
            8,
            47);
        dataInput.byteOrder(byteOrder());

        assertEquals("zażółć gęślą jaźń北查爾斯頓", dataInput.readUTF());
    }

    @Test
    void shouldThrowWhenCannotReadSizeOfUtfString() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out ->
        {
        });

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        assertThrows(EOFException.class, dataInput::readUTF);
    }

    @Test
    void shouldThrowExceptionWhenCannotReadString() throws Throwable
    {
        final UnsafeBuffer buffer = toUnsafeBuffer(out -> out.writeShort(42));

        final DirectBufferDataInput dataInput = new DirectBufferDataInput(buffer);
        dataInput.byteOrder(byteOrder());

        assertThrows(EOFException.class, dataInput::readUTF);
    }
}
