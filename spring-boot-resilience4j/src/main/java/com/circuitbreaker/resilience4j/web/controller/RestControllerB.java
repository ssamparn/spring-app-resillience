package com.circuitbreaker.resilience4j.web.controller;

import com.circuitbreaker.resilience4j.service.BackendService;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

@RestController
@RequestMapping("/backend-b")
public class RestControllerB {

    private static final String BACKEND_B = "backend-b";

    private final BackendService backendServiceB;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final ThreadPoolBulkhead threadPoolBulkhead;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final TimeLimiter timeLimiter;
    private final ScheduledExecutorService scheduledExecutorService;

    @Autowired
    public RestControllerB(@Qualifier("backendServiceB") BackendService backendServiceB,
                           CircuitBreakerRegistry circuitBreakerRegistry,
                           BulkheadRegistry bulkheadRegistry,
                           ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
                           RetryRegistry retryRegistry,
                           RateLimiterRegistry rateLimiterRegistry,
                           TimeLimiterRegistry timeLimiterRegistry) {
        this.backendServiceB = backendServiceB;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(BACKEND_B);
        this.bulkhead = bulkheadRegistry.bulkhead(BACKEND_B);
        this.threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(BACKEND_B);
        this.retry = retryRegistry.retry(BACKEND_B);
        this.rateLimiter = rateLimiterRegistry.rateLimiter(BACKEND_B);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(BACKEND_B);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(3);
    }

    @GetMapping("/failure")
    public String failure() {
        return execute(backendServiceB::failure);
    }

    @GetMapping("/success")
    public String success() {
        return execute(backendServiceB::success);
    }

    @GetMapping("/success-exception")
    public String successException() {
        return execute(backendServiceB::successException);
    }

    @GetMapping("/ignore")
    public String ignore() {
        return Decorators.ofSupplier(backendServiceB::ignoreException)
                .withCircuitBreaker(circuitBreaker)
                .withBulkhead(bulkhead)
                .get();
    }

    @GetMapping("/mono-success")
    public Mono<String> monoSuccess() {
        return execute(backendServiceB::monoSuccess);
    }

    @GetMapping("/mono-failure")
    public Mono<String> monoFailure() {
        return execute(backendServiceB::monoFailure);
    }

    @GetMapping("/mono-timeout")
    public Mono<String> monoTimeout() {
        return executeWithFallback(backendServiceB.monoTimeout(), this::monoFallback);
    }

    @GetMapping("/flux-success")
    public Flux<String> fluxSuccess() {
        return execute(backendServiceB::fluxSuccess);
    }

    @GetMapping("/flux-failure")
    public Flux<String> fluxFailure() {
        return execute(backendServiceB::fluxFailure);
    }

    @GetMapping("/flux-timeout")
    public Flux<String> fluxTimeout() {
        return executeWithFallback(backendServiceB.fluxTimeout(), this::fluxFallback);
    }

    @GetMapping("/future-success")
    public CompletableFuture<String> futureSuccess() {
        return executeAsync(backendServiceB::success);
    }

    @GetMapping("/future-failure")
    public CompletableFuture<String> futureFailure() {
        return executeAsync(backendServiceB::failure);
    }

    @GetMapping("/future-timeout")
    public CompletableFuture<String> futureTimeout() {
        return executeAsyncWithFallback(this::timeout, this::fallback);
    }

    @GetMapping("/fallback")
    public String failureWithFallback() {
        return backendServiceB.failureWithFallback();
    }

    private String timeout(){
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "";
    }

    private <T> T execute(Supplier<T> supplier) {
        return Decorators.ofSupplier(supplier)
                .withCircuitBreaker(circuitBreaker)
                .withBulkhead(bulkhead)
                .withRetry(retry)
                .get();
    }

    private <T> Mono<T> executeWithFallback(Mono<T> publisher, Function<Throwable, Mono<T>> fallback) {
        return publisher
                .transform(TimeLimiterOperator.of(timeLimiter))
                .transform(BulkheadOperator.of(bulkhead))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(TimeoutException.class, fallback)
                .onErrorResume(CallNotPermittedException.class, fallback)
                .onErrorResume(BulkheadFullException.class, fallback);
    }

    private <T> Flux<T> executeWithFallback(Flux<T> publisher, Function<Throwable, Flux<T>> fallback){
        return publisher
                .transform(TimeLimiterOperator.of(timeLimiter))
                .transform(BulkheadOperator.of(bulkhead))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(TimeoutException.class, fallback)
                .onErrorResume(CallNotPermittedException.class, fallback)
                .onErrorResume(BulkheadFullException.class, fallback);
    }

    private <T> CompletableFuture<T> executeAsync(Supplier<T> supplier){
        return Decorators.ofSupplier(supplier)
                .withThreadPoolBulkhead(threadPoolBulkhead)
                .withTimeLimiter(timeLimiter, scheduledExecutorService)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry,scheduledExecutorService)
                .get().toCompletableFuture();
    }

    private <T> CompletableFuture<T> executeAsyncWithFallback(Supplier<T> supplier, Function<Throwable, T> fallback){
        return Decorators.ofSupplier(supplier)
                .withThreadPoolBulkhead(threadPoolBulkhead)
                .withTimeLimiter(timeLimiter, scheduledExecutorService)
                .withCircuitBreaker(circuitBreaker)
                .withFallback(asList(TimeoutException.class, CallNotPermittedException.class, BulkheadFullException.class),
                        fallback)
                .get().toCompletableFuture();
    }

    private String fallback(Throwable ex) {
        return "Recovered: " + ex.toString();
    }

    private Mono<String> monoFallback(Throwable ex) {
        return Mono.just("Recovered: " + ex.toString());
    }

    private Flux<String> fluxFallback(Throwable ex) {
        return Flux.just("Recovered: " + ex.toString());
    }

}
