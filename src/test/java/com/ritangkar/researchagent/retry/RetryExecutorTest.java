package com.ritangkar.researchagent.retry;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryExecutorTest {

    @Test
    void returnsImmediatelyOnFirstSuccess() {
        RetryExecutor retry = new RetryExecutor(3, 1);
        AtomicInteger calls = new AtomicInteger();

        RetryExecutor.Outcome<String> outcome = retry.execute(() -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertThat(outcome.value()).isEqualTo("ok");
        assertThat(outcome.attemptsUsed()).isEqualTo(1);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void retriesUntilSuccessWithinTheAttemptBudget() {
        RetryExecutor retry = new RetryExecutor(3, 1);
        AtomicInteger calls = new AtomicInteger();

        RetryExecutor.Outcome<String> outcome = retry.execute(() -> {
            int attempt = calls.incrementAndGet();
            if (attempt < 3) throw new RuntimeException("simulated transient failure");
            return "recovered";
        });

        assertThat(outcome.value()).isEqualTo("recovered");
        assertThat(outcome.attemptsUsed()).isEqualTo(3);
    }

    @Test
    void throwsRetryExhaustedAfterMaxAttempts() {
        RetryExecutor retry = new RetryExecutor(2, 1);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> retry.execute(() -> {
            calls.incrementAndGet();
            throw new RuntimeException("always fails");
        })).isInstanceOf(RetryExecutor.RetryExhaustedException.class);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void rejectsAMaxAttemptsBelowOne() {
        assertThatThrownBy(() -> new RetryExecutor(0, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
