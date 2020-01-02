/*
 * Copyright 2018 Gil Tene
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
package org.agrona;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReferencesTest
{
    @Test
    public void validateIsCleared()
    {
        BigInteger bigInteger = new BigInteger("42");
        final WeakReference<BigInteger> ref = new WeakReference<>(bigInteger);
        assertFalse(References.isCleared(ref));
        bigInteger = null;
        System.gc();
        assertTrue(References.isCleared(ref));
    }

    @Test
    public void validateIsReferringTo()
    {
        final Long objOne = 42L;
        final Long objTwo = 43L; // Need different value to make sure it is a different instance...
        final WeakReference<Long> ref = new WeakReference<>(objOne);
        assertTrue(References.isReferringTo(ref, objOne));
        assertFalse(References.isReferringTo(ref, objTwo));
    }
}

