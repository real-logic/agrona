/*
 * Copyright 2014-2017 Real Logic Ltd.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.util.Properties;

/**
 * Utilities for inspecting the system.
 */
public class SystemUtil
{
    private static final String OS_NAME;

    static
    {
        OS_NAME = System.getProperty("os.name").toLowerCase();
    }

    /**
     * Get the name of the operating system as a lower case String.
     * <p>
     * This is what is returned from System.getProperty("os.name").toLowerCase().
     *
     * @return the name of the operating system as a lower case String.
     */
    public static String osName()
    {
        return OS_NAME;
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
     * Load system properties from a given filename or url.
     * <p>
     * File is first searched for in resources using the system {@link ClassLoader},
     * then file system, then URL. All are loaded if multiples found.
     *
     * @param filenameOrUrl that holds properties
     */
    public static void loadPropertiesFile(final String filenameOrUrl)
    {
        final Properties properties = new Properties(System.getProperties());

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
     * @param filenamesOrUrls that holds properties
     * @see #loadPropertiesFile(String)
     */
    public static void loadPropertiesFiles(final String[] filenamesOrUrls)
    {
        for (final String filenameOrUrl : filenamesOrUrls)
        {
            loadPropertiesFile(filenameOrUrl);
        }
    }
}
