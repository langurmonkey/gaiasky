/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import java.util.Arrays;

/**
 * Utilities to work with arrays.
 */
public class ArrayUtils {
    /**
     * Concatenates the given arrays into a new array.
     * @param array1 The first array.
     * @param array2 The second array.
     * @return A new array which contains array1 + array2.
     * @param <T> Array element types.
     */
    public static <T> T[] concatWithArrayCopy(T[] array1, T[] array2) {
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }
}
