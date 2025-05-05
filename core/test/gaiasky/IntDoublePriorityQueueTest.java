package gaiasky;

import gaiasky.util.IntDoublePriorityQueue;
import org.junit.Assert;
import org.junit.Test;

public class IntDoublePriorityQueueTest {

    @Test
    public void test() {

        IntDoublePriorityQueue queue = new IntDoublePriorityQueue();
        queue.add(1, 10.5);
        queue.add(2, 5.0);
        queue.add(3, 15.0);
        queue.add(4, 7.2);
        queue.add(5, -2.2);
        queue.add(6, 1.0);
        queue.add(7, 12.0);

        System.out.println("Poll: " + String.format("[%d, %f]", queue.peekIndex(), queue.peekValue()));
        Assert.assertEquals(5, queue.peekIndex());
        Assert.assertEquals(-2.2, queue.peekValue(), 1e-5);
        queue.poll();
        System.out.println("Poll: " + String.format("[%d, %f]", queue.peekIndex(), queue.peekValue()));
        Assert.assertEquals(6, queue.peekIndex());
        Assert.assertEquals(1.0, queue.peekValue(), 1e-5);
        queue.poll();
        System.out.println("Poll: " + String.format("[%d, %f]", queue.peekIndex(), queue.peekValue()));
        Assert.assertEquals(2, queue.peekIndex());
        Assert.assertEquals(5.0, queue.peekValue(), 1e-5);
        queue.poll();
        System.out.println("Poll: " + String.format("[%d, %f]", queue.peekIndex(), queue.peekValue()));
        Assert.assertEquals(4, queue.peekIndex());
        Assert.assertEquals(7.2, queue.peekValue(), 1e-5);
        queue.poll();
    }
}
