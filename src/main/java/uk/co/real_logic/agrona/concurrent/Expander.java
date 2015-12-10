package uk.co.real_logic.agrona.concurrent;

import sun.nio.ch.FileChannelImpl;
import uk.co.real_logic.agrona.LangUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Expander
{
    private static final int RW = 1;
    private static final Method MMAP;
    private static final Method UNMAP;

    static
    {
        MMAP = getMethod(FileChannelImpl.class, "map0", int.class, long.class, long.class);
        UNMAP = getMethod(FileChannelImpl.class, "unmap0", long.class, long.class);
    }

    private static Method getMethod(Class<?> cls, String name, Class<?>... params)
    {
        try
        {
            final Method method = cls.getDeclaredMethod(name, params);
            method.setAccessible(true);
            return method;
        }
        catch (NoSuchMethodException e)
        {
            LangUtil.rethrowUnchecked(e);
            return null;
        }
    }

    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException
    {
        final RandomAccessFile file = new RandomAccessFile("/tmp/example", "rw");
        final long maxInt = Integer.MAX_VALUE;
        final long length = maxInt * 4;
        file.setLength(length);

        final long address = (long) MMAP.invoke(file.getChannel(), RW, 0, length);

        final UnsafeBuffer buffer = new UnsafeBuffer(address, 10);
        //buffer.wrap(address, length);

        readWrite(maxInt * 2, buffer);
    }

    private static void readWrite(final long index, final UnsafeBuffer buffer)
    {
        buffer.putInt(index, 42);
        System.out.println(buffer.getInt(index));
    }
}
