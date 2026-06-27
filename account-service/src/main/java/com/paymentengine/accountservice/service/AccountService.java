package com.paymentengine.accountservice.service;

import com.paymentengine.accountservice.domain.Account;
import com.paymentengine.accountservice.domain.AccountNotFoundException;
import com.paymentengine.accountservice.domain.DebitRecord;
import com.paymentengine.accountservice.domain.DebitStatus;
import com.paymentengine.accountservice.repository.AccountRepository;
import com.paymentengine.accountservice.repository.DebitRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final DebitRecordRepository debitRecordRepo;
    private final KafkaTemplate<String, Object> kafka;

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100))
    @Transactional
    public void debit(UUID paymentId, UUID ownerId, BigDecimal amount) {
        log.info("Debitigng {} from account owned by {}", amount, ownerId);

        Account account = accountRepo.findByOwnerId(ownerId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for owner: " + ownerId));

        account.debit(amount);
        accountRepo.save(account);

        debitRecordRepo.save(DebitRecord.of(paymentId, account.getId(), amount));

        kafka.send("account.events", paymentId.toString(),
                new DebitNotice(paymentId, ownerId, amount));

        log.info("Debit successful - paymentId={}", paymentId);
    }

    @Transactional
    public void credit(UUID paymentId, UUID ownerId, BigDecimal amount) {
        log.info("Creating {} to account owned by {}", amount, ownerId);

        Account account = accountRepo.findByOwnerId(ownerId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found for owner: " + ownerId));
        account.credit(amount);
        accountRepo.save(account);

        kafka.send("account.events", paymentId.toString(),
                new CreditedNotice(paymentId, ownerId, amount));

        log.info("Credit successful - paymentId-{}", paymentId);
    }

    @Transactional
    public void compensate(UUID paymentId,UUID ownerId){
        log.info("Compensating debit for paymentId={}", paymentId);

        DebitRecord record = debitRecordRepo
                .findByPaymentIdAndStatus(paymentId, DebitStatus.DEBITED)
                .orElseThrow(()-> new IllegalStateException(
                        "No Active debit found for paymentId: " + paymentId));

        Account account = accountRepo.findById(record.getAccountId())
                .orElseThrow(()-> new AccountNotFoundException(
                        "Account not found: " + record.getAccountId()));

        account.credit(record.getAmount());
        accountRepo.save(account);

        record.markCompensated();
        debitRecordRepo.save(record);

        kafka.send("account.events", paymentId.toString(),
        new CompensatedNotice(paymentId, ownerId, record.getAmount()));

        log.info("Compensation complete for paymentId={}", paymentId);

    }

    @Transactional(readOnly = true)
    public AccountBalanceDto getBalance(UUID ownerId){
        Account account = accountRepo.findByOwnerId(ownerId)
                .orElseThrow(()-> new AccountNotFoundException(
                        "Account not found for owner: " + ownerId));

        return new AccountBalanceDto(
                account.getId(), account.getOwnerId(),
                account.getBalance(), account.getCurrency());
    }



    record DebitNotice(UUID paymentId, UUID ownerId, BigDecimal amount) {
    }

    record CreditedNotice(UUID paymentId, UUID ownerId, BigDecimal amount) {
    }

    private record CompensatedNotice(UUID paymentId, UUID ownerId, BigDecimal amount) {
    }

    public record AccountBalanceDto(UUID accountId,UUID ownerId, BigDecimal balance,String currency) {
    }
}
