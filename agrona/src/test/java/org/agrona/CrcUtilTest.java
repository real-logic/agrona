package org.agrona;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import static org.agrona.BufferUtil.address;
import static org.agrona.BufferUtil.allocateDirectAligned;
import static org.agrona.CrcUtil.crc32DirectByteBuffer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CrcUtilTest
{

    @Test
    void crc32DirectByteBufferShouldComputeCorrectCrc32Checksum()
    {
        final CRC32 crc32 = new CRC32();
        final byte[] data = new byte[23];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = (byte)i;
            crc32.update(i);
        }
        final int checksum1 = (int)crc32.getValue();
        crc32.update(new byte[10]);
        final int checksum2 = (int)crc32.getValue();

        final ByteBuffer buffer = allocateDirectAligned(100, 64);
        buffer.position(5);
        buffer.put(data);
        final long address = address(buffer);

        assertEquals(checksum1, crc32DirectByteBuffer(0, address, 5, 23));
        assertEquals(checksum2, crc32DirectByteBuffer(checksum1, address, 28, 10));
        assertNotEquals(checksum2, crc32DirectByteBuffer(0, address, 28, 10));
        assertEquals(checksum1, crc32DirectByteBuffer(0, address, 5, 23));
    }

    @ParameterizedTest
    @CsvSource({ "-1,10", "33, 5", "0,-10", "0,100" })
    void crc32DirectByteBufferProducesGarbageWhenWrongOffsetOrLengthSpecified(final int offset, final int length)
    {
        final ByteBuffer buffer = allocateDirectAligned(32, 32);
        final long address = address(buffer);

        assertNotEquals(0, crc32DirectByteBuffer(0, address, offset, length));
    }
}