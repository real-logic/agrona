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
package org.agrona;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

import static org.agrona.BitUtil.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class UnsafeApiTest
{
    static int value;
    static boolean value2;

    @Test
    void allocateInstance()
    {
        final Object x = UnsafeApi.allocateInstance(Integer.class);
        assertNotNull(x);
        assertInstanceOf(Integer.class, x);
        assertEquals(0, (Integer)x);
    }

    @Test
    void isWritebackEnabled()
    {
        assertEquals(0 != UnsafeApi.dataCacheLineFlushSize(), UnsafeApi.isWritebackEnabled());
    }

    @Test
    void pageSize()
    {
        final int pageSize = UnsafeApi.pageSize();
        assertThat(pageSize, greaterThan(0));
        assertTrue(isPowerOfTwo(pageSize));
    }

    @Test
    void cacheLineSize()
    {
        final int cacheLineSize = UnsafeApi.dataCacheLineFlushSize();
        if (UnsafeApi.isWritebackEnabled())
        {
            assertThat(cacheLineSize, greaterThan(0));
            assertTrue(isPowerOfTwo(cacheLineSize));
        }
        else
        {
            assertEquals(0, cacheLineSize);
        }
    }

    @Test
    void addressSize()
    {
        final int addressSize = UnsafeApi.addressSize();
        assertThat(addressSize, greaterThan(0));
        assertTrue(isPowerOfTwo(addressSize));
    }

    @Test
    void allocateUninitializedArray()
    {
        final int length = 10;
        final Object array = UnsafeApi.allocateUninitializedArray(int.class, length);
        assertNotNull(array);
        assertInstanceOf(int[].class, array);
        assertEquals(length, ((int[])array).length);
    }

    @ParameterizedTest
    @ValueSource(classes = {
        byte[].class,
        boolean[].class,
        short[].class,
        char[].class,
        int[].class,
        float[].class,
        long[].class,
        double[].class,
        Object[].class })
    void arrayBaseOffset(final Class<?> clazz)
    {
        assertThat(UnsafeApi.arrayBaseOffset(clazz), greaterThan(0));
    }

    @ParameterizedTest
    @ValueSource(classes = {
        byte[].class,
        boolean[].class,
        short[].class,
        char[].class,
        int[].class,
        float[].class,
        long[].class,
        double[].class,
        Object[].class })
    void arrayIndexScale(final Class<?> clazz)
    {
        assertThat(UnsafeApi.arrayIndexScale(clazz), greaterThan(0));
    }

    @Test
    void allocateAndFreeMemory()
    {
        final long address = UnsafeApi.allocateMemory(8);
        assertThat(address, greaterThan(0L));
        UnsafeApi.freeMemory(address);
    }

    @Test
    void reallocateMemory()
    {
        final long originalAddress = UnsafeApi.allocateMemory(16);
        UnsafeApi.putLong(originalAddress, Long.MAX_VALUE);
        UnsafeApi.putLong(originalAddress + SIZE_OF_LONG, Long.MIN_VALUE);

        final long newAddress = UnsafeApi.reallocateMemory(originalAddress, 32);
        assertNotEquals(0, newAddress);
        assertEquals(Long.MAX_VALUE, UnsafeApi.getLong(newAddress));
        assertEquals(Long.MIN_VALUE, UnsafeApi.getLong(newAddress + SIZE_OF_LONG));

        UnsafeApi.freeMemory(newAddress);
    }

    @Test
    void freeMemoryWithReallocateMemory()
    {
        final long address = UnsafeApi.allocateMemory(8);
        UnsafeApi.putLong(address, -1);

        assertThat(UnsafeApi.reallocateMemory(address, 0), is(0L));
    }

    @Test
    void allocateWithReallocateMemory()
    {
        final long address = UnsafeApi.reallocateMemory(0, 16);
        assertNotEquals(0, address);
        UnsafeApi.putLong(address, -1);

        assertEquals(-1, UnsafeApi.getLong(address));

        UnsafeApi.freeMemory(address);
    }

    @Test
    void loadFence()
    {
        UnsafeApi.loadFence();
    }

    @Test
    void loadLoadFence()
    {
        UnsafeApi.loadLoadFence();
    }

    @Test
    void storeFence()
    {
        UnsafeApi.storeFence();
    }

    @Test
    void storeStoreFence()
    {
        UnsafeApi.storeStoreFence();
    }

    @Test
    void fullFence()
    {
        UnsafeApi.fullFence();
    }

    @Test
    void staticFieldOffset() throws ReflectiveOperationException
    {
        final Field field = getClass().getDeclaredField("value");
        final long fieldOffset = UnsafeApi.staticFieldOffset(field);
        assertThat(fieldOffset, greaterThan(0L));
    }

    @Test
    void staticFieldBase() throws ReflectiveOperationException
    {
        final Field field = getClass().getDeclaredField("value2");
        final Object fieldOffset = UnsafeApi.staticFieldBase(field);
        assertNotNull(fieldOffset);
        assertSame(getClass(), fieldOffset);
    }

    @Test
    void isBigEndian()
    {
        assertEquals(ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder(),
            UnsafeApi.isBigEndian());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    void putLongUnaligned(final int offset)
    {
        final Object array = UnsafeApi.allocateUninitializedArray(byte.class, 32);
        final int arrayBaseOffset = UnsafeApi.arrayBaseOffset(array.getClass());

        final long value = ThreadLocalRandom.current().nextLong();
        UnsafeApi.putLongUnaligned(array, arrayBaseOffset + offset, value);

        assertEquals(value, UnsafeApi.getLongUnaligned(array, arrayBaseOffset + offset));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    void putIntUnaligned(final int offset)
    {
        final long address = UnsafeApi.allocateMemory(16);

        final int value = ThreadLocalRandom.current().nextInt();
        UnsafeApi.putIntUnaligned(null, address + offset, value);

        assertEquals(value, UnsafeApi.getIntUnaligned(null, address + offset));
    }

    @Test
    void unalignedAccess()
    {
        UnsafeApi.unalignedAccess();
    }
}
