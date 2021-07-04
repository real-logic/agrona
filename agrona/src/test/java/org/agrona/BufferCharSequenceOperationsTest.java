/*
 * Copyright 2014-2021 Real Logic Limited.
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

public class BufferCharSequenceOperationsTest
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
    public void shouldInsertNonAsciiAsQuestionMark(final MutableDirectBuffer buffer)
    {
        final CharSequence value = new StringBuilder("Hello World £");
        final CharSequence expected = "Hello World ?";

        buffer.putStringAscii(INDEX, value);
        assertThat(buffer.getStringAscii(INDEX), is(expected));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    public void shouldAppendAsciiStringInParts(final MutableDirectBuffer buffer)
    {
        final CharSequence value = new StringBuilder("Hello World Test");
        final String expected = "Hello World Test";

        int stringIndex = 0;
        int bufferIndex = INDEX + SIZE_OF_INT;

        bufferIndex += buffer.putStringWithoutLengthAscii(bufferIndex, value, stringIndex, 5);

        stringIndex += 5;
        bufferIndex += buffer.putStringWithoutLengthAscii(bufferIndex, value, stringIndex, 5);

        stringIndex += 5;
        bufferIndex += buffer.putStringWithoutLengthAscii(
            bufferIndex, value, stringIndex, value.length() - stringIndex);

        assertThat(bufferIndex, is(expected.length() + INDEX + SIZE_OF_INT));
        buffer.putInt(INDEX, expected.length());

        assertThat(buffer.getStringWithoutLengthAscii(INDEX + SIZE_OF_INT, expected.length()), is(expected));
        assertThat(buffer.getStringAscii(INDEX), is(expected));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    public void shouldRoundTripAsciiStringNativeLength(final MutableDirectBuffer buffer)
    {
        final CharSequence value = new StringBuilder("Hello World");
        final String expected = "Hello World";

        buffer.putStringAscii(INDEX, value);

        assertThat(buffer.getStringAscii(INDEX), is(expected));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    public void shouldRoundTripAsciiStringBigEndianLength(final MutableDirectBuffer buffer)
    {
        final CharSequence value = new StringBuilder("Hello World");
        final String expected = "Hello World";

        buffer.putStringAscii(INDEX, value, ByteOrder.BIG_ENDIAN);

        assertThat(buffer.getStringAscii(INDEX, ByteOrder.BIG_ENDIAN), is(expected));
    }

    @ParameterizedTest
    @MethodSource("buffers")
    public void shouldRoundTripAsciiStringWithoutLength(final MutableDirectBuffer buffer)
    {
        final CharSequence value = new StringBuilder("Hello World");
        final String expected = "Hello World";

        buffer.putStringWithoutLengthAscii(INDEX, value);

        assertThat(buffer.getStringWithoutLengthAscii(INDEX, value.length()), is(expected));
    }
}
