package com.possystem.repository;

import com.possystem.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByEmailIgnoreCase(String email);

    List<Customer> findAllByOrderByCreatedAtDesc();

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
        SELECT c
        FROM Customer c
        WHERE c.status = 'ACTIVE'
          AND (
              LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
              OR c.phone LIKE CONCAT('%', :query, '%')
          )
        ORDER BY c.fullName ASC
        """)
    List<Customer> searchActiveCustomers(
            @Param("query") String query
    );
}