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
import java.nio.channels.FileChannel;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.nio.file.StandardOpenOption.*;
import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * A {@link MarkFile} is used to mark the presence of a running component and to track liveness.
 *
 * The assumptions are: (1) the version field is an int in size, (2) the timestamp field is a long in size,
 * and (3) the version field comes before the timestamp field.
 */
public class MarkFile implements AutoCloseable
{
    private final int versionFieldOffset;
    private final int timestampFieldOffset;

    private final File parentDir;
    private final File markFile;
    private final MappedByteBuffer mappedBuffer;
    private final UnsafeBuffer buffer;

    private volatile boolean isClosed = false;

    /**
     * Create a directory and mark file if none present. Checking if an active Mark file exists and is active.
     * Old Mark file is deleted and recreated if not active.
     *
     * Total length of Mark file will be mapped until {@link #close()} is called.
     *
     * @param directory             for the Mark file
     * @param filename              of the Mark file
     * @param warnIfDirectoryExists for logging purposes
     * @param dirDeleteOnStart      if desired
     * @param versionFieldOffset    to use for version field access
     * @param timestampFieldOffset  to use for timestamp field access
     * @param totalFileLength       to allocate when creating new Mark file
     * @param timeoutMs             for the activity check (in milliseconds)
     * @param epochClock            to use for time checks
     * @param versionCheck          to use for existing Mark file and version field
     * @param logger                to use to signal progress or null
     */
    public MarkFile(
        final File directory,
        final String filename,
        final boolean warnIfDirectoryExists,
        final boolean dirDeleteOnStart,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final int totalFileLength,
        final long timeoutMs,
        final EpochClock epochClock,
        final IntConsumer versionCheck,
        final Consumer<String> logger)
    {
        validateOffsets(versionFieldOffset, timestampFieldOffset);

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

        this.parentDir = directory;
        this.markFile = new File(directory, filename);
        this.mappedBuffer = mapNewFile(markFile, totalFileLength);
        this.buffer = new UnsafeBuffer(mappedBuffer);
        this.versionFieldOffset = versionFieldOffset;
        this.timestampFieldOffset = timestampFieldOffset;
    }

    /**
     * Create a {@link MarkFile} if none present. Checking if an active {@link MarkFile} exists and is active.
     * Existing {@link MarkFile} is used if not active.
     *
     * Total length of Mark file will be mapped until {@link #close()} is called.
     *
     * @param markFile             to use
     * @param shouldPreExist       or not
     * @param versionFieldOffset   to use for version field access
     * @param timestampFieldOffset to use for timestamp field access
     * @param totalFileLength      to allocate when creating new {@link MarkFile}
     * @param timeoutMs            for the activity check (in milliseconds)
     * @param epochClock           to use for time checks
     * @param versionCheck         to use for existing {@link MarkFile} and version field
     * @param logger               to use to signal progress or null
     */

    public MarkFile(
        final File markFile,
        final boolean shouldPreExist,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final int totalFileLength,
        final long timeoutMs,
        final EpochClock epochClock,
        final IntConsumer versionCheck,
        final Consumer<String> logger)
    {
        validateOffsets(versionFieldOffset, timestampFieldOffset);

        this.parentDir = markFile.getParentFile();
        this.markFile = markFile;
        this.mappedBuffer = mapNewOrExistingMarkFile(
            markFile,
            shouldPreExist,
            versionFieldOffset,
            timestampFieldOffset,
            totalFileLength,
            timeoutMs,
            epochClock,
            versionCheck,
            logger);

        this.buffer = new UnsafeBuffer(mappedBuffer);
        this.versionFieldOffset = versionFieldOffset;
        this.timestampFieldOffset = timestampFieldOffset;
    }

    /**
     * Map a pre-existing {@link MarkFile} if one present and is active.
     *
     * Total length of {@link MarkFile}will be mapped until {@link #close()} is called.
     *
     * @param directory            for the {@link MarkFile} file
     * @param filename             of the {@link MarkFile} file
     * @param versionFieldOffset   to use for version field access
     * @param timestampFieldOffset to use for timestamp field access
     * @param timeoutMs            for the activity check (in milliseconds) and for how long to wait for file to exist
     * @param epochClock           to use for time checks
     * @param versionCheck         to use for existing {@link MarkFile} file and version field
     * @param logger               to use to signal progress or null
     */
    public MarkFile(
        final File directory,
        final String filename,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final long timeoutMs,
        final EpochClock epochClock,
        final IntConsumer versionCheck,
        final Consumer<String> logger)
    {
        validateOffsets(versionFieldOffset, timestampFieldOffset);

        this.parentDir = directory;
        this.markFile = new File(directory, filename);
        this.mappedBuffer = mapExistingMarkFile(
            markFile, versionFieldOffset, timestampFieldOffset, timeoutMs, epochClock, versionCheck, logger);
        this.buffer = new UnsafeBuffer(mappedBuffer);
        this.versionFieldOffset = versionFieldOffset;
        this.timestampFieldOffset = timestampFieldOffset;
    }

