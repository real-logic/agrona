/*
 * Copyright 2014-2019 Real Logic Ltd.
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

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AsciiEncodingTest
{
    @Test
    public void shouldParseInt()
    {
        assertThat(AsciiEncoding.parseIntAscii("0", 0, 1), is(0));
        assertThat(AsciiEncoding.parseIntAscii("-0", 0, 2), is(0));
        assertThat(AsciiEncoding.parseIntAscii("7", 0, 1), is(7));
        assertThat(AsciiEncoding.parseIntAscii("-7", 0, 2), is(-7));
        assertThat(AsciiEncoding.parseIntAscii("3333", 1, 2), is(33));
    }

    @Test
    public void shouldParseLong()
    {
        assertThat(AsciiEncoding.parseLongAscii("0", 0, 1), is(0L));
        assertThat(AsciiEncoding.parseLongAscii("-0", 0, 2), is(0L));
        assertThat(AsciiEncoding.parseLongAscii("7", 0, 1), is(7L));
        assertThat(AsciiEncoding.parseLongAscii("-7", 0, 2), is(-7L));
        assertThat(AsciiEncoding.parseLongAscii("3333", 1, 2), is(33L));
    }

    @Test(expected = AsciiNumberFormatException.class)
    public void shouldThrowExceptionWhenDecodingCharNonAsciiValue()
    {
        AsciiEncoding.getDigit(0, 'a');
    }

    @Test(expected = AsciiNumberFormatException.class)
    public void shouldThrowExceptionWhenDecodingByteNonAsciiValue()
    {
        AsciiEncoding.getDigit(0, (byte)'a');
    }
}