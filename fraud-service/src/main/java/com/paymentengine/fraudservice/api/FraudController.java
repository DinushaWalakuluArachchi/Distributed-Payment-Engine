package com.paymentengine.fraudservice.api;

import com.paymentengine.fraudservice.service.FraudScoringService;
import com.paymentengine.fraudservice.service.FraudSignalCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudScoringService fraudScoringService;

    @PostMapping("/check")
    public ResponseEntity<FraudScoringService.FraudCheckResult> check(@RequestBody FraudCheckApiRequest request){

        var result = fraudScoringService.evaluate(
                new FraudSignalCheck.FraudCheckRequest(
                        request.paymentId,
                        request.senderId,
                        request.receiverId,
                        request.amount,
                        request.currency
                )
        );

        return ResponseEntity.ok(result);
    }

    record FraudCheckApiRequest(
            UUID paymentId, UUID senderId, UUID receiverId,
            BigDecimal amount, String currency
    ){}




}
