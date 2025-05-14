package gaiasky.util;

/**
 * A fixed-capacity data structure that maintains the top N elements by value, where N is defined by the buffer capacity.
 * Each element is represented by a pair consisting of an index and a value. When a new element is added, the structure
 * checks if it has reached capacity. If not, the new element is simply added. If the buffer is full, the element with
 * the current maximum value is identified. If the new element's value is smaller than the maximum, the maximum element
 * is removed, and the new element is inserted in its place. Otherwise, the new element is discarded.
 * <p>
 * This structure is useful in scenarios where only the smallest N elements need to be retained, such as implementing
 * a top-N filtering algorithm based on value.
 */
public class TopNBuffer {
    private final int capacity;
    private int size = 0;
    private final int[] indices;
    private final double[] values;

    /**
     * Constructs a TopNBuffer with the specified capacity.
     *
     * @param capacity the maximum number of elements the buffer can hold.
     */
    public TopNBuffer(int capacity) {
        this.capacity = capacity;
        this.indices = new int[capacity];
        this.values = new double[capacity];
    }

    /**
     * Clears the buffer by resetting its size to zero.
     */
    public void clear() {
        size = 0;
    }

    /**
     * Adds a new element to the buffer. If the buffer is not full, the element is added.
     * If the buffer is full and the new element's value is smaller than the current maximum value,
     * the maximum element is removed and the new element is inserted in its place.
     *
     * @param index the index associated with the element.
     * @param value the value associated with the element.
     */
    public void add(int index, double value) {
        if (size < capacity) {
            indices[size] = index;
            values[size] = value;
            size++;
        } else {
            int maxIdx = findMaxIndex();
            if (value < values[maxIdx]) {
                indices[maxIdx] = index;
                values[maxIdx] = value;
            }
        }
    }

    /**
     * Returns the array of indices currently in the buffer.
     *
     * @return the indices array.
     */
    public int[] indexArray() {
        return indices;
    }

    /**
     * Returns the current number of elements in the buffer.
     *
     * @return the number of elements.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the value at the specified index.
     *
     * @param i the index in the buffer.
     * @return the value at the specified index.
     */
    public double getValue(int i) {
        return values[i];
    }

    /**
     * Sorts the buffer in ascending order based on the values, maintaining index-value pairs.
     * The sorting algorithm is a dual-pivot quicksort optimized for small fixed-size arrays.
     */
    public void sort() {
        quickSort(0, size - 1);
    }

    /**
     * Implements a dual-pivot quicksort on the buffer.
     */
    private void quickSort(int low, int high) {
        if (low < high) {
            int pi = partition(low, high);
            quickSort(low, pi - 1);
            quickSort(pi + 1, high);
        }
    }

    /**
     * Partitions the buffer and returns the pivot index.
     */
    private int partition(int low, int high) {
        double pivot = values[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (values[j] < pivot) {
                i++;
                swap(i, j);
            }
        }
        swap(i + 1, high);
        return i + 1;
    }

    /**
     * Swaps elements in both the indices and values arrays.
     */
    private void swap(int i, int j) {
        int tempIndex = indices[i];
        indices[i] = indices[j];
        indices[j] = tempIndex;

        double tempValue = values[i];
        values[i] = values[j];
        values[j] = tempValue;
    }

    /**
     * Finds the index of the maximum value in the buffer.
     *
     * @return the index of the maximum value.
     */
    private int findMaxIndex() {
        int maxIdx = 0;
        double maxVal = values[0];
        for (int i = 1; i < size; i++) {
            if (values[i] > maxVal) {
                maxVal = values[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
}

