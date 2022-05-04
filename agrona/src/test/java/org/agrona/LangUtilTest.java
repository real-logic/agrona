/*
 * Copyright 2014-2022 Real Logic Limited.
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

import static org.junit.jupiter.api.Assertions.*;

class LangUtilTest
{
    @Test
    void shouldCatchCheckedException()
    {
        assertThrows(Exception.class, LangUtilTest::throwHiddenCheckedException);
    }

    @Test
    void exactEqualsShouldReturnTrueIfBothAreNull()
    {
        assertTrue(LangUtil.exactEquals(null, null));
    }

    @Test
    void exactEqualsShouldReturnTrueIfTheSameInstance()
    {
        final Key key = new Key(1);
        assertTrue(LangUtil.exactEquals(key, key));
        final Integer obj = 5;
        assertTrue(LangUtil.exactEquals(obj, obj));
        assertTrue(LangUtil.exactEquals("abc", "abc"));
    }

    @Test
    void exactEqualsShouldReturnTrueIfTheSameValue()
    {
        final Key key1 = new Key(-100);
        final Key key2 = new Key(-100);
        assertTrue(LangUtil.exactEquals(key1, key2));
        assertTrue(LangUtil.exactEquals(key2, key1));
        assertTrue(LangUtil.exactEquals("xyz", new String("xyz".toCharArray())));
    }

    @Test
    void exactEqualsShouldReturnFalseIfNotEqual()
    {
        final Key key1 = new Key(7);
        final Key key2 = new Key(19);
        assertFalse(LangUtil.exactEquals(key1, key2));
        assertFalse(LangUtil.exactEquals(key2, key1));
        assertFalse(LangUtil.exactEquals("xyz", "abc"));
    }

    @Test
    void exactEqualsThrowsNullPointerExceptionIfSrcIsNullAndDstIsNot()
    {
        assertThrowsExactly(NullPointerException.class, () -> LangUtil.exactEquals(null, new Key(4)));
    }

    private static void throwHiddenCheckedException()
    {
        try
        {
            throw new Exception("Test Exception");
        }
        catch (final Exception t)
        {
            LangUtil.rethrowUnchecked(t);
        }
    }

    private static final class Key
    {
        private final int value;

        private Key(final int value)
        {
            this.value = value;
        }

        public boolean equals(final Object o)
        {
            if (this == o)
            {
                throw new IllegalArgumentException("THIS may not be used!");
            }
            return o instanceof Key && value == ((Key)o).value;
        }

        public int hashCode()
        {
            return value;
        }

        public String toString()
        {
            return Integer.toString(value);
        }
    }
}
