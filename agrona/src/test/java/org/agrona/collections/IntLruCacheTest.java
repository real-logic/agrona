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
package org.agrona.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.function.Consumer;
import java.util.function.IntFunction;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class IntLruCacheTest
{
    public static final int CAPACITY = 2;

    @SuppressWarnings("unchecked")
    private final IntFunction<AutoCloseable> mockFactory = mock(IntFunction.class);
    @SuppressWarnings("unchecked")
    private final Consumer<AutoCloseable> mockCloser = mock(Consumer.class);

    private final IntLruCache<AutoCloseable> cache = new IntLruCache<>(CAPACITY, mockFactory, mockCloser);

    private AutoCloseable lastValue;

    @Before
    public void setUp()
    {
        when(mockFactory.apply(anyInt())).thenAnswer(
            (inv) ->
            {
                lastValue = mock(AutoCloseable.class);
                return lastValue;
            });
    }

    @Test
    public void shouldUseFactoryToConstructValues()
    {
        final AutoCloseable actual = cache.lookup(1);

        assertSame(lastValue, actual);
        assertNotNull(lastValue);
        verifyOneConstructed(1);
    }

    @Test
    public void shouldCacheValues()
    {
        final AutoCloseable first = cache.lookup(1);
        final AutoCloseable second = cache.lookup(1);

        assertSame(lastValue, first);
        assertSame(lastValue, second);
        assertNotNull(lastValue);
        verifyOneConstructed(1);
    }

    @Test
    public void shouldEvictLeastRecentlyUsedItem()
    {
        final AutoCloseable first = cache.lookup(1);
        cache.lookup(2);
        cache.lookup(3);

        verify(mockCloser).accept(first);
    }

    @Test
    public void shouldReconstructItemsAfterEviction()
    {
        cache.lookup(1);
        final AutoCloseable second = cache.lookup(2);
        cache.lookup(3);
        cache.lookup(1);

        verify(mockCloser).accept(second);
        verifyOneConstructed(2);
    }

    @Test
    public void shouldSupportKeyOfZero()
    {
        final AutoCloseable actual = cache.lookup(0);

        assertSame(lastValue, actual);
        assertNotNull(lastValue);
    }

    @Test
    public void shouldCloseAllOpenResources()
    {
        final AutoCloseable first = cache.lookup(1);
        final AutoCloseable second = cache.lookup(2);

        cache.close();

        verify(mockCloser).accept(first);
        verify(mockCloser).accept(second);
    }

    private void verifyOneConstructed(final int numberOfInvocations)
    {
        verify(mockFactory, times(numberOfInvocations)).apply(1);
    }
}
