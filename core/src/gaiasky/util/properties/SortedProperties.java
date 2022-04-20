package gaiasky.util.properties;

import java.util.*;

/**
 * Properties which are sorted by key
 */
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
