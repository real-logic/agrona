package uk.co.real_logic.agrona.concurrent;

import static java.nio.ByteOrder.nativeOrder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.ByteOrder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import uk.co.real_logic.agrona.BitUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MemoryAccess.class)
public class UnsafeBufferTest
{
    @Test
    public void usesBufferAndOffsetToFormMemoryAccess() throws Exception
    {
        final byte value = 1;
        final MemoryAccess spy = setupMemorySpy();
        final byte[] byteArray = new byte[16];
        final UnsafeBuffer buffer = new UnsafeBuffer(byteArray);
        byte b = buffer.getByte(0);
        Assert.assertEquals(0, b);
        final long baseOffset = buffer.addressOffset();
        Mockito.verify(spy).getByte(byteArray, baseOffset);

        buffer.putByte(1, value);
        b = buffer.getByte(1);
        final InOrder inOrder = Mockito.inOrder(spy);
        inOrder.verify(spy).putByte(byteArray, baseOffset + 1, value);
        inOrder.verify(spy).getByte(byteArray, baseOffset + 1);

        Assert.assertEquals(value, b);
    }

    @Test
    public void getAndAddOrdered()
    {
        final MemoryAccess spy = setupMemorySpy();
        final byte[] byteArray = new byte[16];
        final UnsafeBuffer buffer = new UnsafeBuffer(byteArray);
        final long baseOffset = buffer.addressOffset();

        final int v = buffer.addIntOrdered(0, 1);
        Assert.assertEquals(0, v);
        final InOrder inOrder = Mockito.inOrder(spy);
        inOrder.verify(spy).getInt(byteArray, baseOffset + 0);
        inOrder.verify(spy).putOrderedInt(byteArray, baseOffset + 0, 1);
    }

    @Test
    public void putIntReverseOrders()
    {
        final ByteOrder reverseBO = (nativeOrder() == ByteOrder.BIG_ENDIAN) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        final MemoryAccess spy = setupMemorySpy();
        final byte[] byteArray = new byte[16];
        final UnsafeBuffer buffer = new UnsafeBuffer(byteArray);
        final long baseOffset = buffer.addressOffset();
        buffer.putInt(0, 1, reverseBO);
        verify(spy).putInt(byteArray, baseOffset + 0, Integer.reverseBytes(1));

        assertEquals(1, buffer.getInt(0, reverseBO));
        assertEquals(Integer.reverseBytes(1), buffer.getInt(0));
        verify(spy, times(2)).getInt(byteArray, baseOffset + 0);
    }

    private MemoryAccess setupMemorySpy()
    {
        final MemoryAccess spy = Mockito.spy(MemoryAccess.class);
        PowerMockito.mockStatic(MemoryAccess.class);
        PowerMockito.when(MemoryAccess.memory()).thenReturn(spy);

        Assert.assertEquals(spy, MemoryAccess.memory());
        return spy;
    }

    @Test
    public void boundChecksPreventMemoryAccess()
    {
        final byte value = 1;
        final MemoryAccess spy = setupMemorySpy();

        final byte[] byteArray = new byte[16];
        final UnsafeBuffer buffer = new UnsafeBuffer(byteArray);
        try
        {
            buffer.getByte(16);
            Assert.fail();
        }
        catch (IndexOutOfBoundsException ioobe)
        {
        }
        Mockito.verifyNoMoreInteractions(spy);
        try
        {
            buffer.putByte(16, value);
            Assert.fail();
        }
        catch (IndexOutOfBoundsException ioobe)
        {
        }
        Mockito.verifyNoMoreInteractions(spy);
    }

    @Test
    public void demonstrateRecording()
    {
        final byte value = 1;
        final MemoryAccess spy = Mockito.mock(MemoryAccess.class, new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                final Object result = invocation.callRealMethod();
                record(invocation.getMethod().getName(), result, invocation.getArguments());
                return result;
            }
        });
        PowerMockito.mockStatic(MemoryAccess.class);
        PowerMockito.when(MemoryAccess.memory()).thenReturn(spy);

        Assert.assertEquals(spy, MemoryAccess.memory());
        final byte[] byteArray = new byte[16];
        final UnsafeBuffer buffer = new UnsafeBuffer(byteArray);
        byte b = buffer.getByte(0);
        Assert.assertEquals(0, b);
        final long baseOffset = buffer.addressOffset();

        buffer.putByte(1, value);
        b = buffer.getByte(1);

        Assert.assertEquals(value, b);
    }

    public static void record(String name, Object result, Object[] params)
    {
        final StringBuilder builder = new StringBuilder(128);
        builder.append(Thread.currentThread().getName());
        builder.append(" - MA: ");
        builder.append(name);
        if (params != null && params.length != 0)
        {
            builder.append("[");
            for (Object p : params)
            {
                if (p instanceof byte[])
                {
                    builder.append(BitUtil.toHex((byte[]) p));
                }
                else
                {
                    builder.append(p);
                }
                builder.append(",");
            }
            builder.setCharAt(builder.length() - 1, ']');

        }
        else
        {
            builder.append("[]");
        }
        builder.append(",");
        builder.append(result);
        System.out.println(builder.toString());
    }
}
