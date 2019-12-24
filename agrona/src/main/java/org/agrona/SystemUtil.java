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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.System.getProperty;

/**
 * Utilities for inspecting the system.
 */
public final class SystemUtil
{
    /**
     * PID value if a process id could not be determined. This value should be equal to a kernel only process
     * id for the platform so that it does not indicate a real process id.
     */
    public static final long PID_NOT_FOUND = 0;

    private static final String SUN_PID_PROP_NAME = "sun.java.launcher.pid";
    private static final long MAX_G_VALUE = 8589934591L;
    private static final long MAX_M_VALUE = 8796093022207L;
    private static final long MAX_K_VALUE = 9007199254739968L;

    private static final String OS_NAME;
    private static final long PID;

    static
    {
        OS_NAME = System.getProperty("os.name").toLowerCase();

        long pid = PID_NOT_FOUND;
        try
        {
            // TODO: if Java 9, then use ProcessHandle.
            final String pidPropertyValue = System.getProperty(SUN_PID_PROP_NAME);
            if (null != pidPropertyValue)
            {
                pid = Long.parseLong(pidPropertyValue);
            }
            else
            {
                final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                pid = Long.parseLong(jvmName.split("@")[0]);
            }
        }
        catch (final Throwable ignore)
        {
        }

        PID = pid;
    }

    private SystemUtil()
    {
    }

    /**
     * Get the name of the operating system as a lower case String.
     * <p>
     * This is what is returned from {@code System.getProperty("os.name").toLowerCase()}.
     *
     * @return the name of the operating system as a lower case String.
     */
    public static String osName()
    {
        return OS_NAME;
    }

    /**
     * Return the current process id from the OS.
     *
     * @return current process id or {@link #PID_NOT_FOUND} if PID was not able to be found.
     * @see #PID_NOT_FOUND
     */
    public static long getPid()
    {
        return PID;
    }

    /**
     * Is the operating system likely to be Windows based on {@link #osName()}.
     *
     * @return true if the operating system is likely to be Windows based on {@link #osName()}.
     */
    public static boolean isWindows()
    {
        return OS_NAME.startsWith("win");
    }

    /**
     * Is the operating system likely to be Linux based on {@link #osName()}.
     *
     * @return true if the operating system is likely to be Linux based on {@link #osName()}.
     */
    public static boolean isLinux()
    {
        return OS_NAME.contains("linux");
    }

