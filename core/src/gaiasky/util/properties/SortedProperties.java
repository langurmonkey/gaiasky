/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.properties;

import java.util.*;

public class SortedProperties extends Properties {
    // Return sorted key set
    public Enumeration<Object> keys() {
        Enumeration<Object> keysEnum = super.keys();
        Vector<Object> keyList = new Vector<>();
        while (keysEnum.hasMoreElements()) {
            keyList.add(keysEnum.nextElement());
        }
        keyList.sort(Comparator.comparing(a -> ((String) a)));
        return keyList.elements();
    }

    // Return sorted entry set
    public Set<Map.Entry<Object, Object>> entrySet() {
        Enumeration<Object> keys = this.keys();
        Set<Map.Entry<Object, Object>> entrySet = new TreeSet<>();
        Iterator<Object> it = keys.asIterator();
        while (it.hasNext()) {
            Object key = it.next();
            entrySet.add(new CompEntry<>(key, this.get(key)));
        }
        return Collections.synchronizedSet(entrySet);
    }

    private static class CompEntry<K, V> extends AbstractMap.SimpleEntry<K, V> implements Comparable<CompEntry<K, V>> {

        public CompEntry(K a, V b) {
            super(a, b);
        }

        @Override
        public int compareTo(CompEntry o) {
            return ((String) this.getKey()).compareTo((String) o.getKey());
        }
    }
}
