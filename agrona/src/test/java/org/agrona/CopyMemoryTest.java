package org.agrona;

import org.junit.Assert;
import org.junit.Test;

public class CopyMemoryTest {
    @Test
    public void shouldCopyMemory(){
        long valueOfLong = 34567;
        long addressOfLong = UnsafeAccess.UNSAFE.allocateMemory(8);
        UnsafeAccess.UNSAFE.putLong(addressOfLong, valueOfLong);

        final ExpandableDirectByteBuffer buffer
                = new ExpandableDirectByteBuffer(128);
        buffer.copyMemory(23,addressOfLong,8);
        long aLong = buffer.getLong(23);
        Assert.assertEquals(valueOfLong, aLong);
    }
}
