package com.circuitbreaker.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.collection.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerTest extends AbstractCircuitBreakerTest {

    @Test
    public void shouldOpenBackendACircuitBreaker() {
        Stream.rangeClosed(1,2).forEach((count) -> produceFailure(BACKEND_A));

        checkHealthStatus(BACKEND_A, CircuitBreaker.State.OPEN);
    }

    @Test
    public void shouldCloseBackendACircuitBreaker() {
        transitionToOpenState(BACKEND_A);
        circuitBreakerRegistry.circuitBreaker(BACKEND_A).transitionToHalfOpenState();

        Stream.rangeClosed(1,3).forEach((count) -> produceSuccess(BACKEND_A));
        checkHealthStatus(BACKEND_A, CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldOpenBackendBCircuitBreaker() {
        Stream.rangeClosed(1,2).forEach((count) -> produceFailure(BACKEND_B));

        checkHealthStatus(BACKEND_B, CircuitBreaker.State.OPEN);
    }

    @Test
    public void shouldCloseBackendBCircuitBreaker() {
        transitionToOpenState(BACKEND_B);
        circuitBreakerRegistry.circuitBreaker(BACKEND_B).transitionToHalfOpenState();

        Stream.rangeClosed(1,3).forEach((count) -> produceSuccess(BACKEND_B));
        checkHealthStatus(BACKEND_B, CircuitBreaker.State.CLOSED);
    }

    private void produceSuccess(String backend) {
        ResponseEntity<String> response = restTemplate.getForEntity("/" + backend + "/success", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void produceFailure(String backend) {
        ResponseEntity<String> response = restTemplate.getForEntity("/" + backend + "/failure", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
