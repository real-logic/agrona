package uk.co.real_logic.agrona.concurrent;

import static uk.co.real_logic.agrona.UnsafeAccess.UNSAFE;

/**
 * A wrapper for {@link sun.misc.Unsafe} direct memory access methods. This is a means of injecting some testable wrapper on top
 * of memory interactions. This adds no further runtime cost as all the methods are static and likely to get inlined. Compiled
 * code has been observed to be the same as direct Unsafe usage.
 */
public final class MemoryAccess
{
    private static final MemoryAccess MEMORY = new MemoryAccess();

    private MemoryAccess()
    {
        // Inconstructable!
    }

    public static MemoryAccess memory()
    {
        return MEMORY;
    }

    public void loadFence()
    {
        UNSAFE.loadFence();
    }

    public void storeFence()
    {
        UNSAFE.storeFence();
    }

    public void fullFence()
    {
        UNSAFE.fullFence();
    }

    public long getLong(final Object ref, final long offset)
    {
        return UNSAFE.getLong(ref, offset);
    }

    public void putLong(final Object ref, final long offset, final long value)
    {
        UNSAFE.putLong(ref, offset, value);
    }

    public long getLongVolatile(final Object ref, final long offset)
    {
        return UNSAFE.getLongVolatile(ref, offset);
    }

    public void putLongVolatile(final Object ref, final long offset, final long value)
    {
        UNSAFE.putLongVolatile(ref, offset, value);
    }

    public void putOrderedLong(final Object ref, final long offset, final long value)
    {
        UNSAFE.putOrderedLong(ref, offset, value);
    }

    public boolean compareAndSwapLong(
            final Object ref,
            final long offset,
            final long expectedValue,
            final long updateValue)
    {
        return UNSAFE.compareAndSwapLong(ref, offset, expectedValue, updateValue);
    }

    public long getAndSetLong(final Object ref, final long offset, final long value)
    {
        return UNSAFE.getAndSetLong(ref, offset, value);
    }

    public long getAndAddLong(final Object ref, final long offset, final long delta)
    {
        return UNSAFE.getAndAddLong(ref, offset, delta);
    }

    public int getInt(final Object ref, final long offset)
    {
        return UNSAFE.getInt(ref, offset);
    }

    public void putInt(final Object ref, final long offset, final int value)
    {
        UNSAFE.putInt(ref, offset, value);
    }

    public int getIntVolatile(final Object ref, final long offset)
    {
        return UNSAFE.getIntVolatile(ref, offset);
    }

    public void putIntVolatile(final Object ref, final long offset, final int value)
    {
        UNSAFE.putIntVolatile(ref, offset, value);
    }

    public void putOrderedInt(final Object ref, final long offset, final int value)
    {
        UNSAFE.putOrderedInt(ref, offset, value);
    }

    public boolean compareAndSwapInt(final Object ref, final long offset, final int expectedValue, final int updateValue)
    {
        return UNSAFE.compareAndSwapInt(ref, offset, expectedValue, updateValue);
    }

    public int getAndSetInt(final Object ref, final long offset, final int value)
    {
        return UNSAFE.getAndSetInt(ref, offset, value);
    }

    public int getAndAddInt(final Object ref, final long offset, final int delta)
    {
        return UNSAFE.getAndAddInt(ref, offset, delta);
    }

    public double getDouble(final Object ref, final long offset)
    {
        return UNSAFE.getDouble(ref, offset);
    }

    public void putDouble(final Object ref, final long offset, final double value)
    {
        UNSAFE.putDouble(ref, offset, value);
    }

    public float getFloat(final Object ref, final long offset)
    {
        return UNSAFE.getFloat(ref, offset);
    }

    public void putFloat(final Object ref, final long offset, final float value)
    {
        UNSAFE.putFloat(ref, offset, value);
    }

    public short getShort(final Object ref, final long offset)
    {
        return UNSAFE.getShort(ref, offset);
    }

    public void putShort(final Object ref, final long offset, final short value)
    {
        UNSAFE.putShort(ref, offset, value);
    }

    public short getShortVolatile(final Object ref, final long offset)
    {
        return UNSAFE.getShortVolatile(ref, offset);
    }

    public void putShortVolatile(final Object ref, final long offset, final short value)
    {
        UNSAFE.putShortVolatile(ref, offset, value);
    }

    public byte getByte(final Object ref, final long offset)
    {
        return UNSAFE.getByte(ref, offset);
    }

    public void putByte(final Object ref, final long offset, final byte value)
    {
        UNSAFE.putByte(ref, offset, value);
    }

    public byte getByteVolatile(final Object ref, final long offset)
    {
        return UNSAFE.getByteVolatile(ref, offset);
    }

    public void putByteVolatile(final Object ref, final long offset, final byte value)
    {
        UNSAFE.putByteVolatile(ref, offset, value);
    }

    public char getChar(final Object ref, final long offset)
    {
        return UNSAFE.getChar(ref, offset);
    }

    public void putChar(final Object ref, final long offset, final char value)
    {
        UNSAFE.putChar(ref, offset, value);
    }

    public char getCharVolatile(final Object ref, final long offset)
    {
        return UNSAFE.getCharVolatile(ref, offset);
    }

    public void putCharVolatile(final Object ref, final long offset, final char value)
    {
        UNSAFE.putCharVolatile(ref, offset, value);
    }

    public void setMemory(final Object ref, final long offset, int amount, byte value)
    {
        UNSAFE.setMemory(ref, offset, amount, value);
    }

    public void copyMemory(
            final Object from,
            final long fromOffset,
            final Object to,
            final long toOffset,
            final int amount)
    {
        UNSAFE.copyMemory(from, fromOffset, to, toOffset, amount);
    }
}
