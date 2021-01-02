/*
 * Copyright 2014-2021 Real Logic Limited.
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Abstraction over a range of buffer types that allows fields to be written in native typed fashion.
 * <p>
 * {@link ByteOrder} of a wrapped buffer is not applied to the {@link MutableDirectBuffer}.
 * To control {@link ByteOrder} use the appropriate method with a {@link ByteOrder} overload.
 */
public interface MutableDirectBuffer extends DirectBuffer
{
    /**
     * Is this buffer expandable to accommodate putting data into it beyond the current capacity?
     *
     * @return true is the underlying storage can expand otherwise false.
     */
    boolean isExpandable();

    /**
     * Set a region of memory to a given byte value.
     *
     * @param index  at which to start.
     * @param length of the run of bytes to set.
     * @param value  the memory will be set to.
     */
    void setMemory(int index, int length, byte value);

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     for at a given index.
     * @param byteOrder of the value when written.
     */
    void putLong(int index, long value, ByteOrder byteOrder);

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putLong(int index, long value);

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     to be written.
     * @param byteOrder of the value when written.
     */
    void putInt(int index, int value, ByteOrder byteOrder);

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value for at a given index.
     */
    void putInt(int index, int value);

    /**
     * Puts an ASCII encoded int into the buffer.
     *
     * @param index the offset at which to put the int.
     * @param value the int to write.
     * @return the number of bytes that the int took up encoded.
     */
    int putIntAscii(int index, int value);

    /**
     * Puts an ASCII encoded int sized natural number into the buffer.
     *
     * @param index the offset at which to put the int.
     * @param value the int to write.
     * @return the number of bytes that the int took up encoded.
     */
    int putNaturalIntAscii(int index, int value);

    /**
     * Encode a natural number with a specified maximum length.
     *
     * If ascii encoding of the number is less than the specified length then the start will be
     * pre-padded with zeros, if the value takes up more space than the allowed length then a
     * <code>{@link NumberFormatException}</code> will be thrown.
     *
     * @param index the offset to start encoding at.
     * @param length the maximum length to encode.
     * @param value the value to encode.
     * @throws NumberFormatException if the value won't fit within the length.
     */
    void putNaturalPaddedIntAscii(int index, int length, int value) throws NumberFormatException;

    /**
     * Encode a natural number starting at its end position.
     *
     * @param value        the natural number to encode.
     * @param endExclusive index after the last character encoded.
     * @return startInclusive index of first character encoded.
     */
    int putNaturalIntAsciiFromEnd(int value, int endExclusive);

    /**
     * Puts an ASCII encoded long sized natural number into the buffer.
     *
     * @param index the offset at which to put the int.
     * @param value the int to write.
     * @return the number of bytes that the int took up encoded.
     */
    int putNaturalLongAscii(int index, long value);

    /**
     * Puts an ASCII encoded long integer into the buffer.
     *
     * @param index the offset at which to put the int.
     * @param value the int to write.
     * @return the number of bytes that the int took up encoded.
     */
    int putLongAscii(int index, long value);

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     to be written.
     * @param byteOrder of the value when written.
     */
    void putDouble(int index, double value, ByteOrder byteOrder);

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value to be written.
     */
    void putDouble(int index, double value);

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     to be written.
     * @param byteOrder of the value when written.
     */
    void putFloat(int index, float value, ByteOrder byteOrder);

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value to be written.
     */
    void putFloat(int index, float value);

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     to be written.
     * @param byteOrder of the value when written.
     */
    void putShort(int index, short value, ByteOrder byteOrder);

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value to be written.
     */
    void putShort(int index, short value);

    /**
     * Put a value to a given index.
     *
     * @param index     in bytes for where to put.
     * @param value     to be written.
     * @param byteOrder of the value when written.
     */
    void putChar(int index, char value, ByteOrder byteOrder);

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value to be written.
     */
    void putChar(int index, char value);

    /**
     * Put a value to a given index.
     *
     * @param index in bytes for where to put.
     * @param value to be written.
     */
    void putByte(int index, byte value);

    /**
     * Put an array of src into the underlying buffer.
     *
     * @param index in the underlying buffer to start from.
     * @param src   to be copied to the underlying buffer.
     */
    void putBytes(int index, byte[] src);

    /**
     * Put an array into the underlying buffer.
     *
     * @param index  in the underlying buffer to start from.
     * @param src    to be copied to the underlying buffer.
     * @param offset in the supplied buffer to begin the copy.
     * @param length of the supplied buffer to copy.
     */
    void putBytes(int index, byte[] src, int offset, int length);

