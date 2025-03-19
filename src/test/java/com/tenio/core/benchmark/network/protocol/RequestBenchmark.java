package com.tenio.core.benchmark.network.protocol;

import com.tenio.common.data.DataCollection;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.network.entity.protocol.Request;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class RequestBenchmark {

    private Request request;
    private DataCollection message;

    @Setup
    public void setup() {
        request = Mockito.mock(Request.class);
        message = Mockito.mock(DataCollection.class);
        
        Mockito.when(request.setEvent(Mockito.any())).thenReturn(request);
        Mockito.when(request.setMessage(Mockito.any())).thenReturn(request);
        Mockito.when(request.getMessage()).thenReturn(message);
    }

    @Benchmark
    public void benchmarkCreateRequest(Blackhole blackhole) {
        Request newRequest = Mockito.mock(Request.class);
        blackhole.consume(newRequest);
    }

    @Benchmark
    public void benchmarkSetEvent(Blackhole blackhole) {
        request.setEvent(ServerEvent.SESSION_READ_MESSAGE);
        blackhole.consume(request);
    }

    @Benchmark
    public void benchmarkSetMessage(Blackhole blackhole) {
        request.setMessage(message);
        blackhole.consume(request);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RequestBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}