package com.paymentengine.accountservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CollectionId;
import org.springframework.retry.annotation.EnableRetry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID ownerId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updateAt;

    @PrePersist
    private void onCreate(){
        createdAt = updateAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate(){
        updateAt = Instant.now();
    }

    public static Account create(UUID ownerId, BigDecimal initialBalance, String currency){
            Account account = new Account();
            account.ownerId = ownerId;
            account.balance = initialBalance;
            account.currency = currency;
            return account;
    }

    public void debit(BigDecimal amount){
        if (amount.compareTo(BigDecimal.ZERO)<= 0){
            throw  new IllegalArgumentException("Debit amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0){
            throw new InsufficientFundException("Balance %s insufficient for debit of %s".formatted(balance, amount));
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount){
        if (amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

}
