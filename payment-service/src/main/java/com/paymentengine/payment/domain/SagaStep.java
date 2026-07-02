package com.paymentengine.payment.domain;

import com.paymentengine.payment.saga.SagaStepStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_steps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false, length = 100)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SagaStepStatus status;

    @Column
    private Instant executedAt;


    public static SagaStep create(UUID paymentId, String stepName){
        SagaStep sagaStep = new SagaStep();
        sagaStep.paymentId = paymentId;
        sagaStep.stepName = stepName;
        sagaStep.status = SagaStepStatus.PENDING;
        return sagaStep;
    }

    public void completed(){
        this.status = SagaStepStatus.COMPLETED;
        this.executedAt =Instant.now();
    }


    public void fail(){
        this.status = SagaStepStatus.FAILED;
        this.executedAt =Instant.now();
    }

    public void compensate(){
        this.status = SagaStepStatus.COMPENSATED;
        this.executedAt =Instant.now();
    }


}
