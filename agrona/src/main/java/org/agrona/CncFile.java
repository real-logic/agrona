/*
 * Copyright 2014-2018 Real Logic Ltd.
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

import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * Interface for managing Command-n-Control files.
 *
 * The version and timestamp fields are assumed to be longs in size.
 */
public class CncFile implements AutoCloseable
{
    private final int versionFieldOffset;
    private final int timestampFieldOffset;

    private final File cncDir;
    private final File cncFile;
    private final MappedByteBuffer mappedCncBuffer;
    private final UnsafeBuffer cncBuffer;

    private volatile boolean isClosed = false;

    /**
     * Create a CnC directory and file if none present. Checking if an active CnC file exists and is active.
     *
     * Total length of CnC file will be mapped until {@link #close()} is called.
     *
     * @param directory             for the CnC file
     * @param filename              of the CnC file
     * @param warnIfDirectoryExists for logging purposes
     * @param dirDeleteOnStart      if desired
     * @param versionFieldOffset    to use for version field access
     * @param timestampFieldOffset  to use for timestamp field access
     * @param totalFileLength       to allocate when creating new CnC file
     * @param timeoutMs             for the activity check (in milliseconds)
     * @param epochClock            to use for time checks
     * @param versionCheck          to use for existing CnC file and version field
     * @param logger                to use to signal progress or null
     */
    public CncFile(
        final File directory,
        final String filename,
        final boolean warnIfDirectoryExists,
        final boolean dirDeleteOnStart,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final int totalFileLength,
        final long timeoutMs,
        final EpochClock epochClock,
        final LongConsumer versionCheck,
        final Consumer<String> logger)
    {
        ensureDirectoryExists(
            directory,
            filename,
            warnIfDirectoryExists,
            dirDeleteOnStart,
            versionFieldOffset,
            timestampFieldOffset,
            timeoutMs,
            epochClock,
            versionCheck,
            logger);

        this.cncDir = directory;
        this.cncFile = new File(directory, filename);
        this.mappedCncBuffer = mapNewFile(cncFile, totalFileLength);
        this.cncBuffer = new UnsafeBuffer(mappedCncBuffer);
        this.versionFieldOffset = versionFieldOffset;
        this.timestampFieldOffset = timestampFieldOffset;
    }

    /**
     * Manage a CnC file given a mapped file and offsets of version and timestamp.
     *
     * If mappedCncBuffer is not null, then it will be unmapped upon {@link #close()}.
     *
     * @param mappedCncBuffer      for the CnC fields
     * @param cncBuffer            for the CnC fields
     * @param versionFieldOffset   for the version field
     * @param timestampFieldOffset for the timestamp field
     */
    public CncFile(
        final MappedByteBuffer mappedCncBuffer,
        final UnsafeBuffer cncBuffer,
        final int versionFieldOffset,
        final int timestampFieldOffset)
    {
        this.cncDir = null;
        this.cncFile = null;
        this.mappedCncBuffer = mappedCncBuffer;
        this.cncBuffer = cncBuffer;
        this.versionFieldOffset = versionFieldOffset;
        this.timestampFieldOffset = timestampFieldOffset;
    }

    /**
     * Manage a CnC file given a buffer and offsets of version and timestamp.
     *
     * @param cncBuffer            for the CnC fields
     * @param versionFieldOffset   for the version field
     * @param timestampFieldOffset for the timestamp field
     */
    public CncFile(
        final UnsafeBuffer cncBuffer,
        final int versionFieldOffset,
        final int timestampFieldOffset)
    {
        this(null, cncBuffer, versionFieldOffset, timestampFieldOffset);
    }

    public boolean isClosed()
    {
        return isClosed;
    }

    public void close()
    {
        if (!isClosed)
        {
            if (null != mappedCncBuffer)
            {
                IoUtil.unmap(mappedCncBuffer);
            }

            isClosed = true;
        }
    }

    public void signalCncReady(final long version)
    {
        cncBuffer.putLongOrdered(versionFieldOffset, version);
    }

    public long versionVolatile()
    {
        return cncBuffer.getLongVolatile(versionFieldOffset);
    }

    public long versionWeak()
    {
        return cncBuffer.getLong(versionFieldOffset);
    }

