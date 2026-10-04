package com.ritangkar.researchagent.evaluation;

import com.ritangkar.researchagent.model.Evidence;
import com.ritangkar.researchagent.model.EvidenceEvaluation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceEvaluatorTest {

    private final EvidenceEvaluator evaluator = new EvidenceEvaluator(0.35, 3);

    private Evidence evidence(String id, String tier, double matchScore, int year) {
        return new Evidence(id, "S1", "DOC-" + id, "Title " + id, "Source " + id, tier,
                year, "https://example-research.org/" + id, "Snippet for " + id + ".", matchScore, Instant.now());
    }

    @Test
    void aHighRelevanceHighCredibilityRecentSourceIsAccepted() {
        Evidence e = evidence("A", "A", 0.7, Year.now().getValue());
        List<EvidenceEvaluation> evaluations = evaluator.evaluate(List.of(e));

        assertThat(evaluations).hasSize(1);
        assertThat(evaluations.get(0).accepted()).isTrue();
        assertThat(evaluations.get(0).overallScore()).isGreaterThan(0.35);
    }

    @Test
    void aLowRelevanceLowCredibilityOldSourceIsRejected() {
        Evidence e = evidence("B", "D", 0.15, Year.now().getValue() - 8);
        List<EvidenceEvaluation> evaluations = evaluator.evaluate(List.of(e));

        assertThat(evaluations.get(0).accepted()).isFalse();
    }

    @Test
    void higherTierSourcesOutrankLowerTierSourcesAtEqualRelevanceAndYear() {
        Evidence tierA = evidence("A", "A", 0.6, Year.now().getValue());
        Evidence tierD = evidence("D", "D", 0.6, Year.now().getValue());

        List<EvidenceEvaluation> evaluations = evaluator.evaluate(List.of(tierD, tierA));

        // evaluate() sorts by overallScore descending — tier A should come first.
        assertThat(evaluations.get(0).evidence().id()).isEqualTo("A");
        assertThat(evaluations.get(0).overallScore()).isGreaterThan(evaluations.get(1).overallScore());
    }

    @Test
    void acceptedOnlyCapsToTheConfiguredMaximum() {
        List<Evidence> five = List.of(
                evidence("1", "A", 0.9, Year.now().getValue()),
                evidence("2", "A", 0.8, Year.now().getValue()),
                evidence("3", "A", 0.7, Year.now().getValue()),
                evidence("4", "A", 0.6, Year.now().getValue()),
                evidence("5", "A", 0.5, Year.now().getValue())
        );
        List<EvidenceEvaluation> evaluations = evaluator.evaluate(five);
        List<EvidenceEvaluation> accepted = evaluator.acceptedOnly(evaluations);

        assertThat(accepted).hasSize(3);
        assertThat(accepted.get(0).evidence().id()).isEqualTo("1"); // highest score first
    }
}
