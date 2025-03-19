#!/bin/bash

# Create benchmark results directory if it doesn't exist
mkdir -p benchmark-results

# Compile the project
mvn clean compile

# Run PlayerManager benchmarks
echo "Running PlayerManager benchmarks..."
java -cp target/classes:target/test-classes:${HOME}/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:${HOME}/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar org.openjdk.jmh.Main -f 1 -i 5 -wi 3 -bm avgt -tu us -r 1s -rf json -rff benchmark-results/player-manager-benchmark.json -rf text -rff benchmark-results/player-manager-benchmark.txt "com.tenio.core.manager.PlayerManagerBenchmark"

# Run RoomManager benchmarks
echo "Running RoomManager benchmarks..."
java -cp target/classes:target/test-classes:${HOME}/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:${HOME}/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar org.openjdk.jmh.Main -f 1 -i 5 -wi 3 -bm avgt -tu us -r 1s -rf json -rff benchmark-results/room-manager-benchmark.json -rf text -rff benchmark-results/room-manager-benchmark.txt "com.tenio.core.manager.RoomManagerBenchmark"

# Run SessionManager benchmarks
echo "Running SessionManager benchmarks..."
java -cp target/classes:target/test-classes:${HOME}/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:${HOME}/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar org.openjdk.jmh.Main -f 1 -i 5 -wi 3 -bm avgt -tu us -r 1s -rf json -rff benchmark-results/session-manager-benchmark.json -rf text -rff benchmark-results/session-manager-benchmark.txt "com.tenio.core.manager.SessionManagerBenchmark"

# Run PacketQueue benchmarks
echo "Running PacketQueue benchmarks..."
java -cp target/classes:target/test-classes:${HOME}/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:${HOME}/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar org.openjdk.jmh.Main -f 1 -i 5 -wi 3 -bm avgt -tu us -r 1s -rf json -rff benchmark-results/packet-queue-benchmark.json -rf text -rff benchmark-results/packet-queue-benchmark.txt "com.tenio.core.network.entity.packet.PacketQueueBenchmark"

# Run Request benchmarks
echo "Running Request benchmarks..."
java -cp target/classes:target/test-classes:${HOME}/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:${HOME}/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar org.openjdk.jmh.Main -f 1 -i 5 -wi 3 -bm avgt -tu us -r 1s -rf json -rff benchmark-results/request-benchmark.json -rf text -rff benchmark-results/request-benchmark.txt "com.tenio.core.network.entity.request.RequestBenchmark"

# Run Response benchmarks
echo "Running Response benchmarks..."
java -cp target/classes:target/test-classes:${HOME}/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:${HOME}/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar org.openjdk.jmh.Main -f 1 -i 5 -wi 3 -bm avgt -tu us -r 1s -rf json -rff benchmark-results/response-benchmark.json -rf text -rff benchmark-results/response-benchmark.txt "com.tenio.core.network.entity.response.ResponseBenchmark"

echo "All benchmark results have been saved to the benchmark-results/ directory"
echo "Results are available in both JSON and text formats for each benchmark class" 