    /**
     * Manage a {@link MarkFile} given a mapped file and offsets of version and timestamp.
     *
     * If mappedBuffer is not null, then it will be unmapped upon {@link #close()}.
     *
     * @param mappedBuffer         for the {@link MarkFile} fields
     * @param versionFieldOffset   for the version field
     * @param timestampFieldOffset for the timestamp field
     */
    public MarkFile(
        final MappedByteBuffer mappedBuffer,
        final int versionFieldOffset,
        final int timestampFieldOffset)
    {
        validateOffsets(versionFieldOffset, timestampFieldOffset);

        this.parentDir = null;
        this.markFile = null;
        this.mappedBuffer = mappedBuffer;
        this.buffer = new UnsafeBuffer(mappedBuffer);
        this.versionFieldOffset = versionFieldOffset;
        this.timestampFieldOffset = timestampFieldOffset;
    }

    /**
     * Manage a {@link MarkFile} given a buffer and offsets of version and timestamp.
     *
     * @param buffer               for the {@link MarkFile} fields
     * @param versionFieldOffset   for the version field
     * @param timestampFieldOffset for the timestamp field
     */
    public MarkFile(
        final UnsafeBuffer buffer,
        final int versionFieldOffset,
        final int timestampFieldOffset)
    {
        validateOffsets(versionFieldOffset, timestampFieldOffset);

        this.parentDir = null;
        this.markFile = null;
        this.mappedBuffer = null;
        this.buffer = buffer;
        this.versionFieldOffset = versionFieldOffset;
        this.timestampFieldOffset = timestampFieldOffset;
    }

    public boolean isClosed()
    {
        return isClosed;
    }

    public void close()
    {
        if (!isClosed)
        {
            if (null != mappedBuffer)
            {
                IoUtil.unmap(mappedBuffer);
            }

            isClosed = true;
        }
    }

    public void signalReady(final int version)
    {
        buffer.putIntOrdered(versionFieldOffset, version);
    }

    public int versionVolatile()
    {
        return buffer.getIntVolatile(versionFieldOffset);
    }

    public int versionWeak()
    {
        return buffer.getInt(versionFieldOffset);
    }

    public void timestampOrdered(final long timestamp)
    {
        buffer.putLongOrdered(timestampFieldOffset, timestamp);
    }

    public long timestampVolatile()
    {
        return buffer.getLongVolatile(timestampFieldOffset);
    }

    public long timestampWeak()
    {
        return buffer.getLong(timestampFieldOffset);
    }

    public void deleteDirectory(final boolean ignoreFailures)
    {
        IoUtil.delete(parentDir, ignoreFailures);
    }

    public File parentDirectory()
    {
        return parentDir;
    }

    public File markFile()
    {
        return markFile;
    }

    public MappedByteBuffer mappedByteBuffer()
    {
        return mappedBuffer;
    }

    public UnsafeBuffer buffer()
    {
        return buffer;
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
        final IntConsumer versionCheck,
        final Consumer<String> logger)
    {
        final File markFile = new File(directory, filename);

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
                final MappedByteBuffer byteBuffer = mapExistingFile(markFile, logger, offset, length);

                try
                {
                    if (isActive(
                        byteBuffer,
                        epochClock,
                        timeoutMs,
                        versionFieldOffset,
                        timestampFieldOffset,
                        versionCheck,
                        logger))
                    {
                        throw new IllegalStateException("Active Mark file detected");
                    }
                }
                finally
                {
                    IoUtil.unmap(byteBuffer);
                }
            }

