package com.example.order_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.order_management.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}