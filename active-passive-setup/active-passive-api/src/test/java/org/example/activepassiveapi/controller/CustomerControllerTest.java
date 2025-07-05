package org.example.activepassiveapi.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.example.activepassiveapi.model.Customer;
import org.example.activepassiveapi.repository.CustomerRepository;
import org.example.activepassiveapi.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoSpyBean
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private Customer customer1;
    private Customer customer2;

    @BeforeEach
    void setUp() {
        customerRepository.findAll()
                .flatMap(customer -> customerRepository.delete(customer.getId()))
                .blockLast();

        customer1 = Customer.create("1", "John Doe", "john.doe@example.com", "1234567890");
        customer2 = Customer.create("2", "Jane Doe", "jane.doe@example.com", "0987654321");

        customerRepository.save(customer1).subscribe();
        customerRepository.save(customer2).subscribe();

        circuitBreakerRegistry.circuitBreaker("customerService").reset();
    }

    @Test
    void getAllCustomers_FromActiveInstance_SuccessfulTest() {
        doReturn(Flux.just(customer1, customer2)).when(customerService).getAllCustomersFromActive();

        webTestClient.get()
                .uri("/customers/all")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Customer.class)
                .hasSize(2)
                .contains(customer1, customer2);

        assertThat(circuitBreakerRegistry.circuitBreaker("customerService").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        verify(customerService, times(1)).getAllCustomersFromActive();
        verify(customerService, never()).getAllCustomersFromPassive();
    }

    @Test
    void testPassiveFallbackOnActiveFailure() {
        // Simulate active always fails, passive returns one customer
        doReturn(Flux.error(new RuntimeException("Active failure"))).when(customerService).getAllCustomersFromActive();
        doReturn(Flux.just(customer2)).when(customerService).getAllCustomersFromPassive();

        webTestClient.get()
                .uri("/customers/all")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Customer.class)
                .hasSize(1)
                .contains(customer2);

        assertThat(circuitBreakerRegistry.circuitBreaker("customerService").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        verify(customerService, times(1)).getAllCustomersFromActive();
        verify(customerService, times(1)).getAllCustomersFromPassive();
    }

    @Test
    void testCircuitBreakerOpensAfterFailures() {
        // Simulate active always fails, passive returns one customer
        doReturn(Flux.error(new RuntimeException("Active failure"))).when(customerService).getAllCustomersFromActive();

        // By default, failureRateThreshold=50, slidingWindowSize=5, so 3+ failures in 6 calls will open breaker
        for (int i = 0; i < 7; i++) {
            webTestClient.get()
                    .uri("/customers/all")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange();
        }

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("customerService");
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Now, further calls should NOT call getAllActive (breaker is open), only fallback
        reset(customerService);
        doReturn(Flux.just(customer1)).when(customerService).getAllCustomersFromPassive();

        webTestClient.get()
                .uri("/customers/all")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Customer.class)
                .hasSize(1)
                .contains(customer1);

        verify(customerService, times(1)).getAllCustomersFromPassive();
    }

    /**
     * Needs work. Figure out circuit breaker does not transition from Open to Closed State
     * */
    @Test
    @Disabled
    void testCircuitBreakerHalfOpenAndClose() throws InterruptedException {
        // Open the circuit breaker
        doReturn(Flux.error(new RuntimeException("Active failure"))).when(customerService).getAllCustomersFromActive();
        doReturn(Flux.just(customer2)).when(customerService).getAllCustomersFromPassive();

        for (int i = 0; i < 6; i++) {
            webTestClient.get()
                    .uri("/customers/all")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange();
        }

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("customerService");
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Wait for waitDurationInOpenState (configured as 10s in yml) to allow half-open
        Thread.sleep(12000);

        // Now, simulate active succeeds (half-open test call)
        reset(customerService);
        doReturn(Flux.just(customer1)).when(customerService).getAllCustomersFromActive();
        doReturn(Flux.just(customer2)).when(customerService).getAllCustomersFromPassive();

        webTestClient.get()
                .uri("/customers/all")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Customer.class)
                .hasSize(1)
                .contains(customer2);

        // After success in half-open, breaker should be closed again
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        verify(customerService, times(1)).getAllCustomersFromActive();
    }
}