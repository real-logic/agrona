/*
 * Copyright 2014-2023 Real Logic Limited.
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
package org.agrona.concurrent;

import org.agrona.LangUtil;
import org.agrona.UnsafeAccess;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;

/**
 * Memory access operations which encapsulate the use of Unsafe.
 */
public final class MemoryAccess
{
    private static final MethodHandle ACQUIRE_FENCE;
    private static final MethodHandle RELEASE_FENCE;
    private static final MethodHandle FULL_FENCE;

    static
    {
        MethodHandle acquireFence = null;
        MethodHandle releaseFence = null;
        MethodHandle fullFence = null;
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final MethodType voidMethod = MethodType.methodType(void.class);
        try
        {
            final Class<?> versionClass = Class.forName("java.lang.Runtime$Version"); // since JDK 9
            final Object version = Runtime.class.getMethod("version").invoke(Runtime.getRuntime());
            final int majorRelease = (int)versionClass.getMethod("feature").invoke(version);
            if (majorRelease > 21)
            {
                final Class<?> varhandleClass = Class.forName("java.lang.invoke.VarHandle");
                acquireFence = lookup.findStatic(varhandleClass, "acquireFence", voidMethod);
                releaseFence = lookup.findStatic(varhandleClass, "releaseFence", voidMethod);
                fullFence = lookup.findStatic(varhandleClass, "fullFence", voidMethod);
            }
        }
        catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                     InvocationTargetException ignored)
        {
        }

        if (null == acquireFence)
        {
            try
            {
                final Class<? extends Unsafe> unsafeClass = UnsafeAccess.UNSAFE.getClass();
                acquireFence = lookup.findVirtual(unsafeClass, "loadFence", voidMethod).bindTo(UnsafeAccess.UNSAFE);
                releaseFence = lookup.findVirtual(unsafeClass, "storeFence", voidMethod).bindTo(UnsafeAccess.UNSAFE);
                fullFence = lookup.findVirtual(unsafeClass, "fullFence", voidMethod).bindTo(UnsafeAccess.UNSAFE);
            }
            catch (final NoSuchMethodException | IllegalAccessException e)
            {
                LangUtil.rethrowUnchecked(e);
            }
        }
        ACQUIRE_FENCE = acquireFence;
        RELEASE_FENCE = releaseFence;
        FULL_FENCE = fullFence;
    }

    private MemoryAccess()
    {
    }

    /**
     * Ensures that loads before the fence will not be reordered with loads and stores after the fence.
     */
    public static void acquireFence()
    {
        try
        {
            ACQUIRE_FENCE.invokeExact();
        }
        catch (final Throwable t)
        {
            LangUtil.rethrowUnchecked(t);
        }
    }

    /**
     * Ensures that loads and stores before the fence will not be reordered with stores after the fence.
     */
    public static void releaseFence()
    {
        try
        {
            RELEASE_FENCE.invokeExact();
        }
        catch (final Throwable t)
        {
            LangUtil.rethrowUnchecked(t);
        }
    }

    /**
     * Ensures that loads and stores before the fence will not be reordered with loads and stores after the fence.
     */
    public static void fullFence()
    {
        try
        {
            FULL_FENCE.invokeExact();
        }
        catch (final Throwable t)
        {
            LangUtil.rethrowUnchecked(t);
        }
    }
}
