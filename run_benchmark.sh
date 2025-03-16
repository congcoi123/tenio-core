#!/bin/bash

# Compile the project
mvn test-compile -Dmaven.javadoc.skip=true

# Run the benchmark and save results to a file
java -cp "target/classes:target/test-classes:target/dependency/*" \
  com.tenio.core.benchmark.TrulySimplifiedCommandManagerBenchmarkRunner > benchmark_results.txt

echo "Benchmark completed. Results saved to benchmark_results.txt" 