package com.tenio.core.benchmark.network.protocol;

import com.tenio.core.network.entity.protocol.Response;
import com.tenio.core.network.entity.protocol.implement.ResponseImpl;
import com.tenio.core.network.define.ResponsePriority;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ResponseBenchmark {

    private Response response;
    private byte[] data;

    @Setup
    public void setup() {
        response = ResponseImpl.newInstance();
        data = new byte[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
    }

    @Benchmark
    public void benchmarkCreateResponse(Blackhole blackhole) {
        Response newResponse = ResponseImpl.newInstance();
        blackhole.consume(newResponse);
    }

    @Benchmark
    public void benchmarkEncryption(Blackhole blackhole) {
        response.setContent(data);
        response.priority(ResponsePriority.NORMAL);
        blackhole.consume(response);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ResponseBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
} 