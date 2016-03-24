/*
 *  Copyright 2014 - 2016 Real Logic Ltd.
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
 * Hashing functions for applying to integers.
 */
public class Hashing
{
    /**
     * Generate a hash for a int value.
     *
     * @param value to be hashed.
     * @param mask  mask to be applied that must be a power of 2 - 1.
     * @return the hash of the value.
     */
    public static int hash(final int value, final int mask)
    {
        final int hash = value ^ (value >>> 16);

        return hash & mask;
    }

    /**
     * Generate a hash for a long value.
     *
     * @param value to be hashed.
     * @param mask  mask to be applied that must be a power of 2 - 1.
     * @return the hash of the value.
     */
    public static int hash(final long value, final int mask)
    {
        int hash = (int)value ^ (int)(value >>> 32);
        hash = hash ^ (hash >>> 16);

        return hash & mask;
    }

    /**
     * Generate a hash for two ints.
     *
     * @param valueOne to be hashed.
     * @param valueTwo to be hashed.
     * @param mask     mask to be applied that must be a power of 2 - 1.
     * @return a hash of the values.
     */
    public static int hash(final int valueOne, final int valueTwo, final int mask)
    {
        int hash = valueOne ^ valueTwo;
        hash = hash ^ (hash >>> 16);

        return hash & mask;
    }

    /**
     * Generate an even hash for a int value.
     *
     * @param value to be hashed.
     * @param mask  mask to be applied that must be a power of 2 - 1.
     * @return the hash of the value which is always even.
     */
    public static int evenHash(final int value, final int mask)
    {
        final int hash = (value << 1) - (value << 8);

        return hash & mask;
    }

    /**
     * Generate an even hash for a long value.
     *
     * @param value to be hashed.
     * @param mask  mask to be applied that must be a power of 2 - 1.
     * @return the hash of the value which is always even.
     */
    public static int evenHash(final long value, final int mask)
    {
        int hash = (int)value ^ (int)(value >>> 32);
        hash = (hash << 1) - (hash << 8);

        return hash & mask;
    }
}
