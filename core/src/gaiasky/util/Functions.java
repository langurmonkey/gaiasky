/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

public class Functions {
    @FunctionalInterface
    public interface Function2<One, Two, R> {
        R apply(One one, Two two);
    }

    @FunctionalInterface
    public interface Function3<One, Two, Three, R> {
        R apply(One one, Two two, Three three);
    }

    @FunctionalInterface
    public interface Function4<One, Two, Three, Four, R> {
        R apply(One one, Two two, Three three, Four four);
    }

    @FunctionalInterface
    public interface Function5<One, Two, Three, Four, Five, R> {
        R apply(One one, Two two, Three three, Four four, Five five);
    }

    @FunctionalInterface
    public interface Function6<One, Two, Three, Four, Five, Six, R> {
        R apply(One one, Two two, Three three, Four four, Five five, Six six);
    }
}
