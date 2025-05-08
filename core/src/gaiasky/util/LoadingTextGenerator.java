/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.StdRandom;

import java.util.MissingResourceException;
import java.util.Scanner;

/**
 * Generates funny sentences from different chunks.
 */
public class LoadingTextGenerator {
    private static final int MAX_KEYS = 100;
    private final String[] verbs;
    private final String[] adjectives;
    private final String[] objects;
    private final String[][] set;

    public LoadingTextGenerator() {
        verbs = read("funny.verb.");
        adjectives = read("funny.adjective.", 4);
        objects = read("funny.object.");
        set = createOrder("funny.order");
    }

    private String[] read(String keyPrefix) {
        return read(keyPrefix, 0);
    }

    private String[] read(String keyPrefix, int nBlanks) {
        Array<String> strings = new Array<>();
        for (int i = 0; i < MAX_KEYS; i++) {
            try {
                String s = I18n.msg(keyPrefix + i);
                strings.add(s);
            } catch (MissingResourceException e) {
                // Skip
            }
        }
        if (nBlanks > 0) {
            for (int i = 0; i < nBlanks; i++) {
                strings.add("");
            }
        }
        String[] out = new String[strings.size];
        for (int i = 0; i < strings.size; i++) {
            out[i] = strings.get(i);
        }
        return out;
    }

    private String[][] createOrder(String key) {
        Scanner scanner = new Scanner(I18n.msg(key));
        String order = null;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            // Skip comments
            if (!line.startsWith("#")) {
                order = line;
                break;
            }
        }
        String[][] result = null;
        if (order != null && !order.isBlank()) {
            // Split by spaces
            String[] tokens = order.split("\\s+");
            if (tokens.length == 3) {
                result = new String[3][];
                for (int i = 0; i < 3; i++) {
                    if (tokens[i].equalsIgnoreCase("V")) {
                        // Verbs
                        result[i] = verbs;
                    } else if (tokens[i].equalsIgnoreCase("A")) {
                        // Adjectives
                        result[i] = adjectives;
                    } else if (tokens[i].equalsIgnoreCase("O")) {
                        // Objects
                        result[i] = objects;
                    }
                }
            }
        }
        // Use default order
        if (result == null) {
            result = new String[][] { verbs, adjectives, objects };
        }
        return result;
    }

    private String next(int index, String sep) {
        String next = set[index][StdRandom.uniform(set[index].length)];
        next = !next.isBlank() ? next + sep : "";
        return next;
    }

    public String next() {
        String first = next(0, " ");
        String second = next(1, " ");
        String third = next(2, "");
        return first + second + third;
    }
}
