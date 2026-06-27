package com.paymentengine.accountservice.api;


import com.paymentengine.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{ownerId}/balance")
    public ResponseEntity<AccountService.AccountBalanceDto> getBalance(@PathVariable UUID ownerId){
        return ResponseEntity.ok(accountService.getBalance(ownerId));
    }

    @PostMapping("/{ownerId}/debit")
    public ResponseEntity<Void> debit(@PathVariable UUID ownerId, @RequestBody DebitRequest request){
        accountService.debit(request.paymentId(), ownerId, request.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{ownerId}/credit")
    public ResponseEntity<Void> credit(@PathVariable UUID ownerId, @RequestBody CreditRequest request){
        accountService.credit(request.paymentId(),ownerId, request.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{ownerId}/compensate")
    public ResponseEntity<Void> compensate(
            @PathVariable UUID ownerId,
            @RequestBody CompensateRequest request) {
        accountService.compensate(request.paymentId(), ownerId);
        return ResponseEntity.ok().build();
    }


    record DebitRequest(UUID paymentId, BigDecimal amount) {}
    record CreditRequest(UUID paymentId, BigDecimal amount) {}
    record CompensateRequest(UUID paymentId) {}
}
