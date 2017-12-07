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
package org.agrona;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class BufferStringOperationsTest
{
    private static final int BUFFER_CAPACITY = 4096;
    private static final int INDEX = 8;

    @DataPoint
    public static final MutableDirectBuffer ARRAY_BUFFER = new UnsafeBuffer(
        new byte[BUFFER_CAPACITY], 0, BUFFER_CAPACITY);

    @DataPoint
    public static final MutableDirectBuffer DIRECT_BYTE_BUFFER = new UnsafeBuffer(
        ByteBuffer.allocateDirect(BUFFER_CAPACITY), 0, BUFFER_CAPACITY);

    @DataPoint
    public static final MutableDirectBuffer EXPANDABLE_ARRAY_BUFFER = new ExpandableArrayBuffer(BUFFER_CAPACITY);

    @DataPoint
    public static final MutableDirectBuffer EXPANDABLE_DIRECT_BYTE_BUFFER =
        new ExpandableDirectByteBuffer(BUFFER_CAPACITY);

    @Theory
    public void shouldInsertNonAsciiAsQuestionMark(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World £";

        buffer.putStringAscii(INDEX, value);
        assertThat(buffer.getStringAscii(INDEX), is("Hello World ?"));
    }

    @Theory
    public void shouldAppendAsciiStringInParts(final MutableDirectBuffer buffer)
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

    @Theory
    public void shouldRoundTripAsciiStringNativeLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringAscii(INDEX, value);

        assertThat(buffer.getStringAscii(INDEX), is(value));
    }

    @Theory
    public void shouldRoundTripAsciiStringBigEndianLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringAscii(INDEX, value, ByteOrder.BIG_ENDIAN);

        assertThat(buffer.getStringAscii(INDEX, ByteOrder.BIG_ENDIAN), is(value));
    }

    @Theory
    public void shouldRoundTripAsciiStringWithoutLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello World";

        buffer.putStringWithoutLengthAscii(INDEX, value);

        assertThat(buffer.getStringWithoutLengthAscii(INDEX, value.length()), is(value));
    }


    @Theory
    public void shouldRoundTripUtf8StringNativeLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello£ World £";

        buffer.putStringUtf8(INDEX, value);

        assertThat(buffer.getStringUtf8(INDEX), is(value));
    }

    @Theory
    public void shouldRoundTripUtf8StringBigEndianLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello£ World £";

        buffer.putStringUtf8(INDEX, value, ByteOrder.BIG_ENDIAN);

        assertThat(buffer.getStringUtf8(INDEX, ByteOrder.BIG_ENDIAN), is(value));
    }

    @Theory
    public void shouldRoundTripUtf8StringWithoutLength(final MutableDirectBuffer buffer)
    {
        final String value = "Hello£ World £";

        final int encodedLength = buffer.putStringWithoutLengthUtf8(INDEX, value);

        assertThat(buffer.getStringWithoutLengthUtf8(INDEX, encodedLength), is(value));
    }
}
