package gaiasky.util;

public class TopNBuffer {
    private final int capacity;
    private int size = 0;
    private final int[] indices;
    private final double[] values;

    public TopNBuffer(int capacity) {
        this.capacity = capacity;
        this.indices = new int[capacity];
        this.values = new double[capacity];
    }

    public void clear() {
        size = 0;
    }

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

    public int[] indexArray() {
        return indices;
    }

    public int size() {
        return size;
    }

    public double getValue(int i) {
        return values[i];
    }

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
