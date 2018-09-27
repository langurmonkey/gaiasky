package gaia.cu9.ari.gaiaorbit.util.ds;

import java.util.Iterator;

import com.badlogic.gdx.utils.Array;

/**
 * A multiple list, holding a number of indexed lists. Useful for threaded
 * applications. Not all methods of {@link java.util.List} are implemented,
 * check the comments.
 * @deprecated Since the scene graph concurrent is no more, there is no need for this. 
 * @author Toni Sagrista
 *
 * @param <T>
 */
public class Multilist<T> {

    private final Array<T>[] lists;
    private Array<T> tolist;

    /**
     * Creates a multiple list with the given number of lists and an initial
     * capacity of each list of ten.
     * 
     * @param numLists
     *            The number of lists.
     */
    public Multilist(int numLists) {
        this(numLists, 10000);
    }

    /**
     * Creates a multiple list with the given number of lists and initial
     * capacity for each list.
     * 
     * @param numLists
     *            The number of lists.
     * @param initialCapacity
     *            The initial capacity for each list.
     */
    public Multilist(int numLists, int initialCapacity) {
        super();
        lists = new Array[numLists];
        for (int i = 0; i < lists.length; i++) {
            lists[i] = new Array<T>(false, initialCapacity);
        }
        tolist = new Array<T>(false, initialCapacity * numLists);
    }

    /**
     * Converts this multilist to a simple list
     * 
     * @return The array
     */
    public Array<T> toList() {
        tolist.clear();
        int size = lists.length;

        if (size == 1)
            return lists[0];

        for (int i = 0; i < size; i++)
            tolist.addAll(lists[i]);
        return tolist;
    }

    public int size() {
        int size = 0;
        for (int i = 0; i < lists.length; i++)
            size += lists[i].size;
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(Object o) {
        for (int i = 0; i < lists.length; i++) {
            if (lists[i].contains((T) o, true))
                return true;
        }
        return false;
    }

    /**
     * Returns true if this collection contains the specified element in the
     * list identified by the given index. More formally, returns true if and
     * only if the list contains at least one element e such that (o==null ?
     * e==null : o.equals(e)).
     * 
     * @param o
     *            element whose presence in this collection is to be tested
     * @param listIndex
     *            <tt>true</tt> if this collection contains the specified
     *            element.
     * @return Whether this collection contains the element
     */
    public boolean contains(T o, int listIndex) {
        return lists[listIndex].contains(o, true);
    }

    /** Not implemented **/
    public Iterator<T> iterator() {
        return new MultilistIterator<T>();
    }

    public boolean add(T e) {
        lists[0].add(e);
        return true;
    }

    /**
     * Adds the element e in the list identified by the given index. See
     * {@link java.util.Collection#add(E)}.
     * 
     * @param e
     *            The element to add.
     * @param index
     *            The index of the list to add the element to.
     * @return <tt>true</tt> if this collection changed as a result of the call
     */
    public boolean add(T e, int index) {
        lists[index].add(e);
        return true;
    }

    public boolean remove(T o) {
        for (int i = 0; i < lists.length; i++) {
            if (lists[i].removeValue(o, true)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the element e in the list identified by the given index. This
     * version is faster than the version without the index. See
     * {@link java.util.Collection#remove(E)}
     * 
     * @param o
     *            The element to remove
     * @param index
     *            The index of the list to remove the element from
     * @return <tt>true</tt> if the element was removed as a result of the call
     */
    public boolean remove(T o, int index) {
        if (lists[index].removeValue(o, true)) {
            return true;
        }
        return false;
    }

    public boolean addAll(Array<? extends T> c) {
        lists[0].addAll(c);
        return true;
    }

    public boolean removeAll(Array<? extends T> c) {
        boolean result = false;
        for (T o : c) {
            boolean r = remove(o);
            if (r) {
                result = true;
            }
        }
        return result;
    }

    public void clear() {
        for (int i = 0; i < lists.length; i++)
            lists[i].clear();
        tolist.clear();
    }

    public T get(int index) {
        return lists[0].get(index);
    }

    /**
     * Gets the element of the given list index at the given index
     * 
     * @param index
     *            The index of the element in the list
     * @param listIndex
     *            The index of the list
     * @return The element if exists, null otherwise
     */
    public T get(int index, int listIndex) {
        return lists[listIndex].get(index);
    }

    public void set(int index, T element) {
        lists[0].set(index, element);
    }

    /**
     * Sets the element at the given index in the given list.
     * 
     * @param index
     *            The index of the element.
     * @param element
     *            The element.
     * @param listIndex
     *            The index of the list.
     */
    public void set(int index, T element, int listIndex) {
        lists[listIndex].set(index, element);
    }

    public T remove(int index) {
        T t = lists[0].removeIndex(index);
        return t;
    }

    /**
     * Removes the element of the given list index at the given index.
     * 
     * @param index
     *            The index of the element in the list.
     * @param listIndex
     *            The index of the list.
     * @return The element if it was removed, null otherwise.
     */
    public T remove(int index, int listIndex) {
        T t = lists[listIndex].removeIndex(index);
        return t;
    }

    public int indexOf(T o) {
        return lists[0].indexOf(o, true);
    }

    public int lastIndexOf(T o) {
        return lists[0].lastIndexOf(o, true);
    }

    private class MultilistIterator<T> implements Iterator<T> {
        /** The index of the list **/
        int listIndex;
        /** The index of the current element in the list **/
        int index;

        public MultilistIterator() {
            super();
            listIndex = 0;
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return (index < lists[listIndex].size) || (listIndex < lists.length - 1 && !emptyFrom(listIndex + 1));
        }

        /** Are the lists from index li onwards empty? **/
        private boolean emptyFrom(int li) {
            for (int i = li; i < lists.length; i++) {
                if (lists[i].size != 0)
                    return false;
            }
            return true;
        }

        @Override
        public T next() {
            T elem = (T) lists[listIndex].get(index);
            if (index == lists[listIndex].size - 1) {
                index = 0;
                listIndex++;
            } else {
                index++;
            }
            return elem;
        }

        @Override
        public void remove() {
            lists[listIndex].removeIndex(index);
            if (index == lists[listIndex].size) {
                index--;
            }

        }

    }

}
