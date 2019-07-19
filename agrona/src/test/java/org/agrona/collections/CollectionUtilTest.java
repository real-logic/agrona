/*
 * Copyright 2014-2019 Real Logic Ltd.
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

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static org.agrona.collections.CollectionUtil.removeIf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CollectionUtilTest
{
    @Test
    public void removeIfRemovesMiddle()
    {
        assertRemoveIfRemoves(1, 2, 3);
    }

    @Test
    public void removeIfRemovesStart()
    {
        assertRemoveIfRemoves(2, 1, 3);
    }

    @Test
    public void removeIfRemovesEnd()
    {
        assertRemoveIfRemoves(3, 1, 2);
    }

    private void assertRemoveIfRemoves(final int requiredValue, final Integer... expectedValues)
    {
        final List<Integer> values = new ArrayList<>(asList(1, 2, 3));
        assertEquals(1, removeIf(values, value -> value == requiredValue));

        assertEquals(values, asList(expectedValues));
    }

    @Test
    public void getOrDefaultUsesSupplier()
    {
        final Map<Integer, Integer> ints = new HashMap<>();
        final Integer result = CollectionUtil.getOrDefault(ints, 0, (x) -> x + 1);

        assertThat(result, is(1));
    }

    @Test
    public void getOrDefaultDoesNotCreateNewValueWhenOneExists()
    {
        final Map<Integer, Integer> ints = new HashMap<>();
        ints.put(0, 0);
        final Integer result = CollectionUtil.getOrDefault(
            ints,
            0,
            (x) ->
            {
                Assert.fail("Shouldn't be called");
                return x + 1;
            });

        assertThat(result, is(0));
    }
}
