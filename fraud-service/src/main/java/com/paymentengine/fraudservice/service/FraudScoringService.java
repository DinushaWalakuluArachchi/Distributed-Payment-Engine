package com.paymentengine.fraudservice.service;

import com.paymentengine.fraudservice.domain.FraudSignal;
import com.paymentengine.fraudservice.repository.FraudSignalRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
public class FraudScoringService {

    private final List<FraudSignalCheck> signalChecks;
    private final FraudSignalRepository signalRepository;
    private final MeterRegistry meterRegistry;

    @Value("${fraud.approval-threshold}")
    private BigDecimal approvalThreshold;

    public FraudScoringService(
            List<FraudSignalCheck> signalChecks,
            FraudSignalRepository signalRepository,
            MeterRegistry meterRegistry){
        this.signalChecks = signalChecks;
        this.signalRepository = signalRepository;
        this.meterRegistry = meterRegistry;
    }


    @Transactional
    public  FraudCheckResult evaluate(FraudSignalCheck.FraudCheckRequest request){

        // Each signal runs independently — Spring injects every @Component
        // implementing FraudSignalCheck into this list automatically
        BigDecimal totalScore = BigDecimal.ZERO;

        for (FraudSignalCheck check : signalChecks){
            BigDecimal score = check.evaluate(request);
            boolean flagged = score.compareTo(BigDecimal.valueOf(0.3)) > 0;

            signalRepository.save(FraudSignal.of(
                    request.paymentId(), check.getSignalType(), score, flagged));

            log.info("Signal {} scored {} for paymentId={}",
            check.getSignalType(),score, request.paymentId());

            totalScore = totalScore.add(score);
        }

        BigDecimal averageScore = totalScore.divide(BigDecimal.valueOf(signalChecks.size()),4, RoundingMode.HALF_UP);

        boolean approved = averageScore.compareTo(approvalThreshold)< 0;

        log.info("Payment {} composite score={}, approved={}",
                request.paymentId(),averageScore,approved);

        meterRegistry.counter("fraud.checks.total").increment();
        if (!approved){
            meterRegistry.counter("fraud.checks.rejected").increment();
        }
        meterRegistry.gauge("fraud.composite.score", averageScore.doubleValue());

        return new FraudCheckResult(request.paymentId(), averageScore, approved);
    }

    public record FraudCheckResult(
            java.util.UUID paymentId, BigDecimal score, boolean approved
    ){}


}
