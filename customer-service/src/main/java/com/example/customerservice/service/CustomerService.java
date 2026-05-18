package com.example.customerservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.customerservice.entity.Customer;
import com.example.customerservice.repository.CustomerRepository;

@Service
public class CustomerService {
    @Autowired
    private CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer request) {
        return customerRepository.findById(id)
            .map(customer -> {
                if (request.getName() != null) customer.setName(request.getName());
                if (request.getEmail() != null) customer.setEmail(request.getEmail());
                if (request.getAddress() != null) customer.setAddress(request.getAddress());
                if (request.getUsername() != null) customer.setUsername(request.getUsername());
                if (request.getPhone() != null) customer.setPhone(request.getPhone());
                if (request.getBalance() != null) customer.setBalance(request.getBalance());
                return customerRepository.save(customer);
            }).orElse(null);
    }

    @Transactional
    public boolean deleteCustomer(Long id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Potong saldo customer secara atomik menggunakan JPQL UPDATE.
     * Mencegah race condition READ-CHECK-WRITE yang bisa menyebabkan saldo negatif
     * jika dua request pembayaran masuk hampir bersamaan untuk customer yang sama.
     *
     * @return saldo baru setelah pemotongan
     * @throws RuntimeException jika customer tidak ditemukan atau saldo tidak mencukupi
     */
    @Transactional
    public Double deductBalanceAtomic(Long customerId, Double amount) {
        int updated = customerRepository.deductBalance(customerId, amount);
        if (updated == 0) {
            // Cek apakah customer tidak ada, atau saldo tidak cukup
            Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan: " + customerId));
            throw new RuntimeException("Saldo tidak cukup. Saldo saat ini: Rp" + c.getBalance() + ", dibutuhkan: Rp" + amount);
        }
        return customerRepository.findById(customerId)
            .map(Customer::getBalance)
            .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan: " + customerId));
    }
}
