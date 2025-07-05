package org.example.activepassiveapi.service;

import com.github.javafaker.Faker;
import org.example.activepassiveapi.model.Customer;
import org.example.activepassiveapi.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final Faker faker = Faker.instance();

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    // Active instance: might fail randomly for demonstration
    public Flux<Customer> getAllCustomersFromActive() {
        if (Math.random() < 0.5) {
            return Flux.error(new RuntimeException("Active failed!"));
        }
        return repository.findAll();
    }

    public Mono<Customer> getByCustomerByIdFromActive(String id) {
        if (Math.random() < 0.5) {
            return Mono.error(new RuntimeException("Active failed!"));
        }
        return repository.findById(id);
    }

    public Mono<Customer> createCustomerInActive(Customer customer) {
        if (Math.random() < 0.5) {
            return Mono.error(new RuntimeException("Active failed!"));
        }
        customer.setId(UUID.randomUUID().toString());
        return repository.save(customer);
    }

    public Mono<Customer> updateCustomerInActive(String id, Customer customer) {
        if (Math.random() < 0.5) {
            return Mono.error(new RuntimeException("Active failed!"));
        }
        return repository.update(id, customer);
    }

    public Mono<Void> deleteCustomerFromActive(String id) {
        if (Math.random() < 0.5) {
            return Mono.error(new RuntimeException("Active failed!"));
        }
        return repository.delete(id);
    }

    /**
     * Passive instance: always generates new fake customers, never fails
     * */
    public Flux<Customer> getAllCustomersFromPassive() {
        return Flux.range(0, 5)
                .map(i -> Customer.create(
                        UUID.randomUUID().toString(),
                        faker.name().fullName(),
                        faker.internet().emailAddress(),
                        faker.phoneNumber().phoneNumber()
                ));
    }

    public Mono<Customer> getCustomerByIdFromPassive(String id) {
        return Mono.just(Customer.create(
                id,
                faker.name().fullName(),
                faker.internet().emailAddress(),
                faker.phoneNumber().phoneNumber()
        ));
    }

    public Mono<Customer> createCustomerInPassive(Customer c) {
        return Mono.just(Customer.create(
                UUID.randomUUID().toString(),
                c.getName(),
                c.getEmail(),
                c.getPhone()
        ));
    }

    public Mono<Customer> updateCustomerInPassive(String id, Customer c) {
        return Mono.just(Customer.create(
                id,
                c.getName() + " (passive)",
                c.getEmail(),
                c.getPhone()
        ));
    }

    public Mono<Void> deleteCustomerFromPassive(String id) {
        return Mono.empty();
    }
}
