/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A map that allows forward and backward referencing.
 * It is implemented by wrapping two hash maps and keeping them up to date.
 *
 * @param <K> Type of first object.
 * @param <V> Type of second object.
 */
public class TwoWayMap<K, V> {

    private final Map<K, V> forward = new HashMap<>();
    private final Map<V, K> backward = new HashMap<>();

    public synchronized void add(K key, V value) {
        forward.put(key, value);
        backward.put(value, key);
    }

    public synchronized V getForward(K key) {
        return forward.get(key);
    }

    public synchronized K getBackward(V key) {
        return backward.get(key);
    }

    public synchronized boolean containsKey(K key) {
        return forward.containsKey(key);
    }

    public synchronized boolean containsValue(V value) {
        return backward.containsKey(value);
    }

    public synchronized void removeKey(K key) {
        V value = forward.get(key);
        remove(key, value);
    }

    public synchronized boolean removeValue(V value) {
        K key = backward.get(value);
        return remove(key, value);
    }

    private synchronized boolean remove(K key, V value) {
        if (value != null && key != null) {
            forward.remove(key);
            backward.remove(value);
            return true;
        }
        return false;
    }
}
