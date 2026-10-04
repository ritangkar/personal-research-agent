package com.ritangkar.researchagent.retry;

import java.util.function.Supplier;

/**
 * A small, generic retry wrapper with exponential backoff. Used around every
 * tool call and (when enabled) the live LLM client — anything that talks to
 * something outside this process is wrapped here rather than trusting the
 * first attempt.
 */
public class RetryExecutor {

    public record Outcome<T>(T value, int attemptsUsed) {
    }

    private final int maxAttempts;
    private final long initialBackoffMs;

    public RetryExecutor(int maxAttempts, long initialBackoffMs) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
    }

    public <T> Outcome<T> execute(Supplier<T> action) {
        RuntimeException lastFailure = null;
        long backoff = initialBackoffMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return new Outcome<>(action.get(), attempt);
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt < maxAttempts) {
                    sleep(backoff);
                    backoff *= 2;
                }
            }
        }
        throw new RetryExhaustedException("Action failed after " + maxAttempts + " attempts", lastFailure);
    }

    private void sleep(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class RetryExhaustedException extends RuntimeException {
        public RetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
