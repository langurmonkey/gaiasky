/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple two-way hashmap implemented with two maps.
 *
 * @param <K> Key type in forward map, value in backward map
 * @param <V> Value type in forward map, key in backward map
 */
public class TwoWayHashmap<K extends Object, V extends Object> {

    private final Map<K, V> forward = new HashMap<K, V>();
    private final Map<V, K> backward = new HashMap<V, K>();

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

    public synchronized boolean removeKey(K key) {
        V value = forward.get(key);
        return remove(key, value);
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
