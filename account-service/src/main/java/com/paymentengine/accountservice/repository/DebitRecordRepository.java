package com.paymentengine.accountservice.repository;

import com.paymentengine.accountservice.domain.DebitRecord;
import com.paymentengine.accountservice.domain.DebitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DebitRecordRepository extends JpaRepository<DebitRecord, UUID> {

    Optional<DebitRecord> findByPaymentIdAndStatus(
            UUID paymentId, DebitStatus status);
}
