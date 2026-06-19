CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS accounts (
                                        id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id      UUID NOT NULL UNIQUE,
    balance       NUMERIC(19,4) NOT NULL,
    currency      VARCHAR(3) NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS transactions (
                                            id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    sender_id       UUID NOT NULL,
    receiver_id     UUID NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS debit_records (
                                             id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id    UUID NOT NULL,
    account_id    UUID NOT NULL,
    amount        NUMERIC(19,4) NOT NULL,
    status        VARCHAR(50) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS saga_steps (
                                          id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id    UUID NOT NULL,
    step_name     VARCHAR(100) NOT NULL,
    status        VARCHAR(50) NOT NULL,
    executed_at   TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS fraud_signals (
                                             id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id    UUID NOT NULL,
    signal_type   VARCHAR(100) NOT NULL,
    score         NUMERIC(5,4) NOT NULL,
    flagged       BOOLEAN NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_saga_steps_payment_id ON saga_steps(payment_id);
CREATE INDEX idx_debit_records_payment_id ON debit_records(payment_id);