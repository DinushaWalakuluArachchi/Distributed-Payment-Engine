package com.paymentengine.payment.saga;

import com.paymentengine.payment.domain.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SagaStepTracker extends JpaRepository<SagaStep, UUID> {
    List<SagaStep> findByPaymentIdOrderByExecutedAtAsc(UUID paymentId);
}
