/*
 * Copyright 2014-2020 Real Logic Limited.
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
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AsciiSequenceViewTest
{
    private static final int INDEX = 2;
    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[128]);
    private final AsciiSequenceView asciiSequenceView = new AsciiSequenceView();

    @Test
    public void shouldBeAbleToGetChars()
    {
        final String data = "stringy";
        buffer.putStringWithoutLengthAscii(INDEX, data);

        asciiSequenceView.wrap(buffer, INDEX, data.length());

        assertThat(asciiSequenceView.charAt(0), is('s'));
        assertThat(asciiSequenceView.charAt(1), is('t'));
        assertThat(asciiSequenceView.charAt(2), is('r'));
        assertThat(asciiSequenceView.charAt(3), is('i'));
        assertThat(asciiSequenceView.charAt(4), is('n'));
        assertThat(asciiSequenceView.charAt(5), is('g'));
        assertThat(asciiSequenceView.charAt(6), is('y'));
    }

    @Test
    public void shouldToString()
    {
        final String data = "a little bit of ascii";
        buffer.putStringWithoutLengthAscii(INDEX, data);

        asciiSequenceView.wrap(buffer, INDEX, data.length());

        assertThat(asciiSequenceView.toString(), is(data));
    }

    @Test
    public void shouldReturnCorrectLength()
    {
        final String data = "a little bit of ascii";
        buffer.putStringWithoutLengthAscii(INDEX, data);

        asciiSequenceView.wrap(buffer, INDEX, data.length());

        assertThat(asciiSequenceView.length(), is(data.length()));
    }

    @Test
    public void shouldCopyDataUnderTheView()
    {
        final String data = "a little bit of ascii";
        final int targetBufferOffset = 56;
        final MutableDirectBuffer targetBuffer = new UnsafeBuffer(new byte[128]);
        buffer.putStringWithoutLengthAscii(INDEX, data);
        asciiSequenceView.wrap(buffer, INDEX, data.length());

        asciiSequenceView.getBytes(targetBuffer, targetBufferOffset);

        assertThat(targetBuffer.getStringWithoutLengthAscii(targetBufferOffset, data.length()), is(data));
    }

    @Test
    public void shouldSubSequence()
    {
        final String data = "a little bit of ascii";
        buffer.putStringWithoutLengthAscii(INDEX, data);

        asciiSequenceView.wrap(buffer, INDEX, data.length());
        final AsciiSequenceView subSequenceView = asciiSequenceView.subSequence(2, 8);

        assertThat(subSequenceView.toString(), is("little"));
    }

    @Test
    public void shouldReturnEmptyStringWhenBufferIsNull()
    {
        assertEquals(0, asciiSequenceView.length());
        assertEquals("", asciiSequenceView.toString());
        assertEquals(0, asciiSequenceView.getBytes(new UnsafeBuffer(new byte[128]), 16));
    }

    @Test
    public void shouldThrowIndexOutOfBoundsExceptionWhenCharNotPresentAtGivenPosition()
    {
        final String data = "foo";
        buffer.putStringWithoutLengthAscii(INDEX, data);
        asciiSequenceView.wrap(buffer, INDEX, data.length());

        assertThrows(StringIndexOutOfBoundsException.class, () -> asciiSequenceView.charAt(4));
    }

    @Test
    public void shouldThrowExceptionWhenCharAtCalledWithNoBuffer()
    {
        assertThrows(StringIndexOutOfBoundsException.class, () -> asciiSequenceView.charAt(0));
    }

    @Test
    public void shouldThrowExceptionWhenCharAtCalledWithNegativeIndex()
    {
        final String data = "foo";
        buffer.putStringWithoutLengthAscii(INDEX, data);
        asciiSequenceView.wrap(buffer, INDEX, data.length());

        assertThrows(StringIndexOutOfBoundsException.class, () -> asciiSequenceView.charAt(-1));
    }
}
