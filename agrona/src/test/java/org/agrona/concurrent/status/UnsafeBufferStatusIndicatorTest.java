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
package org.agrona.concurrent.status;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

import static java.nio.ByteBuffer.allocateDirect;
import static org.agrona.concurrent.status.CountersReader.COUNTER_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UnsafeBufferStatusIndicatorTest
{
    @Test
    void canWrapDifferentKindsOfBuffers()
    {
        final long value = Long.MIN_VALUE;
        final int counterId = 2;
        final UnsafeBuffer buffer = new UnsafeBuffer(allocateDirect(5 * COUNTER_LENGTH));
        final UnsafeBufferStatusIndicator unsafeBufferStatusIndicator =
            new UnsafeBufferStatusIndicator(buffer, counterId);

        unsafeBufferStatusIndicator.setVolatile(value);
        assertEquals(value, unsafeBufferStatusIndicator.getVolatile());
    }
}
