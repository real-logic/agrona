/*
 *  Copyright 2014-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona;

/**
 * Grouping of language level utilities to make programming in Java more convenient.
 */
public class LangUtil
{
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static final char[] EMPTY_CHAR_ARRAY = new char[0];

    public static final short[] EMPTY_SHORT_ARRAY = new short[0];

    public static final int[] EMPTY_INT_ARRAY = new int[0];

    public static final float[] EMPTY_FLOAT_ARRAY = new float[0];

    public static final long[] EMPTY_LONG_ARRAY = new long[0];

    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Rethrow an {@link java.lang.Throwable} preserving the stack trace but making it unchecked.
     *
     * @param ex to be rethrown and unchecked.
     */
    public static void rethrowUnchecked(final Throwable ex)
    {
        LangUtil.<RuntimeException>rethrow(ex);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow(final Throwable t) throws T
    {
        throw (T)t;
    }
}
