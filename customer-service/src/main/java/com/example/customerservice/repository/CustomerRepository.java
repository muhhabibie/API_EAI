package com.example.customerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.customerservice.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);

    /**
     * Potong saldo secara atomik. WHERE balance >= amount memastikan tidak bisa
     * terjadi saldo negatif meskipun ada concurrent request.
     * Mengembalikan jumlah row yang terupdate: 1 = sukses, 0 = saldo tidak cukup/customer tidak ada.
     */
    @Modifying
    @Query("UPDATE Customer c SET c.balance = c.balance - :amount WHERE c.id = :id AND c.balance >= :amount")
    int deductBalance(@Param("id") Long id, @Param("amount") Double amount);
}