    /**
     * Put bytes into the underlying buffer for the view.  Bytes will be copied from current
     * {@link ByteBuffer#position()} for a given length.
     * <p>
     * The source buffer will have its {@link ByteBuffer#position()} advanced as a result.
     *
     * @param index     in the underlying buffer to start from.
     * @param srcBuffer to copy the bytes from.
     * @param length    of the supplied buffer to copy.
     */
    void putBytes(int index, ByteBuffer srcBuffer, int length);

    /**
     * Put bytes into the underlying buffer for the view. Bytes will be copied from the buffer index to
     * the buffer index + length.
     * <p>
     * The source buffer will not have its {@link ByteBuffer#position()} advanced as a result.
     *
     * @param index     in the underlying buffer to start from.
     * @param srcBuffer to copy the bytes from (does not change position).
     * @param srcIndex  in the source buffer from which the copy will begin.
     * @param length    of the bytes to be copied.
     */
    void putBytes(int index, ByteBuffer srcBuffer, int srcIndex, int length);

    /**
     * Put bytes from a source {@link DirectBuffer} into this {@link MutableDirectBuffer} at given indices.
     *
     * @param index     in this buffer to begin putting the bytes.
     * @param srcBuffer from which the bytes will be copied.
     * @param srcIndex  in the source buffer from which the byte copy will begin.
     * @param length    of the bytes to be copied.
     */
    void putBytes(int index, DirectBuffer srcBuffer, int srcIndex, int length);

    /**
     * Encode a String as ASCII bytes to the buffer with a length prefix.
     *
     * @param index at which the String should be encoded.
     * @param value of the String to be encoded.
     * @return the number of bytes put to the buffer.
     */
    int putStringAscii(int index, String value);

    /**
     * Encode a String as ASCII bytes to the buffer with a length prefix.
     *
     * @param index     at which the String should be encoded.
     * @param value     of the String to be encoded.
     * @param byteOrder for the length prefix.
     * @return the number of bytes put to the buffer.
     */
    int putStringAscii(int index, String value, ByteOrder byteOrder);

    /**
     * Encode a String as ASCII bytes in the buffer without a length prefix.
     *
     * @param index at which the String begins.
     * @param value of the String to be encoded.
     * @return the number of bytes encoded.
     */
    int putStringWithoutLengthAscii(int index, String value);

    /**
     * Encode a String as ASCII bytes in the buffer without a length prefix taking a range of the value.
     *
     * @param index       at which the String begins.
     * @param value       of the String to be encoded.
     * @param valueOffset in the value String to begin.
     * @param length      of the value String to encode. If this is greater than valueOffset - value length then the
     *                    lesser will be used.
     * @return the number of bytes encoded.
     */
    int putStringWithoutLengthAscii(int index, String value, int valueOffset, int length);

    /**
     * Encode a String as UTF-8 bytes to the buffer with a length prefix.
     *
     * @param index at which the String should be encoded.
     * @param value of the String to be encoded.
     * @return the number of bytes put to the buffer.
     */
    int putStringUtf8(int index, String value);

    /**
     * Encode a String as UTF-8 bytes to the buffer with a length prefix.
     *
     * @param index     at which the String should be encoded.
     * @param value     of the String to be encoded.
     * @param byteOrder for the length prefix.
     * @return the number of bytes put to the buffer.
     */
    int putStringUtf8(int index, String value, ByteOrder byteOrder);

    /**
     * Encode a String as UTF-8 bytes the buffer with a length prefix with a maximum encoded size check.
     *
     * @param index            at which the String should be encoded.
     * @param value            of the String to be encoded.
     * @param maxEncodedLength to be checked before writing to the buffer.
     * @return the number of bytes put to the buffer.
     * @throws java.lang.IllegalArgumentException if the encoded bytes are greater than maxEncodedLength.
     */
    int putStringUtf8(int index, String value, int maxEncodedLength);

    /**
     * Encode a String as UTF-8 bytes the buffer with a length prefix with a maximum encoded size check.
     *
     * @param index            at which the String should be encoded.
     * @param value            of the String to be encoded.
     * @param byteOrder        for the length prefix.
     * @param maxEncodedLength to be checked before writing to the buffer.
     * @return the number of bytes put to the buffer.
     * @throws java.lang.IllegalArgumentException if the encoded bytes are greater than maxEncodedLength.
     */
    int putStringUtf8(int index, String value, ByteOrder byteOrder, int maxEncodedLength);

    /**
     * Encode a String as UTF-8 bytes in the buffer without a length prefix.
     *
     * @param index at which the String begins.
     * @param value of the String to be encoded.
     * @return the number of bytes encoded.
     */
    int putStringWithoutLengthUtf8(int index, String value);
}
