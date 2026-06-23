package com.paymentengine.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class PaymentStatusTransitionTest {

    @Test
    void cannotJumpFromInitiatedCompleted() {
        Payment payment = Payment.initiate(
                "Key-1", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "USD");

        assertThatThrownBy(() -> payment.transitionTo(PaymentStatus.COMPLETED))
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining("INITIATED")
                .hasMessageContaining("COMPLETED");

    }

    @Test
    void validTransitionPathSucceed() {
        Payment payment = Payment.initiate(
                "key-2", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50.00"), "USD");

        payment.transitionTo(PaymentStatus.FRAUD_CHECKING);
        payment.transitionTo(PaymentStatus.FRAUD_APPROVED);
        payment.transitionTo(PaymentStatus.DEBITING);
        payment.transitionTo(PaymentStatus.CREDITED);
        payment.transitionTo(PaymentStatus.COMPLETED);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getStatus().isTerminal()).isTrue();
    }

    @Test
    void cannotTransitionOutOfTerminalState() {

        Payment payment = Payment.initiate(
                "key-3", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("25.00"), "USD");

        payment.transitionTo(PaymentStatus.FRAUD_CHECKING);
        payment.transitionTo(PaymentStatus.FRAUD_REJECTED);

        assertThatThrownBy(()-> payment.transitionTo(PaymentStatus.FRAUD_CHECKING)).isInstanceOf(IllegalStatusTransitionException.class);
    }

}
