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
package org.agrona;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.Stream;

import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class BufferStringOperationsTest
{
    private static final int BUFFER_CAPACITY = 256;
    private static final int INDEX = 8;

    private static Stream<MutableDirectBuffer> buffers()
    {
        return Stream.of(
            new UnsafeBuffer(new byte[BUFFER_CAPACITY]),
            new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_CAPACITY)),
            new ExpandableArrayBuffer(BUFFER_CAPACITY),
            new ExpandableDirectByteBuffer(BUFFER_CAPACITY));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldInsertNonAsciiAsQuestionMark(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World £";

        buffer.putStringAscii(INDEX, value);
        assertThat(buffer.getStringAscii(INDEX), is("Hello World ?"));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldAppendAsciiStringInParts(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World Test";

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
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripAsciiStringNativeLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringAscii(INDEX, value);

        assertThat(buffer.getStringAscii(INDEX), is(value));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripAsciiStringBigEndianLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringAscii(INDEX, value, ByteOrder.BIG_ENDIAN);

        assertThat(buffer.getStringAscii(INDEX, ByteOrder.BIG_ENDIAN), is(value));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripAsciiStringWithoutLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringWithoutLengthAscii(INDEX, value);

        assertThat(buffer.getStringWithoutLengthAscii(INDEX, value.length()), is(value));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripUtf8StringNativeLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello£ World £";

        buffer.putStringUtf8(INDEX, value);

        assertThat(buffer.getStringUtf8(INDEX), is(value));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripUtf8StringBigEndianLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello£ World £";

        buffer.putStringUtf8(INDEX, value, ByteOrder.BIG_ENDIAN);

        assertThat(buffer.getStringUtf8(INDEX, ByteOrder.BIG_ENDIAN), is(value));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void shouldRoundTripUtf8StringWithoutLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello£ World £";

        final int encodedLength = buffer.putStringWithoutLengthUtf8(INDEX, value);

        assertThat(buffer.getStringWithoutLengthUtf8(INDEX, encodedLength), is(value));
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
        final Appendable appendable = new StringBuilder();
        final int encodedLength = buffer.getStringWithoutLengthAscii(INDEX + SIZE_OF_INT, length, appendable);

        assertThat(encodedLength, is(length));
        assertThat(appendable.toString(), is("Hel?o"));
    }
}
