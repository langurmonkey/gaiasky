/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.benchmark;

import org.apfloat.Apfloat;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.TimeUnit;

public class PrecisionBenchmark {

    private static final int ITERATIONS = 100000;

    @State(Scope.Thread)
    public static class BenchmarkState {
        BigDecimal aBD;
        BigDecimal bBD;
        Apfloat aAF;
        Apfloat bAF;

        @Param({"25", "27", "30", "500", "1000"})  // Add different precision levels here
        int precision;

        @Setup(Level.Trial)
        public void setUp() {
            MathContext mc = new MathContext(precision);
            aBD = new BigDecimal("12345.6789012345678901234567890123456789", mc);
            bBD = new BigDecimal("98765.4321098765432109876543210987654321", mc);
            aAF = new Apfloat("12345.6789012345678901234567890123456789", precision);
            bAF = new Apfloat("98765.4321098765432109876543210987654321", precision);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testBigDecimalAddition(BenchmarkState state) {
        for (int i = 0; i < ITERATIONS; i++) {
            BigDecimal result = state.aBD.add(state.bBD);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testApFloatAddition(BenchmarkState state) {
        for (int i = 0; i < ITERATIONS; i++) {
            Apfloat result = state.aAF.add(state.bAF);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testBigDecimalMultiplication(BenchmarkState state) {
        for (int i = 0; i < ITERATIONS; i++) {
            BigDecimal result = state.aBD.multiply(state.bBD);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testApFloatMultiplication(BenchmarkState state) {
        for (int i = 0; i < ITERATIONS; i++) {
            Apfloat result = state.aAF.multiply(state.bAF);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testBigDecimalDivision(BenchmarkState state) {
        for (int i = 0; i < ITERATIONS; i++) {
            BigDecimal result = state.aBD.divide(state.bBD);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testApFloatDivision(BenchmarkState state) {
        for (int i = 0; i < ITERATIONS; i++) {
            Apfloat result = state.aAF.divide(state.bAF);
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = new OptionsBuilder()
                .include(PrecisionBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(options).run();
    }
}

