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
     *
     * @param array1 The first array.
     * @param array2 The second array.
     * @param <T>    Array element types.
     *
     * @return A new array which contains array1 + array2.
     */
    public static <T> T[] concatWithArrayCopy(T[] array1, T[] array2) {
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    /**
     * Convert a double array to a float array.
     *
     * @param arr The double array.
     *
     * @return The float array.
     */
    public static float[] toFloatArray(double[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        float[] ret = new float[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (float) arr[i];
        }
        return ret;
    }

    /**
     * Returns a new array containing all elements of the original array
     * plus the specified value appended at the end.
     *
     * <p>Since Java arrays are immutable in size, this method creates
     * a new array, copies the original contents, and adds the new value.</p>
     *
     * @param original the source array; may be {@code null}
     * @param value the string to append to the array
     * @return a new array containing the original elements and the appended value
     */
    public static String[] addString(String[] original, String value) {
        // If the original array is null, return a new single-element array
        if (original == null) {
            return new String[]{ value };
        }

        // Create a new array with one additional slot
        String[] result = Arrays.copyOf(original, original.length + 1);

        // Assign the new value to the last position
        result[result.length - 1] = value;

        return result;
    }
}
