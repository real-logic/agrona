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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.agrona.SystemUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

class SystemUtilTest
{
    @Test
    void shouldParseSizesWithSuffix()
    {
        assertEquals(1L, parseSize("", "1"));
        assertEquals(1024L, parseSize("", "1k"));
        assertEquals(1024L, parseSize("", "1K"));
        assertEquals(1024L * 1024L, parseSize("", "1m"));
        assertEquals(1024L * 1024L, parseSize("", "1M"));
        assertEquals(1024L * 1024L * 1024L, parseSize("", "1g"));
        assertEquals(1024L * 1024L * 1024L, parseSize("", "1G"));
    }

    @Test
    void shouldParseTimesWithSuffix()
    {
        assertEquals(1L, parseDuration("", "1"));
        assertEquals(1L, parseDuration("", "1ns"));
        assertEquals(1L, parseDuration("", "1NS"));
        assertEquals(1000L, parseDuration("", "1us"));
        assertEquals(1000L, parseDuration("", "1US"));
        assertEquals(1000L * 1000, parseDuration("", "1ms"));
        assertEquals(1000L * 1000, parseDuration("", "1MS"));
        assertEquals(1000L * 1000 * 1000, parseDuration("", "1s"));
        assertEquals(1000L * 1000 * 1000, parseDuration("", "1S"));
        assertEquals(12L * 1000 * 1000 * 1000, parseDuration("", "12S"));
    }

    @Test
    void shouldThrowWhenParseTimeHasBadSuffix()
    {
        assertThrows(NumberFormatException.class, () -> parseDuration("", "1g"));
    }

    @Test
    void shouldThrowWhenParseTimeHasBadTwoLetterSuffix()
    {
        assertThrows(NumberFormatException.class, () -> parseDuration("", "1zs"));
    }

    @Test
    void shouldThrowWhenParseSizeOverflows()
    {
        assertThrows(NumberFormatException.class, () -> parseSize("", 8589934592L + "g"));
    }

    @Test
    void shouldDoNothingToSystemPropsWhenLoadingFileWhichDoesNotExist()
    {
        final int originalSystemPropSize = System.getProperties().size();

        loadPropertiesFile("$unknown-file$");
        assertEquals(originalSystemPropSize, System.getProperties().size());
    }

    @Test
    void shouldMergeMultiplePropFilesTogether()
    {
        assertThat(System.getProperty("TestFileA.foo"), is(emptyOrNullString()));
        assertThat(System.getProperty("TestFileB.foo"), is(emptyOrNullString()));

        try
        {
            loadPropertiesFiles("TestFileA.properties", "TestFileB.properties");
            assertEquals("AAA", System.getProperty("TestFileA.foo"));
            assertEquals("BBB", System.getProperty("TestFileB.foo"));
        }
        finally
        {
            System.clearProperty("TestFileA.foo");
            System.clearProperty("TestFileB.foo");
        }
    }

    @Test
    void shouldOverrideSystemPropertiesWithConfigFromPropFile()
    {
        System.setProperty("TestFileA.foo", "ToBeOverridden");
        assertEquals("ToBeOverridden", System.getProperty("TestFileA.foo"));

        try
        {
            loadPropertiesFiles("TestFileA.properties");
            assertEquals("AAA", System.getProperty("TestFileA.foo"));
        }
        finally
        {
            System.clearProperty("TestFileA.foo");
        }
    }

    @Test
    void shouldNotOverrideSystemPropertiesWithConfigFromPropFile()
    {
        System.setProperty("TestFileA.foo", "ToBeNotOverridden");
        assertEquals("ToBeNotOverridden", System.getProperty("TestFileA.foo"));

        try
        {
            loadPropertiesFile(PropertyAction.PRESERVE, "TestFileA.properties");
            assertEquals("ToBeNotOverridden", System.getProperty("TestFileA.foo"));
        }
        finally
        {
            System.clearProperty("TestFileA.foo");
        }
    }

    @Test
    void shouldReturnPid()
    {
        assertNotEquals(PID_NOT_FOUND, getPid());
    }

    @Test
    void shouldGetNormalProperty()
    {
        final String key = "org.agrona.test.case";
        final String value = "wibble";

        System.setProperty(key, value);

        try
        {
            assertEquals(value, SystemUtil.getProperty(key));
        }
        finally
        {
            System.clearProperty(key);
        }
    }

    @Test
    void shouldGetNullProperty()
    {
        final String key = "org.agrona.test.case";
        final String value = "@null";

        System.setProperty(key, value);

        try
        {
            assertNull(SystemUtil.getProperty(key));
        }
        finally
        {
            System.clearProperty(key);
        }
    }

    @Test
    void shouldGetNullPropertyWithDefault()
    {
        final String key = "org.agrona.test.case";
        final String value = "@null";

        System.setProperty(key, value);

        try
        {
            assertNull(SystemUtil.getProperty(key, "default"));
        }
        finally
        {
            System.clearProperty(key);
        }
    }

    @Test
    void shouldGetDefaultProperty()
    {
        final String key = "org.agrona.test.case";
        final String defaultValue = "default";

        assertEquals(defaultValue, SystemUtil.getProperty(key, defaultValue));
    }

    @ParameterizedTest
    @ValueSource(strings = { "i386", "x86", "x86_64", "amd64" })
    void isX86ArchShouldDetectProperOsArch(final String arch)
    {
        assertTrue(SystemUtil.isX86Arch(arch));
    }

    @ParameterizedTest
    @ValueSource(strings = { "aarch64", "ppc64", "ppc64le", "unknown", "", "test" })
    void isX86ArchShouldReturnFalse(final String arch)
    {
        assertFalse(SystemUtil.isX86Arch(arch));
    }

    @Test
    @EnabledOnOs(architectures = { "i386", "x86", "x86_64", "amd64" })
    void isX86ArchSystemTest()
    {
        assertTrue(SystemUtil.isX86Arch());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void isLinuxReturnsTrueForLinuxBasedSystems()
    {
        assertTrue(SystemUtil.isLinux());
        assertFalse(SystemUtil.isWindows());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isWindowsReturnsTrueForWindows()
    {
        assertFalse(SystemUtil.isLinux());
        assertTrue(SystemUtil.isWindows());
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void onMacOsChecksReturnFalse()
    {
        assertFalse(SystemUtil.isLinux());
        assertFalse(SystemUtil.isWindows());
    }
}
