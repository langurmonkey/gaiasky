/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.event;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectSet;

/**
 * Simple implementation of a priority queue.
 * @param <E> Object type.
 */
public class PriorityQueue<E extends Comparable<E>> {

    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    private static final double CAPACITY_RATIO_LOW = 1.5f;

    private static final double CAPACITY_RATIO_HI = 2f;
    /** A set used to check elements'uniqueness (if enabled). */
    private final ObjectSet<E> set;
    /**
     * Priority queue represented as a balanced binary heap: the two children of queue[n] are queue[2*n+1] and queue[2*(n+1)]. The
     * priority queue is ordered by the elements' natural ordering: For each node n in the heap and each descendant d of n, n <= d.
     * The element with the lowest value is in queue[0], assuming the queue is nonempty.
     */
    private Object[] queue;
    /** A flag indicating whether elements inserted into the queue must be unique. */
    private boolean uniqueness;

    /** The number of elements in the priority queue. */
    private int size = 0;

    /**
     * Creates a {@code PriorityQueue} with the default initial capacity (11) that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     */
    public PriorityQueue() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a {@code PriorityQueue} with the specified initial capacity that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     *
     * @param initialCapacity the initial capacity for this priority queue
     */
    public PriorityQueue(int initialCapacity) {
        this.queue = new Object[initialCapacity];
        this.set = new ObjectSet<>(initialCapacity);
    }

    /** Returns a value indicating whether only unique elements are allowed to be inserted. */
    public boolean getUniqueness() {
        return uniqueness;
    }

    /** Sets a flag indicating whether only unique elements are allowed to be inserted. */
    public void setUniqueness(boolean uniqueness) {
        this.uniqueness = uniqueness;
    }

    /**
     * Inserts the specified element into this priority queue. If {@code uniqueness} is enabled and this priority queue already
     * contains the element, the call leaves the queue unchanged and returns false.
     *
     * @return true if the element was added to this queue, else false
     *
     * @throws ClassCastException   if the specified element cannot be compared with elements currently in this priority queue
     *                              according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        if (e == null)
            throw new NullPointerException();
        if (uniqueness && !set.add(e))
            return false;
        int i = size;
        if (i >= queue.length)
            growToSize(i + 1);
        size = i + 1;
        if (i == 0)
            queue[0] = e;
        else
            siftUp(i, e);
        return true;
    }

    /**
     * Retrieves, but does not remove, the head of this queue. If this queue is empty {@code null} is returned.
     *
     * @return the head of this queue
     */
    public E peek() {
        return size == 0 ? null : (E) queue[0];
    }

    /**
     * Retrieves the element at the specified index. If such an element doesn't exist {@code null} is returned.
     * <p>
     * Iterating the queue by index is <em>not</em> guaranteed to traverse the elements in any particular order.
     *
     * @return the element at the specified index in this queue.
     */
    public E get(int index) {
        return index >= size ? null : (E) queue[index];
    }

    /** Returns the number of elements in this queue. */
    public int size() {
        return size;
    }

    /** Removes all of the elements from this priority queue. The queue will be empty after this call returns. */
    public void clear() {
        for (int i = 0; i < size; i++)
            queue[i] = null;
        size = 0;
        set.clear();
    }

    /**
     * Retrieves and removes the head of this queue, or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty.
     */
    public E poll() {
        if (size == 0)
            return null;
        int s = --size;
        E result = (E) queue[0];
        E x = (E) queue[s];
        queue[s] = null;
        if (s != 0)
            siftDown(0, x);
        if (uniqueness)
            set.remove(result);
        return result;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by promoting x up the tree until it is greater than or equal to its
     * parent, or is the root.
     *
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftUp(int k, E x) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            E e = (E) queue[parent];
            if (x.compareTo(e) >= 0)
                break;
            queue[k] = e;
            k = parent;
        }
        queue[k] = x;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     *
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftDown(int k, E x) {
        int half = size >>> 1; // loop while a non-leaf
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least
            E c = (E) queue[child];
            int right = child + 1;
            if (right < size && c.compareTo((E) queue[right]) > 0)
                c = (E) queue[child = right];
            if (x.compareTo(c) <= 0)
                break;
            queue[k] = c;
            k = child;
        }
        queue[k] = x;
    }

    /**
     * Increases the capacity of the array.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void growToSize(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new GdxRuntimeException("Capacity upper limit exceeded.");
        int oldCapacity = queue.length;
        // Double size if small; else grow by 50%
        int newCapacity = (int) ((oldCapacity < 64) ? ((oldCapacity + 1) * CAPACITY_RATIO_HI) : (oldCapacity * CAPACITY_RATIO_LOW));
        if (newCapacity < 0) // overflow
            newCapacity = Integer.MAX_VALUE;
        if (newCapacity < minCapacity)
            newCapacity = minCapacity;
        Object[] newQueue = new Object[newCapacity];
        System.arraycopy(queue, 0, newQueue, 0, size);
        queue = newQueue;
    }

}
