/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.StringBuilder;
import net.jafama.FastMath;

import java.util.Arrays;

/**
 * A wrapper around a double array to make it resizable and optionally ordered.
 */
public class DoubleArray {
    public double[] items;
    public int size;
    public boolean ordered;

    /** Creates an ordered array with a capacity of 16. */
    public DoubleArray() {
        this(true, 16);
    }

    /** Creates an ordered array with the specified capacity. */
    public DoubleArray(int capacity) {
        this(true, capacity);
    }

    /**
     * @param ordered  If false, methods that remove elements may change the order of other elements in the array, which avoids a
     *                 memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown.
     */
    public DoubleArray(boolean ordered, int capacity) {
        this.ordered = ordered;
        items = new double[capacity];
    }

    /**
     * Creates a new array containing the elements in the specific array. The new array will be ordered if the specific array is
     * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
     * grown.
     */
    public DoubleArray(DoubleArray array) {
        this.ordered = array.ordered;
        size = array.size;
        items = new double[size];
        System.arraycopy(array.items, 0, items, 0, size);
    }

    /**
     * Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
     * so any subsequent elements added will cause the backing array to be grown.
     */
    public DoubleArray(double[] array) {
        this(true, array, 0, array.length);
    }

    /**
     * Creates a new array containing the elements in the specified array. The capacity is set to the number of elements, so any
     * subsequent elements added will cause the backing array to be grown.
     *
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     *                memory copy.
     */
    public DoubleArray(boolean ordered, double[] array, int startIndex, int count) {
        this(ordered, count);
        size = count;
        System.arraycopy(array, startIndex, items, 0, count);
    }

    /** @see #DoubleArray(double[]) */
    static public DoubleArray with(double... array) {
        return new DoubleArray(array);
    }

    public void add(double value) {
        double[] items = this.items;
        if (size == items.length)
            items = resize(Math.max(8, (int) (size * 1.75)));
        items[size++] = value;
    }

    public void add(double value1, double value2) {
        double[] items = this.items;
        if (size + 1 >= items.length)
            items = resize(Math.max(8, (int) (size * 1.75)));
        items[size] = value1;
        items[size + 1] = value2;
        size += 2;
    }

    public void add(double value1, double value2, double value3) {
        double[] items = this.items;
        if (size + 2 >= items.length)
            items = resize(Math.max(8, (int) (size * 1.75)));
        items[size] = value1;
        items[size + 1] = value2;
        items[size + 2] = value3;
        size += 3;
    }

    public void add(double value1, double value2, double value3, double value4) {
        double[] items = this.items;
        if (size + 3 >= items.length)
            items = resize(Math.max(8, (int) (size * 1.8))); // 1.75 isn't enough when size=5.
        items[size] = value1;
        items[size + 1] = value2;
        items[size + 2] = value3;
        items[size + 3] = value4;
        size += 4;
    }

    public void addAll(DoubleArray array) {
        addAll(array.items, 0, array.size);
    }

    public void addAll(DoubleArray array, int offset, int length) {
        if (offset + length > array.size)
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
        addAll(array.items, offset, length);
    }

    public void addAll(double... array) {
        addAll(array, 0, array.length);
    }

