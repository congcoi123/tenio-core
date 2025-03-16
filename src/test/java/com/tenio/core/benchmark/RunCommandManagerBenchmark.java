package com.tenio.core.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * A simple program to run the CommandManagerBenchmarkTest directly.
 */
public class RunCommandManagerBenchmark {

    /**
     * Main method to run the benchmark.
     * 
     * @param args Command line arguments (not used)
     * @throws RunnerException If there's an error running the benchmark
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(CommandManagerBenchmarkTest.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(5)
                .forks(1)
                .resultFormat(ResultFormatType.TEXT)
                .build();
        
        new Runner(options).run();
    }
} 