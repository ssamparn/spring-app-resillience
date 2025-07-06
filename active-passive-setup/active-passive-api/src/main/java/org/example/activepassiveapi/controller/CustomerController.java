package org.example.activepassiveapi.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
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
    @CircuitBreaker(name = "customer-service", fallbackMethod = "getAllPassive")
    @TimeLimiter(name = "customer-service")
    public Flux<Customer> getAllCustomers() {
        log.info("get-customers-from-active-instances");
        return customerService.getAllCustomersFromActive();
    }

    @GetMapping("/{customerId}")
    @CircuitBreaker(name = "customer-service", fallbackMethod = "getByIdPassive")
    public Mono<Customer> getCustomerById(@PathVariable String customerId) {
        return customerService.getByCustomerByIdFromActive(customerId);
    }

    @PostMapping
    @CircuitBreaker(name = "customer-service", fallbackMethod = "createPassive")
    public Mono<Customer> createNewCustomer(@RequestBody Customer customer) {
        return customerService.createCustomerInActive(customer);
    }

    @PutMapping("/{customerId}")
    @CircuitBreaker(name = "customer-service", fallbackMethod = "updatePassive")
    public Mono<Customer> updateCustomer(@PathVariable String customerId, @RequestBody Customer customer) {
        return customerService.updateCustomerInActive(customerId, customer);
    }

    @DeleteMapping("/{customerId}")
    @CircuitBreaker(name = "customer-service", fallbackMethod = "deletePassive")
    public Mono<Void> deleteCustomer(@PathVariable String customerId) {
        return customerService.deleteCustomerFromActive(customerId);
    }

    /**
     * Fallbacks for circuit breaker or timeout
     * */
    public Flux<Customer> getAllPassive(Throwable t) {
        log.info("get-customers-from-passive-instances");
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

    /**
     * Note: When you deploy “active” and “passive” controllers/services to separate servers (or regions),
     * the fallback between them is not handled automatically by the fallbackMethod parameter of the Resilience4j @CircuitBreaker annotation.
     * How fallbackMethod Works?
     *   - The fallbackMethod in Resilience4j only handles local fallbacks.
     *   - It is meant for methods within the same application (the same JVM process).
     *   - When the main method fails, Resilience4j calls the fallback method in the same bean/controller.
     * What Happens with Separate Deployments (to active endpoint in one server and passive endpoint in another server)?
     *   - If you deploy your “active” and “passive” services on different servers/regions,
     *     the fallbackMethod cannot magically call a method on another remote server.
     *   - The fallback method will still execute locally — so, for real cross-region failover, the fallback method must make an HTTP (or gRPC, etc.) call to the passive service.
     *
     *   @CircuitBreaker(name = "active-instance", fallbackMethod = "callPassiveInstance")
     *   public Mono<String> callActiveInstance(Mono<byte[]> dataMono) {
     *      // calls active-instance endpoint (local or via WebClient)
     *   }
     *
     *   public Mono<String> callPassiveInstance(Mono<byte[]> dataMono, Throwable t) {
     *      // Here: Make an HTTP call to the service in passive region
     *      return webClient.post()
     *          .uri("https://passive-instance.example.com/api/sign/data")
     *          .body(dataMono, byte[].class)
     *          .retrieve()
     *          .bodyToMono(String.class);
     *   }
     *
     *   - The fallbackMethod makes a remote call to the passive service endpoint.
     *   - This is how you achieve region-to-region failover.
     *
     *   User → [Active Service] --fails--> fallbackMethod() --calls--> [Passive Service on another server]
     * */
}
