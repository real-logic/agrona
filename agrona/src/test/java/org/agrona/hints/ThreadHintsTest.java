/*
 * Copyright 2014-2024 Real Logic Limited.
 * Copyright 2016 Gil Tene
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
package org.agrona.hints;

import org.junit.jupiter.api.Test;

class ThreadHintsTest
{
    @Test
    void shouldDoNothingOnSpinWait()
    {
        for (int i = 0; i < 100; i++)
        {
            ThreadHints.onSpinWait();
        }
    }
}
