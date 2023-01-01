/*
 * Copyright 2014-2023 Real Logic Limited.
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

/**
 * Store and extract a semantic version in a 4 byte integer.
 */
public final class SemanticVersion
{
    private SemanticVersion()
    {
    }

    /**
     * Compose a 4-byte integer with major, minor, and patch version stored in the least significant 3 bytes.
     * The sum of the components must be greater than zero.
     *
     * @param major version in the range 0-255.
     * @param minor version in the range 0-255
     * @param patch version in the range 0-255.
     * @return the semantic version made from the three components.
     * @throws IllegalArgumentException if the values are outside acceptable range.
     */
    public static int compose(final int major, final int minor, final int patch)
    {
        if (major < 0 || major > 255)
        {
            throw new IllegalArgumentException("major must be 0-255: " + major);
        }

        if (minor < 0 || minor > 255)
        {
            throw new IllegalArgumentException("minor must be 0-255: " + minor);
        }

        if (patch < 0 || patch > 255)
        {
            throw new IllegalArgumentException("patch must be 0-255: " + patch);
        }

        if (major + minor + patch == 0)
        {
            throw new IllegalArgumentException("all parts cannot be zero");
        }

        return (major << 16) | (minor << 8) | patch;
    }

    /**
     * Get the major version from a composite value.
     *
     * @param version as a composite from which to extract the major version.
     * @return the major version value.
     */
    public static int major(final int version)
    {
        return (version >> 16) & 0xFF;
    }

    /**
     * Get the minor version from a composite value.
     *
     * @param version as a composite from which to extract the minor version.
     * @return the minor version value.
     */
    public static int minor(final int version)
    {
        return (version >> 8) & 0xFF;
    }

    /**
     * Get the patch version from a composite value.
     *
     * @param version as a composite from which to extract the patch version.
     * @return the patch version value.
     */
    public static int patch(final int version)
    {
        return version & 0xFF;
    }

    /**
     * Generate a {@link String} representation of the semantic version in the format {@code major.minor.patch}.
     *
     * @param version to be converted to a string.
     * @return the {@link String} representation of the semantic version in the format {@code major.minor.patch}.
     */
    public static String toString(final int version)
    {
        return major(version) + "." + minor(version) + "." + patch(version);
    }
}
