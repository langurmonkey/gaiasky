/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

public class IntDoubleHeap {
    private int[] indices;
    private double[] values;
    private int size;

    public IntDoubleHeap(int capacity) {
        indices = new int[capacity];
        values = new double[capacity];
        size = 0;
    }

    public void add(int index, double value) {
        ensureCapacity();
        indices[size] = index;
        values[size] = value;
        heapifyUp(size);
        size++;
    }

    public double peekValue() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        return values[0];
    }
    public int peekIndex() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        return indices[0];
    }

    public void poll() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        size--;
        indices[0] = indices[size];
        values[0] = values[size];
        heapifyDown(0);
    }

    public int maxIndex() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        int maxIdx = 0;
        for (int i = 1; i < size; i++) {
            if (values[i] > values[maxIdx]) {
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    public double value(int index) {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        return values[index];
    }

    public int index(int index) {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        return indices[index];
    }

    public void remove(int index) {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        size--;
        indices[index] = indices[size];
        values[index] = values[size];
        heapifyDown(index);  // maxIdx might violate heap property after replacement
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    private void ensureCapacity() {
        if (size >= indices.length) {
            int newCapacity = indices.length * 2;
            indices = java.util.Arrays.copyOf(indices, newCapacity);
            values = java.util.Arrays.copyOf(values, newCapacity);
        }
    }

    public int[] indexArray() {
        return indices;
    }

    private void heapifyUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (values[i] < values[parent]) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }

    private void heapifyDown(int i) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int smallest = i;

            if (left < size && values[left] < values[smallest]) smallest = left;
            if (right < size && values[right] < values[smallest]) smallest = right;

            if (smallest != i) {
                swap(i, smallest);
                i = smallest;
            } else {
                break;
            }
        }
    }

    private void swap(int i, int j) {
        int tmpIndex = indices[i];
        indices[i] = indices[j];
        indices[j] = tmpIndex;

        double tmpValue = values[i];
        values[i] = values[j];
        values[j] = tmpValue;
    }
}