            IoUtil.delete(directory, false);
        }

        IoUtil.ensureDirectoryExists(directory, directory.toString());
    }

    public static MappedByteBuffer mapExistingMarkFile(
        final File markFile,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final long timeoutMs,
        final EpochClock epochClock,
        final IntConsumer versionCheck,
        final Consumer<String> logger)
    {
        final long startTimeMs = epochClock.time();

        while (!markFile.exists() || markFile.length() <= 0)
        {
            if (epochClock.time() > (startTimeMs + timeoutMs))
            {
                throw new IllegalStateException("Mark file not created: " + markFile.getName());
            }

            sleep(16);
        }

        MappedByteBuffer byteBuffer = null;
        int bufferLength = 0;
        while (bufferLength < markFile.length() || bufferLength < 4)
        {
            if (epochClock.time() > (startTimeMs + timeoutMs))
            {
                throw new IllegalStateException("Mark file is created but not populated.");
            }

            sleep(1);
            byteBuffer = mapExistingFile(markFile, logger);
            bufferLength = byteBuffer.capacity();
        }

        final UnsafeBuffer buffer = new UnsafeBuffer(byteBuffer);

        int version;
        while (0 == (version = buffer.getIntVolatile(versionFieldOffset)))
        {
            if (epochClock.time() > (startTimeMs + timeoutMs))
            {
                throw new IllegalStateException("Mark file is created but not initialised.");
            }

            sleep(1);
        }

        versionCheck.accept(version);

        while (0 == buffer.getLongVolatile(timestampFieldOffset))
        {
            if (epochClock.time() > (startTimeMs + timeoutMs))
            {
                throw new IllegalStateException("No non zero timestamp detected.");
            }

            sleep(1);
        }

        return byteBuffer;
    }

    public static MappedByteBuffer mapNewOrExistingMarkFile(
        final File markFile,
        final boolean shouldPreExist,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final long totalFileLength,
        final long timeoutMs,
        final EpochClock epochClock,
        final IntConsumer versionCheck,
        final Consumer<String> logger)
    {
        MappedByteBuffer byteBuffer = null;

        try (FileChannel channel = FileChannel.open(markFile.toPath(), CREATE, READ, WRITE, SPARSE))
        {
            byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, totalFileLength);
            final UnsafeBuffer buffer = new UnsafeBuffer(byteBuffer);

            if (shouldPreExist)
            {
                final int version = buffer.getIntVolatile(versionFieldOffset);

                if (null != logger)
                {
                    logger.accept("INFO: Mark file exists: " + markFile);
                }

                versionCheck.accept(version);

                final long timestamp = buffer.getLongVolatile(timestampFieldOffset);
                final long now = epochClock.time();
                final long timestampAge = now - timestamp;

                if (null != logger)
                {
                    logger.accept("INFO: heartbeat is (ms): " + timestampAge);
                }

                if (timestampAge < timeoutMs)
                {
                    throw new IllegalStateException("Active Mark file detected");
                }
            }
        }
        catch (final Exception ex)
        {
            if (null != byteBuffer)
            {
                IoUtil.unmap(byteBuffer);
            }

            throw new RuntimeException(ex);
        }

        return byteBuffer;
    }

    public static MappedByteBuffer mapExistingFile(
        final File markFile, final Consumer<String> logger, final long offset, final long length)
    {
        if (markFile.exists())
        {
            if (null != logger)
            {
                logger.accept("INFO: Mark file exists: " + markFile);
            }

            return IoUtil.mapExistingFile(markFile, markFile.toString(), offset, length);
        }

        return null;
    }

    public static MappedByteBuffer mapExistingFile(final File markFile, final Consumer<String> logger)
    {
        if (markFile.exists())
        {
            if (null != logger)
            {
                logger.accept("INFO: Mark" + " file exists: " + markFile);
            }

            return IoUtil.mapExistingFile(markFile, markFile.toString());
        }

        return null;
    }

    public static MappedByteBuffer mapNewFile(final File markFile, final long length)
    {
        return IoUtil.mapNewFile(markFile, length);
    }

    public static boolean isActive(
        final MappedByteBuffer byteBuffer,
        final EpochClock epochClock,
        final long timeoutMs,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final IntConsumer versionCheck,
        final Consumer<String> logger)
    {
        if (null == byteBuffer)
        {
            return false;
        }

        final UnsafeBuffer buffer = new UnsafeBuffer(byteBuffer);

        final long startTimeMs = epochClock.time();
        int version;
        while (0 == (version = buffer.getIntVolatile(versionFieldOffset)))
        {
            if (epochClock.time() > (startTimeMs + timeoutMs))
            {
                throw new IllegalStateException("Mark file is created but not initialised.");
            }

            sleep(1);
        }

        versionCheck.accept(version);

        final long timestampMs = buffer.getLongVolatile(timestampFieldOffset);
        final long nowMs = epochClock.time();
        final long timestampAgeMs = nowMs - timestampMs;

        if (null != logger)
        {
            logger.accept("INFO: heartbeat is (ms): " + timestampAgeMs);
        }

        return timestampAgeMs <= timeoutMs;
    }

    private static void validateOffsets(final int versionFieldOffset, final int timestampFieldOffset)
    {
        if ((versionFieldOffset + SIZE_OF_INT) > timestampFieldOffset)
        {
            throw new IllegalArgumentException("version field must precede the timestamp field");
        }
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
