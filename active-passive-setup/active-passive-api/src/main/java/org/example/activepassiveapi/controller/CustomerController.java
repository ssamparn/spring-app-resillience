package org.example.activepassiveapi.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.example.activepassiveapi.model.Customer;
import org.example.activepassiveapi.service.CustomerService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/all")
    @CircuitBreaker(name = "customerService", fallbackMethod = "getAllPassive")
    public Flux<Customer> getAllCustomers() {
        log.info("getAllActive");
        return customerService.getAllCustomersFromActive();
    }

    @GetMapping("/{customerId}")
    @CircuitBreaker(name = "customerService", fallbackMethod = "getByIdPassive")
    public Mono<Customer> getCustomerById(@PathVariable String customerId) {
        return customerService.getByCustomerByIdFromActive(customerId);
    }

    @PostMapping
    @CircuitBreaker(name = "customerService", fallbackMethod = "createPassive")
    public Mono<Customer> createNewCustomer(@RequestBody Customer customer) {
        return customerService.createCustomerInActive(customer);
    }

    @PutMapping("/{customerId}")
    @CircuitBreaker(name = "customerService", fallbackMethod = "updatePassive")
    public Mono<Customer> updateCustomer(@PathVariable String customerId, @RequestBody Customer customer) {
        return customerService.updateCustomerInActive(customerId, customer);
    }

    @DeleteMapping("/{customerId}")
    @CircuitBreaker(name = "customerService", fallbackMethod = "deletePassive")
    public Mono<Void> deleteCustomer(@PathVariable String customerId) {
        return customerService.deleteCustomerFromActive(customerId);
    }

    // Fallbacks
    public Flux<Customer> getAllPassive(Throwable t) {
        log.info("getAllPassive", t);
        return customerService.getAllCustomersFromPassive();
    }

    public Mono<Customer> getByIdPassive(String customerId, Throwable t) {
        return customerService.getCustomerByIdFromPassive(customerId);
    }

    public Mono<Customer> createPassive(Customer customer, Throwable t) {
        return customerService.createCustomerInPassive(customer);
    }

    public Mono<Customer> updatePassive(String customerId, Customer customer, Throwable t) {
        return customerService.updateCustomerInPassive(customerId, customer);
    }

    public Mono<Void> deletePassive(String customerId, Throwable t) {
        return customerService.deleteCustomerFromPassive(customerId);
    }

}
