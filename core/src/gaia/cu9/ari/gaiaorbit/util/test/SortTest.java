package gaia.cu9.ari.gaiaorbit.util.test;

import com.badlogic.gdx.utils.Array;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * Compares performances of different lists such as {@link java.util.ArrayList}
 * and {@link com.badlogic.gdx.utils.Array}.
 * 
 * @author tsagrista
 *
 */
public class SortTest {
    private static int N_SORTS = 5;
    private static int N_ITEMS = 1000000;

    public static void main(String[] args) {
        Random rnd = new Random(5522l);

        long totalArray = 0;
        long totalList = 0;
        long totalRaw = 0;

        long start, end;

        ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();

        Comparator<Double> comp = new DoubleComparator();

        for (int i = 0; i < N_SORTS; i++) {
            List<Object> l = newRandomLists(rnd);

            @SuppressWarnings("unchecked")
            List<Double> list = (List<Double>) l.get(0);
            @SuppressWarnings("unchecked")
            Array<Double> array = (Array<Double>) l.get(1);
            Double[] raw = (Double[]) l.get(2);

            /** Java's ArrayList **/
            start = tmxb.getCurrentThreadCpuTime();
            list.sort(comp);
            end = tmxb.getCurrentThreadCpuTime();
            long listTime = end - start;
            totalList += listTime;

            /** Libgdx's Array **/
            start = tmxb.getCurrentThreadCpuTime();
            array.sort(comp);
            end = tmxb.getCurrentThreadCpuTime();
            long arrayTime = end - start;
            totalArray += arrayTime;

            /** Raw array **/
            start = tmxb.getCurrentThreadCpuTime();
            Arrays.sort(raw);
            end = tmxb.getCurrentThreadCpuTime();
            long rawTime = end - start;
            totalRaw += rawTime;

            /** Report **/
            System.out.println("Run " + (i + 1));
            System.out.println("Java ArrayList: " + (double) listTime / 1e9d + " seconds");
            System.out.println("Libgdx Array:   " + (double) arrayTime / 1e9d + " seconds");
            System.out.println("Raw array:      " + (double) rawTime / 1e9d + " seconds");
            System.out.println();
        }

        System.out.println("Aggregated results");
        System.out.println("------------------");
        System.out.println();
        System.out.println("N items: " + N_ITEMS);
        System.out.println("N sorts: " + N_SORTS);
        System.out.println();
        System.out.println("Java ArrayList");
        System.out.println("Total time: " + (double) totalList / 1e9d);
        System.out.println("Avg time:   " + (double) totalList / N_SORTS / 1e9d);

        System.out.println();
        System.out.println("Libgdx Array");
        System.out.println("Total time: " + (double) totalArray / 1e9d);
        System.out.println("Avg time:   " + (double) totalArray / N_SORTS / 1e9d);

        System.out.println();
        System.out.println("Raw array");
        System.out.println("Total time: " + (double) totalRaw / 1e9d);
        System.out.println("Avg time:   " + (double) totalRaw / N_SORTS / 1e9d);
    }

    public static List<Object> newRandomLists(Random rnd) {
        List<Double> l = new ArrayList<Double>(N_ITEMS);
        Array<Double> a = new Array<Double>(N_ITEMS);
        Double[] r = new Double[N_ITEMS];
        for (int i = 0; i < N_ITEMS; i++) {
            double next = rnd.nextDouble();
            l.add(next);
            a.add(next);
            r[i] = next;
        }
        List<Object> result = new ArrayList<Object>(3);
        result.add(l);
        result.add(a);
        result.add(r);
        return result;
    }

    private static class DoubleComparator implements Comparator<Double> {

        @Override
        public int compare(Double o1, Double o2) {
            return Double.compare(o1, o2);
        }

    }

}
