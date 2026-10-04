package com.ritangkar.researchagent.synthesis.llm;

import com.ritangkar.researchagent.model.EvidenceEvaluation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The default synthesis "client": no network call, no API key, fully
 * deterministic. It stitches the accepted evidence's own snippets into a
 * short paragraph using a fixed template, and cites exactly the evidence it
 * was given — nothing invented, nothing paraphrased into a different claim.
 *
 * This is what makes the whole pipeline runnable with zero setup (see the
 * AI DEMO REQUIREMENT in the portfolio's master brief): swapping in a real
 * LLM later only changes how the sentence is *phrased*, not what it is
 * allowed to claim.
 */
@Component
public class DemoLlmClient implements LlmClient {

    @Override
    public String name() {
        return "demo-template-client";
    }

    @Override
    public SynthesisResult synthesize(String subQuestion, List<EvidenceEvaluation> acceptedEvidence) {
        if (acceptedEvidence.isEmpty()) {
            return new SynthesisResult(
                    "No evidence in the available sources cleared the acceptance threshold for this sub-question.",
                    List.of());
        }

        String bulletBody = acceptedEvidence.stream()
                .map(e -> firstSentence(e.evidence().snippet()) + " (" + e.evidence().sourceName()
                        + ", " + e.evidence().publishedYear() + ")")
                .collect(Collectors.joining(" "));

        String lead = acceptedEvidence.size() == 1
                ? "Based on 1 source: "
                : "Based on " + acceptedEvidence.size() + " sources: ";

        List<String> citedIds = acceptedEvidence.stream().map(e -> e.evidence().id()).toList();

        return new SynthesisResult(lead + bulletBody, citedIds);
    }

    private String firstSentence(String text) {
        int end = text.indexOf(". ");
        return end > 0 ? text.substring(0, end + 1) : text;
    }
}
