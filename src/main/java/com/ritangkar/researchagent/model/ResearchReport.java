package com.ritangkar.researchagent.model;

import java.time.Instant;
import java.util.List;

public record ResearchReport(
        String question,
        String extractedTopic,
        Instant generatedAt,
        List<ReportSection> sections,
        List<Evidence> sourcesUsed,
        double confidence,
        List<String> caveats,
        CostSummary costSummary,
        boolean llmSynthesisUsed
) {
}
