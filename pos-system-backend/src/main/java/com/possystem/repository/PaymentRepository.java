package com.possystem.repository;

import com.possystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySaleIdOrderByCreatedAtAsc(Long saleId);
    Optional<Payment> findBySale_Id(Long saleId);
}