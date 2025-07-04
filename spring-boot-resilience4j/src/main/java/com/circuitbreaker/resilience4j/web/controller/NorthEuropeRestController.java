package com.circuitbreaker.resilience4j.web.controller;

import com.circuitbreaker.resilience4j.service.BackendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/backend-a")
public class NorthEuropeRestController {

    private final BackendService backendServiceA;

    @Autowired
    public NorthEuropeRestController(@Qualifier("backendServiceA") BackendService backendServiceA) {
        this.backendServiceA = backendServiceA;
    }

    @GetMapping("/failure")
    public String failure() {
        return backendServiceA.failure();
    }

    @GetMapping("/success")
    public String success() {
        return backendServiceA.success();
    }

    @GetMapping("/success-exception")
    public String successException() {
        return backendServiceA.successException();
    }

    @GetMapping("/ignore")
    public String ignore() {
        return backendServiceA.ignoreException();
    }

    @GetMapping("/mono-success")
    public Mono<String> monoSuccess() {
        return backendServiceA.monoSuccess();
    }

    @GetMapping("/mono-failure")
    public Mono<String> monoFailure() {
        return backendServiceA.monoFailure();
    }

    @GetMapping("/mono-timeout")
    public Mono<String> monoTimeout() {
        return backendServiceA.monoTimeout();
    }

    @GetMapping("/flux-success")
    public Flux<String> fluxSuccess() {
        return backendServiceA.fluxSuccess();
    }

    @GetMapping("/flux-failure")
    public Flux<String> fluxFailure() {
        return backendServiceA.fluxFailure();
    }

    @GetMapping("/flux-timeout")
    public Flux<String> fluxTimeout() {
        return backendServiceA.fluxTimeout();
    }

    @GetMapping("/future-success")
    public CompletableFuture<String> futureSuccess() {
        return backendServiceA.futureSuccess();
    }

    @GetMapping("/future-failure")
    public CompletableFuture<String> futureFailure() {
        return backendServiceA.futureFailure();
    }

    @GetMapping("/future-timeout")
    public CompletableFuture<String> futureTimeout() {
        return backendServiceA.futureTimeout();
    }

    @GetMapping("/fallback")
    public String failureWithFallback() {
        return backendServiceA.failureWithFallback();
    }
}
