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
package org.agrona.io;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.nio.ByteOrder;

public class DirectBufferDataInputJDKTest extends DirectBufferDataInputTest
{
    UnsafeBuffer toUnsafeBuffer(final ThrowingConsumer<DataOutput> dataProvider) throws Throwable
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(20);
        try (DataOutputStream out = new DataOutputStream(baos))
        {
            dataProvider.accept(out);
        }

        return new UnsafeBuffer(baos.toByteArray());
    }

    @Override
    ByteOrder byteOrder()
    {
        return ByteOrder.BIG_ENDIAN;
    }
}
