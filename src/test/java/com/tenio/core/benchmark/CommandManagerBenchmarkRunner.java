package com.tenio.core.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Runner class for CommandManagerBenchmarkTest.
 * This class provides a main method to execute the JMH benchmarks.
 */
public class CommandManagerBenchmarkRunner {

    /**
     * Main method to run the CommandManagerBenchmarkTest benchmarks.
     * 
     * @param args Command line arguments (not used)
     * @throws RunnerException If there's an error running the benchmarks
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(CommandManagerBenchmarkTest.class.getSimpleName())
                .forks(1)
                .build();
        
        new Runner(options).run();
    }
} 