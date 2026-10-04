package com.ritangkar.researchagent.synthesis.llm;

import com.ritangkar.researchagent.model.EvidenceEvaluation;

import java.util.List;

/**
 * Produces prose for one report section from a sub-question and its
 * accepted evidence. Implementations must only reference the evidence ids
 * they were given — {@code ReportSynthesizer} independently validates every
 * returned citation against that set regardless of which implementation
 * produced it, so a non-compliant client cannot smuggle an invented
 * citation into the report.
 */
public interface LlmClient {

    record SynthesisResult(String answerText, List<String> citedEvidenceIds) {
    }

    SynthesisResult synthesize(String subQuestion, List<EvidenceEvaluation> acceptedEvidence);

    String name();
}
