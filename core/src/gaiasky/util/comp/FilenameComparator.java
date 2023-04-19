/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.comp;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;

public final class FilenameComparator implements Comparator<Path> {
    private static final Pattern NUMBERS =
            Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

    @Override
    public final int compare(Path p1, Path p2) {
        // Optional "NULLS LAST" semantics:
        if (p1 == null || p2 == null)
            return p1 == null ? p2 == null ? 0 : -1 : 1;

        String o1 = p1.getFileName().toString();
        String o2 = p2.getFileName().toString();

        // Splitting both input strings by the above patterns
        String[] split1 = NUMBERS.split(o1);
        String[] split2 = NUMBERS.split(o2);
        for (int i = 0; i < Math.min(split1.length, split2.length); i++) {
            char c1 = split1[i].charAt(0);
            char c2 = split2[i].charAt(0);
            int cmp = 0;

            // If both segments start with a digit, sort them numerically using
            // BigInteger to stay safe
            if (c1 >= '0' && c1 <= '9' && c2 >= '0' && c2 <= '9')
                cmp = new BigInteger(split1[i]).compareTo(new BigInteger(split2[i]));

            // If we haven't sorted numerically before, or if numeric sorting yielded
            // equality (e.g 007 and 7) then sort lexicographically
            if (cmp == 0)
                cmp = split1[i].compareTo(split2[i]);

            // Abort once some prefix has unequal ordering
            if (cmp != 0)
                return cmp;
        }

        // If we reach this, then both strings have equally ordered prefixes, but
        // maybe one string is longer than the other (i.e. has more segments)
        return split1.length - split2.length;
    }
}
