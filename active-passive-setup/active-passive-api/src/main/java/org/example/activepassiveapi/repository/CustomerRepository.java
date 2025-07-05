package org.example.activepassiveapi.repository;

import org.example.activepassiveapi.model.Customer;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CustomerRepository {

    private final Map<String, Customer> customerStore = new ConcurrentHashMap<>();

    public Flux<Customer> findAll() {
        return Flux.fromIterable(customerStore.values());
    }

    public Mono<Customer> findById(String id) {
        return Mono.justOrEmpty(customerStore.get(id));
    }

    public Mono<Customer> save(Customer customer) {
        customerStore.put(customer.getId(), customer);
        return Mono.just(customer);
    }

    public Mono<Customer> update(String id, Customer customer) {
        if (!customerStore.containsKey(id)) {
            return Mono.empty();
        }
        customer.setId(id);
        customerStore.put(id, customer);
        return Mono.just(customer);
    }

    public Mono<Void> delete(String id) {
        customerStore.remove(id);
        return Mono.empty();
    }
}
