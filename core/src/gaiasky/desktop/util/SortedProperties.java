package gaiasky.desktop.util;

import java.util.*;

/**
 * Properties which are sorted by key
 */
public class SortedProperties extends Properties {
    // Return sorted key set
    public Enumeration keys() {
        Enumeration keysEnum = super.keys();
        Vector<String> keyList = new Vector<>();
        while (keysEnum.hasMoreElements()) {
            keyList.add((String) keysEnum.nextElement());
        }
        Collections.sort(keyList);
        return keyList.elements();
    }

    // Return sorted entry set
    public Set<Map.Entry<Object, Object>> entrySet() {
        Enumeration keys = this.keys();
        Set<Map.Entry<Object, Object>> entrySet = new TreeSet<>();
        Iterator it = keys.asIterator();
        while(it.hasNext()){
            Object key = it.next();
            entrySet.add(new CompEntry<>(key, this.get(key)));
        }
        return Collections.synchronizedSet(entrySet);
    }

    private class CompEntry<K, V> extends AbstractMap.SimpleEntry<K, V> implements Comparable{

        public CompEntry(K a, V b) {
            super(a, b);
        }

        @Override
        public int compareTo(Object o) {
            if(o instanceof CompEntry){
                return ((String)this.getKey()).compareTo((String) ((CompEntry) o).getKey());
            }
            return -1;
        }
    }
}
