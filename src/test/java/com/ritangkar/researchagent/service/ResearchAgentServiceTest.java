package com.ritangkar.researchagent.service;

import com.ritangkar.researchagent.evaluation.EvidenceEvaluator;
import com.ritangkar.researchagent.model.Evidence;
import com.ritangkar.researchagent.model.ReportSection;
import com.ritangkar.researchagent.model.ResearchReport;
import com.ritangkar.researchagent.model.ResearchRequest;
import com.ritangkar.researchagent.planning.ResearchPlanner;
import com.ritangkar.researchagent.repository.CorpusRepository;
import com.ritangkar.researchagent.synthesis.ReportSynthesizer;
import com.ritangkar.researchagent.synthesis.llm.DemoLlmClient;
import com.ritangkar.researchagent.tool.MockSearchTool;
import com.ritangkar.researchagent.tool.SearchTool;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wires the real pipeline components together (the same way Spring would)
 * and runs it end to end, entirely offline, using the bundled demo corpus.
 */
class ResearchAgentServiceTest {

    private ResearchAgentService buildService() {
        CorpusRepository corpus = new CorpusRepository();
        SearchTool searchTool = new MockSearchTool(corpus);
        EvidenceEvaluator evaluator = new EvidenceEvaluator(0.35, 3);
        DemoLlmClient demoLlmClient = new DemoLlmClient();
        ReportSynthesizer synthesizer = new ReportSynthesizer(demoLlmClient, Optional.empty(), false);
        ResearchPlanner planner = new ResearchPlanner();
        return new ResearchAgentService(planner, searchTool, evaluator, synthesizer, 600, 3, 10);
    }

    @Test
    void producesAFourSectionReportForAQuestionTheCorpusCovers() {
        ResearchAgentService service = buildService();
        ResearchReport report = service.run(new ResearchRequest("What are the effects of a four-day work week?"));

        assertThat(report.sections()).hasSize(4);
        assertThat(report.sourcesUsed()).isNotEmpty();
        assertThat(report.confidence()).isBetween(0.0, 1.0);
        assertThat(report.llmSynthesisUsed()).isFalse();
        assertThat(report.costSummary().toolCallsTotal()).isEqualTo(4);
    }

    @Test
    void everyCitedEvidenceIdResolvesToExactlyOneSourceInSourcesUsed() {
        ResearchAgentService service = buildService();
        ResearchReport report = service.run(new ResearchRequest("What are the effects of a four-day work week?"));

        Set<String> availableIds = new HashSet<>();
        for (Evidence e : report.sourcesUsed()) availableIds.add(e.id());

        for (ReportSection section : report.sections()) {
            for (String citedId : section.citedEvidenceIds()) {
                assertThat(availableIds)
                        .as("cited id " + citedId + " in section " + section.subQuestionId() + " must exist in sourcesUsed")
                        .contains(citedId);
            }
        }
    }

    @Test
    void reportsInsufficientEvidenceRatherThanInventingAnAnswerForAnOffTopicQuestion() {
        ResearchAgentService service = buildService();
        ResearchReport report = service.run(new ResearchRequest("What is the migratory pattern of the Arctic tern?"));

        assertThat(report.sections()).allMatch(ReportSection::insufficientEvidence);
        assertThat(report.confidence()).isEqualTo(0.0);
        assertThat(report.sourcesUsed()).isEmpty();
        assertThat(report.caveats()).anyMatch(c -> c.contains("No sufficiently strong evidence"));
    }

    @Test
    void secondIdenticalRequestIsServedFromCache() {
        ResearchAgentService service = buildService();
        ResearchRequest request = new ResearchRequest("What are the effects of a four-day work week?");

        ResearchReport first = service.run(request);
        ResearchReport second = service.run(request);

        // Second run's tool calls should all be cache hits (same service instance, same cache).
        assertThat(second.costSummary().cacheHits()).isEqualTo(4);
        assertThat(first.sections()).hasSize(second.sections().size());
    }
}
