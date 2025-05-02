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

        System.out.println("Checking... (no more output means success)");

        for (int i = GAP; i != 0; i = (i + GAP) % NUMS)
            t.insert(i);

        for (int i = 1; i < NUMS; i += 2)
            t.remove(i);

        if ((Integer) (t.findMin()) != 2 || (Integer) (t.findMax()) != NUMS - 2)
            System.out.println("FindMin or FindMax error!");

        for (int i = 2; i < NUMS; i += 2)
            if ((Integer) (t.find(i)) != i)
                System.out.println("Find error1!");

        for (int i = 1; i < NUMS; i += 2) {
            if (t.find(i) != null)
                System.out.println("Find error2!");
        }
    }
}
