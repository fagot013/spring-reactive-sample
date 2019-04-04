package com.alexm.reactivesample;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@SpringBootApplication
@RestController
public class ReactiveSampleApplication {

	private WebClient client = WebClient.create();
	private RestTemplate restTemplate = new RestTemplate();
	private Scheduler scheduler = Schedulers.parallel();
	@Value("${app.url:http://example.com}")
	private String url;
	private static Logger log  = LoggerFactory.getLogger(ReactiveSampleApplication.class);

	@RequestMapping("/serial")
	public Mono<Result> serial() {
		log.info("Handling serial");
		return Flux.range(0, 10)
				.log()
				.map(this::block)
				.collect(Result::new, Result::add)
				.doOnSuccess(Result::stop)
				.subscribeOn(scheduler);
		// <1> make 10 calls
		// <2> stay in the same publisher chain
		// <3> blocking code not deferred
		// <4> collect results and aggregate into a single object
		// <5> at the end stop the clock
		// <6> subscribe on a background thread
	}

	@RequestMapping("/parallel")
	public Mono<Result> parallel() {
		log.info("Handling parallel");
		return Flux.range(0, 10)
				.log()
				.flatMap(val -> Mono.fromCallable( ()-> block(val))
						.subscribeOn(scheduler), 4)
				.collect(Result::new, Result::add)
				.doOnSuccess(Result::stop);
		// <1> make 10 calls
		// <2> drop down to a new publisher to process in parallel
		// <3> blocking code here inside a callable to defer execution
		// <4> subscribe to the slow publisher on background thread
		// <5> concurrency hint in flatMap
		// <6> collect result and aggregate into a single object
		// <7> at the end stop the clock
	}

	@RequestMapping("/netty")
	public Mono<Result> netty() {
		return Flux.range(0, 10)
				.log()
				.flatMap(this::fetch)
				.collect(Result::new, Result::add)
				.doOnSuccess(Result::stop)
				;
		// <1> make 10 calls
		// <2>
	}

	private Mono<HttpStatus> fetch(Integer integer) {
		return this.client.method(HttpMethod.GET).uri(url).exchange().map(ClientResponse::statusCode);
	}

	private HttpStatus block(int value) {
		return this.restTemplate.getForEntity(url, String.class, value).getStatusCode();
	}

	public static void main(String[] args) {
		SpringApplication.run(ReactiveSampleApplication.class, args);
	}


	class Result {
		private ConcurrentHashMap<HttpStatus, AtomicLong> counts = new ConcurrentHashMap<>();
		private long timestamp = System.currentTimeMillis();

		private long duration;

		public Result() {
			log.info("Creating Result");
		}

		public long add(HttpStatus httpStatus) {
			AtomicLong count = counts.getOrDefault(httpStatus, new AtomicLong());
			this.counts.putIfAbsent(httpStatus, count);
			return count.incrementAndGet();
		}

		public void stop() {
			this.duration = System.currentTimeMillis() - timestamp;
			log.info("Stop creating result. process took:"  + duration + " ms");

		}

		public ConcurrentHashMap<HttpStatus, AtomicLong> getCounts() {
			return counts;
		}
	}

}
