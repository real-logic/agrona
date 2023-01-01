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

import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.nio.file.StandardOpenOption.*;
import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * A {@link MarkFile} is used to mark the presence of a running component and to track liveness.
 * <p>
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

    private final AtomicBoolean isClosed = new AtomicBoolean();

    /**
     * Create a directory and mark file if none present. Checking if an active Mark file exists and is active.
     * Old Mark file is deleted and recreated if not active.
     * <p>
     * Total length of Mark file will be mapped until {@link #close()} is called.
     *
     * @param directory             for the Mark file.
     * @param filename              of the Mark file.
     * @param warnIfDirectoryExists for logging purposes.
     * @param dirDeleteOnStart      if desired.
     * @param versionFieldOffset    to use for version field access.
     * @param timestampFieldOffset  to use for timestamp field access.
     * @param totalFileLength       to allocate when creating new Mark file.
     * @param timeoutMs             for the activity check (in milliseconds).
     * @param epochClock            to use for time checks.
     * @param versionCheck          to use for existing Mark file and version field.
     * @param logger                to use to signal progress or null.
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
        this.mappedBuffer = IoUtil.mapNewFile(markFile, totalFileLength);
        this.buffer = new UnsafeBuffer(mappedBuffer);
        this.versionFieldOffset = versionFieldOffset;
        this.timestampFieldOffset = timestampFieldOffset;
    }

    /**
     * Create a {@link MarkFile} if none present. Checking if an active {@link MarkFile} exists and is active.
     * Existing {@link MarkFile} is used if not active.
     * <p>
     * Total length of Mark file will be mapped until {@link #close()} is called.
     *
     * @param markFile             to use.
     * @param shouldPreExist       or not.
     * @param versionFieldOffset   to use for version field access.
     * @param timestampFieldOffset to use for timestamp field access.
     * @param totalFileLength      to allocate when creating new {@link MarkFile}.
     * @param timeoutMs            for the activity check (in milliseconds).
     * @param epochClock           to use for time checks.
     * @param versionCheck         to use for existing {@link MarkFile} and version field.
     * @param logger               to use to signal progress or null.
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
     * <p>
     * Total length of {@link MarkFile} will be mapped until {@link #close()} is called.
     *
     * @param directory            for the {@link MarkFile} file.
     * @param filename             of the {@link MarkFile} file.
     * @param versionFieldOffset   to use for version field access.
     * @param timestampFieldOffset to use for timestamp field access.
     * @param timeoutMs            for the activity check (in milliseconds) and for how long to wait for file to exist.
     * @param epochClock           to use for time checks.
     * @param versionCheck         to use for existing {@link MarkFile} file and version field.
     * @param logger               to use to signal progress or null.
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
     * <p>
     * If mappedBuffer is not null, then it will be unmapped upon {@link #close()}.
     *
     * @param mappedBuffer         for the {@link MarkFile} fields.
     * @param versionFieldOffset   for the version field.
     * @param timestampFieldOffset for the timestamp field.
     */
    public MarkFile(final MappedByteBuffer mappedBuffer, final int versionFieldOffset, final int timestampFieldOffset)
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
    public MarkFile(final UnsafeBuffer buffer, final int versionFieldOffset, final int timestampFieldOffset)
    {
        validateOffsets(versionFieldOffset, timestampFieldOffset);

        this.parentDir = null;
        this.markFile = null;
        this.mappedBuffer = null;
        this.buffer = buffer;
        this.versionFieldOffset = versionFieldOffset;
        this.timestampFieldOffset = timestampFieldOffset;
    }

    /**
     * Checks if {@link MarkFile} is closed.
     *
     * @return {@code true} if {@link MarkFile} is closed.
     */
    public boolean isClosed()
    {
        return isClosed.get();
    }

    /**
     * {@inheritDoc}
     */
    public void close()
    {
        if (isClosed.compareAndSet(false, true))
        {
            BufferUtil.free(mappedBuffer);
        }
    }

    /**
     * Perform an ordered put of the version field.
     *
     * @param version to be signaled.
     */
    public void signalReady(final int version)
    {
        buffer.putIntOrdered(versionFieldOffset, version);
    }

    /**
     * Perform volatile read of the version field.
     *
     * @return value of the version field.
     */
    public int versionVolatile()
    {
        return buffer.getIntVolatile(versionFieldOffset);
    }

    /**
     * Perform weak/plain read of the version field.
     *
     * @return value of the version field.
     */
    public int versionWeak()
    {
        return buffer.getInt(versionFieldOffset);
    }

    /**
     * Set timestamp field using an ordered put.
     *
     * @param timestamp to be set.
     */
    public void timestampOrdered(final long timestamp)
    {
        buffer.putLongOrdered(timestampFieldOffset, timestamp);
    }

    /**
     * Perform volatile read of the timestamp field.
     *
     * @return value of the timestamp field.
     */
    public long timestampVolatile()
    {
        return buffer.getLongVolatile(timestampFieldOffset);
    }

    /**
     * Perform weak/plain read of the timestamp field.
     *
     * @return value of the timestamp field.
     */
    public long timestampWeak()
    {
        return buffer.getLong(timestampFieldOffset);
    }

    /**
     * Delete parent directory.
     *
     * @param ignoreFailures should the failures be silently ignored.
     */
    public void deleteDirectory(final boolean ignoreFailures)
    {
        IoUtil.delete(parentDir, ignoreFailures);
    }

    /**
     * Returns parent directory.
     *
     * @return parent directory.
     */
    public File parentDirectory()
    {
        return parentDir;
    }

    /**
     * Returns {@link MarkFile}.
     *
     * @return {@link MarkFile}.
     */
    public File markFile()
    {
        return markFile;
    }

    /**
     * Returns the underlying {@link MappedByteBuffer}.
     *
     * @return reference to the {@link MappedByteBuffer}.
     */
    public MappedByteBuffer mappedByteBuffer()
    {
        return mappedBuffer;
    }

    /**
     * Returns the underlying {@link UnsafeBuffer}.
     *
     * @return reference to the {@link UnsafeBuffer}.
     */
    public UnsafeBuffer buffer()
    {
        return buffer;
    }

    /**
     * Ensure the directory exists, i.e. create if it does not exist yet and re-create if it already exists.
     *
     * @param directory             to create.
     * @param filename              of the {@link MarkFile}.
     * @param warnIfDirectoryExists should print warning if directory already exists.
     * @param dirDeleteOnStart      should directory be deleted if it already exists. When the flag is set to
     *                              {@code false} the check will be made to see if the {@link MarkFile} is active.
     *                              <p>Note: the directory will be deleted anyway even if the flag is {@code false}.
     * @param versionFieldOffset    offset of the version field.
     * @param timestampFieldOffset  offset of the timestamp field.
     * @param timeoutMs             timeout in milliseconds.
     * @param epochClock            epoch clock.
     * @param versionCheck          {@link MarkFile} version check function.
     * @param logger                to use for reporting warnings.
     * @throws IllegalStateException if {@link MarkFile} already exists and is active and
     *                               {@code dirDeleteOnStart=false}.
     */
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
                        throw new IllegalStateException("active Mark file detected");
                    }
                }
                finally
                {
                    BufferUtil.free(byteBuffer);
                }
            }

            IoUtil.delete(directory, false);
        }

        IoUtil.ensureDirectoryExists(directory, directory.toString());
    }

    /**
     * Await the creation of the {@link MarkFile}.
     *
     * @param logger     to use for warnings.
     * @param markFile   the {@link MarkFile}.
     * @param deadlineMs deadline timeout in milliseconds.
     * @param epochClock epoch clock.
     * @return {@link MappedByteBuffer} for the {@link MarkFile}.
     * @throws IllegalStateException if deadline timeout is reached.
     */
    @SuppressWarnings("try")
    public static MappedByteBuffer waitForFileMapping(
        final Consumer<String> logger, final File markFile, final long deadlineMs, final EpochClock epochClock)
    {
        while (true)
        {
            try (FileChannel fileChannel = FileChannel.open(markFile.toPath(), READ, WRITE))
            {
                final long size = fileChannel.size();
                if (size < (SIZE_OF_INT + SIZE_OF_LONG))
                {
                    if (epochClock.time() > deadlineMs)
                    {
                        throw new IllegalStateException("Mark file is created but not populated");
                    }

                    fileChannel.close();
                    sleep(16);
                    continue;
                }

                if (null != logger)
                {
                    logger.accept("INFO: Mark file exists: " + markFile);
                }

                return fileChannel.map(READ_WRITE, 0, size);
            }
            catch (final IOException ex)
            {
                throw new IllegalStateException("cannot open mark file", ex);
            }
        }
    }

    /**
     * Map existing {@link MarkFile}.
     *
     * @param markFile             the {@link MarkFile}.
     * @param versionFieldOffset   offset of the version field.
     * @param timestampFieldOffset offset of the timestamp field.
     * @param timeoutMs            timeout in milliseconds.
     * @param epochClock           epoch clock.
     * @param versionCheck         version check function.
     * @param logger               for the warnings.
     * @return {@link MappedByteBuffer} for the {@link MarkFile}.
     * @throws IllegalStateException if timeout is reached.
     * @throws IllegalStateException if {@link MarkFile} has wrong size.
     */
    public static MappedByteBuffer mapExistingMarkFile(
        final File markFile,
        final int versionFieldOffset,
        final int timestampFieldOffset,
        final long timeoutMs,
        final EpochClock epochClock,
        final IntConsumer versionCheck,
        final Consumer<String> logger)
    {
        final long deadlineMs = epochClock.time() + timeoutMs;

        while (!markFile.exists() || markFile.length() < (timestampFieldOffset + SIZE_OF_LONG))
        {
            if (epochClock.time() > deadlineMs)
            {
                throw new IllegalStateException("Mark file not created: " + markFile.getName());
            }

            sleep(16);
        }

        final MappedByteBuffer byteBuffer = waitForFileMapping(logger, markFile, deadlineMs, epochClock);
        if (byteBuffer.capacity() < (timestampFieldOffset + SIZE_OF_LONG))
        {
            throw new IllegalStateException("Mark file mapping is to small: capacity=" + byteBuffer.capacity());
        }

        try
        {
            final UnsafeBuffer buffer = new UnsafeBuffer(byteBuffer);
            int version;
            while (0 == (version = buffer.getIntVolatile(versionFieldOffset)))
            {
                if (epochClock.time() > deadlineMs)
                {
                    throw new IllegalStateException("Mark file is created but not initialised");
                }

                sleep(1);
            }

            versionCheck.accept(version);

            while (0 == buffer.getLongVolatile(timestampFieldOffset))
            {
                if (epochClock.time() > deadlineMs)
                {
                    throw new IllegalStateException("no non-zero timestamp detected");
                }

                sleep(1);
            }
        }
        catch (final Exception ex)
        {
            BufferUtil.free(byteBuffer);
            LangUtil.rethrowUnchecked(ex);
        }

        return byteBuffer;
    }

    /**
     * Map new of existing {@link MarkFile}.
     *
     * @param markFile             the {@link MarkFile}.
     * @param shouldPreExist       should {@link MarkFile} already exist.
     * @param versionFieldOffset   offset of the version field.
     * @param timestampFieldOffset offset of the timestamp field.
     * @param totalFileLength      total file length to be mapped.
     * @param timeoutMs            timeout in milliseconds.
     * @param epochClock           epoch clock.
     * @param versionCheck         version check function.
     * @param logger               for the warnings.
     * @return {@link MappedByteBuffer} for the {@link MarkFile}.
     * @throws IllegalStateException if timeout is reached.
     */
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
                if (buffer.capacity() < (timestampFieldOffset + SIZE_OF_LONG))
                {
                    throw new IllegalStateException("active MarkFile too short capacity=" + buffer.capacity() +
                        " < " + (timestampFieldOffset + SIZE_OF_LONG));
                }

                final int version = buffer.getIntVolatile(versionFieldOffset);

                if (null != logger)
                {
                    logger.accept("INFO: Mark file exists: " + markFile);
                }

                versionCheck.accept(version);

                final long timestampMs = buffer.getLongVolatile(timestampFieldOffset);
                final long timestampAgeMs = epochClock.time() - timestampMs;

                if (null != logger)
                {
                    logger.accept("INFO: heartbeat timestampMs=" + timestampMs + " ageMs=" + timestampAgeMs);
                }

                if (timestampAgeMs < timeoutMs)
                {
                    throw new IllegalStateException("active Mark file detected");
                }
            }
        }
        catch (final Exception ex)
        {
            if (null != byteBuffer)
            {
                BufferUtil.free(byteBuffer);
            }

            throw new RuntimeException(ex);
        }

        return byteBuffer;
    }

    /**
     * Map existing {@link MarkFile}.
     *
     * @param markFile the {@link MarkFile}.
     * @param logger   for the warnings.
     * @param offset   offset to map at.
     * @param length   to map.
     * @return {@link MappedByteBuffer} for the {@link MarkFile}.
     */
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

    /**
     * Check if {@link MarkFile} is active, i.e. still in use.
     *
     * @param byteBuffer           the {@link MappedByteBuffer}.
     * @param epochClock           epoch clock.
     * @param timeoutMs            timeout in milliseconds.
     * @param versionFieldOffset   offset of the version field.
     * @param timestampFieldOffset offset of the timestamp field.
     * @param versionCheck         version check function.
     * @param logger               for the warnings.
     * @return {@code true} if {@link MarkFile} is active.
     */
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
        final long deadlineMs = epochClock.time() + timeoutMs;
        int version;

        while (0 == (version = buffer.getIntVolatile(versionFieldOffset)))
        {
            if (epochClock.time() > deadlineMs)
            {
                throw new IllegalStateException("Mark file is created but not initialised");
            }

            sleep(1);
        }

        versionCheck.accept(version);

        final long timestampMs = buffer.getLongVolatile(timestampFieldOffset);
        final long nowMs = epochClock.time();
        final long timestampAgeMs = nowMs - timestampMs;

        if (null != logger)
        {
            logger.accept("INFO: heartbeat timestampMs=" + timestampMs + " ageMs=" + timestampAgeMs);
        }

        return timestampAgeMs <= timeoutMs;
    }

    /**
     * Put thread to sleep for the given duration and restore interrupted status if thread is interrupted while
     * sleeping.
     *
     * @param durationMs sleep duration in milliseconds.
     */
    protected static void sleep(final long durationMs)
    {
        try
        {
            Thread.sleep(durationMs);
        }
        catch (final InterruptedException ignore)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static void validateOffsets(final int versionFieldOffset, final int timestampFieldOffset)
    {
        if ((versionFieldOffset + SIZE_OF_INT) > timestampFieldOffset)
        {
            throw new IllegalArgumentException("version field must precede the timestamp field");
        }
    }
}