    /**
     * Is a debugger attached to the JVM?
     *
     * @return true if attached otherwise false.
     */
    public static boolean isDebuggerAttached()
    {
        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        for (final String arg : runtimeMXBean.getInputArguments())
        {
            if (arg.contains("-agentlib:jdwp"))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the system property for java.io.tmpdir ensuring a {@link File#separator} is at the end.
     *
     * @return tmp directory for the runtime.
     */
    public static String tmpDirName()
    {
        String tmpDirName = System.getProperty("java.io.tmpdir");
        if (!tmpDirName.endsWith(File.separator))
        {
            tmpDirName += File.separator;
        }

        return tmpDirName;
    }

    /**
     * Get a formatted dump of all threads with associated state and stack traces.
     *
     * @return a formatted dump of all threads with associated state and stack traces.
     */
    public static String threadDump()
    {
        final StringBuilder sb = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        for (final ThreadInfo info : threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), Integer.MAX_VALUE))
        {
            sb.append('"').append(info.getThreadName()).append("\": ").append(info.getThreadState());

            for (final StackTraceElement stackTraceElement : info.getStackTrace())
            {
                sb.append("\n    at ").append(stackTraceElement.toString());
            }

            sb.append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Load system properties from a given filename or url.
     * <p>
     * File is first searched for in resources using the system {@link ClassLoader},
     * then file system, then URL. All are loaded if multiples found.
     *
     * @param filenameOrUrl that holds properties.
     */
    public static void loadPropertiesFile(final String filenameOrUrl)
    {
        final Properties properties = new Properties(System.getProperties());
        System.getProperties().forEach(properties::put);

        final URL resource = ClassLoader.getSystemClassLoader().getResource(filenameOrUrl);
        if (null != resource)
        {
            try (InputStream in = resource.openStream())
            {
                properties.load(in);
            }
            catch (final Exception ignore)
            {
            }
        }

        final File file = new File(filenameOrUrl);
        if (file.exists())
        {
            try (FileInputStream in = new FileInputStream(file))
            {
                properties.load(in);
            }
            catch (final Exception ignore)
            {
            }
        }

        try (InputStream in = new URL(filenameOrUrl).openStream())
        {
            properties.load(in);
        }
        catch (final Exception ignore)
        {
        }

        System.setProperties(properties);
    }

    /**
     * Load system properties from a given set of filenames or URLs.
     *
     * @param filenamesOrUrls that holds properties.
     * @see #loadPropertiesFile(String)
     */
    public static void loadPropertiesFiles(final String[] filenamesOrUrls)
    {
        for (final String filenameOrUrl : filenamesOrUrls)
        {
            loadPropertiesFile(filenameOrUrl);
        }
    }

    /**
     * Get a size value as an int from a system property. Supports a 'g', 'm', and 'k' suffix to indicate
     * gigabytes, megabytes, or kilobytes respectively.
     *
     * @param propertyName to lookup.
     * @param defaultValue to be applied if the system property is not set.
     * @return the int value.
     * @throws NumberFormatException if the value is out of range or mal-formatted.
     */
    public static int getSizeAsInt(final String propertyName, final int defaultValue)
    {
        final String propertyValue = getProperty(propertyName);
        if (propertyValue != null)
        {
            final long value = parseSize(propertyName, propertyValue);
            if (value < 0 || value > Integer.MAX_VALUE)
            {
                throw new NumberFormatException(
                    propertyName + " must positive and less than Integer.MAX_VALUE :" + value);
            }

            return (int)value;
        }

        return defaultValue;
    }

    /**
     * Get a size value as a long from a system property. Supports a 'g', 'm', and 'k' suffix to indicate
     * gigabytes, megabytes, or kilobytes respectively.
     *
     * @param propertyName to lookup.
     * @param defaultValue to be applied if the system property is not set.
     * @return the long value.
     * @throws NumberFormatException if the value is out of range or mal-formatted.
     */
    public static long getSizeAsLong(final String propertyName, final long defaultValue)
    {
        final String propertyValue = getProperty(propertyName);
        if (propertyValue != null)
        {
            final long value = parseSize(propertyName, propertyValue);
            if (value < 0)
            {
                throw new NumberFormatException(propertyName + " must be positive: " + value);
            }

            return value;
        }

        return defaultValue;
    }

    /**
     * Parse a string representation of a value with optional suffix of 'g', 'm', and 'k' suffix to indicate
     * gigabytes, megabytes, or kilobytes respectively.
     *
     * @param propertyName  that associated with the size value.
     * @param propertyValue to be parsed.
     * @return the long value.
     * @throws NumberFormatException if the value is out of range or mal-formatted.
     */
    public static long parseSize(final String propertyName, final String propertyValue)
    {
        final int lengthMinusSuffix = propertyValue.length() - 1;
        final char lastCharacter = propertyValue.charAt(lengthMinusSuffix);
        if (Character.isDigit(lastCharacter))
        {
            return Long.valueOf(propertyValue);
        }

        final long value = AsciiEncoding.parseLongAscii(propertyValue, 0, lengthMinusSuffix);

        switch (lastCharacter)
        {
            case 'k':
            case 'K':
                if (value > MAX_K_VALUE)
                {
                    throw new NumberFormatException(propertyName + " would overflow long: " + propertyValue);
                }
                return value * 1024;

            case 'm':
            case 'M':
                if (value > MAX_M_VALUE)
                {
                    throw new NumberFormatException(propertyName + " would overflow long: " + propertyValue);
                }
                return value * 1024 * 1024;

            case 'g':
            case 'G':
                if (value > MAX_G_VALUE)
                {
                    throw new NumberFormatException(propertyName + " would overflow long: " + propertyValue);
                }
                return value * 1024 * 1024 * 1024;

            default:
                throw new NumberFormatException(
                    propertyName + ": " + propertyValue + " should end with: k, m, or g.");
        }
    }

    /**
     * Get a string representation of a time duration with an optional suffix of 's', 'ms', 'us', or 'ns' suffix to
     * indicate seconds, milliseconds, microseconds, or nanoseconds respectively.
     * <p>
     * If the resulting duration is greater than {@link Long#MAX_VALUE} then {@link Long#MAX_VALUE} is used.
     *
     * @param propertyName associated with the duration value.
     * @param defaultValue to be used if the property is not present.
     * @return the long value.
     * @throws NumberFormatException if the value is negative or malformed.
     */
    public static long getDurationInNanos(final String propertyName, final long defaultValue)
    {
        final String propertyValue = getProperty(propertyName);
        if (propertyValue != null)
        {
            final long value = parseDuration(propertyName, propertyValue);
            if (value < 0)
            {
                throw new NumberFormatException(propertyName + " must be positive: " + value);
            }

            return value;
        }

        return defaultValue;
    }

    /**
     * Parse a string representation of a time duration with an optional suffix of 's', 'ms', 'us', or 'ns' to
     * indicate seconds, milliseconds, microseconds, or nanoseconds respectively.
     * <p>
     * If the resulting duration is greater than {@link Long#MAX_VALUE} then {@link Long#MAX_VALUE} is used.
     *
     * @param propertyName  associated with the duration value.
     * @param propertyValue to be parsed.
     * @return the long value.
     * @throws NumberFormatException if the value is negative or malformed.
     */
    public static long parseDuration(final String propertyName, final String propertyValue)
    {
        final char lastCharacter = propertyValue.charAt(propertyValue.length() - 1);
        if (Character.isDigit(lastCharacter))
        {
            return Long.valueOf(propertyValue);
        }

        if (lastCharacter != 's' && lastCharacter != 'S')
        {
            throw new NumberFormatException(
                propertyName + ": " + propertyValue + " should end with: s, ms, us, or ns.");
        }

        final char secondLastCharacter = propertyValue.charAt(propertyValue.length() - 2);
        if (Character.isDigit(secondLastCharacter))
        {
            final long value = AsciiEncoding.parseLongAscii(propertyValue, 0, propertyValue.length() - 1);
            return TimeUnit.SECONDS.toNanos(value);
        }

        final long value = AsciiEncoding.parseLongAscii(propertyValue, 0, propertyValue.length() - 2);

        switch (secondLastCharacter)
        {
            case 'n':
            case 'N':
                return value;

            case 'u':
            case 'U':
                return TimeUnit.MICROSECONDS.toNanos(value);

            case 'm':
            case 'M':
                return TimeUnit.MILLISECONDS.toNanos(value);

            default:
                throw new NumberFormatException(
                    propertyName + ": " + propertyValue + " should end with: s, ms, us, or ns.");
        }
    }
}
