/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of an LRU (least recently used) cache.
 *
 * @param <A> The key type.
 * @param <B> The object type.
 */
public class LruCache<A, B> extends LinkedHashMap<A, B> {
    private final int maxEntries;

    public LruCache(final int maxEntries) {
        super(maxEntries, 1.0f, true);
        this.maxEntries = maxEntries;
    }

    /**
     * Returns <tt>true</tt> if this <code>LruCache</code> has more entries than the maximum specified when it was
     * created.
     *
     * <p>
     * This method <em>does not</em> modify the underlying <code>Map</code>; it relies on the implementation of
     * <code>LinkedHashMap</code> to do that, but that behavior is documented in the JavaDoc for
     * <code>LinkedHashMap</code>.
     * </p>
     *
     * @param eldest the <code>Entry</code> in question; this implementation doesn't care what it is, since the
     *               implementation is only dependent on the size of the cache
     * @return <tt>true</tt> if the oldest
     * @see java.util.LinkedHashMap#removeEldestEntry(Map.Entry)
     */
    @Override
    protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
        return super.size() >= maxEntries;
    }
}
