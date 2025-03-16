package com.tenio.core.benchmark;

import org.junit.jupiter.api.BeforeEach;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Base class for benchmark tests using JMH (Java Microbenchmark Harness).
 * This class provides common setup and utility methods for performance testing.
 * 
 * <p>Default benchmark configuration:
 * <ul>
 * <li>Scope: Thread level</li>
 * <li>Mode: Average time measurement</li>
 * <li>Output time unit: Microseconds</li>
 * <li>Warmup: 3 iterations of 1 second each</li>
 * <li>Measurement: 5 iterations of 1 second each</li>
 * <li>Forking: 1 fork</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * public class MyBenchmark extends BenchmarkTestBase {
 *     
 *     private MyClass testObject;
 *     
 *     &#64;Setup
 *     public void setup() {
 *         testObject = new MyClass();
 *     }
 *     
 *     &#64;Benchmark
 *     public void testMethod() {
 *         testObject.methodToTest();
 *     }
 * }
 * </pre>
 * 
 * @see org.openjdk.jmh.annotations.State
 * @see org.openjdk.jmh.annotations.BenchmarkMode
 * @see org.openjdk.jmh.annotations.OutputTimeUnit
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public abstract class BenchmarkTestBase {
    
    /**
     * Setup method to be overridden by subclasses to initialize test data.
     * This method is called before each benchmark iteration.
     * 
     * <p>Example:
     * <pre>
     * &#64;Override
     * public void setup() {
     *     testData = new ArrayList<>();
     *     for (int i = 0; i < 1000; i++) {
     *         testData.add(new TestObject());
     *     }
     * }
     * </pre>
     */
    @BeforeEach
    public void setup() {
        // Override in subclasses to set up test data
    }
    
    /**
     * Runs the benchmark test suite with default configuration.
     * This method handles the JMH runner setup and execution.
     *
     * @param benchmarkClass The class containing the benchmark methods
     * @throws RunnerException If there's an error running the benchmark
     * 
     * <p>Example:
     * <pre>
     * public static void main(String[] args) throws RunnerException {
     *     runBenchmark(MyBenchmark.class);
     * }
     * </pre>
     */
    protected static void runBenchmark(Class<?> benchmarkClass) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(benchmarkClass.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }
    
    /**
     * Utility method to measure execution time of a code block.
     * This method provides high-precision timing using System.nanoTime().
     *
     * @param runnable The code block to measure
     * @return Execution time in milliseconds
     * 
     * <p>Example:
     * <pre>
     * long time = measureExecutionTime(() -> {
     *     // Code to measure
     *     complexOperation();
     * });
     * System.out.println("Execution time: " + time + " ms");
     * </pre>
     */
    protected long measureExecutionTime(Runnable runnable) {
        long startTime = System.nanoTime();
        runnable.run();
        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }
    
    /**
     * Utility method to format execution time with appropriate units.
     * This method converts raw millisecond values into a human-readable format.
     *
     * @param timeInMillis Time in milliseconds
     * @return Formatted string with appropriate time unit (ns, μs, ms, s)
     * 
     * <p>Example:
     * <pre>
     * String formattedTime = formatExecutionTime(1234);
     * // Returns "1.234 seconds" or "1234 ms" depending on the value
     * </pre>
     */
    protected String formatExecutionTime(long timeInMillis) {
        if (timeInMillis < 1) {
            return timeInMillis * 1_000_000 + " ns";
        } else if (timeInMillis < 1_000) {
            return timeInMillis * 1_000 + " μs";
        } else if (timeInMillis < 1_000_000) {
            return timeInMillis + " ms";
        } else {
            return (timeInMillis / 1_000.0) + " seconds";
        }
    }
} 