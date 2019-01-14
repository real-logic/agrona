/*
 * Copyright 2014-2019 Real Logic Ltd.
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

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.agrona.SystemUtil.parseDuration;
import static org.agrona.SystemUtil.parseSize;
import static org.junit.Assert.*;

public class SystemUtilTest
{
    @Test
    public void shouldParseSizesWithSuffix()
    {
        assertThat(parseSize("", "1"), Matchers.equalTo(1L));
        assertThat(parseSize("", "1k"), Matchers.equalTo(1024L));
        assertThat(parseSize("", "1K"), Matchers.equalTo(1024L));
        assertThat(parseSize("", "1m"), Matchers.equalTo(1024L * 1024));
        assertThat(parseSize("", "1M"), Matchers.equalTo(1024L * 1024));
        assertThat(parseSize("", "1g"), Matchers.equalTo(1024L * 1024 * 1024));
        assertThat(parseSize("", "1G"), Matchers.equalTo(1024L * 1024 * 1024));
    }

    @Test
    public void shouldParseTimesWithSuffix()
    {
        assertThat(parseDuration("", "1"), Matchers.equalTo(1L));
        assertThat(parseDuration("", "1ns"), Matchers.equalTo(1L));
        assertThat(parseDuration("", "1NS"), Matchers.equalTo(1L));
        assertThat(parseDuration("", "1us"), Matchers.equalTo(1000L));
        assertThat(parseDuration("", "1US"), Matchers.equalTo(1000L));
        assertThat(parseDuration("", "1ms"), Matchers.equalTo(1000L * 1000));
        assertThat(parseDuration("", "1MS"), Matchers.equalTo(1000L * 1000));
        assertThat(parseDuration("", "1s"), Matchers.equalTo(1000L * 1000 * 1000));
        assertThat(parseDuration("", "1S"), Matchers.equalTo(1000L * 1000 * 1000));
        assertThat(parseDuration("", "12s"), Matchers.equalTo(12L * 1000 * 1000 * 1000));
    }

    @Test(expected = NumberFormatException.class)
    public void shouldThrowWhenParseTimeHasBadSuffix()
    {
        parseDuration("", "1g");
    }

    @Test(expected = NumberFormatException.class)
    public void shouldThrowWhenParseTimeHasBadTwoLetterSuffix()
    {
        parseDuration("", "1zs");
    }

    @Test(expected = NumberFormatException.class)
    public void shouldThrowWhenParseSizeOverflows()
    {
        parseSize("", 8589934592L + "g");
    }
}