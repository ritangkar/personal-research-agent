package com.ritangkar.researchagent.model;

/**
 * The evaluator's scoring of one piece of evidence: how relevant it is to
 * the sub-question, how credible its source tier is, and how recent it is.
 * {@code overallScore} is the weighted combination used to accept or reject
 * the evidence (see EvidenceEvaluator).
 */
public record EvidenceEvaluation(
        Evidence evidence,
        double relevanceScore,
        double credibilityScore,
        double recencyScore,
        double overallScore,
        boolean accepted,
        String rationale
) {
}
