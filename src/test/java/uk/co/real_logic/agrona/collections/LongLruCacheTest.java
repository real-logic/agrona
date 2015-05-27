/*
 * Copyright 2015 Real Logic Ltd.
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
package uk.co.real_logic.agrona.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.function.LongFunction;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

public class LongLruCacheTest
{
    public static final int CAPACITY = 2;

    private LongFunction<AutoCloseable> mockFactory = mock(LongFunction.class);
    private LongLruCache<AutoCloseable> cache = new LongLruCache<>(CAPACITY, mockFactory);

    private AutoCloseable lastValue;

    @Before
    public void setUp()
    {
        when(mockFactory.apply(anyLong())).thenAnswer(inv ->
        {
            lastValue = mock(AutoCloseable.class);
            return lastValue;
        });
    }

    @Test
    public void shouldUseFactoryToConstructValues()
    {
        final AutoCloseable actual = cache.lookup(1L);

        assertSame(lastValue, actual);
        assertNotNull(lastValue);
        verifyOneConstructed(1);
    }

    @Test
    public void shouldCacheValues()
    {
        final AutoCloseable first = cache.lookup(1L);
        final AutoCloseable second = cache.lookup(1L);

        assertSame(lastValue, first);
        assertSame(lastValue, second);
        assertNotNull(lastValue);
        verifyOneConstructed(1);
    }

    @Test
    public void shouldEvictLeastRecentlyUsedItem() throws Exception
    {
        final AutoCloseable first = cache.lookup(1L);
        cache.lookup(2L);
        cache.lookup(3L);

        verify(first).close();
    }

    @Test
    public void shouldReconstructItemsAfterEviction() throws Exception
    {
        cache.lookup(1L);
        final AutoCloseable second = cache.lookup(2L);
        cache.lookup(3L);
        cache.lookup(1L);

        verify(second).close();
        verifyOneConstructed(2);
    }

    private void verifyOneConstructed(final int numberOfInvocations)
    {
        verify(mockFactory, times(numberOfInvocations)).apply(1L);
    }

}
