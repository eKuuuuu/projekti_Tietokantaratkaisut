package fi.metropolia.erikroi.projekti.customers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-addresses")
public class CustomerAddressController {

    private final CustomerAddressRepository addressRepository;

    public CustomerAddressController(CustomerAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @GetMapping
    public List<CustomerAddress> getAllAddresses() {
        return addressRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerAddress> getAddressById(@PathVariable Long id) {
        return addressRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CustomerAddress> createAddress(@RequestBody CustomerAddress address) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressRepository.save(address));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerAddress> updateAddress(@PathVariable Long id, @RequestBody CustomerAddress details) {
        return addressRepository.findById(id).map(address -> {
            address.setStreetAddress(details.getStreetAddress());
            address.setCity(details.getCity());
            address.setPostalCode(details.getPostalCode());
            address.setCountry(details.getCountry());
            return ResponseEntity.ok(addressRepository.save(address));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        if (addressRepository.existsById(id)) {
            addressRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}