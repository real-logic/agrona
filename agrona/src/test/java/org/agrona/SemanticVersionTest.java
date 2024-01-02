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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemanticVersionTest
{
    @Test
    void shouldComposeValidVersion()
    {
        final int major = 17;
        final int minor = 9;
        final int patch = 127;

        final int version = SemanticVersion.compose(major, minor, patch);

        assertEquals(major, SemanticVersion.major(version));
        assertEquals(minor, SemanticVersion.minor(version));
        assertEquals(patch, SemanticVersion.patch(version));
    }

    @Test
    void shouldDetectNegativeMajor()
    {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.compose(-1, 1, 1));
    }

    @Test
    void shouldDetectNegativeMinor()
    {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.compose(1, -1, 1));
    }

    @Test
    void shouldDetectNegativePatch()
    {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.compose(1, 1, -1));
    }

    @Test
    void shouldDetectZeroVersion()
    {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.compose(0, 0, 0));
    }

    @Test
    void shouldDetectExcessiveMajor()
    {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.compose(256, 1, 1));
    }

    @Test
    void shouldDetectExcessiveMinor()
    {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.compose(1, 256, 1));
    }

    @Test
    void shouldDetectExcessivePatch()
    {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.compose(1, 1, 256));
    }

    @Test
    void shouldConvertToString()
    {
        final int major = 17;
        final int minor = 9;
        final int patch = 127;

        final int version = SemanticVersion.compose(major, minor, patch);

        assertEquals("17.9.127", SemanticVersion.toString(version));
    }
}
