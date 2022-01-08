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
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IoUtilTest
{
    @TempDir
    Path tempDir;

    @Test
    void deleteIgnoreFailuresNonExistingDirectory()
    {
        final File dir = tempDir.resolve("shadow-dir").toFile();
        assertTrue(dir.mkdir());
        assertTrue(dir.delete());

        IoUtil.delete(dir, false);

        assertFalse(dir.exists());
    }

    @Test
    void deleteIgnoreFailuresNonExistingFile()
    {
        final File file = tempDir.resolve("shadow-file").toFile();

        IoUtil.delete(file, false);

        assertFalse(file.exists());
    }

    @Test
    void deleteIgnoreFailuresDirectory() throws IOException
    {
        final Path dir2 = tempDir.resolve("dir1").resolve("dir2");
        Files.createDirectories(dir2);
        Files.createFile(dir2.resolve("file2.txt"));
        Files.createFile(dir2.getParent().resolve("file1.txt"));
        final File dir = dir2.getParent().toFile();

        IoUtil.delete(dir, false);

        assertFalse(dir.exists());
        assertFalse(Files.exists(dir2));
    }

    @Test
    void deleteIgnoreFailuresFile() throws IOException
    {
        final Path file = tempDir.resolve("file.txt");
        Files.createFile(file);

        IoUtil.delete(file.toFile(), false);

        assertFalse(Files.exists(file));
    }

    @Test
    void deleteIgnoreFailuresShouldThrowExceptionIfDeleteOfAFileFails()
    {
        final File file = mock(File.class);
        when(file.exists()).thenReturn(true);
        when(file.delete()).thenReturn(false);

        assertThrows(NullPointerException.class, () -> IoUtil.delete(file, false));
    }

    @Test
    void deleteIgnoreFailuresShouldThrowExceptionIfDeleteOfADirectoryFails()
    {
        final File dir = mock(File.class);
        when(dir.exists()).thenReturn(true);
        when(dir.isDirectory()).thenReturn(true);
        when(dir.delete()).thenReturn(false);

        assertThrows(NullPointerException.class, () -> IoUtil.delete(dir, false));
    }

    @Test
    void deleteErrorHandlerNonExistingDirectory()
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final File dir = tempDir.resolve("shadow-dir").toFile();
        assertTrue(dir.mkdir());
        assertTrue(dir.delete());

        IoUtil.delete(dir, errorHandler);

        assertFalse(dir.exists());
        verifyNoInteractions(errorHandler);
    }

    @Test
    void deleteErrorHandlerIgnoreFailuresNonExistingFile()
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final File file = tempDir.resolve("shadow-file").toFile();

        IoUtil.delete(file, errorHandler);

        assertFalse(file.exists());
        verifyNoInteractions(errorHandler);
    }

    @Test
    void deleteErrorHandlerDirectory() throws IOException
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final Path dir2 = tempDir.resolve("dir1").resolve("dir2");
        Files.createDirectories(dir2);
        Files.createFile(dir2.resolve("file2.txt"));
        Files.createFile(dir2.getParent().resolve("file1.txt"));
        final File dir = dir2.getParent().toFile();

        IoUtil.delete(dir, errorHandler);

        assertFalse(dir.exists());
        assertFalse(Files.exists(dir2));
        verifyNoInteractions(errorHandler);
    }

    @Test
    void deleteErrorHandlerFile() throws IOException
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final Path file = tempDir.resolve("file.txt");
        Files.createFile(file);

        IoUtil.delete(file.toFile(), errorHandler);

        assertFalse(Files.exists(file));
        verifyNoInteractions(errorHandler);
    }

    @Test
    void deleteErrorHandlerShouldCatchExceptionIfDeleteOfAFileFails()
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final File file = mock(File.class);
        when(file.exists()).thenReturn(true);
        when(file.delete()).thenReturn(false);

        IoUtil.delete(file, errorHandler);

        verify(errorHandler).onError(isA(NullPointerException.class));
    }

    @Test
    void deleteErrorHandlerShouldCatchExceptionIfDeleteOfADirectoryFails()
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final File dir = mock(File.class);
        when(dir.exists()).thenReturn(true);
        when(dir.isDirectory()).thenReturn(true);
        when(dir.delete()).thenReturn(false);

        IoUtil.delete(dir, errorHandler);

        verify(errorHandler).onError(isA(NullPointerException.class));
    }

    @Test
    void deleteIfExistsNonExistingFile()
    {
        final Path file = tempDir.resolve("non-existing.txt");

        IoUtil.deleteIfExists(file.toFile());

        assertFalse(Files.exists(file));
    }

    @Test
    void deleteIfExistsFile() throws IOException
    {
        final Path file = tempDir.resolve("delete-me.txt");
        Files.createFile(file);

        IoUtil.deleteIfExists(file.toFile());

        assertFalse(Files.exists(file));
    }

    @Test
    void deleteIfExistsEmptyDirectory() throws IOException
    {
        final Path dir = tempDir.resolve("dir");
        Files.createDirectory(dir);

        IoUtil.deleteIfExists(dir.toFile());

        assertFalse(Files.exists(dir));
    }

    @Test
    void deleteIfExistsFailsOnNonEmptyDirectory() throws IOException
    {
        final Path dir = tempDir.resolve("dir");
        Files.createDirectory(dir);
        Files.createFile(dir.resolve("file.txt"));

        assertThrows(DirectoryNotEmptyException.class, () -> IoUtil.deleteIfExists(dir.toFile()));
    }

    @Test
    void deleteIfExistsErrorHandlerNonExistingFile()
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final Path file = tempDir.resolve("non-existing.txt");

        IoUtil.deleteIfExists(file.toFile(), errorHandler);

        assertFalse(Files.exists(file));
        verifyNoInteractions(errorHandler);
    }

    @Test
    void deleteIfExistsErrorHandlerFile() throws IOException
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final Path file = tempDir.resolve("delete-me.txt");
        Files.createFile(file);

        IoUtil.deleteIfExists(file.toFile(), errorHandler);

        assertFalse(Files.exists(file));
        verifyNoInteractions(errorHandler);
    }

    @Test
    void deleteIfExistsErrorHandlerEmptyDirectory() throws IOException
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final Path dir = tempDir.resolve("dir");
        Files.createDirectory(dir);

        IoUtil.deleteIfExists(dir.toFile(), errorHandler);

        assertFalse(Files.exists(dir));
        verifyNoInteractions(errorHandler);
    }

    @Test
    void deleteIfExistsErrorHandlerFailsOnNonEmptyDirectory() throws IOException
    {
        final ErrorHandler errorHandler = mock(ErrorHandler.class);
        final Path dir = tempDir.resolve("dir");
        Files.createDirectory(dir);
        Files.createFile(dir.resolve("file.txt"));

        IoUtil.deleteIfExists(dir.toFile(), errorHandler);

        verify(errorHandler).onError(isA(DirectoryNotEmptyException.class));
    }
}