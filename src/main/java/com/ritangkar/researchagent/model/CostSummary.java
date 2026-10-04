package com.ritangkar.researchagent.model;

/**
 * A rough accounting of what the pipeline run cost, used to demonstrate
 * cost/token awareness. estimatedTokens and estimatedCostUsd are simple,
 * clearly-labelled estimates (evidence text volume, not a real LLM bill) —
 * demo mode makes no external LLM call, so there is no real token spend to
 * report.
 */
public record CostSummary(
        int toolCallsTotal,
        int cacheHits,
        int cacheMisses,
        int evidenceRetrieved,
        int evidenceAccepted,
        long estimatedTokensProcessed,
        long pipelineDurationMs
) {
}
