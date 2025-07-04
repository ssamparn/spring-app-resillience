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
@RequestMapping("/north-europe")
public class NorthEuropeRestController {

    private final BackendService northEuropeService;

    @Autowired
    public NorthEuropeRestController(@Qualifier("northEuropeService") BackendService northEuropeService) {
        this.northEuropeService = northEuropeService;
    }

    @GetMapping("/failure")
    public String failure() {
        return northEuropeService.failure();
    }

    @GetMapping("/success")
    public String success() {
        return northEuropeService.success();
    }

    @GetMapping("/success-exception")
    public String successException() {
        return northEuropeService.successException();
    }

    @GetMapping("/ignore")
    public String ignore() {
        return northEuropeService.ignoreException();
    }

    @GetMapping("/mono-success")
    public Mono<String> monoSuccess() {
        return northEuropeService.monoSuccess();
    }

    @GetMapping("/mono-failure")
    public Mono<String> monoFailure() {
        return northEuropeService.monoFailure();
    }

    @GetMapping("/mono-timeout")
    public Mono<String> monoTimeout() {
        return northEuropeService.monoTimeout();
    }

    @GetMapping("/flux-success")
    public Flux<String> fluxSuccess() {
        return northEuropeService.fluxSuccess();
    }

    @GetMapping("/flux-failure")
    public Flux<String> fluxFailure() {
        return northEuropeService.fluxFailure();
    }

    @GetMapping("/flux-timeout")
    public Flux<String> fluxTimeout() {
        return northEuropeService.fluxTimeout();
    }

    @GetMapping("/future-success")
    public CompletableFuture<String> futureSuccess() {
        return northEuropeService.futureSuccess();
    }

    @GetMapping("/future-failure")
    public CompletableFuture<String> futureFailure() {
        return northEuropeService.futureFailure();
    }

    @GetMapping("/future-timeout")
    public CompletableFuture<String> futureTimeout() {
        return northEuropeService.futureTimeout();
    }

    @GetMapping("/fallback")
    public String failureWithFallback() {
        return northEuropeService.failureWithFallback();
    }
}
