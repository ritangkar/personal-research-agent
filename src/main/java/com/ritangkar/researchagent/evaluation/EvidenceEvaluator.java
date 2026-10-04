package com.ritangkar.researchagent.evaluation;

import com.ritangkar.researchagent.model.Evidence;
import com.ritangkar.researchagent.model.EvidenceEvaluation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Scores each piece of retrieved evidence on three axes and accepts only
 * evidence that clears a minimum overall score. This is the pipeline's
 * primary hallucination-mitigation lever: synthesis is only ever allowed to
 * cite evidence that passed this gate (see ReportSynthesizer).
 */
@Component
public class EvidenceEvaluator {

    private static final Map<String, Double> TIER_CREDIBILITY = Map.of(
            "A", 1.0, "B", 0.75, "C", 0.5, "D", 0.2
    );
    private static final double RELEVANCE_WEIGHT = 0.5;
    private static final double CREDIBILITY_WEIGHT = 0.3;
    private static final double RECENCY_WEIGHT = 0.2;
    private static final int RECENCY_HALF_LIFE_YEARS = 6;

    private final double minAcceptScore;
    private final int maxPerSubQuestion;

    public EvidenceEvaluator(
            @Value("${research.evidence.min-score:0.35}") double minAcceptScore,
            @Value("${research.evidence.max-per-subquestion:3}") int maxPerSubQuestion) {
        this.minAcceptScore = minAcceptScore;
        this.maxPerSubQuestion = maxPerSubQuestion;
    }

    public List<EvidenceEvaluation> evaluate(List<Evidence> evidenceList) {
        return evidenceList.stream()
                .map(this::evaluateOne)
                .sorted(Comparator.comparingDouble(EvidenceEvaluation::overallScore).reversed())
                .toList();
    }

    /** The evaluations that passed the acceptance gate, capped to the configured max. */
    public List<EvidenceEvaluation> acceptedOnly(List<EvidenceEvaluation> evaluations) {
        return evaluations.stream()
                .filter(EvidenceEvaluation::accepted)
                .limit(maxPerSubQuestion)
                .toList();
    }

    private EvidenceEvaluation evaluateOne(Evidence evidence) {
        double relevance = clamp01(evidence.matchScore());
        double credibility = TIER_CREDIBILITY.getOrDefault(evidence.sourceTier(), 0.3);
        double recency = recencyScore(evidence.publishedYear());

        double overall = round2(RELEVANCE_WEIGHT * relevance + CREDIBILITY_WEIGHT * credibility + RECENCY_WEIGHT * recency);
        boolean accepted = overall >= minAcceptScore;

        String rationale = accepted
                ? String.format("Accepted: relevance %.2f, credibility %.2f (tier %s), recency %.2f -> overall %.2f",
                        relevance, credibility, evidence.sourceTier(), recency, overall)
                : String.format("Rejected: overall score %.2f is below the %.2f acceptance threshold", overall, minAcceptScore);

        return new EvidenceEvaluation(evidence, relevance, credibility, recency, overall, accepted, rationale);
    }

    private double recencyScore(int publishedYear) {
        int currentYear = Year.now().getValue();
        int age = Math.max(0, currentYear - publishedYear);
        return clamp01(1.0 - (age / (double) RECENCY_HALF_LIFE_YEARS));
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
