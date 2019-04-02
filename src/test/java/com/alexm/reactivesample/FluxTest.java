package com.alexm.reactivesample;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author AlexM
 */
public class FluxTest {

    private Flux<String> flux = Flux.just("red", "white", "blue");
    private static Logger log = LoggerFactory.getLogger(FluxTest.class);

    @Test
    public void flux() {
        flux.log().map(String::toUpperCase).subscribe(System.out::println);

        System.out.println("=========================");
        flux.log().subscribe(new Subscriber<String>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String s) {
                System.out.println(s);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
        System.out.println("=========================");
        flux.log().subscribe(new Subscriber<String>() {
            private long count = 0;
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(2);
            }

            @Override
            public void onNext(String s) {
                count++;
                if (count >= 2) {
                    count = 0;
                    subscription.request(2);
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

    }

    @Test
    public void fluxBatch() {
        flux.log().map(String::toUpperCase).subscribe(System.out::println, 2);
    }

    @Test
    public void schedulers() throws InterruptedException {
        Flux<String> localFlux = Flux.just("one", "two", "three", "four", "five", "six", "seven");
        localFlux.log().map(String::toUpperCase)
                .subscribeOn(Schedulers.parallel())
                .subscribe(System.out::println, 2);
        Thread.currentThread().sleep(1000);
    }

    @Test
    public void multipleThreads() {
        final Stream<String> stream = IntStream.range(0, 1000).mapToObj(operand -> String.valueOf(operand));
        Flux<String> lFlux = Flux.fromStream(stream);
        lFlux.log()
                .flatMap(value ->
                        Mono.just(value).subscribeOn(Schedulers.parallel())
                )
                .subscribe(s -> System.out.println("Consumed:" + s), 2);
    }

    @Test
    public void publishSubscribe() throws InterruptedException {
        flux.log()
                .map(String::toUpperCase)
                .subscribeOn(Schedulers.newParallel("sub"))
                .publishOn(Schedulers.newParallel("pub"), 2)
            .subscribe(s -> log.info("Consumed:" + s));
        Thread.sleep(1500L);
    }


}
