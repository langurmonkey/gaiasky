/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import java.util.Arrays;

public class IntDoublePriorityQueue {
    private int[] indices;  // To store the indices (integer values)
    private double[] values;  // To store the values (double values)
    private int size;  // To track the current number of elements in the queue
    private static final int INITIAL_CAPACITY = 10;

    public IntDoublePriorityQueue() {
        indices = new int[INITIAL_CAPACITY];
        values = new double[INITIAL_CAPACITY];
        size = 0;
    }

    public IntDoublePriorityQueue(int capacity) {
        indices = new int[capacity];
        values = new double[capacity];
        size = 0;
    }

    public void clear() {
        size = 0;
    }

    // Adds a new index-value pair to the queue
    public void add(int index, double value) {
        // Resize arrays if needed
        if (size >= indices.length) {
            int newCapacity = indices.length * 2;
            indices = Arrays.copyOf(indices, newCapacity);
            values = Arrays.copyOf(values, newCapacity);
        }

        // Insert the new element at the end of the queue
        indices[size] = index;
        values[size] = value;
        size++;

        // Re-order the queue based on the value (sorting in ascending order)
        heapifyUp(size - 1);
    }

    public int peekIndex() {
        if (size == 0) {
            throw new IllegalStateException("Queue is empty");
        }
        return indices[0];
    }

    public double peekValue() {
        if (size == 0) {
            throw new IllegalStateException("Queue is empty");
        }
        return values[0];
    }

    // Polls (removes) the top element from the queue
    public void poll() {
        if (size == 0) {
            throw new IllegalStateException("Queue is empty");
        }

        // Swap the first and last elements
        int result = indices[0];
        indices[0] = indices[size - 1];
        values[0] = values[size - 1];
        size--;

        // Re-order the queue after removal (down-heapify)
        heapifyDown(0);
    }

    public int peekLastIndex() {
        if (size == 0) {
            throw new IllegalStateException("Queue is empty");
        }
        return indices[size - 1];
    }
    public double peekLastValue() {
        if (size == 0) {
            throw new IllegalStateException("Queue is empty");
        }
        return values[size - 1];
    }

    // Removes the last element from the queue without reordering the heap
    public void removeLast() {
        if (size == 0) {
            throw new IllegalStateException("Queue is empty");
        }
        size--;  // Decrease the size of the queue
    }

    public int[] indexArray() {
        return indices;
    }


    // Reorganize the queue (heapify-up) after an element is added
    private void heapifyUp(int index) {
        while (index > 0 && values[index] < values[parent(index)]) {
            swap(index, parent(index));
            index = parent(index);
        }
    }

    // Reorganize the queue (heapify-down) after an element is removed
    private void heapifyDown(int index) {
        int leftChild = leftChild(index);
        int rightChild = rightChild(index);
        int smallest = index;

        if (leftChild < size && values[leftChild] < values[smallest]) {
            smallest = leftChild;
        }
        if (rightChild < size && values[rightChild] < values[smallest]) {
            smallest = rightChild;
        }
        if (smallest != index) {
            swap(index, smallest);
            heapifyDown(smallest);
        }
    }

    // Helper function to swap elements at two indices
    private void swap(int i, int j) {
        int tempIndex = indices[i];
        double tempValue = values[i];
        indices[i] = indices[j];
        values[i] = values[j];
        indices[j] = tempIndex;
        values[j] = tempValue;
    }

    // Helper function to get the parent index of a given index
    private int parent(int index) {
        return (index - 1) / 2;
    }

    // Helper function to get the left child index of a given index
    private int leftChild(int index) {
        return 2 * index + 1;
    }

    // Helper function to get the right child index of a given index
    private int rightChild(int index) {
        return 2 * index + 2;
    }

    // Checks if the queue is empty
    public boolean isEmpty() {
        return size == 0;
    }

    // Gets the current size of the queue
    public int size() {
        return size;
    }
}
