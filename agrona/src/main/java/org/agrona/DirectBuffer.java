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

import static org.agrona.BitUtil.SIZE_OF_INT;

/**
 * Abstraction over a range of buffer types that allows fields to be read in native typed fashion.
 * <p>
 * {@link ByteOrder} of a wrapped buffer is not applied to the {@link DirectBuffer};
 * To control {@link ByteOrder} use the appropriate method with the {@link ByteOrder} overload.
 */
public interface DirectBuffer extends Comparable<DirectBuffer>
{
    /**
     * Length of the header on strings to denote the length of the string in bytes.
     */
    int STR_HEADER_LEN = SIZE_OF_INT;

    /**
     * Don't print the content of the array while calling toString() on buffer instance.
     */
    @Deprecated
    String DISABLE_ARRAY_CONTENT_PRINTOUT_PROP_NAME = "agrona.disable.array.printout";

    /**
     * Attach a view to a byte[] for providing direct access.
     *
     * @param buffer to which the view is attached.
     */
    void wrap(byte[] buffer);

    /**
     * Attach a view to a byte[] for providing direct access.
     *
     * @param buffer to which the view is attached.
     * @param offset at which the view begins.
     * @param length of the buffer included in the view
     */
    void wrap(byte[] buffer, int offset, int length);

    /**
     * Attach a view to a {@link ByteBuffer} for providing direct access, the {@link ByteBuffer} can be
     * heap based or direct. The {@link ByteBuffer#order()} is not relevant for accessing the wrapped buffer.
     * <p>
     * When using this method to wrap the view of the ByteBuffer the entire ByteBuffer gets wrapped
     * between index 0 and capacity. If you want to just wrap the ByteBuffer between the position
     * and the limit then you should use the {@link #wrap(ByteBuffer, int, int)} method, eg:
     *
     * <code>
     * directBuffer.wrap(byteBuffer, byteBuffer.position(), byteBuffer.remaining());
     * </code>
     *
     * @param buffer to which the view is attached.
     */
    void wrap(ByteBuffer buffer);

    /**
     * Attach a view to a {@link ByteBuffer} for providing direct access.
     * <p>
     * The {@link ByteBuffer#order()} is not relevant for accessing the wrapped buffer.
     *
     * @param buffer to which the view is attached.
     * @param offset at which the view begins.
     * @param length of the buffer included in the view.
     */
    void wrap(ByteBuffer buffer, int offset, int length);

    /**
     * Attach a view to an existing {@link DirectBuffer}
     *
     * @param buffer to which the view is attached.
     */
    void wrap(DirectBuffer buffer);

    /**
     * Attach a view to a {@link DirectBuffer} for providing direct access.
     *
     * @param buffer to which the view is attached.
     * @param offset at which the view begins.
     * @param length of the buffer included in the view.
     */
    void wrap(DirectBuffer buffer, int offset, int length);

    /**
     * Attach a view to an off-heap memory region by address.
     *
     * @param address where the memory begins off-heap.
     * @param length  of the buffer from the given address.
     */
    void wrap(long address, int length);

    /**
     * Reads the underlying offset to to the memory address.
     *
     * @return the underlying offset to to the memory address.
     */
    long addressOffset();

    /**
     * Get the underlying byte[] if one exists.
     * <p>
     * NB: there may not be a one-to-one mapping between indices on this buffer
     * and the underlying byte[], see {@link DirectBuffer#wrapAdjustment()}.
     *
     * @return the underlying byte[] if one exists.
     */
    byte[] byteArray();

    /**
     * Get the underlying {@link ByteBuffer} if one exists.
     * <p>
     * NB: there may not be a one-to-one mapping between indices on this buffer
     * and the underlying byte[], see {@link DirectBuffer#wrapAdjustment()}.
     *
     * @return the underlying {@link ByteBuffer} if one exists.
     */
    ByteBuffer byteBuffer();

    /**
     * Get the capacity of the underlying buffer.
     *
     * @return the capacity of the underlying buffer in bytes.
     */
    int capacity();

