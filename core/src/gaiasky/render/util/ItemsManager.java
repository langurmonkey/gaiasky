/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.util;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.Iterator;

public class ItemsManager<T extends Disposable> implements Iterable<T>, Disposable {
    private static final int ItemNotFound = -1;
    protected final Array<Boolean> owned = new Array<Boolean>();
    private final Array<T> items = new Array<T>();

    @Override
    public void dispose() {
        for (int i = 0; i < items.size; i++) {
            if (owned.get(i)) {
                items.get(i).dispose();
            }
        }

        items.clear();
        owned.clear();
    }

    /** Add an item to the manager, if own is true the manager will manage the resource's lifecycle */
    public void add(T item, boolean own) {
        if (item == null) {
            return;
        }

        items.add(item);
        owned.add(own);
    }

    public void add(T item, int index, boolean own) {
        if (item == null) {
            return;
        }

        items.insert(index, item);
        owned.insert(index, own);
    }

    /** Add an item to the manager and transfer ownership to it */
    public void add(T item) {
        add(item, true);
    }

    public void add(T item, int index) {
       add(item, index, true);
    }

    /** Returns the item at the specified index */
    public T get(int index) {
        return items.get(index);
    }

    /** Returns the number of items managed by this instance */
    public int count() {
        return items.size;
    }

    /* Returns an iterator on the managed items */
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    /** Removes a previously added resource */
    public void remove(T item) {
        int index = items.indexOf(item, true);
        if (index == ItemNotFound) {
            return;
        }

        if (owned.get(index)) {
            items.get(index).dispose();
        }

        items.removeIndex(index);
        owned.removeIndex(index);
        items.removeValue(item, true);
    }
}