    public void addAll(double[] array, int offset, int length) {
        double[] items = this.items;
        int sizeNeeded = size + length;
        if (sizeNeeded > items.length)
            items = resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75)));
        System.arraycopy(array, offset, items, size, length);
        size += length;
    }

    public double get(int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        return items[index];
    }

    public void set(int index, double value) {
        if (index >= size)
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        items[index] = value;
    }

    public void incr(int index, double value) {
        if (index >= size)
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        items[index] += value;
    }

    public void incr(double value) {
        double[] items = this.items;
        for (int i = 0, n = size; i < n; i++)
            items[i] += value;
    }

    public void mul(int index, double value) {
        if (index >= size)
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        items[index] *= value;
    }

    public void mul(double value) {
        double[] items = this.items;
        for (int i = 0, n = size; i < n; i++)
            items[i] *= value;
    }

    public void insert(int index, double value) {
        if (index > size)
            throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
        double[] items = this.items;
        if (size == items.length)
            items = resize(Math.max(8, (int) (size * 1.75)));
        if (ordered)
            System.arraycopy(items, index, items, index + 1, size - index);
        else
            items[size] = items[index];
        size++;
        items[index] = value;
    }

    /**
     * Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
     * indices before the insertion.
     */
    public void insertRange(int index, int count) {
        if (index > size)
            throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
        int sizeNeeded = size + count;
        if (sizeNeeded > items.length)
            items = resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75)));
        System.arraycopy(items, index, items, index + count, size - index);
        size = sizeNeeded;
    }

    public void swap(int first, int second) {
        if (first >= size)
            throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size);
        if (second >= size)
            throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size);
        double[] items = this.items;
        double firstValue = items[first];
        items[first] = items[second];
        items[second] = firstValue;
    }

    public boolean contains(double value) {
        int i = size - 1;
        double[] items = this.items;
        while (i >= 0)
            if (items[i--] == value)
                return true;
        return false;
    }

    public int indexOf(double value) {
        double[] items = this.items;
        for (int i = 0, n = size; i < n; i++)
            if (items[i] == value)
                return i;
        return -1;
    }

    public int lastIndexOf(double value) {
        double[] items = this.items;
        for (int i = size - 1; i >= 0; i--)
            if (items[i] == value)
                return i;
        return -1;
    }

    public boolean removeValue(double value) {
        double[] items = this.items;
        for (int i = 0, n = size; i < n; i++) {
            if (items[i] == value) {
                removeIndex(i);
                return true;
            }
        }
        return false;
    }

    /** Removes and returns the item at the specified index. */
    public void removeIndex(int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        double[] items = this.items;
        size--;
        if (ordered)
            System.arraycopy(items, index + 1, items, index, size - index);
        else
            items[index] = items[size];
    }

    /** Removes the items between the specified indices, inclusive. */
    public void removeRange(int start, int end) {
        int n = size;
        if (end >= n)
            throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + size);
        if (start > end)
            throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);
        int count = end - start + 1, lastIndex = n - count;
        if (ordered)
            System.arraycopy(items, start + count, items, start, n - (start + count));
        else {
            int i = FastMath.max(lastIndex, end + 1);
            System.arraycopy(items, i, items, start, n - i);
        }
        size = n - count;
    }

    /**
     * Removes from this array all the elements contained in the specified array.
     *
     * @return true if this array was modified.
     */
    public boolean removeAll(DoubleArray array) {
        int size = this.size;
        int startSize = size;
        double[] items = this.items;
        for (int i = 0, n = array.size; i < n; i++) {
            double item = array.get(i);
            for (int ii = 0; ii < size; ii++) {
                if (item == items[ii]) {
                    removeIndex(ii);
                    size--;
                    break;
                }
            }
        }
        return size != startSize;
    }

    /** Removes and returns the last item. */
    public double pop() {
        return items[--size];
    }

    /** Returns the last item. */
    public double peek() {
        return items[size - 1];
    }

    /** Returns the first item. */
    public double first() {
        if (size == 0)
            throw new IllegalStateException("Array is empty.");
        return items[0];
    }

    /** Returns true if the array has one or more items. */
    public boolean notEmpty() {
        return size > 0;
    }

    /** Returns true if the array is empty. */
    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    /**
     * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     *
     * @return {@link #items}
     */
    public double[] shrink() {
        if (items.length != size)
            resize(size);
        return items;
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     *
     * @return {@link #items}
     */
    public double[] ensureCapacity(int additionalCapacity) {
        if (additionalCapacity < 0)
            throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
        int sizeNeeded = size + additionalCapacity;
        if (sizeNeeded > items.length)
            resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75)));
        return items;
    }

    /**
     * Sets the array size, leaving any values beyond the current size undefined.
     *
     * @return {@link #items}
     */
    public double[] setSize(int newSize) {
        if (newSize < 0)
            throw new IllegalArgumentException("newSize must be >= 0: " + newSize);
        if (newSize > items.length)
            resize(Math.max(8, newSize));
        size = newSize;
        return items;
    }

    protected double[] resize(int newSize) {
        double[] newItems = new double[newSize];
        double[] items = this.items;
        System.arraycopy(items, 0, newItems, 0, FastMath.min(size, newItems.length));
        this.items = newItems;
        return newItems;
    }

    public void sort() {
        Arrays.sort(items, 0, size);
    }

    public void reverse() {
        double[] items = this.items;
        for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
            int ii = lastIndex - i;
            double temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void shuffle() {
        double[] items = this.items;
        for (int i = size - 1; i >= 0; i--) {
            int ii = MathUtils.random(i);
            double temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    /**
     * Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
     * taken.
     */
    public void truncate(int newSize) {
        if (size > newSize)
            size = newSize;
    }

    /** Returns a random item from the array, or zero if the array is empty. */
    public double random() {
        if (size == 0)
            return 0;
        return items[MathUtils.random(0, size - 1)];
    }

    public double[] toArray() {
        double[] array = new double[size];
        System.arraycopy(items, 0, array, 0, size);
        return array;
    }

    public int hashCode() {
        if (!ordered)
            return super.hashCode();
        double[] items = this.items;
        int h = 1;
        for (int i = 0, n = size; i < n; i++)
            h = h * 31 + NumberUtils.floatToRawIntBits((float) items[i]);
        return h;
    }

    /** Returns false if either array is unordered. */
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!ordered)
            return false;
        if (!(object instanceof DoubleArray array))
            return false;
        if (!array.ordered)
            return false;
        int n = size;
        if (n != array.size)
            return false;
        double[] items1 = this.items, items2 = array.items;
        for (int i = 0; i < n; i++)
            if (items1[i] != items2[i])
                return false;
        return true;
    }

    /** Returns false if either array is unordered. */
    public boolean equals(Object object, double epsilon) {
        if (object == this)
            return true;
        if (!(object instanceof DoubleArray array))
            return false;
        int n = size;
        if (n != array.size)
            return false;
        if (!ordered)
            return false;
        if (!array.ordered)
            return false;
        double[] items1 = this.items, items2 = array.items;
        for (int i = 0; i < n; i++)
            if (Math.abs(items1[i] - items2[i]) > epsilon)
                return false;
        return true;
    }

    public String toString() {
        if (size == 0)
            return "[]";
        double[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(items[0]);
        for (int i = 1; i < size; i++) {
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    public String toString(String separator) {
        if (size == 0)
            return "";
        double[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append(items[0]);
        for (int i = 1; i < size; i++) {
            buffer.append(separator);
            buffer.append(items[i]);
        }
        return buffer.toString();
    }
}
