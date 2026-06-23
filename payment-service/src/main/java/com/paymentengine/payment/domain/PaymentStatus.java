package com.paymentengine.payment.domain;

import java.util.EnumSet;
import java.util.Set;

public enum PaymentStatus {
    INITIATED {
        @Override public Set<PaymentStatus> nextStates(){
            return EnumSet.of(FRAUD_CHECKING);
        }
    },
    FRAUD_CHECKING{
        @Override public Set<PaymentStatus> nextStates(){
            return EnumSet.of(FRAUD_APPROVED, FRAUD_REJECTED);
        }
    },
    FRAUD_APPROVED {
        @Override public Set<PaymentStatus> nextStates(){
            return EnumSet.of(DEBITING);
        }
    }, FRAUD_REJECTED {
        @Override public Set<PaymentStatus> nextStates(){
            return EnumSet.noneOf(PaymentStatus.class);
        }
    },
    DEBITING{
        @Override public Set<PaymentStatus> nextStates(){
            return EnumSet.of(CREDITED, COMPENSATING);
        }
    },
    CREDITED{
        @Override public Set<PaymentStatus> nextStates(){
            return EnumSet.of(COMPLETED);
        }
    },
    COMPENSATING{
        @Override public Set<PaymentStatus> nextStates(){
            return EnumSet.of(FAILED);
        }
    },
    COMPLETED{
        @Override public Set<PaymentStatus> nextStates(){
            return EnumSet.noneOf(PaymentStatus.class);
        }
    },
    FAILED{
        @Override public Set<PaymentStatus> nextStates(){
            return EnumSet.noneOf(PaymentStatus.class);
        }
    };

    public abstract Set<PaymentStatus> nextStates();

    public boolean canTransitionTo(PaymentStatus target){
        return nextStates().contains(target);
    }

    public boolean isTerminal(){
        return nextStates().isEmpty();
    }
}
