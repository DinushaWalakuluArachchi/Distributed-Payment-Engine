package com.paymentengine.fraudservice.repository;

import com.paymentengine.fraudservice.domain.FraudSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudSignalRepository extends JpaRepository<FraudSignal, UUID> {
    List<FraudSignal> findByPaymentId(UUID paymentId);
}