    /**
     * Check that a given limit is not greater than the capacity of a buffer from a given offset.
     * <p>
     * Can be overridden in a DirectBuffer subclass to enable an extensible buffer or handle retry after a flush.
     *
     * @param limit up to which access is required.
     * @throws IndexOutOfBoundsException if limit is beyond buffer capacity.
     */
    void checkLimit(int limit);

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value for at a given index.
     */
    long getLong(int index, ByteOrder byteOrder);

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value for at a given index.
     */
    long getLong(int index);

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value at a given index.
     */
    int getInt(int index, ByteOrder byteOrder);

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value for a given index.
     */
    int getInt(int index);

    /**
     * Parse the ASCII encoded int sized natural value at a given index.
     *
     * @param index  in bytes from which to get.
     * @param length the length in bytes to parse.
     * @return the value at a given index.
     */
    int parseNaturalIntAscii(int index, int length);

    /**
     * Parse the ASCII encoded long sized natural value at a given index.
     *
     * @param index  in bytes from which to get.
     * @param length the length in bytes to parse.
     * @return the value at a given index.
     */
    long parseNaturalLongAscii(int index, int length);

    /**
     * Parse the ASCII encoded integer value at a given index.
     *
     * @param index  in bytes from which to get.
     * @param length the length in bytes to parse.
     * @return the value at a given index.
     */
    int parseIntAscii(int index, int length);

    /**
     * Parse the ASCII encoded long integer value at a given index.
     *
     * @param index  in bytes from which to get.
     * @param length the length in bytes to parse.
     * @return the value at a given index.
     */
    long parseLongAscii(int index, int length);

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value at a given index.
     */
    double getDouble(int index, ByteOrder byteOrder);

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value at a given index.
     */
    double getDouble(int index);

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value at a given index.
     */
    float getFloat(int index, ByteOrder byteOrder);

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value at a given index.
     */
    float getFloat(int index);

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value at a given index.
     */
    short getShort(int index, ByteOrder byteOrder);

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value at a given index.
     */
    short getShort(int index);

    /**
     * Get the value at a given index.
     *
     * @param index     in bytes from which to get.
     * @param byteOrder of the value to be read.
     * @return the value at a given index.
     */
    char getChar(int index, ByteOrder byteOrder);

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value at a given index.
     */
    char getChar(int index);

    /**
     * Get the value at a given index.
     *
     * @param index in bytes from which to get.
     * @return the value at a given index.
     */
    byte getByte(int index);

    /**
     * Get from the underlying buffer into a supplied byte array.
     * This method will try to fill the supplied byte array.
     *
     * @param index in the underlying buffer to start from.
     * @param dst   into which the dst will be copied.
     */
    void getBytes(int index, byte[] dst);

    /**
     * Get bytes from the underlying buffer into a supplied byte array.
     *
     * @param index  in the underlying buffer to start from.
     * @param dst    into which the bytes will be copied.
     * @param offset in the supplied buffer to start the copy.
     * @param length of the supplied buffer to use.
     */
    void getBytes(int index, byte[] dst, int offset, int length);

    /**
     * Get bytes from this {@link DirectBuffer} into the provided {@link MutableDirectBuffer} at given indices.
     *
     * @param index     in this buffer to begin getting the bytes.
     * @param dstBuffer to which the bytes will be copied.
     * @param dstIndex  in the channel buffer to which the byte copy will begin.
     * @param length    of the bytes to be copied.
     */
    void getBytes(int index, MutableDirectBuffer dstBuffer, int dstIndex, int length);

    /**
     * Get from the underlying buffer into a supplied {@link ByteBuffer} current {@link ByteBuffer#position()}.
     * <p>
     * The destination buffer will have its {@link ByteBuffer#position()} advanced as a result.
     *
     * @param index     in the underlying buffer to start from.
     * @param dstBuffer into which the bytes will be copied.
     * @param length    of the supplied buffer to use.
     */
    void getBytes(int index, ByteBuffer dstBuffer, int length);

    /**
     * Get from the underlying buffer into a supplied {@link ByteBuffer} at a offset.
     * <p>
     * The destination buffer will not have its {@link ByteBuffer#position()} advanced as a result.
     *
     * @param index     in the underlying buffer to start from.
     * @param dstBuffer into which the bytes will be copied.
     * @param dstOffset in the target buffer.
     * @param length    of the supplied buffer to use.
     */
    void getBytes(int index, ByteBuffer dstBuffer, int dstOffset, int length);

    /**
     * Get a String from bytes encoded in ASCII format that is length prefixed.
     *
     * @param index at which the String begins.
     * @return the String as represented by the ASCII encoded bytes.
     */
    String getStringAscii(int index);

