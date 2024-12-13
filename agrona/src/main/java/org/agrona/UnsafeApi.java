/*
 * Copyright 2014-2024 Real Logic Limited.
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

/**
 * Entry point for accessing {@code jdk.internal.misc.Unsafe} APIs.
 */
public final class UnsafeApi
{
    private UnsafeApi()
    {
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#addressSize}.
     * @return value
     */
    public static int addressSize()
    {
        throw new UnsupportedOperationException("'addressSize' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#allocateInstance(java.lang.Class)}.
     * @param arg0 arg0
     * @return value
     */
    public static Object allocateInstance(
        final Class<?> arg0)
    {
        throw new UnsupportedOperationException("'allocateInstance' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#allocateMemory(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static long allocateMemory(
        final long arg0)
    {
        throw new UnsupportedOperationException("'allocateMemory' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#allocateUninitializedArray(java.lang.Class, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static Object allocateUninitializedArray(
        final Class<?> arg0,
        final int arg1)
    {
        throw new UnsupportedOperationException("'allocateUninitializedArray' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#arrayBaseOffset(java.lang.Class)}.
     * @param arg0 arg0
     * @return value
     */
    public static int arrayBaseOffset(
        final Class<?> arg0)
    {
        throw new UnsupportedOperationException("'arrayBaseOffset' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#arrayIndexScale(java.lang.Class)}.
     * @param arg0 arg0
     * @return value
     */
    public static int arrayIndexScale(
        final Class<?> arg0)
    {
        throw new UnsupportedOperationException("'arrayIndexScale' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeBoolean(java.lang.Object, long, boolean, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndExchangeBoolean(
        final Object arg0,
        final long arg1,
        final boolean arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeBoolean' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeBooleanAcquire(java.lang.Object, long, boolean, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndExchangeBooleanAcquire(
        final Object arg0,
        final long arg1,
        final boolean arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeBooleanAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeBooleanRelease(java.lang.Object, long, boolean, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndExchangeBooleanRelease(
        final Object arg0,
        final long arg1,
        final boolean arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeBooleanRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeByte(java.lang.Object, long, byte, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static byte compareAndExchangeByte(
        final Object arg0,
        final long arg1,
        final byte arg2,
        final byte arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeByteAcquire(java.lang.Object, long, byte, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static byte compareAndExchangeByteAcquire(
        final Object arg0,
        final long arg1,
        final byte arg2,
        final byte arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeByteAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeByteRelease(java.lang.Object, long, byte, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static byte compareAndExchangeByteRelease(
        final Object arg0,
        final long arg1,
        final byte arg2,
        final byte arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeByteRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeChar(java.lang.Object, long, char, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static char compareAndExchangeChar(
        final Object arg0,
        final long arg1,
        final char arg2,
        final char arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeCharAcquire(java.lang.Object, long, char, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static char compareAndExchangeCharAcquire(
        final Object arg0,
        final long arg1,
        final char arg2,
        final char arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeCharAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeCharRelease(java.lang.Object, long, char, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static char compareAndExchangeCharRelease(
        final Object arg0,
        final long arg1,
        final char arg2,
        final char arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeCharRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeDouble(java.lang.Object, long, double, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static double compareAndExchangeDouble(
        final Object arg0,
        final long arg1,
        final double arg2,
        final double arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeDouble' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeDoubleAcquire(java.lang.Object, long, double, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static double compareAndExchangeDoubleAcquire(
        final Object arg0,
        final long arg1,
        final double arg2,
        final double arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeDoubleAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeDoubleRelease(java.lang.Object, long, double, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static double compareAndExchangeDoubleRelease(
        final Object arg0,
        final long arg1,
        final double arg2,
        final double arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeDoubleRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeFloat(java.lang.Object, long, float, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static float compareAndExchangeFloat(
        final Object arg0,
        final long arg1,
        final float arg2,
        final float arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeFloat' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeFloatAcquire(java.lang.Object, long, float, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static float compareAndExchangeFloatAcquire(
        final Object arg0,
        final long arg1,
        final float arg2,
        final float arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeFloatAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeFloatRelease(java.lang.Object, long, float, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static float compareAndExchangeFloatRelease(
        final Object arg0,
        final long arg1,
        final float arg2,
        final float arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeFloatRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeInt(java.lang.Object, long, int, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static int compareAndExchangeInt(
        final Object arg0,
        final long arg1,
        final int arg2,
        final int arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeIntAcquire(java.lang.Object, long, int, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static int compareAndExchangeIntAcquire(
        final Object arg0,
        final long arg1,
        final int arg2,
        final int arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeIntAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeIntRelease(java.lang.Object, long, int, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static int compareAndExchangeIntRelease(
        final Object arg0,
        final long arg1,
        final int arg2,
        final int arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeIntRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeLong(java.lang.Object, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static long compareAndExchangeLong(
        final Object arg0,
        final long arg1,
        final long arg2,
        final long arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeLongAcquire(java.lang.Object, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static long compareAndExchangeLongAcquire(
        final Object arg0,
        final long arg1,
        final long arg2,
        final long arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeLongAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeLongRelease(java.lang.Object, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static long compareAndExchangeLongRelease(
        final Object arg0,
        final long arg1,
        final long arg2,
        final long arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeLongRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeReference(java.lang.Object, long, java.lang.Object, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static Object compareAndExchangeReference(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final Object arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeReference' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeReferenceAcquire(java.lang.Object, long, java.lang.Object, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static Object compareAndExchangeReferenceAcquire(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final Object arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeReferenceAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeReferenceRelease(java.lang.Object, long, java.lang.Object, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static Object compareAndExchangeReferenceRelease(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final Object arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeReferenceRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeShort(java.lang.Object, long, short, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static short compareAndExchangeShort(
        final Object arg0,
        final long arg1,
        final short arg2,
        final short arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeShortAcquire(java.lang.Object, long, short, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static short compareAndExchangeShortAcquire(
        final Object arg0,
        final long arg1,
        final short arg2,
        final short arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeShortAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndExchangeShortRelease(java.lang.Object, long, short, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static short compareAndExchangeShortRelease(
        final Object arg0,
        final long arg1,
        final short arg2,
        final short arg3)
    {
        throw new UnsupportedOperationException("'compareAndExchangeShortRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndSetBoolean(java.lang.Object, long, boolean, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndSetBoolean(
        final Object arg0,
        final long arg1,
        final boolean arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'compareAndSetBoolean' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndSetByte(java.lang.Object, long, byte, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndSetByte(
        final Object arg0,
        final long arg1,
        final byte arg2,
        final byte arg3)
    {
        throw new UnsupportedOperationException("'compareAndSetByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndSetChar(java.lang.Object, long, char, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndSetChar(
        final Object arg0,
        final long arg1,
        final char arg2,
        final char arg3)
    {
        throw new UnsupportedOperationException("'compareAndSetChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndSetDouble(java.lang.Object, long, double, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndSetDouble(
        final Object arg0,
        final long arg1,
        final double arg2,
        final double arg3)
    {
        throw new UnsupportedOperationException("'compareAndSetDouble' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndSetFloat(java.lang.Object, long, float, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndSetFloat(
        final Object arg0,
        final long arg1,
        final float arg2,
        final float arg3)
    {
        throw new UnsupportedOperationException("'compareAndSetFloat' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndSetInt(java.lang.Object, long, int, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndSetInt(
        final Object arg0,
        final long arg1,
        final int arg2,
        final int arg3)
    {
        throw new UnsupportedOperationException("'compareAndSetInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndSetLong(java.lang.Object, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndSetLong(
        final Object arg0,
        final long arg1,
        final long arg2,
        final long arg3)
    {
        throw new UnsupportedOperationException("'compareAndSetLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndSetReference(java.lang.Object, long, java.lang.Object, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndSetReference(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final Object arg3)
    {
        throw new UnsupportedOperationException("'compareAndSetReference' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#compareAndSetShort(java.lang.Object, long, short, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean compareAndSetShort(
        final Object arg0,
        final long arg1,
        final short arg2,
        final short arg3)
    {
        throw new UnsupportedOperationException("'compareAndSetShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#copyMemory(long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void copyMemory(
        final long arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'copyMemory' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#copyMemory(java.lang.Object, long, java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @param arg4 arg4
     */
    public static void copyMemory(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final long arg3,
        final long arg4)
    {
        throw new UnsupportedOperationException("'copyMemory' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#copySwapMemory(long, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     */
    public static void copySwapMemory(
        final long arg0,
        final long arg1,
        final long arg2,
        final long arg3)
    {
        throw new UnsupportedOperationException("'copySwapMemory' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#copySwapMemory(java.lang.Object, long, java.lang.Object, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @param arg4 arg4
     * @param arg5 arg5
     */
    public static void copySwapMemory(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final long arg3,
        final long arg4,
        final long arg5)
    {
        throw new UnsupportedOperationException("'copySwapMemory' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#dataCacheLineAlignDown(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static long dataCacheLineAlignDown(
        final long arg0)
    {
        throw new UnsupportedOperationException("'dataCacheLineAlignDown' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#dataCacheLineFlushSize}.
     * @return value
     */
    public static int dataCacheLineFlushSize()
    {
        throw new UnsupportedOperationException("'dataCacheLineFlushSize' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#defineClass(java.lang.String, byte[], int, int, java.lang.ClassLoader, java.security.ProtectionDomain)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @param arg4 arg4
     * @param arg5 arg5
     * @return value
     */
    public static Class<?> defineClass(
        final String arg0,
        final byte[] arg1,
        final int arg2,
        final int arg3,
        final ClassLoader arg4,
        final java.security.ProtectionDomain arg5)
    {
        throw new UnsupportedOperationException("'defineClass' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#ensureClassInitialized(java.lang.Class)}.
     * @param arg0 arg0
     */
    public static void ensureClassInitialized(
        final Class<?> arg0)
    {
        throw new UnsupportedOperationException("'ensureClassInitialized' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#freeMemory(long)}.
     * @param arg0 arg0
     */
    public static void freeMemory(
        final long arg0)
    {
        throw new UnsupportedOperationException("'freeMemory' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#fullFence}.
     */
    public static void fullFence()
    {
        throw new UnsupportedOperationException("'fullFence' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAddress(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static long getAddress(
        final long arg0)
    {
        throw new UnsupportedOperationException("'getAddress' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAddress(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static long getAddress(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getAddress' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddByte(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndAddByte(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndAddByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddByteAcquire(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndAddByteAcquire(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndAddByteAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddByteRelease(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndAddByteRelease(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndAddByteRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddChar(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndAddChar(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndAddChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddCharAcquire(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndAddCharAcquire(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndAddCharAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddCharRelease(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndAddCharRelease(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndAddCharRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddDouble(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static double getAndAddDouble(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'getAndAddDouble' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddDoubleAcquire(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static double getAndAddDoubleAcquire(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'getAndAddDoubleAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddDoubleRelease(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static double getAndAddDoubleRelease(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'getAndAddDoubleRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddFloat(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static float getAndAddFloat(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'getAndAddFloat' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddFloatAcquire(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static float getAndAddFloatAcquire(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'getAndAddFloatAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddFloatRelease(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static float getAndAddFloatRelease(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'getAndAddFloatRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddInt(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndAddInt(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndAddInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddIntAcquire(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndAddIntAcquire(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndAddIntAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddIntRelease(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndAddIntRelease(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndAddIntRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddLong(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndAddLong(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndAddLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddLongAcquire(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndAddLongAcquire(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndAddLongAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddLongRelease(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndAddLongRelease(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndAddLongRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddShort(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndAddShort(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndAddShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddShortAcquire(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndAddShortAcquire(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndAddShortAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndAddShortRelease(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndAddShortRelease(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndAddShortRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndBoolean(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndBitwiseAndBoolean(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndBoolean' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndBooleanAcquire(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndBitwiseAndBooleanAcquire(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndBooleanAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndBooleanRelease(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndBitwiseAndBooleanRelease(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndBooleanRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndByte(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndBitwiseAndByte(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndByteAcquire(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndBitwiseAndByteAcquire(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndByteAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndByteRelease(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndBitwiseAndByteRelease(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndByteRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndChar(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndBitwiseAndChar(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndCharAcquire(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndBitwiseAndCharAcquire(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndCharAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndCharRelease(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndBitwiseAndCharRelease(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndCharRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndInt(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndBitwiseAndInt(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndIntAcquire(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndBitwiseAndIntAcquire(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndIntAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndIntRelease(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndBitwiseAndIntRelease(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndIntRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndLong(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndBitwiseAndLong(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndLongAcquire(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndBitwiseAndLongAcquire(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndLongAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndLongRelease(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndBitwiseAndLongRelease(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndLongRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndShort(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndBitwiseAndShort(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndShortAcquire(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndBitwiseAndShortAcquire(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndShortAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseAndShortRelease(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndBitwiseAndShortRelease(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseAndShortRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrBoolean(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndBitwiseOrBoolean(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrBoolean' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrBooleanAcquire(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndBitwiseOrBooleanAcquire(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrBooleanAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrBooleanRelease(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndBitwiseOrBooleanRelease(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrBooleanRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrByte(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndBitwiseOrByte(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrByteAcquire(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndBitwiseOrByteAcquire(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrByteAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrByteRelease(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndBitwiseOrByteRelease(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrByteRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrChar(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndBitwiseOrChar(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrCharAcquire(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndBitwiseOrCharAcquire(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrCharAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrCharRelease(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndBitwiseOrCharRelease(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrCharRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrInt(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndBitwiseOrInt(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrIntAcquire(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndBitwiseOrIntAcquire(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrIntAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrIntRelease(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndBitwiseOrIntRelease(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrIntRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrLong(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndBitwiseOrLong(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrLongAcquire(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndBitwiseOrLongAcquire(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrLongAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrLongRelease(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndBitwiseOrLongRelease(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrLongRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrShort(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndBitwiseOrShort(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrShortAcquire(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndBitwiseOrShortAcquire(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrShortAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseOrShortRelease(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndBitwiseOrShortRelease(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseOrShortRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorBoolean(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndBitwiseXorBoolean(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorBoolean' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorBooleanAcquire(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndBitwiseXorBooleanAcquire(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorBooleanAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorBooleanRelease(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndBitwiseXorBooleanRelease(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorBooleanRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorByte(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndBitwiseXorByte(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorByteAcquire(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndBitwiseXorByteAcquire(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorByteAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorByteRelease(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndBitwiseXorByteRelease(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorByteRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorChar(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndBitwiseXorChar(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorCharAcquire(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndBitwiseXorCharAcquire(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorCharAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorCharRelease(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndBitwiseXorCharRelease(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorCharRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorInt(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndBitwiseXorInt(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorIntAcquire(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndBitwiseXorIntAcquire(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorIntAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorIntRelease(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndBitwiseXorIntRelease(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorIntRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorLong(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndBitwiseXorLong(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorLongAcquire(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndBitwiseXorLongAcquire(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorLongAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorLongRelease(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndBitwiseXorLongRelease(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorLongRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorShort(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndBitwiseXorShort(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorShortAcquire(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndBitwiseXorShortAcquire(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorShortAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndBitwiseXorShortRelease(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndBitwiseXorShortRelease(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndBitwiseXorShortRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetBoolean(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndSetBoolean(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndSetBoolean' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetBooleanAcquire(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndSetBooleanAcquire(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndSetBooleanAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetBooleanRelease(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static boolean getAndSetBooleanRelease(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getAndSetBooleanRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetByte(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndSetByte(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndSetByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetByteAcquire(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndSetByteAcquire(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndSetByteAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetByteRelease(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static byte getAndSetByteRelease(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'getAndSetByteRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetChar(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndSetChar(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndSetChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetCharAcquire(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndSetCharAcquire(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndSetCharAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetCharRelease(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getAndSetCharRelease(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'getAndSetCharRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetDouble(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static double getAndSetDouble(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'getAndSetDouble' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetDoubleAcquire(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static double getAndSetDoubleAcquire(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'getAndSetDoubleAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetDoubleRelease(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static double getAndSetDoubleRelease(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'getAndSetDoubleRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetFloat(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static float getAndSetFloat(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'getAndSetFloat' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetFloatAcquire(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static float getAndSetFloatAcquire(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'getAndSetFloatAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetFloatRelease(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static float getAndSetFloatRelease(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'getAndSetFloatRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetInt(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndSetInt(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndSetInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetIntAcquire(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndSetIntAcquire(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndSetIntAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetIntRelease(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getAndSetIntRelease(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'getAndSetIntRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetLong(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndSetLong(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndSetLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetLongAcquire(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndSetLongAcquire(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndSetLongAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetLongRelease(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getAndSetLongRelease(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'getAndSetLongRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetReference(java.lang.Object, long, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static Object getAndSetReference(
        final Object arg0,
        final long arg1,
        final Object arg2)
    {
        throw new UnsupportedOperationException("'getAndSetReference' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetReferenceAcquire(java.lang.Object, long, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static Object getAndSetReferenceAcquire(
        final Object arg0,
        final long arg1,
        final Object arg2)
    {
        throw new UnsupportedOperationException("'getAndSetReferenceAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetReferenceRelease(java.lang.Object, long, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static Object getAndSetReferenceRelease(
        final Object arg0,
        final long arg1,
        final Object arg2)
    {
        throw new UnsupportedOperationException("'getAndSetReferenceRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetShort(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndSetShort(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndSetShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetShortAcquire(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndSetShortAcquire(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndSetShortAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getAndSetShortRelease(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getAndSetShortRelease(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'getAndSetShortRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getBoolean(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static boolean getBoolean(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getBoolean' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getBooleanAcquire(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static boolean getBooleanAcquire(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getBooleanAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getBooleanOpaque(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static boolean getBooleanOpaque(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getBooleanOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getBooleanVolatile(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static boolean getBooleanVolatile(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getBooleanVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getByte(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static byte getByte(
        final long arg0)
    {
        throw new UnsupportedOperationException("'getByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getByte(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static byte getByte(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getByteAcquire(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static byte getByteAcquire(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getByteAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getByteOpaque(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static byte getByteOpaque(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getByteOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getByteVolatile(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static byte getByteVolatile(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getByteVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getChar(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static char getChar(
        final long arg0)
    {
        throw new UnsupportedOperationException("'getChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getChar(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static char getChar(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getCharAcquire(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static char getCharAcquire(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getCharAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getCharOpaque(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static char getCharOpaque(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getCharOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getCharUnaligned(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static char getCharUnaligned(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getCharUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getCharUnaligned(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static char getCharUnaligned(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getCharUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getCharVolatile(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static char getCharVolatile(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getCharVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getDouble(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static double getDouble(
        final long arg0)
    {
        throw new UnsupportedOperationException("'getDouble' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getDouble(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static double getDouble(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getDouble' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getDoubleAcquire(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static double getDoubleAcquire(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getDoubleAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getDoubleOpaque(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static double getDoubleOpaque(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getDoubleOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getDoubleVolatile(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static double getDoubleVolatile(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getDoubleVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getFloat(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static float getFloat(
        final long arg0)
    {
        throw new UnsupportedOperationException("'getFloat' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getFloat(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static float getFloat(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getFloat' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getFloatAcquire(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static float getFloatAcquire(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getFloatAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getFloatOpaque(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static float getFloatOpaque(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getFloatOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getFloatVolatile(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static float getFloatVolatile(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getFloatVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getInt(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static int getInt(
        final long arg0)
    {
        throw new UnsupportedOperationException("'getInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getInt(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static int getInt(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getIntAcquire(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static int getIntAcquire(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getIntAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getIntOpaque(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static int getIntOpaque(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getIntOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getIntUnaligned(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static int getIntUnaligned(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getIntUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getIntUnaligned(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static int getIntUnaligned(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getIntUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getIntVolatile(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static int getIntVolatile(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getIntVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getLoadAverage(double[], int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static int getLoadAverage(
        final double[] arg0,
        final int arg1)
    {
        throw new UnsupportedOperationException("'getLoadAverage' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getLong(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static long getLong(
        final long arg0)
    {
        throw new UnsupportedOperationException("'getLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getLong(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static long getLong(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getLongAcquire(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static long getLongAcquire(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getLongAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getLongOpaque(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static long getLongOpaque(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getLongOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getLongUnaligned(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static long getLongUnaligned(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getLongUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getLongUnaligned(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static long getLongUnaligned(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getLongUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getLongVolatile(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static long getLongVolatile(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getLongVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getReference(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static Object getReference(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getReference' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getReferenceAcquire(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static Object getReferenceAcquire(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getReferenceAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getReferenceOpaque(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static Object getReferenceOpaque(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getReferenceOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getReferenceVolatile(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static Object getReferenceVolatile(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getReferenceVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getShort(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static short getShort(
        final long arg0)
    {
        throw new UnsupportedOperationException("'getShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getShort(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static short getShort(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getShortAcquire(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static short getShortAcquire(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getShortAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getShortOpaque(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static short getShortOpaque(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getShortOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getShortUnaligned(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static short getShortUnaligned(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getShortUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getShortUnaligned(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @return value
     */
    public static short getShortUnaligned(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'getShortUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getShortVolatile(java.lang.Object, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static short getShortVolatile(
        final Object arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'getShortVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#getUncompressedObject(long)}.
     * @param arg0 arg0
     * @return value
     */
    public static Object getUncompressedObject(
        final long arg0)
    {
        throw new UnsupportedOperationException("'getUncompressedObject' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#invokeCleaner(java.nio.ByteBuffer)}.
     * @param arg0 arg0
     */
    public static void invokeCleaner(
        final java.nio.ByteBuffer arg0)
    {
        throw new UnsupportedOperationException("'invokeCleaner' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#isBigEndian}.
     * @return value
     */
    public static boolean isBigEndian()
    {
        throw new UnsupportedOperationException("'isBigEndian' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#isWritebackEnabled}.
     * @return value
     */
    public static boolean isWritebackEnabled()
    {
        throw new UnsupportedOperationException("'isWritebackEnabled' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#loadFence}.
     */
    public static void loadFence()
    {
        throw new UnsupportedOperationException("'loadFence' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#loadLoadFence}.
     */
    public static void loadLoadFence()
    {
        throw new UnsupportedOperationException("'loadLoadFence' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#objectFieldOffset(java.lang.reflect.Field)}.
     * @param arg0 arg0
     * @return value
     */
    public static long objectFieldOffset(
        final java.lang.reflect.Field arg0)
    {
        throw new UnsupportedOperationException("'objectFieldOffset' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#objectFieldOffset(java.lang.Class, java.lang.String)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static long objectFieldOffset(
        final Class<?> arg0,
        final String arg1)
    {
        throw new UnsupportedOperationException("'objectFieldOffset' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#pageSize}.
     * @return value
     */
    public static int pageSize()
    {
        throw new UnsupportedOperationException("'pageSize' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#park(boolean, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void park(
        final boolean arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'park' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putAddress(long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void putAddress(
        final long arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'putAddress' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putAddress(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putAddress(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'putAddress' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putBoolean(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putBoolean(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'putBoolean' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putBooleanOpaque(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putBooleanOpaque(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'putBooleanOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putBooleanRelease(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putBooleanRelease(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'putBooleanRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putBooleanVolatile(java.lang.Object, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putBooleanVolatile(
        final Object arg0,
        final long arg1,
        final boolean arg2)
    {
        throw new UnsupportedOperationException("'putBooleanVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putByte(long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void putByte(
        final long arg0,
        final byte arg1)
    {
        throw new UnsupportedOperationException("'putByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putByte(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putByte(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'putByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putByteOpaque(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putByteOpaque(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'putByteOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putByteRelease(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putByteRelease(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'putByteRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putByteVolatile(java.lang.Object, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putByteVolatile(
        final Object arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'putByteVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putChar(long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void putChar(
        final long arg0,
        final char arg1)
    {
        throw new UnsupportedOperationException("'putChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putChar(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putChar(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'putChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putCharOpaque(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putCharOpaque(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'putCharOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putCharRelease(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putCharRelease(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'putCharRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putCharUnaligned(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putCharUnaligned(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'putCharUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putCharUnaligned(java.lang.Object, long, char, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     */
    public static void putCharUnaligned(
        final Object arg0,
        final long arg1,
        final char arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'putCharUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putCharVolatile(java.lang.Object, long, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putCharVolatile(
        final Object arg0,
        final long arg1,
        final char arg2)
    {
        throw new UnsupportedOperationException("'putCharVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putDouble(long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void putDouble(
        final long arg0,
        final double arg1)
    {
        throw new UnsupportedOperationException("'putDouble' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putDouble(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putDouble(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'putDouble' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putDoubleOpaque(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putDoubleOpaque(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'putDoubleOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putDoubleRelease(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putDoubleRelease(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'putDoubleRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putDoubleVolatile(java.lang.Object, long, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putDoubleVolatile(
        final Object arg0,
        final long arg1,
        final double arg2)
    {
        throw new UnsupportedOperationException("'putDoubleVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putFloat(long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void putFloat(
        final long arg0,
        final float arg1)
    {
        throw new UnsupportedOperationException("'putFloat' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putFloat(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putFloat(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'putFloat' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putFloatOpaque(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putFloatOpaque(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'putFloatOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putFloatRelease(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putFloatRelease(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'putFloatRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putFloatVolatile(java.lang.Object, long, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putFloatVolatile(
        final Object arg0,
        final long arg1,
        final float arg2)
    {
        throw new UnsupportedOperationException("'putFloatVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putInt(long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void putInt(
        final long arg0,
        final int arg1)
    {
        throw new UnsupportedOperationException("'putInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putInt(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putInt(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'putInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putIntOpaque(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putIntOpaque(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'putIntOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putIntRelease(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putIntRelease(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'putIntRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putIntUnaligned(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putIntUnaligned(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'putIntUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putIntUnaligned(java.lang.Object, long, int, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     */
    public static void putIntUnaligned(
        final Object arg0,
        final long arg1,
        final int arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'putIntUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putIntVolatile(java.lang.Object, long, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putIntVolatile(
        final Object arg0,
        final long arg1,
        final int arg2)
    {
        throw new UnsupportedOperationException("'putIntVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putLong(long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void putLong(
        final long arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'putLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putLong(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putLong(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'putLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putLongOpaque(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putLongOpaque(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'putLongOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putLongRelease(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putLongRelease(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'putLongRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putLongUnaligned(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putLongUnaligned(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'putLongUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putLongUnaligned(java.lang.Object, long, long, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     */
    public static void putLongUnaligned(
        final Object arg0,
        final long arg1,
        final long arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'putLongUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putLongVolatile(java.lang.Object, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putLongVolatile(
        final Object arg0,
        final long arg1,
        final long arg2)
    {
        throw new UnsupportedOperationException("'putLongVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putReference(java.lang.Object, long, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putReference(
        final Object arg0,
        final long arg1,
        final Object arg2)
    {
        throw new UnsupportedOperationException("'putReference' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putReferenceOpaque(java.lang.Object, long, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putReferenceOpaque(
        final Object arg0,
        final long arg1,
        final Object arg2)
    {
        throw new UnsupportedOperationException("'putReferenceOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putReferenceRelease(java.lang.Object, long, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putReferenceRelease(
        final Object arg0,
        final long arg1,
        final Object arg2)
    {
        throw new UnsupportedOperationException("'putReferenceRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putReferenceVolatile(java.lang.Object, long, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putReferenceVolatile(
        final Object arg0,
        final long arg1,
        final Object arg2)
    {
        throw new UnsupportedOperationException("'putReferenceVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putShort(long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void putShort(
        final long arg0,
        final short arg1)
    {
        throw new UnsupportedOperationException("'putShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putShort(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putShort(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'putShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putShortOpaque(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putShortOpaque(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'putShortOpaque' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putShortRelease(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putShortRelease(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'putShortRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putShortUnaligned(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putShortUnaligned(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'putShortUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putShortUnaligned(java.lang.Object, long, short, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     */
    public static void putShortUnaligned(
        final Object arg0,
        final long arg1,
        final short arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'putShortUnaligned' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#putShortVolatile(java.lang.Object, long, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void putShortVolatile(
        final Object arg0,
        final long arg1,
        final short arg2)
    {
        throw new UnsupportedOperationException("'putShortVolatile' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#reallocateMemory(long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @return value
     */
    public static long reallocateMemory(
        final long arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'reallocateMemory' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#setMemory(long, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     */
    public static void setMemory(
        final long arg0,
        final long arg1,
        final byte arg2)
    {
        throw new UnsupportedOperationException("'setMemory' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#setMemory(java.lang.Object, long, long, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     */
    public static void setMemory(
        final Object arg0,
        final long arg1,
        final long arg2,
        final byte arg3)
    {
        throw new UnsupportedOperationException("'setMemory' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#shouldBeInitialized(java.lang.Class)}.
     * @param arg0 arg0
     * @return value
     */
    public static boolean shouldBeInitialized(
        final Class<?> arg0)
    {
        throw new UnsupportedOperationException("'shouldBeInitialized' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#staticFieldBase(java.lang.reflect.Field)}.
     * @param arg0 arg0
     * @return value
     */
    public static Object staticFieldBase(
        final java.lang.reflect.Field arg0)
    {
        throw new UnsupportedOperationException("'staticFieldBase' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#staticFieldOffset(java.lang.reflect.Field)}.
     * @param arg0 arg0
     * @return value
     */
    public static long staticFieldOffset(
        final java.lang.reflect.Field arg0)
    {
        throw new UnsupportedOperationException("'staticFieldOffset' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#storeFence}.
     */
    public static void storeFence()
    {
        throw new UnsupportedOperationException("'storeFence' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#storeStoreFence}.
     */
    public static void storeStoreFence()
    {
        throw new UnsupportedOperationException("'storeStoreFence' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#throwException(java.lang.Throwable)}.
     * @param arg0 arg0
     */
    public static void throwException(
        final Throwable arg0)
    {
        throw new UnsupportedOperationException("'throwException' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#unalignedAccess}.
     * @return value
     */
    public static boolean unalignedAccess()
    {
        throw new UnsupportedOperationException("'unalignedAccess' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#unpark(java.lang.Object)}.
     * @param arg0 arg0
     */
    public static void unpark(
        final Object arg0)
    {
        throw new UnsupportedOperationException("'unpark' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetBoolean(java.lang.Object, long, boolean, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetBoolean(
        final Object arg0,
        final long arg1,
        final boolean arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetBoolean' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetBooleanAcquire(java.lang.Object, long, boolean, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetBooleanAcquire(
        final Object arg0,
        final long arg1,
        final boolean arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetBooleanAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetBooleanPlain(java.lang.Object, long, boolean, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetBooleanPlain(
        final Object arg0,
        final long arg1,
        final boolean arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetBooleanPlain' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetBooleanRelease(java.lang.Object, long, boolean, boolean)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetBooleanRelease(
        final Object arg0,
        final long arg1,
        final boolean arg2,
        final boolean arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetBooleanRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetByte(java.lang.Object, long, byte, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetByte(
        final Object arg0,
        final long arg1,
        final byte arg2,
        final byte arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetByte' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetByteAcquire(java.lang.Object, long, byte, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetByteAcquire(
        final Object arg0,
        final long arg1,
        final byte arg2,
        final byte arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetByteAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetBytePlain(java.lang.Object, long, byte, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetBytePlain(
        final Object arg0,
        final long arg1,
        final byte arg2,
        final byte arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetBytePlain' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetByteRelease(java.lang.Object, long, byte, byte)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetByteRelease(
        final Object arg0,
        final long arg1,
        final byte arg2,
        final byte arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetByteRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetChar(java.lang.Object, long, char, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetChar(
        final Object arg0,
        final long arg1,
        final char arg2,
        final char arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetChar' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetCharAcquire(java.lang.Object, long, char, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetCharAcquire(
        final Object arg0,
        final long arg1,
        final char arg2,
        final char arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetCharAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetCharPlain(java.lang.Object, long, char, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetCharPlain(
        final Object arg0,
        final long arg1,
        final char arg2,
        final char arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetCharPlain' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetCharRelease(java.lang.Object, long, char, char)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetCharRelease(
        final Object arg0,
        final long arg1,
        final char arg2,
        final char arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetCharRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetDouble(java.lang.Object, long, double, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetDouble(
        final Object arg0,
        final long arg1,
        final double arg2,
        final double arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetDouble' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetDoubleAcquire(java.lang.Object, long, double, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetDoubleAcquire(
        final Object arg0,
        final long arg1,
        final double arg2,
        final double arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetDoubleAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetDoublePlain(java.lang.Object, long, double, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetDoublePlain(
        final Object arg0,
        final long arg1,
        final double arg2,
        final double arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetDoublePlain' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetDoubleRelease(java.lang.Object, long, double, double)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetDoubleRelease(
        final Object arg0,
        final long arg1,
        final double arg2,
        final double arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetDoubleRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetFloat(java.lang.Object, long, float, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetFloat(
        final Object arg0,
        final long arg1,
        final float arg2,
        final float arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetFloat' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetFloatAcquire(java.lang.Object, long, float, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetFloatAcquire(
        final Object arg0,
        final long arg1,
        final float arg2,
        final float arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetFloatAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetFloatPlain(java.lang.Object, long, float, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetFloatPlain(
        final Object arg0,
        final long arg1,
        final float arg2,
        final float arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetFloatPlain' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetFloatRelease(java.lang.Object, long, float, float)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetFloatRelease(
        final Object arg0,
        final long arg1,
        final float arg2,
        final float arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetFloatRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetInt(java.lang.Object, long, int, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetInt(
        final Object arg0,
        final long arg1,
        final int arg2,
        final int arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetInt' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetIntAcquire(java.lang.Object, long, int, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetIntAcquire(
        final Object arg0,
        final long arg1,
        final int arg2,
        final int arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetIntAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetIntPlain(java.lang.Object, long, int, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetIntPlain(
        final Object arg0,
        final long arg1,
        final int arg2,
        final int arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetIntPlain' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetIntRelease(java.lang.Object, long, int, int)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetIntRelease(
        final Object arg0,
        final long arg1,
        final int arg2,
        final int arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetIntRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetLong(java.lang.Object, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetLong(
        final Object arg0,
        final long arg1,
        final long arg2,
        final long arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetLong' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetLongAcquire(java.lang.Object, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetLongAcquire(
        final Object arg0,
        final long arg1,
        final long arg2,
        final long arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetLongAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetLongPlain(java.lang.Object, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetLongPlain(
        final Object arg0,
        final long arg1,
        final long arg2,
        final long arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetLongPlain' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetLongRelease(java.lang.Object, long, long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetLongRelease(
        final Object arg0,
        final long arg1,
        final long arg2,
        final long arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetLongRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetReference(java.lang.Object, long, java.lang.Object, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetReference(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final Object arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetReference' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetReferenceAcquire(java.lang.Object, long, java.lang.Object, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetReferenceAcquire(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final Object arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetReferenceAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetReferencePlain(java.lang.Object, long, java.lang.Object, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetReferencePlain(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final Object arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetReferencePlain' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetReferenceRelease(java.lang.Object, long, java.lang.Object, java.lang.Object)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetReferenceRelease(
        final Object arg0,
        final long arg1,
        final Object arg2,
        final Object arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetReferenceRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetShort(java.lang.Object, long, short, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetShort(
        final Object arg0,
        final long arg1,
        final short arg2,
        final short arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetShort' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetShortAcquire(java.lang.Object, long, short, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetShortAcquire(
        final Object arg0,
        final long arg1,
        final short arg2,
        final short arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetShortAcquire' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetShortPlain(java.lang.Object, long, short, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetShortPlain(
        final Object arg0,
        final long arg1,
        final short arg2,
        final short arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetShortPlain' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#weakCompareAndSetShortRelease(java.lang.Object, long, short, short)}.
     * @param arg0 arg0
     * @param arg1 arg1
     * @param arg2 arg2
     * @param arg3 arg3
     * @return value
     */
    public static boolean weakCompareAndSetShortRelease(
        final Object arg0,
        final long arg1,
        final short arg2,
        final short arg3)
    {
        throw new UnsupportedOperationException("'weakCompareAndSetShortRelease' not implemented");
    }

    /**
     * See {@code jdk.internal.misc.Unsafe#writebackMemory(long, long)}.
     * @param arg0 arg0
     * @param arg1 arg1
     */
    public static void writebackMemory(
        final long arg0,
        final long arg1)
    {
        throw new UnsupportedOperationException("'writebackMemory' not implemented");
    }

}

