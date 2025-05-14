/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.utils.LongMap;

/**
 * A fixed-capacity cache that uses a least-recently-used (LRU) eviction policy.
 * This implementation uses primitive long keys to avoid boxing overhead.
 *
 * @param <V> The type of values stored in the cache.
 */
public class LruCacheLong<V> {
    private static class Node<V> {
        long key;
        V value;
        Node<V> prev, next;

        /**
         * Node class representing a doubly linked list entry for the LRU cache.
         * Each node holds a long key and a value of type V.
         */
        Node(long key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final LongMap<Node<V>> map;
    private Node<V> head, tail;

    /**
     * Constructs an LRU cache with the specified capacity.
     *
     * @param capacity the maximum number of entries in the cache
     */
    public LruCacheLong(int capacity) {
        this.capacity = capacity;
        this.map = new LongMap<>(capacity);
    }


    /**
     * Retrieves the least recently used entry without removing it.
     *
     * @return the value of the least recently used entry, or null if the cache is empty
     */
    public V getLeastRecentlyUsed() {
        return tail != null ? tail.value : null;
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     *
     * @return the value associated with the specified key, or null if not found
     */
    public V get(long key) {
        Node<V> node = map.get(key);
        if (node == null) return null;
        moveToHead(node);
        return node.value;
    }

    /**
     * Adds a key-value pair to the cache or updates the value if the key already exists.
     *
     * @param key   the key
     * @param value the value
     */
    public void put(long key, V value) {
        Node<V> node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
        } else {
            Node<V> newNode = new Node<>(key, value);
            map.put(key, newNode);
            addNode(newNode);
            if (map.size > capacity) {
                Node<V> tailNode = popTail();
                map.remove(tailNode.key);
            }
        }
    }

    /**
     * Checks if the cache contains the specified key.
     *
     * @param key the key to check
     *
     * @return true if the key is present, false otherwise
     */
    public boolean containsKey(long key) {
        return map.containsKey(key);
    }

    /**
     * Removes the entry associated with the specified key.
     *
     * @param key the key to remove
     *
     * @return the removed value, or null if the key was not present
     */
    public V remove(long key) {
        Node<V> node = map.remove(key);
        if (node == null) return null;
        removeNode(node);
        return node.value;
    }

    /**
     * Returns the current number of entries in the cache.
     *
     * @return the size of the cache
     */
    public int size() {
        return map.size;
    }

    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        map.clear();
        head = tail = null;
    }

    private void addNode(Node<V> node) {
        node.next = head;
        node.prev = null;
        if (head != null) head.prev = node;
        head = node;
        if (tail == null) tail = node;
    }

    private void removeNode(Node<V> node) {
        if (node.prev != null) node.prev.next = node.next;
        else head = node.next;
        if (node.next != null) node.next.prev = node.prev;
        else tail = node.prev;
    }

    private void moveToHead(Node<V> node) {
        removeNode(node);
        addNode(node);
    }

    private Node<V> popTail() {
        Node<V> node = tail;
        removeNode(node);
        return node;
    }
}
