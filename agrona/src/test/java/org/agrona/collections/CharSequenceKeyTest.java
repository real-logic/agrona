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
package org.agrona.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CharSequenceKeyTest
{
    @Test
    void equalsThrowsIllegalArgumentExceptionWhenMatchedOnThis()
    {
        final CharSequenceKey key = new CharSequenceKey("abc");

        final IllegalArgumentException exception =
            assertThrowsExactly(IllegalArgumentException.class, () -> key.equals(key));
        assertEquals("This equality violation!", exception.getMessage());
    }

    @Test
    void equalsReturnsTrueWhenMatchedAgainstTheKeyWithTheSameContents()
    {
        final CharSequenceKey key1 = new CharSequenceKey("key 1");
        final CharSequenceKey key2 = new CharSequenceKey("key 1");

        assertEquals(key2, key1);
        assertEquals(key1, key2);
    }

    @Test
    void equalsReturnsTrueWhenMatchedAgainstTheString()
    {
        final CharSequenceKey key = new CharSequenceKey("my key");
        final String data = new String("my key".toCharArray());

        assertEquals(key, data);
        assertNotEquals(data, key);
    }

    @Test
    void equalsReturnsFalseWhenNotMatched()
    {
        final CharSequenceKey key = new CharSequenceKey("this is it");

        assertNotEquals(key, new CharSequenceKey("THIS IS IT"));
        assertNotEquals(key, "haha");
        assertNotEquals(key, "this is it".length());
        assertNotEquals(key, new Object());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "ARbyhVv", "Hello, World!", "روباه قهوه ای سریع از روی سگ تنبل می پرد.", "どうもありがとうございます" })
    void hashCodeDelegatesToTheString(final String value)
    {
        final CharSequenceKey key = new CharSequenceKey(value);
        assertEquals(value.hashCode(), key.hashCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "ARbyhVv", "Hello, World!", "روباه قهوه ای سریع از روی سگ تنبل می پرد.", "どうもありがとうございます" })
    void toStringReturnsTheStringContents(final String value)
    {
        final CharSequenceKey key = new CharSequenceKey(value);
        assertSame(value, key.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "test", "Дякую!" })
    void lengthReturnsTheLengthOfTheUnderlyingString(final String value)
    {
        final CharSequenceKey key = new CharSequenceKey(value);
        assertEquals(value.length(), key.length());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "thanks", "Дякую!", "謝謝", "ありがとうございました" })
    void charAtReturnsCharacterFromTheUnderlyingString(final String value)
    {
        final CharSequenceKey key = new CharSequenceKey(value);
        for (int i = 0; i < value.length(); i++)
        {
            assertEquals(value.charAt(i), key.charAt(i));
        }
    }

    @Test
    void subSequenceReturnsASubstring()
    {
        final CharSequenceKey key = new CharSequenceKey("Hello, World!");

        final CharSequence subSequence = key.subSequence(7, 12);
        assertInstanceOf(String.class, subSequence);
        assertEquals("World", subSequence);
    }
}
