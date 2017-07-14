/*
 * Copyright 2014-2017 Real Logic Ltd.
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
 * Holder for an long value that is mutable. Useful for being a counter in a {@link java.util.Map} or for passing by
 * reference.
 */
public class MutableLong
{
    public long value = 0;

    public MutableLong()
    {
    }

    public MutableLong(final long value)
    {
        this.value = value;
    }

    public long get()
    {
        return value;
    }

    public void set(final long value)
    {
        this.value = value;
    }
}
