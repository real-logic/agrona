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

import org.agrona.UnsafeAccess;

/**
 * Memory access operations which encapsulate the use of Unsafe.
 */
public final class MemoryAccess
{
    private MemoryAccess()
    {
    }

    /**
     * Ensures that loads before the fence will not be reordered with loads and stores after the fence.
     */
    public static void acquireFence()
    {
        UnsafeAccess.UNSAFE.loadFence();
    }

    /**
     * Ensures that loads and stores before the fence will not be reordered with stores after the fence.
     */
    public static void releaseFence()
    {
        UnsafeAccess.UNSAFE.storeFence();
    }

    /**
     * Ensures that loads and stores before the fence will not be reordered with loads and stores after the fence.
     */
    public static void fullFence()
    {
        UnsafeAccess.UNSAFE.fullFence();
    }
}
