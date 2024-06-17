/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.test;

import net.jafama.FastMath;

import java.text.DecimalFormat;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AbstractBenchmark {
    /** Number of rounds for each test. **/
    protected final int ROUNDS;
    /** Number of warm-up rounds. **/
    protected final int ROUNDS_WARMUP;

    protected final DecimalFormat df;
    protected Logger log;

    protected AbstractBenchmark(int rounds, int roundsWarmup, String loggerName) {
        this.ROUNDS = rounds;
        this.ROUNDS_WARMUP = roundsWarmup;

        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-2s] %5$s %n");
        log = Logger.getLogger(loggerName);
        log.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());

        this.df = new DecimalFormat("0.0#");
    }

    protected double mean(long[] array) {
        long total = 0;
        for (long l : array) {
            total += l;
        }
        return total / array.length;
    }

    protected double stdev(long[] array,
                         double mean) {
        double sum = 0;
        double n = array.length;
        for (long l : array) {
            sum += FastMath.pow(l / 1_000_000d - mean, 2.0);
        }
        return FastMath.sqrt(sum / n);
    }

    protected String pad(String str,
                       int len) {
        StringBuilder strPad = new StringBuilder(str);
        while (strPad.length() < len) {
            strPad.append(" ");
        }
        return strPad.toString();
    }

    protected String format(double num) {
        return df.format(num);
    }

    protected String formatNumber(int num,
                                int pad) {
        return pad(formatNumber(num), 22);
    }

    protected String formatNumber(int num) {
        if (num > 1e9) {
            return df.format(num / 1_000_000_000d) + " G";
        } else if (num > 1e6) {
            return df.format(num / 1_000_000d) + " M";
        } else if (num > 1e3) {
            return df.format(num / 1_000d) + " k";
        } else {
            return Integer.toString(num);
        }
    }
}
