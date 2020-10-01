/*
 * Copyright 2014-2020 Real Logic Limited.
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
package org.agrona.agent;

import net.bytebuddy.asm.Advice;
import org.agrona.BitUtil;
import org.agrona.DirectBuffer;

/**
 * Interceptor to be applied when verifying buffer alignment accesses.
 */
@SuppressWarnings("unused")
public class BufferAlignmentInterceptor
{
    abstract static class Verifier
    {
        /**
         * Interceptor for type alignment verifier.
         *
         * @param index     into the buffer.
         * @param buffer    the buffer.
         * @param alignment to be verified.
         */
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer, final int alignment)
        {
            final int alignmentOffset = (int)(buffer.addressOffset() + index) % alignment;
            if (0 != alignmentOffset)
            {
                throw new BufferAlignmentException(
                    "Unaligned " + alignment + "-byte access (index=" + index + ", offset=" + alignmentOffset + ")");
            }
        }
    }

    /**
     * Verifier for {@code long} types.
     */
    public static final class LongVerifier extends Verifier
    {
        /**
         * Verify alignment of the {@code long} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_LONG);
        }
    }

    /**
     * Verifier for {@code double} types.
     */
    public static final class DoubleVerifier extends Verifier
    {
        /**
         * Verify alignment of the {@code double} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_DOUBLE);
        }
    }

    /**
     * Verifier for {@code int} types.
     */
    public static final class IntVerifier extends Verifier
    {
        /**
         * Verify alignment of the {@code int} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_INT);
        }
    }

    /**
     * Verifier for {@code float} types.
     */
    public static final class FloatVerifier extends Verifier
    {
        /**
         * Verify alignment of the {@code float} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_FLOAT);
        }
    }

    /**
     * Verifier for {@code short} types.
     */
    public static final class ShortVerifier extends Verifier
    {
        /**
         * Verify alignment of the {@code short} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_SHORT);
        }
    }

    /**
     * Verifier for {@code char} types.
     */
    public static final class CharVerifier extends Verifier
    {
        /**
         * Verify alignment of the {@code char} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_CHAR);
        }
    }
}
