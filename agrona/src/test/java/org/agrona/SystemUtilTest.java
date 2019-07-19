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

import static org.agrona.SystemUtil.parseDuration;
import static org.agrona.SystemUtil.parseSize;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SystemUtilTest
{
    @Test
    public void shouldParseSizesWithSuffix()
    {
        assertThat(parseSize("", "1"), is(1L));
        assertThat(parseSize("", "1k"), is(1024L));
        assertThat(parseSize("", "1K"), is(1024L));
        assertThat(parseSize("", "1m"), is(1024L * 1024));
        assertThat(parseSize("", "1M"), is(1024L * 1024));
        assertThat(parseSize("", "1g"), is(1024L * 1024 * 1024));
        assertThat(parseSize("", "1G"), is(1024L * 1024 * 1024));
    }

    @Test
    public void shouldParseTimesWithSuffix()
    {
        assertThat(parseDuration("", "1"), is(1L));
        assertThat(parseDuration("", "1ns"), is(1L));
        assertThat(parseDuration("", "1NS"), is(1L));
        assertThat(parseDuration("", "1us"), is(1000L));
        assertThat(parseDuration("", "1US"), is(1000L));
        assertThat(parseDuration("", "1ms"), is(1000L * 1000));
        assertThat(parseDuration("", "1MS"), is(1000L * 1000));
        assertThat(parseDuration("", "1s"), is(1000L * 1000 * 1000));
        assertThat(parseDuration("", "1S"), is(1000L * 1000 * 1000));
        assertThat(parseDuration("", "12s"), is(12L * 1000 * 1000 * 1000));
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

    @Test
    public void shouldDoNothingToSystemPropsWhenLoadingFileWhichDoesNotExist()
    {
        final int originalSystemPropSize = System.getProperties().size();

        SystemUtil.loadPropertiesFile("$unknown-file$");

        assertThat(originalSystemPropSize, is(System.getProperties().size()));
    }

    @Test
    public void shouldMergeMultiplePropFilesTogether()
    {
        assertThat(System.getProperty("TestFileA.foo"), is(emptyOrNullString()));
        assertThat(System.getProperty("TestFileB.foo"), is(emptyOrNullString()));

        SystemUtil.loadPropertiesFiles(new String[]{ "TestFileA.properties", "TestFileB.properties" });

        assertThat(System.getProperty("TestFileA.foo"), is("AAA"));
        assertThat(System.getProperty("TestFileB.foo"), is("BBB"));
    }

    @Test
    public void shouldOverrideSystemPropertiesWithConfigFromPropFile()
    {
        System.setProperty("TestFileA.foo", "ToBeOverridden");
        assertThat(System.getProperty("TestFileA.foo"), is("ToBeOverridden"));

        SystemUtil.loadPropertiesFile("TestFileA.properties");

        assertThat(System.getProperty("TestFileA.foo"), is("AAA"));

        System.clearProperty("TestFileA.foo");
    }
}