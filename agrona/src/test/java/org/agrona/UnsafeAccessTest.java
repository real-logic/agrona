/*
 * Copyright 2014-2021 Real Logic Limited.
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
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnsafeAccessTest
{
    @Test
    @EnabledOnJre(JRE.JAVA_8)
    void memsetHackIsEnabledOnJDK8()
    {
        assertTrue(UnsafeAccess.MEMSET_HACK_REQUIRED);
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_9)
    void memsetHackIsDisabledOnJDKAfter8()
    {
        assertFalse(UnsafeAccess.MEMSET_HACK_REQUIRED);
    }
}