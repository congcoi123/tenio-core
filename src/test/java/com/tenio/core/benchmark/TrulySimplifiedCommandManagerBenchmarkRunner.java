package com.tenio.core.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Runner for the truly simplified command manager benchmark.
 */
public class TrulySimplifiedCommandManagerBenchmarkRunner {

    /**
     * The main method to run the benchmark.
     *
     * @param args command line arguments (not used)
     * @throws RunnerException if there's an error running the benchmark
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(TrulySimplifiedCommandManagerBenchmarkTest.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(options).run();
    }
} 