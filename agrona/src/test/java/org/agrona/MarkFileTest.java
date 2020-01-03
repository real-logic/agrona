package org.agrona;

import org.agrona.concurrent.SystemEpochClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class MarkFileTest
{
    @TempDir
    File directory;

    @Test
    public void shouldWaitForMarkFileToContainEnoughDataForVersionCheck() throws IOException
    {

        final String filename = "markfile.dat";
        final Path markFilePath = directory.toPath().resolve(filename);
        Files.createFile(markFilePath);

        try (FileChannel channel = FileChannel.open(markFilePath, StandardOpenOption.WRITE))
        {
            channel.write(ByteBuffer.allocate(1));
        }
        assertThrows(IllegalStateException.class, () ->
            new MarkFile(directory, filename, 0, 16, 10, new SystemEpochClock(), v -> {}, msg -> {}));
    }
}