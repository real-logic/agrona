package org.agrona;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.agrona.concurrent.SystemEpochClock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MarkFileTest
{
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test(expected = IllegalStateException.class)
    public void shouldWaitForMarkFileToContainEnoughDataForVersionCheck() throws IOException
    {
        final File directory = tmpDir.newFolder();
        final String filename = "markfile.dat";
        final Path markFilePath = directory.toPath().resolve(filename);
        Files.createFile(markFilePath);

        try (FileChannel channel = FileChannel.open(markFilePath, StandardOpenOption.WRITE))
        {
            channel.write(ByteBuffer.allocate(1));
        }
        new MarkFile(directory, filename, 0,
          16, 10, new SystemEpochClock(), v -> {}, msg -> {});
    }
}