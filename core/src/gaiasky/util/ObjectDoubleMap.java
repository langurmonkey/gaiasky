/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.*;
import net.jafama.FastMath;

import java.lang.StringBuilder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Fast implementation of map where keys are generic {@link Object}s of type {@link K} and values are double-precision integers.
 * Tries to avoid allocations as much as possible.
 *
 * @param <K> Key object type.
 */
public class ObjectDoubleMap<K> implements Iterable<ObjectDoubleMap.Entry<K>> {
    public int size;
    /**
     * Used by {@link #place(Object)} to bit shift the upper bits of a {@code long} into a usable range (&gt;= 0 and &lt;=
     * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
     * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
     * which, if used with an int, will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
     * shifts.
     * <p>
     * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hash codes, if
     * {@link #place(Object)} is overridden.
     */
    protected int shift;
    /**
     * A bitmask used to confine hash codes to the size of the table. Must be all 1 bit in its low positions, ie a power of two
     * minus 1. If {@link #place(Object)} is overwritten, this can be used instead of {@link #shift} to isolate usable bits of a
     * hash.
     */
    protected int mask;
    K[] keyTable;
    double[] valueTable;
    float loadFactor;
    int threshold;
    Entries entries1, entries2;
    Values values1, values2;
    Keys keys1, keys2;

    /** Creates a new map with an initial capacity of 51 and a load factor of 0.8. */
    public ObjectDoubleMap() {
        this(51, 0.8f);
    }

    /**
     * Creates a new map with a load factor of 0.8.
     *
     * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
     */
    public ObjectDoubleMap(int initialCapacity) {
        this(initialCapacity, 0.8f);
    }

    /**
     * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
     * growing the backing table.
     *
     * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
     */
    public ObjectDoubleMap(int initialCapacity, float loadFactor) {
        if (loadFactor <= 0f || loadFactor >= 1f)
            throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
        this.loadFactor = loadFactor;

        int tableSize = tableSize(initialCapacity, loadFactor);
        threshold = (int) (tableSize * loadFactor);
        mask = tableSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        keyTable = (K[]) new Object[tableSize];
        valueTable = new double[tableSize];
    }

    /** Creates a new map identical to the specified map. */
    public ObjectDoubleMap(ObjectDoubleMap<? extends K> map) {
        this((int) FastMath.floor(map.keyTable.length * map.loadFactor), map.loadFactor);
        System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
        System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
        size = map.size;
    }

