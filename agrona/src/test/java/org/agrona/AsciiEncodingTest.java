package org.agrona;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

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
}