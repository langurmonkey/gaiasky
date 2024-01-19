/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.test;

import gaiasky.util.TextUtils;

import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Compares the native {@link String#hashCode()} method against a fast version in {@link TextUtils#hashFast(String)},
 * the murmur hash implementation * in {@link TextUtils#hashMurmur(String)} and the FNV1 in {@link TextUtils#hashFNV1(String)}.
 */
public class HashBenchmark extends AbstractBenchmark {

    /** Number of strings. **/
    private static final int N = 1000_000;

    public HashBenchmark() {
        super(10, 3, HashBenchmark.class.getSimpleName());
    }

    public static void main(String[] args) {
        new HashBenchmark().test();
    }

    private void test() {
        int pad = 22;
        log.info(pad("Java version", pad) + System.getProperty("java.version"));
        log.info(pad("ROUNDS", pad) + formatNumber(ROUNDS, pad));
        log.info(pad("ROUNDS (warm-up)", pad) + formatNumber(ROUNDS_WARMUP, pad));
        log.info(pad("N_ELEMENTS", pad) + pad(Integer.toString(N), pad));
        log.info("");

        // Prepare test data.
        log.info("Preparing test strings...");
        final Random rng = new Random(1234L);
        final String[] strings = new String[N];
        for (int i = 0; i < N; i++) {
            strings[i] = genRandomAlphanumericString(rng);
        }

        // Java hashCode
        Consumer<String[]> javaHash = (arr) -> {
            int ignored = Integer.MIN_VALUE;
            for (String str : arr) {
                ignored += str.hashCode();
            }
        };

        // Fast hashCode
        Consumer<String[]> fastHash = (arr) -> {
            int ignored = Integer.MIN_VALUE;
            for (String str : arr) {
                ignored += TextUtils.hashFast(str);
            }
        };

        // Murmur hash
        Consumer<String[]> murmurHash = (arr) -> {
            int ignored = Integer.MIN_VALUE;
            for (String str : arr) {
                ignored += TextUtils.hashMurmur(str);
            }
        };

        // FNV1 hash
        Consumer<String[]> fnv1Hash = (arr) -> {
            int ignored = Integer.MIN_VALUE;
            for (String str : arr) {
                ignored += TextUtils.hashFNV1(str);
            }
        };

        // Warm-up.
        test("Java hash", ROUNDS_WARMUP, strings, javaHash, false);
        test("Fast hash", ROUNDS_WARMUP, strings, fastHash, false);
        test("Murmur hash", ROUNDS_WARMUP, strings, murmurHash, false);
        test("FNV1 hash", ROUNDS_WARMUP, strings, murmurHash, false);

        // Test.
        test("Java hash", ROUNDS, strings, javaHash, true);
        test("Fast hash", ROUNDS_WARMUP, strings, fastHash, true);
        test("Murmur hash", ROUNDS, strings, murmurHash, true);
        test("FNV1 hash", ROUNDS, strings, murmurHash, true);
    }

    private void test(String name,
                      final int rounds,
                      final String[] array,
                      Consumer<String[]> consumer,
                      boolean report) {
        long[][] elapsed = new long[2][rounds];
        for (int round = 0; round < rounds; round++) {
            long cpuStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
            long clockStart = System.nanoTime();

            // Run sort.
            consumer.accept(array);

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

    public String genRandomAlphanumericString(Random random) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = random.nextInt(5, 20);

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
