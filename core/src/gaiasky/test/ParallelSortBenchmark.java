/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.test;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ParallelSortBenchmark extends AbstractBenchmark {

    /**
     * Number of elements to use.
     */
    private static final int[] SIZES = new int[] { 100, 1_000, 10_000, 100_000, 1000_000, 10_000_000, 20_000_000 };

    public ParallelSortBenchmark() {
        super(10, 10, ParallelSortBenchmark.class.getSimpleName());
    }

    public static void main(String[] args) {
        (new ParallelSortBenchmark()).test();
    }

    private void test() {
        int pad = 22;
        log.info(pad("Java version", pad) + System.getProperty("java.version"));
        log.info(pad("ROUNDS", pad) + formatNumber(ROUNDS, pad));
        log.info(pad("ROUNDS (warm-up)", pad) + formatNumber(ROUNDS_WARMUP, pad));
        log.info(pad("N_ELEMENTS", pad) + pad(Arrays.toString(SIZES), pad));
        log.info(pad("Parallelism", pad) + pad(String.valueOf(ForkJoinPool.getCommonPoolParallelism()), pad));
        log.info("");

        // Prepare test data.
        log.info("Preparing test arrays...");
        Random rng = new Random(1234L);
        int[][] arrays = new int[SIZES.length][];
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            arrays[i] = new int[size];
            for (int j = 0; j < arrays[i].length; j++) {
                arrays[i][j] = rng.nextInt();
            }
        }

        // Running tests.
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            int[] array = arrays[i];

            // Warm-up.
            test(size + " sequential ", ROUNDS_WARMUP, array, Arrays::sort, false);
            test(size + " parallel   ", ROUNDS_WARMUP, array, Arrays::parallelSort, false);

            // Actual tests.
            test(size + " sequential ", ROUNDS, array, Arrays::sort, true);
            test(size + " parallel   ", ROUNDS, array, Arrays::parallelSort, true);

        }
    }

    private void test(String name,
                      int rounds,
                      int[] array,
                      Consumer<int[]> consumer,
                      boolean report) {
        long[][] elapsed = new long[2][rounds];
        for (int round = 0; round < rounds; round++) {
            // Use copy of array for each round.
            int[] testArray = Arrays.copyOf(array, array.length);

            long cpuStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
            long clockStart = System.nanoTime();

            // Run sort.
            consumer.accept(testArray);

            elapsed[0][round] = System.nanoTime() - clockStart;
            elapsed[1][round] = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - cpuStart;
        }

        if (report) {
            double meanClockMs = mean(elapsed[0]) / 1_000_000d;
            double stDevClock = stdev(elapsed[0], meanClockMs);

            double meanCpuMs = mean(elapsed[1]) / 1_000_000d;
            double stDevCpu = stdev(elapsed[1], meanCpuMs);

            log.info(pad(name, 20) + pad(meanClockMs + " (±" + format(stDevClock) + ") ms", 28) + pad(meanCpuMs + " (±" + format(stDevCpu) + ") ms", 28));
        }
    }


}
