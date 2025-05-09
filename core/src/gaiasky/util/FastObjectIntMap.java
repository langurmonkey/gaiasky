/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

/**
 * A high-performance map implementation that stores object keys and unboxed int values.
 * This implementation uses open addressing with linear probing to reduce memory overhead
 * and avoid boxing of integer values.
 *
 * @param <K> the type of keys maintained by this map
 */
public class FastObjectIntMap<K> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    private K[] keys;
    private int[] values;
    private int size;
    private int capacity;
    private int threshold;
    private final Class<K> clazz;

    /**
     * Constructs a new map with the specified initial capacity and key class.
     *
     * @param initialCapacity the initial capacity
     * @param c the class of the keys
     */
    public FastObjectIntMap(int initialCapacity, Class<K> c) {
        clazz = c;
        capacity = Math.max(DEFAULT_CAPACITY, initialCapacity);
        keys = (K[]) Array.newInstance(clazz, capacity);
        values = new int[capacity];
        Arrays.fill(keys, null);
        threshold = (int) (capacity * LOAD_FACTOR);
        size = 0;
    }

    /**
     * Returns the value associated with the specified key, or 0 if not found.
     *
     * @param key the key
     * @return the associated value, or 0 if the key is not present
     */
    public int get(K key) {
        int index = indexOf(key);
        return index != -1 ? values[index] : 0;
    }

    /**
     * Associates the specified value with the specified key in the map.
     * If the key already exists, its value will be overwritten.
     *
     * @param key the key
     * @param value the value
     */
    public void put(K key, int value) {
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
    public boolean containsKey(K key) {
        return indexOf(key) != -1;
    }

    /**
     * Returns an array of all keys in the map. This is the internal array, so it is subject to change.
     * Also, it may contain null values
     *
     * @return an array containing all keys in the map
     */
    public K[] keys() {
        return keys;
    }

    /**
     * Returns the number of key-value mappings in the map.
     *
     * @return the size of the map
     */
    public int size() {
        return size;
    }

    /**
     * Finds the slot index for the given key, using linear probing.
     *
     * @param key the key
     * @return the index for the key
     */
    private int findSlot(K key) {
        int hash = hash(key);
        int index = hash % capacity;

        while (keys[index] != null && !Objects.equals(keys[index], key)) {
            index = (index + 1) % capacity;
        }

        return index;
    }

    /**
     * Finds the index of the given key in the map.
     *
     * @param key the key
     * @return the index of the key, or -1 if not found
     */
    private int indexOf(K key) {
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

    /**
     * Resizes the map to the specified new capacity.
     *
     * @param newCapacity the new capacity
     */
    private void resize(int newCapacity) {
        K[] oldKeys = keys;
        int[] oldValues = values;
        capacity = newCapacity;
        threshold = (int) (capacity * LOAD_FACTOR);
        keys = (K[]) Array.newInstance(clazz, capacity);
        values = new int[capacity];
        Arrays.fill(keys, null);
        size = 0;

        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != null) {
                put(oldKeys[i], oldValues[i]);
            }
        }
    }

    /**
     * Computes the hash for the given key.
     *
     * @param key the key
     * @return the hash value
     */
    private int hash(Object key) {
        return (key == null) ? 0 : key.hashCode() & 0x7FFFFFFF;
    }
}