    public void timestampOrdered(final long timestamp)
    {
        cncBuffer.putLongOrdered(timestampFieldOffset, timestamp);
    }

    public long timestampVolatile()
    {
        return cncBuffer.getLongVolatile(timestampFieldOffset);
    }

    public long timestampWeak()
    {
        return cncBuffer.getLong(timestampFieldOffset);
    }

    public void deleteDirectory(final boolean ignoreFailures)
    {
        IoUtil.delete(cncDir, ignoreFailures);
    }

    public File cncDirectory()
    {
        return cncDir;
    }

    public File cncFile()
    {
        return cncFile;
    }

    public UnsafeBuffer buffer()
    {
        return cncBuffer;
    }

    public static void ensureDirectoryExists(
        final File directory,
        final String filename,
        final boolean warnIfDirectoryExists,
        final boolean dirDeleteOnStart,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final long timeoutMs,
        final EpochClock epochClock,
        final LongConsumer versionCheck,
        final Consumer<String> logger)
    {
        final File cncFile =  new File(directory, filename);

        if (directory.isDirectory())
        {
            if (warnIfDirectoryExists && null != logger)
            {
                logger.accept("WARNING: " + directory + " already exists.");
            }

            if (!dirDeleteOnStart)
            {
                final int offset = Math.min(versionFieldOffset, timestampFieldOffset);
                final int length = Math.max(versionFieldOffset, timestampFieldOffset) + SIZE_OF_LONG - offset;
                final MappedByteBuffer cncByteBuffer = mapExistingFile(cncFile, logger, offset, length);

                try
                {
                    if (isActive(
                        cncByteBuffer,
                        epochClock,
                        timeoutMs,
                        versionFieldOffset,
                        timestampFieldOffset,
                        versionCheck,
                        logger))
                    {
                        throw new IllegalStateException("Active CnC file detected");
                    }

                }
                finally
                {
                    IoUtil.unmap(cncByteBuffer);
                }
            }

            IoUtil.delete(directory, false);
        }

        IoUtil.ensureDirectoryExists(directory, directory.toString());
    }

    public static MappedByteBuffer mapExistingFile(
        final File cncFile, final Consumer<String> logger, final long offset, final long length)
    {
        if (cncFile.exists())
        {
            if (null != logger)
            {
                logger.accept("INFO: CnC file exists: " + cncFile);
            }

            return IoUtil.mapExistingFile(cncFile, cncFile.toString(), offset, length);
        }

        return null;
    }

    public static MappedByteBuffer mapExistingFile(final File cncFile, final Consumer<String> logger)
    {
        if (cncFile.exists())
        {
            if (null != logger)
            {
                logger.accept("INFO: CnC file exists: " + cncFile);
            }

            return IoUtil.mapExistingFile(cncFile, cncFile.toString());
        }

        return null;
    }

    public static MappedByteBuffer mapNewFile(final File cncFile, final long length)
    {
        return IoUtil.mapNewFile(cncFile, length);
    }

    public static boolean isActive(
        final MappedByteBuffer cncByteBuffer,
        final EpochClock epochClock,
        final long timeoutMs,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final LongConsumer versionCheck,
        final Consumer<String> logger)
    {
        if (null == cncByteBuffer)
        {
            return false;
        }

        final UnsafeBuffer cncBuffer = new UnsafeBuffer(cncByteBuffer);

        final long startTimeMs = epochClock.time();
        long cncVersion;
        while (0 == (cncVersion = cncBuffer.getLongVolatile(versionFieldOffset)))
        {
            if (epochClock.time() > (startTimeMs + timeoutMs))
            {
                throw new IllegalStateException("CnC file is created but not initialised.");
            }

            sleep(1);
        }

        versionCheck.accept(cncVersion);

        final long timestamp = cncBuffer.getLongVolatile(timestampFieldOffset);
        final long now = epochClock.time();
        final long timestampAge = now - timestamp;

        if (null != logger)
        {
            logger.accept("INFO: heartbeat is (ms): " + timestampAge);
        }

        return timestampAge <= timeoutMs;
    }

    static void sleep(final long durationMs)
    {
        try
        {
            Thread.sleep(durationMs);
        }
        catch (final InterruptedException ignore)
        {
            Thread.interrupted();
        }
    }
}
