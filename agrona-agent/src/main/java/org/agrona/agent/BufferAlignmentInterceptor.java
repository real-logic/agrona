package org.agrona.agent;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;

import net.bytebuddy.asm.Advice;

public class BufferAlignmentInterceptor
{

    abstract static class Verifier
    {
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer, int alignment)
        {
            if ((buffer.addressOffset() + index) % alignment != 0)
            {
                final String message = String.format("Unaligned %d-byte access (Index=%d, Buffer Alignment Offset=%d)",
                        alignment, index, (int) (buffer.addressOffset() % alignment));
                throw new BufferAlignmentException(message);
            }
        }
    }

    public static final class LongVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_LONG);
        }
    }

    public static final class DoubleVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_DOUBLE);
        }
    }

    public static final class IntVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_INT);
        }
    }

    public static final class FloatVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_FLOAT);
        }
    }

    public static final class ShortVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_SHORT);
        }
    }

    public static final class CharVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_CHAR);
        }
    }
}