    /**
     * Get a String from bytes encoded in ASCII format that is length prefixed and append to an {@link Appendable}.
     *
     * @param index      at which the String begins.
     * @param appendable to append the chars to.
     * @return the number of bytes copied.
     */
    int getStringAscii(int index, Appendable appendable);

    /**
     * Get a String from bytes encoded in ASCII format that is length prefixed.
     *
     * @param index     at which the String begins.
     * @param byteOrder for the length at the beginning of the String.
     * @return the String as represented by the ASCII encoded bytes.
     */
    String getStringAscii(int index, ByteOrder byteOrder);

    /**
     * Get a String from bytes encoded in ASCII format that is length prefixed and append to an {@link Appendable}.
     *
     * @param index      at which the String begins.
     * @param appendable to append the chars to.
     * @param byteOrder  for the length at the beginning of the String.
     * @return the number of bytes copied.
     */
    int getStringAscii(int index, Appendable appendable, ByteOrder byteOrder);

    /**
     * Get part of a String from bytes encoded in ASCII format that is length prefixed.
     *
     * @param index  at which the String begins.
     * @param length of the String in bytes to decode.
     * @return the String as represented by the ASCII encoded bytes.
     */
    String getStringAscii(int index, int length);

    /**
     * Get part of a String from bytes encoded in ASCII format that is length prefixed and append to an
     * {@link Appendable}.
     *
     * @param index      at which the String begins.
     * @param length     of the String in bytes to decode.
     * @param appendable to append the chars to.
     * @return the number of bytes copied.
     */
    int getStringAscii(int index, int length, Appendable appendable);

    /**
     * Get an encoded ASCII String from the buffer that does not have a length prefix.
     *
     * @param index  at which the String begins.
     * @param length of the String in bytes to decode.
     * @return the String as represented by the Ascii encoded bytes.
     */
    String getStringWithoutLengthAscii(int index, int length);

    /**
     * Get an encoded ASCII String from the buffer that does not have a length prefix and append to an
     * {@link Appendable}.
     *
     * @param index      at which the String begins.
     * @param length     of the String in bytes to decode.
     * @param appendable to append the chars to.
     * @return the number of bytes copied.
     */
    int getStringWithoutLengthAscii(int index, int length, Appendable appendable);

    /**
     * Get a String from bytes encoded in UTF-8 format that is length prefixed.
     *
     * @param index at which the String begins.
     * @return the String as represented by the UTF-8 encoded bytes.
     */
    String getStringUtf8(int index);

    /**
     * Get a String from bytes encoded in UTF-8 format that is length prefixed.
     *
     * @param index     at which the String begins.
     * @param byteOrder for the length at the beginning of the String.
     * @return the String as represented by the UTF-8 encoded bytes.
     */
    String getStringUtf8(int index, ByteOrder byteOrder);

    /**
     * Get part of String from bytes encoded in UTF-8 format that is length prefixed.
     *
     * @param index  at which the String begins.
     * @param length of the String in bytes to decode.
     * @return the String as represented by the UTF-8 encoded bytes.
     */
    String getStringUtf8(int index, int length);

    /**
     * Get an encoded UTF-8 String from the buffer that does not have a length prefix.
     *
     * @param index  at which the String begins.
     * @param length of the String in bytes to decode.
     * @return the String as represented by the UTF-8 encoded bytes.
     */
    String getStringWithoutLengthUtf8(int index, int length);

    /**
     * Check that a given length of bytes is within the bounds from a given index.
     *
     * @param index  from which to check.
     * @param length in bytes of the range to check.
     * @throws java.lang.IndexOutOfBoundsException if the length goes outside of the capacity range.
     */
    void boundsCheck(int index, int length);

    /**
     * Get the adjustment in indices between an index in this buffer and the wrapped object.
     * The wrapped object might be a {@link ByteBuffer} or a byte[].
     * <p>
     * You only need to use this adjustment if you plan to perform operations on the underlying
     * byte array or byte buffer that rely on their indices.
     *
     * @return the adjustment in indices between an index in this buffer and the wrapped object.
     * @see DirectBuffer#byteArray()
     * @see DirectBuffer#byteBuffer()
     */
    int wrapAdjustment();
}
