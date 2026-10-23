/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.util.FastObjectIntMap;
import gaiasky.util.TextUtils;
import org.junit.Assert;
import org.junit.Test;

public class FastObjectIntMapTest {

    @Test
    public void testMissingKeySentinel() {
        var map = new FastObjectIntMap<String>(16, String.class);
        map.put("a", 0);
        map.put("b", 3);

        // Present keys, including value 0, must be returned as stored.
        Assert.assertEquals(0, map.get("a"));
        Assert.assertEquals(3, map.get("b"));

        // Missing keys must not silently resolve to the first element.
        Assert.assertEquals(-1, map.get("c"));
        Assert.assertEquals(-1, map.get("A"));
        Assert.assertEquals(7, map.getOrDefault("c", 7));
        Assert.assertEquals(3, map.getOrDefault("b", 7));
    }

    @Test
    public void testNormalizeName() {
        Assert.assertEquals("sirius", TextUtils.normalizeName("  Sirius  "));
        Assert.assertEquals("sirius", TextUtils.normalizeName("SIRIUS"));
        Assert.assertEquals("hip 32349", TextUtils.normalizeName("HIP 32349 "));
        Assert.assertNull(TextUtils.normalizeName(null));
    }

    @Test
    public void testInsertLookupConsistency() {
        // Names are normalized on insertion and on lookup, so both sides must agree.
        var map = new FastObjectIntMap<String>(16, String.class);
        String[] names = {" Sirius ", "Vega", " Betelgeuse"};
        for (int i = 0; i < names.length; i++) {
            map.put(TextUtils.normalizeName(names[i]), i);
        }
        for (int i = 0; i < names.length; i++) {
            Assert.assertEquals(i, map.get(TextUtils.normalizeName(names[i])));
            Assert.assertTrue(map.containsKey(names[i].toLowerCase().trim()));
        }
        Assert.assertEquals(-1, map.get(TextUtils.normalizeName("Rigel")));
    }
}
