/*
 * Copyright 2014-2024 Real Logic Limited.
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.DirectBuffer.STR_HEADER_LEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BufferStringOperationsTest
{
    private static final int BUFFER_CAPACITY = 256;
    private static final int INDEX = 8;
    private static final byte[] BUFFER_DATA = new byte[BUFFER_CAPACITY];

    private static Stream<MutableDirectBuffer> buffers()
    {
        return Stream.of(
            randomBytes(new UnsafeBuffer(new byte[BUFFER_CAPACITY])),
            randomBytes(new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_CAPACITY))),
            randomBytes(new ExpandableArrayBuffer(BUFFER_CAPACITY)),
            randomBytes(new ExpandableDirectByteBuffer(BUFFER_CAPACITY)));
    }

    private static MutableDirectBuffer randomBytes(final MutableDirectBuffer buffer)
    {
        buffer.setMemory(0, BUFFER_CAPACITY, (byte)ThreadLocalRandom.current().nextInt(1, 128));
        return buffer;
    }

    private static void assertOtherDataWasNotModified(
        final MutableDirectBuffer buffer, final int index, final int stringLength)
    {
        for (int i = 0; i < index; i++)
        {
            assertEquals(BUFFER_DATA[i], buffer.getByte(i));
        }

        for (int i = index + stringLength; i < BUFFER_CAPACITY; i++)
        {
            assertEquals(BUFFER_DATA[i], buffer.getByte(i));
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldInsertNonAsciiAsQuestionMark(final MutableDirectBuffer buffer)
    {
        final String value = "Helloế World £";
        buffer.getBytes(0, BUFFER_DATA);

        assertEquals(value.length() + STR_HEADER_LEN, buffer.putStringAscii(INDEX, value));

        assertThat(buffer.getStringAscii(INDEX), is("Hello? World ?"));
        assertOtherDataWasNotModified(buffer, INDEX, value.length() + STR_HEADER_LEN);
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldAppendAsciiStringInParts(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World Test";
        buffer.getBytes(0, BUFFER_DATA);

        int stringIndex = 0;
        int bufferIndex = INDEX + SIZE_OF_INT;

        bufferIndex += buffer.putStringWithoutLengthAscii(bufferIndex, value, stringIndex, 5);

        stringIndex += 5;
        bufferIndex += buffer.putStringWithoutLengthAscii(bufferIndex, value, stringIndex, 5);

        stringIndex += 5;
        bufferIndex += buffer.putStringWithoutLengthAscii(
            bufferIndex, value, stringIndex, value.length() - stringIndex);

        assertThat(bufferIndex, is(value.length() + INDEX + SIZE_OF_INT));
        buffer.putInt(INDEX, value.length());

        assertThat(buffer.getStringWithoutLengthAscii(INDEX + SIZE_OF_INT, value.length()), is(value));
        assertThat(buffer.getStringAscii(INDEX), is(value));
        assertOtherDataWasNotModified(buffer, INDEX, value.length() + STR_HEADER_LEN);
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripAsciiStringNativeLength(final MutableDirectBuffer buffer)
    {
        for (final String value : new String[]{ "Hello World", "42" })
        {
            buffer.getBytes(0, BUFFER_DATA);

            assertEquals(value.length() + STR_HEADER_LEN, buffer.putStringAscii(INDEX, value));

            assertThat(buffer.getStringAscii(INDEX), is(value));
            assertOtherDataWasNotModified(buffer, INDEX, value.length() + STR_HEADER_LEN);
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripAsciiStringBigEndianLength(final MutableDirectBuffer buffer)
    {
        for (final String value : new String[]{ "Linux", "", "MS Dos" })
        {
            for (final ByteOrder byteOrder : new ByteOrder[]{ ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN })
            {
                buffer.getBytes(0, BUFFER_DATA);

                assertEquals(value.length() + STR_HEADER_LEN, buffer.putStringAscii(INDEX, value, byteOrder));

                assertThat(buffer.getStringAscii(INDEX, byteOrder), is(value));
                assertOtherDataWasNotModified(buffer, INDEX, value.length() + STR_HEADER_LEN);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripAsciiStringWithoutLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";
        buffer.getBytes(0, BUFFER_DATA);

        assertEquals(value.length(), buffer.putStringWithoutLengthAscii(INDEX, value));

        assertThat(buffer.getStringWithoutLengthAscii(INDEX, value.length()), is(value));
        assertOtherDataWasNotModified(buffer, INDEX, value.length());
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripUtf8StringNativeLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello, ngân hà!";
        final byte[] encodedBytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.getBytes(0, BUFFER_DATA);

        assertEquals(encodedBytes.length + STR_HEADER_LEN, buffer.putStringUtf8(INDEX, value));

        assertThat(buffer.getStringUtf8(INDEX), is(value));
        assertOtherDataWasNotModified(buffer, INDEX, encodedBytes.length + STR_HEADER_LEN);
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripUtf8StringBigEndianLength(final MutableDirectBuffer buffer)
    {
        final String value = "שלום עולם!";
        final byte[] encodedBytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.getBytes(0, BUFFER_DATA);

        assertEquals(encodedBytes.length + STR_HEADER_LEN, buffer.putStringUtf8(INDEX, value, ByteOrder.BIG_ENDIAN));

        assertThat(buffer.getStringUtf8(INDEX, ByteOrder.BIG_ENDIAN), is(value));
        for (int i = 0; i < encodedBytes.length; i++)
        {
            assertEquals(encodedBytes[i], buffer.getByte(INDEX + STR_HEADER_LEN + i));
        }
        assertOtherDataWasNotModified(buffer, INDEX, encodedBytes.length + STR_HEADER_LEN);
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripUtf8StringWithoutLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello£ World £";
        final int index = 42;
        buffer.getBytes(0, BUFFER_DATA);

        final int encodedLength = buffer.putStringWithoutLengthUtf8(index, value);

        assertThat(buffer.getStringWithoutLengthUtf8(index, encodedLength), is(value));
        assertOtherDataWasNotModified(buffer, index, encodedLength);
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldGetAsciiToAppendable(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringAscii(INDEX, value);

        final Appendable appendable = new StringBuilder();
        final int encodedLength = buffer.getStringAscii(INDEX, appendable);

        assertThat(encodedLength, is(value.length()));
        assertThat(appendable.toString(), is(value));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldGetAsciiWithByteOrderToAppendable(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringAscii(INDEX, value, ByteOrder.BIG_ENDIAN);

        final Appendable appendable = new StringBuilder();
        final int encodedLength = buffer.getStringAscii(INDEX, appendable, ByteOrder.BIG_ENDIAN);

        assertThat(encodedLength, is(value.length()));
        assertThat(appendable.toString(), is(value));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldGetAsciiToAppendableForLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringAscii(INDEX, value);

        final int length = 5;
        final Appendable appendable = new StringBuilder();
        final int encodedLength = buffer.getStringAscii(INDEX, length, appendable);

        assertThat(encodedLength, is(length));
        assertThat(appendable.toString(), is(value.substring(0, length)));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldAppendWithInvalidChar(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringAscii(INDEX, value);
        buffer.putByte(INDEX + SIZE_OF_INT + 5, (byte)163);

        final Appendable appendable = new StringBuilder();
        final int encodedLength = buffer.getStringAscii(INDEX, appendable);

        assertThat(encodedLength, is(value.length()));
        assertThat(appendable.toString(), is("Hello?World"));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldAppendWithInvalidCharWithoutLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringAscii(INDEX, value);
        buffer.putByte(INDEX + SIZE_OF_INT + 3, (byte)163);

        final int length = 5;
        final StringBuilder appendable = new StringBuilder();
        final int encodedLength = buffer.getStringWithoutLengthAscii(INDEX + SIZE_OF_INT, length, appendable);

        assertThat(encodedLength, is(length));
        assertThat(appendable.toString(), is("Hel?o"));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void putStringAsciiTreatsNullAndEmptyValueTheSame(final MutableDirectBuffer buffer)
    {
        for (final String emptyValue : new String[]{ null, "" })
        {
            buffer.getBytes(0, BUFFER_DATA);

            assertEquals(STR_HEADER_LEN, buffer.putStringAscii(INDEX, emptyValue));

            assertEquals(0, buffer.getInt(INDEX));
            assertEquals("", buffer.getStringAscii(INDEX));
            assertOtherDataWasNotModified(buffer, INDEX, STR_HEADER_LEN);
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void putStringAsciiWithByteOrderTreatsNullAndEmptyValueTheSame(final MutableDirectBuffer buffer)
    {
        for (final String emptyValue : new String[]{ null, "" })
        {
            for (final ByteOrder byteOrder : new ByteOrder[]{ ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN })
            {
                final int index = 17;
                buffer.getBytes(0, BUFFER_DATA);

                assertEquals(STR_HEADER_LEN, buffer.putStringAscii(index, emptyValue, byteOrder));

                assertEquals(0, buffer.getInt(index, byteOrder));
                assertEquals("", buffer.getStringAscii(index, byteOrder));
                assertOtherDataWasNotModified(buffer, index, STR_HEADER_LEN);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void putStringWithoutLengthAsciiTreatsNullAndEmptyValueTheSame(final MutableDirectBuffer buffer)
    {
        for (final String emptyValue : new String[]{ null, "" })
        {
            buffer.getBytes(0, BUFFER_DATA);

            assertEquals(0, buffer.putStringWithoutLengthAscii(INDEX, emptyValue));

            assertOtherDataWasNotModified(buffer, INDEX, 0);
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void putStringAsciiCharSequenceTreatsNullAndEmptyValueTheSame(final MutableDirectBuffer buffer)
    {
        for (final CharSequence emptyValue : new CharSequence[]{ null, "" })
        {
            buffer.getBytes(0, BUFFER_DATA);

            assertEquals(STR_HEADER_LEN, buffer.putStringAscii(INDEX, emptyValue));

            assertEquals(0, buffer.getInt(INDEX));
            assertEquals("", buffer.getStringAscii(INDEX));
            assertOtherDataWasNotModified(buffer, INDEX, STR_HEADER_LEN);
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void putStringAsciiCharSequenceWithByteOrderTreatsNullAndEmptyValueTheSame(final MutableDirectBuffer buffer)
    {
        for (final CharSequence emptyValue : new CharSequence[]{ null, "" })
        {
            for (final ByteOrder byteOrder : new ByteOrder[]{ ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN })
            {
                buffer.getBytes(0, BUFFER_DATA);

                assertEquals(STR_HEADER_LEN, buffer.putStringAscii(INDEX, emptyValue, byteOrder));

                assertEquals(0, buffer.getInt(INDEX, byteOrder));
                assertEquals("", buffer.getStringAscii(INDEX, byteOrder));
                assertOtherDataWasNotModified(buffer, INDEX, STR_HEADER_LEN);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void putStringWithoutLengthAsciiCharSequenceTreatsNullAndEmptyValueTheSame(final MutableDirectBuffer buffer)
    {
        for (final CharSequence emptyValue : new CharSequence[]{ null, "" })
        {
            buffer.getBytes(0, BUFFER_DATA);

            assertEquals(0, buffer.putStringWithoutLengthAscii(INDEX, emptyValue));

            assertOtherDataWasNotModified(buffer, INDEX, 0);
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void putStringUtf8EncodesNullAsAConstantString(final MutableDirectBuffer buffer)
    {
        buffer.getBytes(0, BUFFER_DATA);

        assertEquals(4 + STR_HEADER_LEN, buffer.putStringUtf8(INDEX, null));

        assertEquals(4, buffer.getInt(INDEX));
        assertEquals("null", buffer.getStringUtf8(INDEX));
        assertOtherDataWasNotModified(buffer, INDEX, 4 + STR_HEADER_LEN);
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void putStringUtf8WithByteOrderEncodesNullAsAConstantString(final MutableDirectBuffer buffer)
    {
        final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        buffer.getBytes(0, BUFFER_DATA);

        assertEquals(4 + STR_HEADER_LEN, buffer.putStringUtf8(INDEX, null, byteOrder));

        assertEquals(4, buffer.getInt(INDEX, byteOrder));
        assertEquals("null", buffer.getStringUtf8(INDEX, byteOrder));
        assertOtherDataWasNotModified(buffer, INDEX, 4 + STR_HEADER_LEN);
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void putStringWithoutLengthUtf8EncodesNullAsAConstantString(final MutableDirectBuffer buffer)
    {
        final int index = 100;
        buffer.getBytes(0, BUFFER_DATA);

        assertEquals(4, buffer.putStringWithoutLengthUtf8(index, null));

        assertEquals("null", buffer.getStringWithoutLengthUtf8(index, 4));
        assertOtherDataWasNotModified(buffer, index, 4);
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void getStringAsciiWithLengthReturnsASubString(final MutableDirectBuffer buffer)
    {
        final int index = 29;
        final String value = "One Two Three";
        buffer.getBytes(0, BUFFER_DATA);

        buffer.putStringAscii(index, value);

        assertEquals("One Tw", buffer.getStringAscii(index, 6));
        assertEquals("ne Two", buffer.getStringAscii(index + 1, 6));
        assertSame("", buffer.getStringAscii(index, 0));
        assertSame("", buffer.getStringAscii(index + 10, 0));
        assertOtherDataWasNotModified(buffer, index, value.length() + STR_HEADER_LEN);
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void getStringAsciiReturnsAnEmptyStringConstantWhenValueIsEmpty(final MutableDirectBuffer buffer)
    {
        final int index = 13;

        buffer.putStringAscii(index, null);

        assertSame("", buffer.getStringAscii(index));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void getStringAsciiWithByteOrderReturnsAnEmptyStringConstantWhenValueIsEmpty(final MutableDirectBuffer buffer)
    {
        final int index = 52;
        for (final ByteOrder byteOrder : new ByteOrder[]{ ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN })
        {
            buffer.putStringAscii(index, "", byteOrder);

            assertSame("", buffer.getStringAscii(index, byteOrder));
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void getStringWithoutLengthAsciiReturnsAnEmptyStringConstantWhenLengthIsZero(final MutableDirectBuffer buffer)
    {
        assertSame("", buffer.getStringWithoutLengthAscii(32, 0));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void getStringUtf8ReturnsAnEmptyStringConstantWhenValueIsEmpty(final MutableDirectBuffer buffer)
    {
        final int index = 99;

        buffer.putStringUtf8(index, "");

        assertSame("", buffer.getStringUtf8(index));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void getStringUtf8WithByteOrderReturnsAnEmptyStringConstantWhenValueIsEmpty(final MutableDirectBuffer buffer)
    {
        final int index = 25;
        for (final ByteOrder byteOrder : new ByteOrder[]{ ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN })
        {
            buffer.putStringUtf8(index, "", byteOrder);

            assertSame("", buffer.getStringUtf8(index, byteOrder));
        }
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void getStringWithoutLengthUtf8ReturnsAnEmptyStringConstantWhenLengthIsZero(final MutableDirectBuffer buffer)
    {
        assertSame("", buffer.getStringWithoutLengthUtf8(32, 0));
    }
}
