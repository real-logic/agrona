/*
 * Copyright 2018 Gil Tene
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

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReferenceHelperTest
{

    @Test
    public void validateIsCleared()
    {
        BigInteger big = new BigInteger("42");
        final WeakReference<BigInteger> ref = new WeakReference<>(big);
        assertFalse(ReferenceHelper.isCleared(ref));
        big = null;
        System.gc();
        assertTrue(ReferenceHelper.isCleared(ref));
    }

    @Test
    public void validateIsReferringto()
    {
        final Long longObject1 = 42L;
        final Long longObject2 = 43L; // Need different value to make sure it is a different instance...
        final WeakReference<Long> ref = new WeakReference<>(longObject1);
        assertTrue(ReferenceHelper.isReferringTo(ref, longObject1));
        assertFalse(ReferenceHelper.isReferringTo(ref, longObject2));
    }

}

