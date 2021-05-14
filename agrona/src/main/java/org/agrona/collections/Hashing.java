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
package org.agrona.collections;

/**
 * Hashing functions for applying to integers.
 */
public final class Hashing
{
    /**
     * Default load factor to be used in open addressing hashed data structures.
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.55f;

    private Hashing()
    {
    }

    /**
     * Generate a hash for an int value.
     *
     * @param value to be hashed.
     * @return the hashed value.
     */
    public static int hash(final int value)
    {
        int x = value;

        x = ((x >>> 16) ^ x) * 0x119de1f3;
        x = ((x >>> 16) ^ x) * 0x119de1f3;
        x = (x >>> 16) ^ x;

        return x;
    }

    /**
     * Generate a hash for an long value.
     *
     * @param value to be hashed.
     * @return the hashed value.
     */
    public static int hash(final long value)
    {
        long x = value;

        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        x = x ^ (x >>> 31);

        return (int)x ^ (int)(x >>> 32);
    }

    /**
     * Generate a hash for a int value and apply mask to get reminder.
     *
     * @param value to be hashed.
     * @param mask  mask to be applied that must be a power of 2 - 1.
     * @return the hash of the value.
     */
    public static int hash(final int value, final int mask)
    {
        return hash(value) & mask;
    }

    /**
     * Generate a hash for an object and apply mask to get a reminder.
     *
     * @param value to be hashed.
     * @param mask  mask to be applied that must be a power of 2 - 1.
     * @return the hash of the value.
     */
    public static int hash(final Object value, final int mask)
    {
        return hash(value.hashCode()) & mask;
    }

    /**
     * Generate a hash for a long value and apply mask to get a reminder.
     *
     * @param value to be hashed.
     * @param mask  mask to be applied that must be a power of 2 - 1.
     * @return the hash of the value.
     */
    public static int hash(final long value, final int mask)
    {
        return hash(value) & mask;
    }

    /**
     * Generate an even hash for a int value and apply mask to get a reminder that will be even.
     *
     * @param value to be hashed.
     * @param mask  mask to be applied that must be a power of 2 - 1.
     * @return the hash of the value which is always even.
     */
    public static int evenHash(final int value, final int mask)
    {
        final int hash = hash(value);
        final int evenHash = (hash << 1) - (hash << 8);

        return evenHash & mask;
    }

    /**
     * Generate an even hash for a long value and apply mask to get a reminder that will be even.
     *
     * @param value to be hashed.
     * @param mask  mask to be applied that must be a power of 2 - 1.
     * @return the hash of the value which is always even.
     */
    public static int evenHash(final long value, final int mask)
    {
        final int hash = hash(value);
        final int evenHash = (hash << 1) - (hash << 8);

        return evenHash & mask;
    }

    /**
     * Combined two 32 bit keys into a 64-bit compound.
     *
     * @param keyPartA to make the upper bits
     * @param keyPartB to make the lower bits.
     * @return the compound key
     */
    public static long compoundKey(final int keyPartA, final int keyPartB)
    {
        return ((long)keyPartA << 32) | (keyPartB & 0xFFFF_FFFFL);
    }
}
