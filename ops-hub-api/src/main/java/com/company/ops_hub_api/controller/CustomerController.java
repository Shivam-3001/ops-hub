package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.domain.Customer;
import com.company.ops_hub_api.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Get all customers
     * Returns all customers if user has VIEW_CUSTOMERS permission
     * Otherwise returns only allocated customers
     */
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    /**
     * Get a specific customer by ID
     * Users can only access customers they have permission to view
     */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    /**
     * Get customers by area
     * Filtered by permissions
     */
    @GetMapping("/area/{areaId}")
    public ResponseEntity<List<Customer>> getCustomersByArea(@PathVariable Long areaId) {
        List<Customer> customers = customerService.getCustomersByArea(areaId);
        return ResponseEntity.ok(customers);
    }
}
