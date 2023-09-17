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
package org.agrona.collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BiLong2ObjectMapTest
{
    private final BiLong2ObjectMap<String> map = new BiLong2ObjectMap<>();

    @Test
    void shouldInitialiseUnderlyingImplementation()
    {
        final int initialCapacity = 10;
        final float loadFactor = 0.6f;
        final BiLong2ObjectMap<String> map = new BiLong2ObjectMap<>(initialCapacity, loadFactor);

        assertThat(map.capacity(), either(is(initialCapacity)).or(greaterThan(initialCapacity)));
        assertThat(map.loadFactor(), is(loadFactor));
    }

    @Test
    void shouldReportEmpty()
    {
        assertThat(map.isEmpty(), is(true));
    }

    @Test
    void shouldPutItem()
    {
        final String testValue = "Test";
        final long keyPartA = 3;
        final long keyPartB = 7;

        assertNull(map.put(keyPartA, keyPartB, testValue));
        assertThat(map.size(), is(1));
    }

    @Test
    void shouldPutAndGetItem()
    {
        final String testValue = "Test";
        final long keyPartA = 3;
        final long keyPartB = 7;

        assertNull(map.put(keyPartA, keyPartB, testValue));
        assertThat(map.get(keyPartA, keyPartB), is(testValue));
    }

    @Test
    void shouldReturnNullWhenNotFoundItem()
    {
        final int keyPartA = 3;
        final int keyPartB = 7;

        assertNull(map.get(keyPartA, keyPartB));
    }

    @Test
    void shouldRemoveItem()
    {
        final String testValue = "Test";
        final int keyPartA = 3;
        final int keyPartB = 7;

        map.put(keyPartA, keyPartB, testValue);
        assertThat(map.remove(keyPartA, keyPartB), is(testValue));
        assertNull(map.get(keyPartA, keyPartB));
    }

    @Test
    void shouldIterateValues()
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
    void shouldIterateEntries()
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
    void shouldToString()
    {
        final int count = 7;

        for (int i = 0; i < count; i++)
        {
            final String value = String.valueOf(i);
            map.put(i, i + 97, value);
        }

        assertThat(map.toString(), is("{1_98=1, 0_97=0, 2_99=2, 4_101=4, 5_102=5, 3_100=3, 6_103=6}"));
    }

    @Test
    void shouldPutAndGetKeysOfNegativeValue()
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

    @Test
    void shouldRejectNullValues()
    {
        assertThrows(NullPointerException.class, () -> map.put(1, 2, null));
        assertThrows(NullPointerException.class, () -> map.putIfAbsent(1, 2, null));
    }

    @Test
    void shouldGrowAndCanCompact()
    {
        final Map<UUID, String> reference = new HashMap<>();

        for (int i = 0; i < 20000; i++)
        {
            final UUID key = UUID.randomUUID();
            final String value = "" + i;
            final String other = "other" + i;
            assertNull(map.put(key.getMostSignificantBits(), key.getLeastSignificantBits(), other));
            assertThat(map.containsKey(key.getMostSignificantBits(), key.getLeastSignificantBits()), is(true));
            assertThat(map.remove(key.getMostSignificantBits(), key.getLeastSignificantBits(), value), is(false));
            assertThat(map.remove(key.getMostSignificantBits(), key.getLeastSignificantBits(), other), is(true));
            assertNull(map.remove(key.getMostSignificantBits(), key.getLeastSignificantBits()));
            assertThat(map.containsKey(key.getMostSignificantBits(), key.getLeastSignificantBits()), is(false));
            assertNull(map.putIfAbsent(key.getMostSignificantBits(), key.getLeastSignificantBits(), value));
            assertThat(map.putIfAbsent(key.getMostSignificantBits(), key.getLeastSignificantBits(), other), is(value));
            reference.put(key, value);
        }

        assertThat(map.size(), is(reference.size()));

        final UUID notInMap = UUID.randomUUID();
        assertThat(map.containsKey(notInMap.getMostSignificantBits(), notInMap.getLeastSignificantBits()), is(false));
        assertNull(map.get(notInMap.getMostSignificantBits(), notInMap.getLeastSignificantBits()));
        assertThat(map.getOrDefault(notInMap.getMostSignificantBits(), notInMap.getLeastSignificantBits(), "default"),
            is("default"));

        for (final UUID key : reference.keySet())
        {
            assertThat(map.containsKey(key.getMostSignificantBits(), key.getLeastSignificantBits()), is(true));
            assertThat(map.get(key.getMostSignificantBits(), key.getLeastSignificantBits()), is(reference.get(key)));
            assertThat(map.getOrDefault(key.getMostSignificantBits(), key.getLeastSignificantBits(), "default"),
                is(reference.get(key)));
        }

        while (map.size() > 0)
        {
            final int toRemove = Math.max(1, map.size() / 2);
            final List<UUID> keysToRemove = reference.keySet().stream().limit(toRemove).collect(Collectors.toList());
            for (final UUID key : keysToRemove)
            {
                final String expected = reference.remove(key);
                assertThat(expected, is(not(nullValue())));
                assertThat(map.remove(key.getMostSignificantBits(), key.getLeastSignificantBits()), is(expected));
            }
            map.compact();
        }

        assertThat(map.isEmpty(), is(true));
    }

    private void testAgainstReference(final PutFunction putFunction, final RemoveFunction removeFunction)
    {
        final Map<UUID, String> reference = new HashMap<>();

        for (int i = 0; i < 20000; i++)
        {
            final UUID key = UUID.randomUUID();
            final long keyPartA = key.getMostSignificantBits();
            final long keyPartB = key.getLeastSignificantBits();
            final String value = "" + i;
            final String other = "other" + i;
            putFunction.putInMap(map, keyPartA, keyPartB, null, value);
            assertThat(map.containsKey(key.getMostSignificantBits(), key.getLeastSignificantBits()), is(true));
            removeFunction.removeFromMap(map, keyPartA, keyPartB, value);
            assertThat(map.containsKey(key.getMostSignificantBits(), key.getLeastSignificantBits()), is(false));
            putFunction.putInMap(map, keyPartA, keyPartB, null, other);
            putFunction.putInMap(map, keyPartA, keyPartB, other, value);
            reference.put(key, value);
        }

        assertThat(map.size(), is(reference.size()));

        final UUID notInMap = UUID.randomUUID();
        assertThat(map.containsKey(notInMap.getMostSignificantBits(), notInMap.getLeastSignificantBits()), is(false));
        assertNull(map.get(notInMap.getMostSignificantBits(), notInMap.getLeastSignificantBits()));
        assertThat(map.getOrDefault(notInMap.getMostSignificantBits(), notInMap.getLeastSignificantBits(), "default"),
            is("default"));

        for (final UUID key : reference.keySet())
        {
            final long keyPartA = key.getMostSignificantBits();
            final long keyPartB = key.getLeastSignificantBits();
            assertThat(map.containsKey(keyPartA, keyPartB), is(true));
            assertThat(map.get(keyPartA, keyPartB), is(reference.get(key)));
            assertThat(map.getOrDefault(keyPartA, keyPartB, "default"), is(reference.get(key)));
        }
    }

    @Test
    void shouldBlindReplace()
    {
        testAgainstReference(
            (map, keyPartA, keyPartB, expectedCurrentValue, newValue) ->
            {
                if (expectedCurrentValue == null)
                {
                    assertNull(map.put(keyPartA, keyPartB, "placeholder"));
                    assertThat(map.replace(keyPartA, keyPartB, newValue), is("placeholder"));
                }
                else
                {
                    final String actualOldValue = map.replace(keyPartA, keyPartB, newValue);
                    assertThat(actualOldValue, is(expectedCurrentValue));
                }
            },
            (map, keyPartA, keyPartB, expectedCurrentValue) ->
            {
                final String previousValue = map.remove(keyPartA, keyPartB);
                assertThat(previousValue, is(expectedCurrentValue));
            }
        );
    }

    @Test
    void shouldReplace()
    {
        testAgainstReference(
            (map, keyPartA, keyPartB, expectedCurrentValue, newValue) ->
            {
                if (expectedCurrentValue == null)
                {
                    assertThat(map.replace(keyPartA, keyPartB, "placeholder", newValue), is(false));
                    assertNull(map.put(keyPartA, keyPartB, "placeholder"));
                    assertThat(map.replace(keyPartA, keyPartB, "placeholder", newValue), is(true));
                }
                else
                {
                    assertThat(map.replace(keyPartA, keyPartB, expectedCurrentValue, newValue), is(true));
                }
                assertThat(map.replace(keyPartA, keyPartB, newValue, newValue), is(true));
                assertThat(map.replace(keyPartA, keyPartB, "placeholder", newValue + "diff"), is(false));
            },
            (map, keyPartA, keyPartB, expectedCurrentValue) ->
            {
                final String previousValue = map.remove(keyPartA, keyPartB);
                assertThat(previousValue, is(expectedCurrentValue));
            }
        );
    }

    @Test
    void shouldConditionallyCompute()
    {
        testAgainstReference(
            (map, keyPartA, keyPartB, expectedCurrentValue, newValue) ->
            {
                if (expectedCurrentValue == null)
                {
                    assertThat(map.computeIfAbsent(keyPartA, keyPartB, (a, b) -> newValue), is(newValue));
                }
                else
                {
                    assertThat(map.computeIfPresent(keyPartA, keyPartB, (a, b, current) ->
                    {
                        assertThat(current, is(expectedCurrentValue));
                        return newValue;
                    }), is(newValue));
                }
                assertThat(map.computeIfAbsent(keyPartA, keyPartB, (a, b) -> "placeholder"), is(newValue));
            },
            (map, keyPartA, keyPartB, expectedCurrentValue) ->
            {
                assertNull(map.computeIfPresent(keyPartA, keyPartB, (a, b, current) ->
                {
                    assertThat(current, is(expectedCurrentValue));
                    return null;
                }));
            }
        );
    }

    @Test
    void shouldCompute()
    {
        testAgainstReference(
            (map, keyPartA, keyPartB, expectedCurrentValue, newValue) ->
            {
                assertThat(map.compute(keyPartA, keyPartB, (a, b, current) ->
                {
                    assertThat(current, is(expectedCurrentValue));
                    return newValue;
                }), is(newValue));
            },
            (map, keyPartA, keyPartB, expectedCurrentValue) ->
            {
                assertNull(map.compute(keyPartA, keyPartB, (a, b, current) ->
                {
                    assertThat(current, is(expectedCurrentValue));
                    return null;
                }));
            }
        );
    }

    @Test
    void shouldMerge()
    {
        testAgainstReference(
            (map, keyPartA, keyPartB, expectedCurrentValue, newValue) ->
            {
                map.merge(keyPartA, keyPartB, newValue, (oldValue, value) ->
                {
                    assertThat(oldValue, is(expectedCurrentValue));
                    assertThat(value, is(newValue));
                    return value;
                });
            },
            (map, keyPartA, keyPartB, expectedCurrentValue) ->
            {
                map.merge(keyPartA, keyPartB, "placeholder", (oldValue, value) ->
                {
                    assertThat(oldValue, is(expectedCurrentValue));
                    return null;
                });
            }
        );
    }

    @FunctionalInterface
    interface PutFunction
    {
        void putInMap(BiLong2ObjectMap<String> map, long keyPartA, long keyPartB,
            String expectedCurrentValue, String newValue);
    }

    @FunctionalInterface
    interface RemoveFunction
    {
        void removeFromMap(BiLong2ObjectMap<String> map, long keyPartA, long keyPartB, String expectedCurrentValue);
    }

    static final class EntryCapture<V>
    {
        public final long keyPartA;
        public final long keyPartB;
        public final V value;

        EntryCapture(final long keyPartA, final long keyPartB, final V value)
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

            if (!(o instanceof EntryCapture))
            {
                return false;
            }

            final EntryCapture<?> that = (EntryCapture<?>)o;

            return keyPartA == that.keyPartA && keyPartB == that.keyPartB && value.equals(that.value);
        }

        public int hashCode()
        {
            long result = keyPartA;
            result = 31 * result + keyPartB;
            result = 31 * result + value.hashCode();

            return Long.hashCode(result);
        }
    }

}
