package com.ritangkar.researchagent.synthesis;

import com.ritangkar.researchagent.model.EvidenceEvaluation;
import com.ritangkar.researchagent.model.PlanStep;
import com.ritangkar.researchagent.model.ReportSection;
import com.ritangkar.researchagent.synthesis.llm.DemoLlmClient;
import com.ritangkar.researchagent.synthesis.llm.LlmClient;
import com.ritangkar.researchagent.synthesis.llm.OpenAiCompatibleLlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Turns one plan step's accepted evidence into a {@link ReportSection}.
 *
 * Whichever {@link LlmClient} produced the text (the deterministic demo
 * client, or the optional live client), every id it claims to cite is
 * checked against the evidence it was actually given. An id that doesn't
 * appear in that set is dropped rather than trusted — this is what keeps a
 * hallucinated citation from ever reaching the final report, independent of
 * which synthesis backend is active.
 */
@Component
public class ReportSynthesizer {

    private final DemoLlmClient demoLlmClient;
    private final Optional<OpenAiCompatibleLlmClient> liveLlmClient;
    private final boolean llmEnabled;

    public ReportSynthesizer(DemoLlmClient demoLlmClient,
                              Optional<OpenAiCompatibleLlmClient> liveLlmClient,
                              @Value("${research.llm.enabled:false}") boolean llmEnabled) {
        this.demoLlmClient = demoLlmClient;
        this.liveLlmClient = liveLlmClient;
        this.llmEnabled = llmEnabled;
    }

    public boolean isUsingLiveLlm() {
        return llmEnabled && liveLlmClient.isPresent();
    }

    public String activeClientName() {
        return isUsingLiveLlm() ? liveLlmClient.get().name() : demoLlmClient.name();
    }

    public ReportSection synthesize(PlanStep step, List<EvidenceEvaluation> acceptedEvidence) {
        LlmClient client = isUsingLiveLlm() ? liveLlmClient.get() : demoLlmClient;

        LlmClient.SynthesisResult result = client.synthesize(step.subQuestion(), acceptedEvidence);
        List<String> validatedCitations = validateCitations(result.citedEvidenceIds(), acceptedEvidence);

        boolean insufficient = acceptedEvidence.isEmpty();
        return new ReportSection(step.id(), step.subQuestion(), result.answerText(), validatedCitations, insufficient);
    }

    /**
     * Drops any cited id that doesn't correspond to a piece of evidence this
     * section actually accepted. See class-level javadoc.
     */
    private List<String> validateCitations(List<String> claimedIds, List<EvidenceEvaluation> acceptedEvidence) {
        Set<String> allowedIds = acceptedEvidence.stream()
                .map(e -> e.evidence().id())
                .collect(java.util.stream.Collectors.toSet());

        return claimedIds.stream()
                .filter(allowedIds::contains)
                .distinct()
                .toList();
    }
}
