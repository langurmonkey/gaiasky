package gaiasky.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

/**
 * A high-performance map implementation that stores String keys and generic Object values.
 * This implementation uses open addressing with linear probing to reduce memory overhead
 * and avoid boxing of integer values.
 *
 * @param <V> the type of values maintained by this map
 */
public class FastStringObjectMap<V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    private String[] keys;
    private V[] values;
    private int size;
    private int capacity;
    private int threshold;
    private final Class<V> clazz;

    /**
     * Constructs a new map with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity
     */
    public FastStringObjectMap(int initialCapacity, Class<V> c) {
        clazz = c;
        capacity = Math.max(DEFAULT_CAPACITY, initialCapacity);
        keys = new String[capacity];
        values = (V[]) Array.newInstance(clazz, capacity);
        Arrays.fill(keys, null);
        threshold = (int) (capacity * LOAD_FACTOR);
        size = 0;
    }

    /**
     * Returns the value associated with the specified key, or null if not found.
     *
     * @param key the key
     * @return the associated value, or null if the key is not present
     */
    public V get(String key) {
        int index = indexOf(key);
        return index != -1 ? (V) values[index] : null;
    }

    /**
     * Associates the specified value with the specified key in the map.
     * If the key already exists, its value will be overwritten.
     *
     * @param key the key
     * @param value the value
     */
    public void put(String key, V value) {
        if (size >= threshold) {
            resize(capacity * 2);
        }
        int index = findSlot(key);
        if (keys[index] == null) {
            keys[index] = key;
            values[index] = value;
            size++;
        } else {
            values[index] = value;
        }
    }


    /**
     * Checks if the map contains the specified key.
     *
     * @param key the key to check
     * @return true if the key is present, false otherwise
     */
    public boolean containsKey(String key) {
        return indexOf(key) != -1;
    }

    /**
     * Returns an array of all keys in the map. This is the internal array, so it is subject to change.
     * Also, it may contain null values
     *
     * @return an array containing all keys in the map
     */
    public String[] keys() {
        return keys;
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key the key whose mapping is to be removed
     * @return the previous value associated with the key, or null if none
     */
    public V remove(String key) {
        int index = indexOf(key);
        if (index == -1) return null;

        V oldValue = (V) values[index];
        keys[index] = null;
        values[index] = null;
        size--;

        // Rehash cluster of entries following the removed entry
        index = (index + 1) % capacity;
        while (keys[index] != null) {
            String rehashKey = keys[index];
            V rehashValue = (V) values[index];
            keys[index] = null;
            values[index] = null;
            size--;
            put(rehashKey, rehashValue);
            index = (index + 1) % capacity;
        }

        return oldValue;
    }


    /**
     * Returns the number of key-value mappings in the map.
     *
     * @return the size of the map
     */
    public int size() {
        return size;
    }

    private int findSlot(String key) {
        int hash = hash(key);
        int index = hash % capacity;

        while (keys[index] != null && !Objects.equals(keys[index], key)) {
            index = (index + 1) % capacity;
        }

        return index;
    }

    private int indexOf(String key) {
        int hash = hash(key);
        int index = hash % capacity;

        while (keys[index] != null) {
            if (Objects.equals(keys[index], key)) {
                return index;
            }
            index = (index + 1) % capacity;
        }

        return -1;
    }

    private void resize(int newCapacity) {
        String[] oldKeys = keys;
        Object[] oldValues = values;
        capacity = newCapacity;
        threshold = (int) (capacity * LOAD_FACTOR);
        keys = new String[capacity];
        values = (V[]) Array.newInstance(clazz, capacity);
        Arrays.fill(keys, null);
        size = 0;

        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != null) {
                put(oldKeys[i], (V) oldValues[i]);
            }
        }
    }

    private int hash(String key) {
        return (key == null) ? 0 : key.hashCode() & 0x7FFFFFFF;
    }
}
