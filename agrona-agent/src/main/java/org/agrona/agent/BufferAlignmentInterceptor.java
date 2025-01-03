/*
 * Copyright 2014-2025 Real Logic Limited.
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
import org.agrona.DirectBuffer;

import static org.agrona.BitUtil.*;

/**
 * Interceptor to be applied when verifying buffer alignment accesses.
 */
@SuppressWarnings("unused")
public final class BufferAlignmentInterceptor
{
    private BufferAlignmentInterceptor()
    {
    }

    /**
     * Verifier for {@code long} types.
     */
    public static final class LongVerifier
    {
        private LongVerifier()
        {
        }

        /**
         * Verify alignment of the {@code long} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            if (0 != ((buffer.addressOffset() + index) & (SIZE_OF_LONG - 1)))
            {
                throw new BufferAlignmentException("Unaligned long access", index, buffer.addressOffset());
            }
        }
    }

    /**
     * Verifier for {@code double} types.
     */
    public static final class DoubleVerifier
    {
        private DoubleVerifier()
        {
        }

        /**
         * Verify alignment of the {@code double} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            if (0 != ((buffer.addressOffset() + index) & (SIZE_OF_DOUBLE - 1)))
            {
                throw new BufferAlignmentException("Unaligned double access", index, buffer.addressOffset());
            }
        }
    }

    /**
     * Verifier for {@code int} types.
     */
    public static final class IntVerifier
    {
        private IntVerifier()
        {
        }

        /**
         * Verify alignment of the {@code int} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            if (0 != ((buffer.addressOffset() + index) & (SIZE_OF_INT - 1)))
            {
                throw new BufferAlignmentException("Unaligned int access", index, buffer.addressOffset());
            }
        }
    }

    /**
     * Verifier for {@code float} types.
     */
    public static final class FloatVerifier
    {
        private FloatVerifier()
        {
        }

        /**
         * Verify alignment of the {@code float} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            if (0 != ((buffer.addressOffset() + index) & (SIZE_OF_FLOAT - 1)))
            {
                throw new BufferAlignmentException("Unaligned float access", index, buffer.addressOffset());
            }
        }
    }

    /**
     * Verifier for {@code short} types.
     */
    public static final class ShortVerifier
    {
        private ShortVerifier()
        {
        }

        /**
         * Verify alignment of the {@code short} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            if (0 != ((buffer.addressOffset() + index) & (SIZE_OF_SHORT - 1)))
            {
                throw new BufferAlignmentException("Unaligned short access", index, buffer.addressOffset());
            }
        }
    }

    /**
     * Verifier for {@code char} types.
     */
    public static final class CharVerifier
    {
        private CharVerifier()
        {
        }

        /**
         * Verify alignment of the {@code char} types.
         *
         * @param index  into the buffer.
         * @param buffer the buffer.
         */
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            if (0 != ((buffer.addressOffset() + index) & (SIZE_OF_CHAR - 1)))
            {
                throw new BufferAlignmentException("Unaligned char access", index, buffer.addressOffset());
            }
        }
    }
}
