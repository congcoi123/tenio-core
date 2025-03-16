package com.tenio.core.benchmark;

import com.tenio.core.bootstrap.injector.Injector;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark tests for dependency injection operations.
 * This class measures the performance of bean creation, retrieval, and autowiring
 * in the Injector component.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class InjectorBenchmarkTest extends BenchmarkTestBase {

    private Injector injector;
    private static final String TEST_PACKAGE = "com.tenio.core.entity";

    @Setup
    public void setup() {
        injector = Injector.newInstance();
    }

    @Benchmark
    public void benchmarkInjectorCreation() {
        // Create a new injector instance
        Injector newInjector = Injector.newInstance();
    }

    @Benchmark
    public void benchmarkBeanRetrieval() {
        // Retrieve a bean from the injector
        // Note: This will return null since we haven't scanned packages yet
        // but we're just measuring the lookup performance
        Player bean = injector.getBean(Player.class);
    }

    @Benchmark
    public void benchmarkPackageScanningAndBeanCreation() throws Exception {
        // Create a temporary injector for package scanning
        Injector tempInjector = Injector.newInstance();
        
        // Scan packages to create beans
        // This is a more complex operation that involves reflection
        try {
            tempInjector.scanPackages(InjectorBenchmarkTest.class, TEST_PACKAGE);
        } catch (Exception e) {
            // Ignore exceptions for benchmarking purposes
        }
    }

    @Benchmark
    public void benchmarkCompleteInjectionCycle() throws Exception {
        // Create a temporary injector for the complete cycle
        Injector tempInjector = Injector.newInstance();
        
        // Scan packages
        try {
            tempInjector.scanPackages(InjectorBenchmarkTest.class, TEST_PACKAGE);
            
            // Retrieve a bean
            Room bean = tempInjector.getBean(Room.class);
        } catch (Exception e) {
            // Ignore exceptions for benchmarking purposes
        }
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(InjectorBenchmarkTest.class);
    }
} 