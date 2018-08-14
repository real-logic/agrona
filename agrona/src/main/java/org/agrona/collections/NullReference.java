/*
 * Copyright 2014-2018 Real Logic Ltd.
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
package org.agrona.collections;

/**
 * Internal class for {@link Int2ObjectHashMap} and (generated) {@code Long2ObjectHashMap} to mask {@code null}
 * map values with the appropriate implementations for {@link #hashCode()} + {@link #equals(Object)}.
 */
final class NullReference
{
    static final NullReference INSTANCE = new NullReference();

    public int hashCode()
    {
        return 0;
    }

    public boolean equals(final Object obj)
    {
        return obj == this;
    }
}