    static int tableSize(int capacity, float loadFactor) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0: " + capacity);
        } else {
            int tableSize = MathUtils.nextPowerOfTwo(Math.max(2, (int) FastMath.ceil((float) capacity / loadFactor)));
            if (tableSize > 1073741824) {
                throw new IllegalArgumentException("The required capacity is too large: " + capacity);
            } else {
                return tableSize;
            }
        }
    }

    /**
     * Returns an index &ge; 0 and &le; {@link #mask} for the specified {@code item}.
     * <p>
     * The default implementation uses Fibonacci hashing on the item's {@link Object#hashCode()}: the hashcode is multiplied by a
     * long constant (2 to the 64th, divided by the golden ratio) then the uppermost bits are shifted into the lowest positions to
     * obtain an index in the desired range. Multiplication by a long may be slower than int (e.g., on GWT) but greatly improves
     * rehashing, allowing even very poor hash codes, such as those that only differ in their upper bits, to be used without high
     * collision rates. Fibonacci hashing has increased collision rates when all or most hash codes are multiples of larger
     * Fibonacci numbers (see <a href=
     * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
     * Skarupke's blog post</a>).
     * <p>
     * This method can be overridden to customizing hashing. This may be useful eg in the unlikely event that most hash codes are
     * Fibonacci numbers, if keys provide poor or incorrect hash codes, or to simplify hashing if keys provide high quality
     * hash codes and don't need Fibonacci hashing: {@code return item.hashCode() & mask;}
     */
    protected int place(K item) {
        return (int) (item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
    }

    /**
     * Returns the index of the key if already present, else -(index + 1) for the next empty index. This can be overridden in this
     * package to compare for equality differently than {@link Object#equals(Object)}.
     */
    int locateKey(K key) {
        if (key == null)
            throw new IllegalArgumentException("key cannot be null.");
        K[] keyTable = this.keyTable;
        for (int i = place(key); ; i = i + 1 & mask) {
            K other = keyTable[i];
            if (other == null)
                return -(i + 1); // Empty space is available.
            if (other.equals(key))
                return i; // Same key was found.
        }
    }

    public void put(K key, double value) {
        int i = locateKey(key);
        if (i >= 0) { // Existing key was found.
            valueTable[i] = value;
            return;
        }
        i = -(i + 1); // Empty space was found.
        keyTable[i] = key;
        valueTable[i] = value;
        if (++size >= threshold)
            resize(keyTable.length << 1);
    }

    /**
     * Returns the old value associated with the specified key, or the specified default value.
     *
     * @param defaultValue {@link Double#NaN} can be used for a value unlikely to be in the map.
     */
    public double put(K key, double value, double defaultValue) {
        int i = locateKey(key);
        if (i >= 0) { // Existing key was found.
            double oldValue = valueTable[i];
            valueTable[i] = value;
            return oldValue;
        }
        i = -(i + 1); // Empty space was found.
        keyTable[i] = key;
        valueTable[i] = value;
        if (++size >= threshold)
            resize(keyTable.length << 1);
        return defaultValue;
    }

    public void putAll(ObjectDoubleMap<? extends K> map) {
        ensureCapacity(map.size);
        K[] keyTable = map.keyTable;
        double[] valueTable = map.valueTable;
        K key;
        for (int i = 0, n = keyTable.length; i < n; i++) {
            key = keyTable[i];
            if (key != null)
                put(key, valueTable[i]);
        }
    }

    /** Skips checks for existing keys, doesn't increment size. */
    private void putResize(K key, double value) {
        K[] keyTable = this.keyTable;
        for (int i = place(key); ; i = (i + 1) & mask) {
            if (keyTable[i] == null) {
                keyTable[i] = key;
                valueTable[i] = value;
                return;
            }
        }
    }

    /**
     * Returns the value for the specified key, or the default value if the key is not in the map.
     *
     * @param defaultValue {@link Double#NaN} can be used for a value unlikely to be in the map.
     */
    public double get(K key, double defaultValue) {
        int i = locateKey(key);
        return i < 0 ? defaultValue : valueTable[i];
    }

    /**
     * Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
     * put into the map and defaultValue is returned.
     */
    public double getAndIncrement(K key, double defaultValue, double increment) {
        int i = locateKey(key);
        if (i >= 0) { // Existing key was found.
            double oldValue = valueTable[i];
            valueTable[i] += increment;
            return oldValue;
        }
        i = -(i + 1); // Empty space was found.
        keyTable[i] = key;
        valueTable[i] = defaultValue + increment;
        if (++size >= threshold)
            resize(keyTable.length << 1);
        return defaultValue;
    }

    /**
     * Returns the value for the removed key, or the default value if the key is not in the map.
     *
     * @param defaultValue {@link Double#NaN} can be used for a value unlikely to be in the map.
     */
    public double remove(K key, double defaultValue) {
        int i = locateKey(key);
        if (i < 0)
            return defaultValue;
        K[] keyTable = this.keyTable;
        double[] valueTable = this.valueTable;
        double oldValue = valueTable[i];
        int mask = this.mask, next = i + 1 & mask;
        while ((key = keyTable[next]) != null) {
            int placement = place(key);
            if ((next - placement & mask) > (i - placement & mask)) {
                keyTable[i] = key;
                valueTable[i] = valueTable[next];
                i = next;
            }
            next = next + 1 & mask;
        }
        keyTable[i] = null;
        size--;
        return oldValue;
    }

    /** Returns true if the map has one or more items. */
    public boolean notEmpty() {
        return size > 0;
    }

    /** Returns true if the map is empty. */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
     * nothing is done. If the map contains more items than the specified capacity, the next highest power of two capacity is used
     * instead.
     */
    public void shrink(int maximumCapacity) {
        if (maximumCapacity < 0)
            throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if (keyTable.length > tableSize)
            resize(tableSize);
    }

    /** Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger. */
    public void clear(int maximumCapacity) {
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if (keyTable.length <= tableSize) {
            clear();
            return;
        }
        size = 0;
        resize(tableSize);
    }

    public void clear() {
        if (size == 0)
            return;
        size = 0;
        Arrays.fill(keyTable, null);
    }

    /**
     * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
     * be an expensive operation.
     */
    public boolean containsValue(double value) {
        K[] keyTable = this.keyTable;
        double[] valueTable = this.valueTable;
        for (int i = valueTable.length - 1; i >= 0; i--)
            if (keyTable[i] != null && valueTable[i] == value)
                return true;
        return false;
    }

    /**
     * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
     * be an expensive operation.
     */
    public boolean containsValue(double value, double epsilon) {
        K[] keyTable = this.keyTable;
        double[] valueTable = this.valueTable;
        for (int i = valueTable.length - 1; i >= 0; i--)
            if (keyTable[i] != null && FastMath.abs(valueTable[i] - value) <= epsilon)
                return true;
        return false;
    }

    public boolean containsKey(K key) {
        return locateKey(key) >= 0;
    }

    /**
     * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
     * every value, which may be an expensive operation.
     */
    public @Null
    K findKey(double value) {
        K[] keyTable = this.keyTable;
        double[] valueTable = this.valueTable;
        for (int i = valueTable.length - 1; i >= 0; i--) {
            K key = keyTable[i];
            if (key != null && valueTable[i] == value)
                return key;
        }
        return null;
    }

    /**
     * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
     * every value, which may be an expensive operation.
     */
    public @Null
    K findKey(double value, double epsilon) {
        K[] keyTable = this.keyTable;
        double[] valueTable = this.valueTable;
        for (int i = valueTable.length - 1; i >= 0; i--) {
            K key = keyTable[i];
            if (key != null && FastMath.abs(valueTable[i] - value) <= epsilon)
                return key;
        }
        return null;
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
     * adding many items to avoid multiple backing array resizes.
     */
    public void ensureCapacity(int additionalCapacity) {
        int tableSize = tableSize(size + additionalCapacity, loadFactor);
        if (keyTable.length < tableSize)
            resize(tableSize);
    }

    final void resize(int newSize) {
        int oldCapacity = keyTable.length;
        threshold = (int) (newSize * loadFactor);
        mask = newSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        K[] oldKeyTable = keyTable;
        double[] oldValueTable = valueTable;

        keyTable = (K[]) new Object[newSize];
        valueTable = new double[newSize];

        if (size > 0) {
            for (int i = 0; i < oldCapacity; i++) {
                K key = oldKeyTable[i];
                if (key != null)
                    putResize(key, oldValueTable[i]);
            }
        }
    }

    public int hashCode() {
        int h = size;
        K[] keyTable = this.keyTable;
        double[] valueTable = this.valueTable;
        for (int i = 0, n = keyTable.length; i < n; i++) {
            K key = keyTable[i];
            if (key != null)
                h += key.hashCode() + NumberUtils.floatToRawIntBits((float) valueTable[i]);
        }
        return h;
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof ObjectDoubleMap))
            return false;
        ObjectDoubleMap other = (ObjectDoubleMap) obj;
        if (other.size != size)
            return false;
        K[] keyTable = this.keyTable;
        double[] valueTable = this.valueTable;
        for (int i = 0, n = keyTable.length; i < n; i++) {
            K key = keyTable[i];
            if (key != null) {
                double otherValue = other.get(key, 0);
                if (otherValue == 0 && !other.containsKey(key))
                    return false;
                if (otherValue != valueTable[i])
                    return false;
            }
        }
        return true;
    }

    public String toString(String separator) {
        return toString(separator, false);
    }

    public String toString() {
        return toString(", ", true);
    }

    private String toString(String separator, boolean braces) {
        if (size == 0)
            return braces ? "{}" : "";
        StringBuilder buffer = new StringBuilder(32);
        if (braces)
            buffer.append('{');
        K[] keyTable = this.keyTable;
        double[] valueTable = this.valueTable;
        int i = keyTable.length;
        while (i-- > 0) {
            K key = keyTable[i];
            if (key == null)
                continue;
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
            break;
        }
        while (i-- > 0) {
            K key = keyTable[i];
            if (key == null)
                continue;
            buffer.append(separator);
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
        }
        if (braces)
            buffer.append('}');
        return buffer.toString();
    }

    public Entries<K> iterator() {
        return entries();
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     * <p>
     * If {@link Collections#allocateIterators} is false, the same iterator instance is returned each time this method is called.
     * Use the {@link Entries} constructor for nested or multithreaded iteration.
     */
    public Entries<K> entries() {
        if (Collections.allocateIterators)
            return new Entries(this);
        if (entries1 == null) {
            entries1 = new Entries(this);
            entries2 = new Entries(this);
        }
        if (!entries1.valid) {
            entries1.reset();
            entries1.valid = true;
            entries2.valid = false;
            return entries1;
        }
        entries2.reset();
        entries2.valid = true;
        entries1.valid = false;
        return entries2;
    }

    /**
     * Returns an iterator for the values in the map. Remove is supported.
     * <p>
     * If {@link Collections#allocateIterators} is false, the same iterator instance is returned each time this method is called.
     * Use the {@link Values} constructor for nested or multithreaded iteration.
     */
    public Values values() {
        if (Collections.allocateIterators)
            return new Values(this);
        if (values1 == null) {
            values1 = new Values(this);
            values2 = new Values(this);
        }
        if (!values1.valid) {
            values1.reset();
            values1.valid = true;
            values2.valid = false;
            return values1;
        }
        values2.reset();
        values2.valid = true;
        values1.valid = false;
        return values2;
    }

    /**
     * Returns an iterator for the keys in the map. Remove is supported.
     * <p>
     * If {@link Collections#allocateIterators} is false, the same iterator instance is returned each time this method is called.
     * Use the {@link Keys} constructor for nested or multithreaded iteration.
     */
    public Keys<K> keys() {
        if (Collections.allocateIterators)
            return new Keys(this);
        if (keys1 == null) {
            keys1 = new Keys(this);
            keys2 = new Keys(this);
        }
        if (!keys1.valid) {
            keys1.reset();
            keys1.valid = true;
            keys2.valid = false;
            return keys1;
        }
        keys2.reset();
        keys2.valid = true;
        keys1.valid = false;
        return keys2;
    }

    static public class Entry<K> {
        public K key;
        public double value;

        public String toString() {
            return key + "=" + value;
        }
    }

    static private class MapIterator<K> {
        final ObjectDoubleMap<K> map;
        public boolean hasNext;
        int nextIndex, currentIndex;
        boolean valid = true;

        public MapIterator(ObjectDoubleMap<K> map) {
            this.map = map;
            reset();
        }

        public void reset() {
            currentIndex = -1;
            nextIndex = -1;
            findNextIndex();
        }

        void findNextIndex() {
            K[] keyTable = map.keyTable;
            for (int n = keyTable.length; ++nextIndex < n; ) {
                if (keyTable[nextIndex] != null) {
                    hasNext = true;
                    return;
                }
            }
            hasNext = false;
        }

        public void remove() {
            int i = currentIndex;
            if (i < 0)
                throw new IllegalStateException("next must be called before remove.");
            K[] keyTable = map.keyTable;
            double[] valueTable = map.valueTable;
            int mask = map.mask, next = i + 1 & mask;
            K key;
            while ((key = keyTable[next]) != null) {
                int placement = map.place(key);
                if ((next - placement & mask) > (i - placement & mask)) {
                    keyTable[i] = key;
                    valueTable[i] = valueTable[next];
                    i = next;
                }
                next = next + 1 & mask;
            }
            keyTable[i] = null;
            map.size--;
            if (i != currentIndex)
                --nextIndex;
            currentIndex = -1;
        }
    }

    static public class Entries<K> extends MapIterator<K> implements Iterable<Entry<K>>, Iterator<Entry<K>> {
        Entry<K> entry = new Entry<K>();

        public Entries(ObjectDoubleMap<K> map) {
            super(map);
        }

        /** Note the same entry instance is returned each time this method is called. */
        public Entry<K> next() {
            if (!hasNext)
                throw new NoSuchElementException();
            if (!valid)
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            K[] keyTable = map.keyTable;
            entry.key = keyTable[nextIndex];
            entry.value = map.valueTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return entry;
        }

        public boolean hasNext() {
            if (!valid)
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        public Entries<K> iterator() {
            return this;
        }
    }

    static public class Values extends MapIterator<Object> {
        public Values(ObjectDoubleMap<?> map) {
            super((ObjectDoubleMap<Object>) map);
        }

        public boolean hasNext() {
            if (!valid)
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        public double next() {
            if (!hasNext)
                throw new NoSuchElementException();
            if (!valid)
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            double value = map.valueTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return value;
        }

        public Values iterator() {
            return this;
        }

        /** Returns a new array containing the remaining values. */
        public DoubleArray toArray() {
            DoubleArray array = new DoubleArray(true, map.size);
            while (hasNext)
                array.add(next());
            return array;
        }

        /** Adds the remaining values to the specified array. */
        public DoubleArray toArray(DoubleArray array) {
            while (hasNext)
                array.add(next());
            return array;
        }
    }

    static public class Keys<K> extends MapIterator<K> implements Iterable<K>, Iterator<K> {
        public Keys(ObjectDoubleMap<K> map) {
            super(map);
        }

        public boolean hasNext() {
            if (!valid)
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        public K next() {
            if (!hasNext)
                throw new NoSuchElementException();
            if (!valid)
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            K key = map.keyTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return key;
        }

        public Keys<K> iterator() {
            return this;
        }

        /** Returns a new array containing the remaining keys. */
        public Array<K> toArray() {
            return toArray(new Array<K>(true, map.size));
        }

        /** Adds the remaining keys to the array. */
        public Array<K> toArray(Array<K> array) {
            while (hasNext)
                array.add(next());
            return array;
        }
    }
}
