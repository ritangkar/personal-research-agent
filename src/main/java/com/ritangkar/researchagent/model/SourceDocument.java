package com.ritangkar.researchagent.model;

/**
 * One raw document from the bundled demo corpus (resources/corpus/documents.json).
 * sourceTier is a synthetic credibility tier: A (research/institute), B (trade
 * press), C (blog), D (unverified social post) — used by EvidenceEvaluator.
 */
public record SourceDocument(
        String id,
        String title,
        String source,
        String sourceTier,
        int publishedYear,
        String url,
        String body
) {
}
