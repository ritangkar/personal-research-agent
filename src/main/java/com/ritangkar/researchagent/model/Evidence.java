package com.ritangkar.researchagent.model;

import java.time.Instant;

/**
 * One piece of retrieved evidence, already resolved to a specific
 * sub-question. {@code id} is unique per retrieval and is the identifier
 * every downstream citation refers back to.
 */
public record Evidence(
        String id,
        String subQuestionId,
        String sourceId,
        String sourceTitle,
        String sourceName,
        String sourceTier,
        int publishedYear,
        String url,
        String snippet,
        double matchScore,
        Instant retrievedAt
) {
}
