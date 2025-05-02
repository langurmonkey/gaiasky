package gaiasky;


import gaiasky.util.BinarySearchTree;
import org.junit.Test;

/**
 * Tests {@link BinarySearchTree}.
 */
public class BinarySearchTreeTest {

    @Test
    public void test() {

        BinarySearchTree<Integer> t = new BinarySearchTree<>();
        final int NUMS = 4000;
        final int GAP = 37;

        for (int i = GAP; i != 0; i = (i + GAP) % NUMS)
            t.insert(i);

        for (int i = 1; i < NUMS; i += 2)
            t.remove(i);

        if ((Integer) (t.findMin()) != 2 || (Integer) (t.findMax()) != NUMS - 2)
            throw new AssertionError("FindMin or FindMax error");

        for (int i = 2; i < NUMS; i += 2)
            if ((Integer) (t.find(i)) != i)
                throw new AssertionError("Find error1!");

        for (int i = 1; i < NUMS; i += 2) {
            if (t.find(i) != null)
                throw new AssertionError("Find error2!");
        }
    }
}
