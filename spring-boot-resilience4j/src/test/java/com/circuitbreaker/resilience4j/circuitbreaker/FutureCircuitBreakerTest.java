package com.circuitbreaker.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.collection.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class FutureCircuitBreakerTest extends AbstractCircuitBreakerTest {

    @Test
    public void shouldOpenBackendACircuitBreaker() {
        Stream.rangeClosed(1,2).forEach((count) -> produceFailure(NORTH_EUROPE));

        checkHealthStatus(NORTH_EUROPE, CircuitBreaker.State.OPEN);
    }

    @Test
    public void shouldCloseBackendACircuitBreaker() {
        transitionToOpenState(NORTH_EUROPE);
        circuitBreakerRegistry.circuitBreaker(NORTH_EUROPE).transitionToHalfOpenState();

        Stream.rangeClosed(1,3).forEach((count) -> produceSuccess(NORTH_EUROPE));
        checkHealthStatus(NORTH_EUROPE, CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldOpenBackendBCircuitBreaker() {
        Stream.rangeClosed(1,2).forEach((count) -> produceFailure(WEST_EUROPE));

        checkHealthStatus(WEST_EUROPE, CircuitBreaker.State.OPEN);
    }

    @Test
    public void shouldCloseBackendBCircuitBreaker() {
        transitionToOpenState(WEST_EUROPE);
        circuitBreakerRegistry.circuitBreaker(WEST_EUROPE).transitionToHalfOpenState();

        Stream.rangeClosed(1,3).forEach((count) -> produceSuccess(WEST_EUROPE));
        checkHealthStatus(WEST_EUROPE, CircuitBreaker.State.CLOSED);
    }

    private void produceSuccess(String backend) {
        ResponseEntity<String> response = restTemplate.getForEntity("/" + backend + "/future-success", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void produceFailure(String backend) {
        ResponseEntity<String> response = restTemplate.getForEntity("/" + backend + "/future-failure", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
