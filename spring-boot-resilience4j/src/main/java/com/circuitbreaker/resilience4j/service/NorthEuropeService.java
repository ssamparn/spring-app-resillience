package com.circuitbreaker.resilience4j.service;

import com.circuitbreaker.resilience4j.exception.BusinessException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.vavr.control.Try;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * This Service shows how to use the CircuitBreaker annotation.
 */

@Service
public class NorthEuropeService implements BackendService {

    private static final String NORTH_EUROPE = "north-europe";

    @Override
    @CircuitBreaker(name = NORTH_EUROPE)
    @Bulkhead(name = NORTH_EUROPE)
    @Retry(name = NORTH_EUROPE)
    public String failure() {
        throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "This is a remote exception");
    }

    @Override
    @CircuitBreaker(name = NORTH_EUROPE)
    @Bulkhead(name = NORTH_EUROPE)
    @Retry(name = NORTH_EUROPE)
    public String success() {
        return "Successful response from backend A";
    }

    @Override
    @CircuitBreaker(name = NORTH_EUROPE)
    @Bulkhead(name = NORTH_EUROPE)
    public String successException() {
        throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "This is a remote client exception");
    }

    @Override
    @CircuitBreaker(name = NORTH_EUROPE)
    @Bulkhead(name = NORTH_EUROPE)
    public String ignoreException() {
        throw new BusinessException("This exception is ignored by the CircuitBreaker of backend A");
    }

    @Override
    @TimeLimiter(name = NORTH_EUROPE)
    @CircuitBreaker(name = NORTH_EUROPE)
    @Bulkhead(name = NORTH_EUROPE)
    @Retry(name = NORTH_EUROPE)
    public Mono<String> monoSuccess() {
        return Mono.just("Hello World Mono from backend A");
    }

    @Override
    @CircuitBreaker(name = NORTH_EUROPE)
    @Bulkhead(name = NORTH_EUROPE)
    @Retry(name = NORTH_EUROPE)
    public Mono<String> monoFailure() {
        return Mono.error(new IOException("BAM!"));
    }

    @Override
    @CircuitBreaker(name = NORTH_EUROPE, fallbackMethod = "fallback")
    public String failureWithFallback() {
        return failure();
    }

    @Override
    @TimeLimiter(name = NORTH_EUROPE)
    @Bulkhead(name = NORTH_EUROPE)
    @CircuitBreaker(name = NORTH_EUROPE, fallbackMethod = "monoFallback")
    public Mono<String> monoTimeout() {
        return Mono.just("Hello World from backend A")
                .delayElement(Duration.ofSeconds(10));
    }

    @Override
    @TimeLimiter(name = NORTH_EUROPE)
    @CircuitBreaker(name = NORTH_EUROPE)
    @Retry(name = NORTH_EUROPE)
    public Flux<String> fluxSuccess() {
        return Flux.just("Hello", "World");
    }

    @Override
    @CircuitBreaker(name = NORTH_EUROPE)
    @Bulkhead(name = NORTH_EUROPE)
    @Retry(name = NORTH_EUROPE)
    public Flux<String> fluxFailure() {
        return Flux.error(new IOException("BAM!"));
    }

    @Override
    @TimeLimiter(name = NORTH_EUROPE)
    @CircuitBreaker(name = NORTH_EUROPE, fallbackMethod = "fluxFallback")
    public Flux<String> fluxTimeout() {
        return Flux.just("Hello World from backend A")
                .delayElements(Duration.ofSeconds(10));
    }

    @Override
    @Bulkhead(name = NORTH_EUROPE, type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = NORTH_EUROPE)
    @CircuitBreaker(name = NORTH_EUROPE)
    @Retry(name = NORTH_EUROPE)
    public CompletableFuture<String> futureSuccess() {
        return CompletableFuture.completedFuture("Hello World from backend A");
    }

    @Override
    @Bulkhead(name = NORTH_EUROPE, type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = NORTH_EUROPE)
    @CircuitBreaker(name = NORTH_EUROPE)
    @Retry(name = NORTH_EUROPE)
    public CompletableFuture<String> futureFailure() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new IOException("BAM!"));
        return future;
    }

    @Override
    @Bulkhead(name = NORTH_EUROPE, type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = NORTH_EUROPE)
    @CircuitBreaker(name = NORTH_EUROPE, fallbackMethod = "futureFallback")
    public CompletableFuture<String> futureTimeout() {
        Try.run(() -> Thread.sleep(5000));
        return CompletableFuture.completedFuture("Hello World from backend A");
    }

    private String fallback(HttpServerErrorException ex) {
        return "Recovered HttpServerErrorException: " + ex.getMessage();
    }

    private String fallback(Exception ex) {
        return "Recovered: " + ex.toString();
    }

    private CompletableFuture<String> futureFallback(TimeoutException ex) {
        return CompletableFuture.completedFuture("Recovered specific TimeoutException: " + ex.toString());
    }

    private CompletableFuture<String> futureFallback(BulkheadFullException ex) {
        return CompletableFuture.completedFuture("Recovered specific BulkheadFullException: " + ex.toString());
    }

    private CompletableFuture<String> futureFallback(CallNotPermittedException ex) {
        return CompletableFuture.completedFuture("Recovered specific CallNotPermittedException: " + ex.toString());
    }

    private Mono<String> monoFallback(Exception ex) {
        return Mono.just("Recovered: " + ex.toString());
    }

    private Flux<String> fluxFallback(Exception ex) {
        return Flux.just("Recovered: " + ex.toString());
    }
}
