package gaiasky.util;

import java.util.Arrays;


public class IntPriorityQueue {
    @FunctionalInterface
    public interface IntComparator {
        int compare(int a, int b);
    }

    private int[] heap;
    private int size;

    private final IntComparator comparator;

    public IntPriorityQueue(int capacity, IntComparator comparator) {
        if (capacity < 1) throw new IllegalArgumentException("Initial capacity must be > 0");
        this.heap = new int[capacity];
        this.size = 0;
        this.comparator = comparator;
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(int value) {
        if (size >= heap.length) grow();
        heap[size] = value;
        siftUp(size++);
    }

    public int peek() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        return heap[0];
    }

    public int poll() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        int result = heap[0];
        heap[0] = heap[--size];
        siftDown(0);
        return result;
    }

    public int[] toSortedArray() {
        int[] result = new int[size];
        IntPriorityQueue copy = new IntPriorityQueue(size, comparator);
        System.arraycopy(this.heap, 0, copy.heap, 0, size);
        copy.size = size;
        for (int i = 0; i < result.length; i++) {
            result[i] = copy.poll();
        }
        return result;
    }

    private void siftUp(int i) {
        int v = heap[i];
        while (i > 0) {
            int parent = (i - 1) >>> 1;
            int pv = heap[parent];
            if (comparator.compare(v, pv) >= 0) break;
            heap[i] = pv;
            i = parent;
        }
        heap[i] = v;
    }

    private void siftDown(int i) {
        int v = heap[i];
        int half = size >>> 1;
        while (i < half) {
            int left = (i << 1) + 1;
            int right = left + 1;
            int best = left;

            if (right < size && comparator.compare(heap[right], heap[left]) < 0) {
                best = right;
            }

            if (comparator.compare(v, heap[best]) <= 0) break;

            heap[i] = heap[best];
            i = best;
        }
        heap[i] = v;
    }

    private void grow() {
        int newCap = heap.length + (heap.length >>> 1) + 1;
        heap = Arrays.copyOf(heap, newCap);
    }
}
