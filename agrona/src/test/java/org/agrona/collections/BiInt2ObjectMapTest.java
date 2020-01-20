/*
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
package org.agrona.collections;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BiInt2ObjectMapTest
{
    private final BiInt2ObjectMap<String> map = new BiInt2ObjectMap<>();

    @Test
    public void shouldInitialiseUnderlyingImplementation()
    {
        final int initialCapacity = 10;
        final float loadFactor = 0.6f;
        final BiInt2ObjectMap<String> map = new BiInt2ObjectMap<>(initialCapacity, loadFactor);

        assertThat(map.capacity(), either(is(initialCapacity)).or(greaterThan(initialCapacity)));
        assertThat(map.loadFactor(), is(loadFactor));
    }

    @Test
    public void shouldReportEmpty()
    {
        assertThat(map.isEmpty(), is(true));
    }

    @Test
    public void shouldPutItem()
    {
        final String testValue = "Test";
        final int keyPartA = 3;
        final int keyPartB = 7;

        assertNull(map.put(keyPartA, keyPartB, testValue));
        assertThat(map.size(), is(1));
    }

    @Test
    public void shouldPutAndGetItem()
    {
        final String testValue = "Test";
        final int keyPartA = 3;
        final int keyPartB = 7;

        assertNull(map.put(keyPartA, keyPartB, testValue));
        assertThat(map.get(keyPartA, keyPartB), is(testValue));
    }

    @Test
    public void shouldReturnNullWhenNotFoundItem()
    {
        final int keyPartA = 3;
        final int keyPartB = 7;

        assertNull(map.get(keyPartA, keyPartB));
    }

    @Test
    public void shouldRemoveItem()
    {
        final String testValue = "Test";
        final int keyPartA = 3;
        final int keyPartB = 7;

        map.put(keyPartA, keyPartB, testValue);
        assertThat(map.remove(keyPartA, keyPartB), is(testValue));
        assertNull(map.get(keyPartA, keyPartB));
    }

    @Test
    public void shouldIterateValues()
    {
        final Set<String> expectedSet = new HashSet<>();
        final int count = 7;

        for (int i = 0; i < count; i++)
        {
            final String value = String.valueOf(i);
            expectedSet.add(value);
            map.put(i, i + 97, value);
        }

        final Set<String> actualSet = new HashSet<>();

        map.forEach(actualSet::add);

        assertThat(actualSet, equalTo(expectedSet));
    }

    @Test
    public void shouldIterateEntries()
    {
        final Set<EntryCapture<String>> expectedSet = new HashSet<>();
        final int count = 7;

        for (int i = 0; i < count; i++)
        {
            final String value = String.valueOf(i);
            expectedSet.add(new EntryCapture<>(i, i + 97, value));
            map.put(i, i + 97, value);
        }

        final Set<EntryCapture<String>> actualSet = new HashSet<>();

        map.forEach((keyPartA, keyPartB, value) -> actualSet.add(new EntryCapture<>(keyPartA, keyPartB, value)));

        assertThat(actualSet, equalTo(expectedSet));
    }

    @Test
    public void shouldToString()
    {
        final int count = 7;

        for (int i = 0; i < count; i++)
        {
            final String value = String.valueOf(i);
            map.put(i, i + 97, value);
        }

        assertThat(map.toString(), is("{1_98=1, 3_100=3, 2_99=2, 5_102=5, 6_103=6, 4_101=4, 0_97=0}"));
    }

    @Test
    public void shouldPutAndGetKeysOfNegativeValue()
    {
        map.put(721632679, 333118496, "a");
        assertThat(map.get(721632679, 333118496), is("a"));

        map.put(721632719, -659033725, "b");
        assertThat(map.get(721632719, -659033725), is("b"));

        map.put(721632767, -235401032, "c");
        assertThat(map.get(721632767, -235401032), is("c"));

        map.put(721632839, 1791470537, "d");
        assertThat(map.get(721632839, 1791470537), is("d"));

        map.put(721633069, -939458690, "e");
        assertThat(map.get(721633069, -939458690), is("e"));

        map.put(721633127, 1620485039, "f");
        assertThat(map.get(721633127, 1620485039), is("f"));

        map.put(721633163, -1503337805, "g");
        assertThat(map.get(721633163, -1503337805), is("g"));

        map.put(721633229, -2073657736, "h");
        assertThat(map.get(721633229, -2073657736), is("h"));

        map.put(721633255, -1278969172, "i");
        assertThat(map.get(721633255, -1278969172), is("i"));

        map.put(721633257, -1230662585, "j");
        assertThat(map.get(721633257, -1230662585), is("j"));

        map.put(721633319, -532637417, "k");
        assertThat(map.get(721633319, -532637417), is("k"));
    }

    public static class EntryCapture<V>
    {
        public final int keyPartA;
        public final int keyPartB;
        public final V value;

        public EntryCapture(final int keyPartA, final int keyPartB, final V value)
        {
            this.keyPartA = keyPartA;
            this.keyPartB = keyPartB;
            this.value = value;
        }

        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }

            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final EntryCapture that = (EntryCapture)o;

            return keyPartA == that.keyPartA && keyPartB == that.keyPartB && value.equals(that.value);

        }

        public int hashCode()
        {
            int result = keyPartA;
            result = 31 * result + keyPartB;
            result = 31 * result + value.hashCode();

            return result;
        }
    }
}
