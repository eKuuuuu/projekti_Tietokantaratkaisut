package fi.metropolia.erikroi.projekti.customers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;

    public CustomerController(CustomerRepository customerRepository, CustomerAddressRepository addressRepository) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer details) {
        return customerRepository.findById(id).map(customer -> {
            customer.setFirstName(details.getFirstName());
            customer.setLastName(details.getLastName());
            customer.setEmail(details.getEmail());
            return ResponseEntity.ok(customerRepository.save(customer));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/addresses")
    public List<CustomerAddress> getCustomerAddresses(@PathVariable Long id) {
        return addressRepository.findByCustomerId(id);
    }

    @PostMapping("/{id}/addresses")
    public ResponseEntity<CustomerAddress> addAddress(@PathVariable Long id, @RequestBody CustomerAddress address) {
        return customerRepository.findById(id).map(customer -> {
            address.setCustomer(customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(addressRepository.save(address));
        }).orElse(ResponseEntity.notFound().build());
    }
}