package com.ritangkar.researchagent.service;

import com.ritangkar.researchagent.cache.InMemoryEvidenceCache;
import com.ritangkar.researchagent.evaluation.EvidenceEvaluator;
import com.ritangkar.researchagent.model.*;
import com.ritangkar.researchagent.planning.ResearchPlanner;
import com.ritangkar.researchagent.retry.RetryExecutor;
import com.ritangkar.researchagent.synthesis.ReportSynthesizer;
import com.ritangkar.researchagent.tool.SearchTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Runs the full pipeline for one request:
 * Planning -&gt; Tools/Search (cached, retried) -&gt; Evidence Evaluation -&gt; Synthesis -&gt; Structured Output.
 */
@Service
public class ResearchAgentService {

    private final ResearchPlanner planner;
    private final SearchTool searchTool;
    private final EvidenceEvaluator evaluator;
    private final ReportSynthesizer synthesizer;
    private final InMemoryEvidenceCache cache;
    private final RetryExecutor retryExecutor;

    public ResearchAgentService(
            ResearchPlanner planner,
            SearchTool searchTool,
            EvidenceEvaluator evaluator,
            ReportSynthesizer synthesizer,
            @Value("${research.cache.ttl-seconds:600}") long cacheTtlSeconds,
            @Value("${research.retry.max-attempts:3}") int maxAttempts,
            @Value("${research.retry.initial-backoff-ms:100}") long initialBackoffMs) {
        this.planner = planner;
        this.searchTool = searchTool;
        this.evaluator = evaluator;
        this.synthesizer = synthesizer;
        this.cache = new InMemoryEvidenceCache(cacheTtlSeconds);
        this.retryExecutor = new RetryExecutor(maxAttempts, initialBackoffMs);
    }

    public ResearchReport run(ResearchRequest request) {
        long startedAt = System.currentTimeMillis();
        ResearchPlan plan = planner.plan(request);

        int toolCalls = 0, cacheHits = 0, cacheMisses = 0, evidenceRetrieved = 0, evidenceAccepted = 0;
        long estimatedTokens = 0;

        List<ReportSection> sections = new ArrayList<>();
        List<Evidence> sourcesUsed = new ArrayList<>();
        List<Double> sectionConfidences = new ArrayList<>();
        List<String> caveats = new ArrayList<>();
        caveats.add("Demo mode: evidence is drawn only from a small bundled synthetic corpus, not the live web — see sample-data/corpus-documents.json.");

        for (PlanStep step : plan.steps()) {
            toolCalls++;
            InMemoryEvidenceCache.CacheResult cacheResult = cache.getOrCompute(
                    searchTool.name(), step.subQuestion(), step.id(),
                    () -> retryExecutor.execute(() -> searchTool.search(step.subQuestion(), step.id())).value()
            );
            if (cacheResult.cacheHit()) cacheHits++; else cacheMisses++;

            List<Evidence> raw = cacheResult.evidence();
            evidenceRetrieved += raw.size();
            estimatedTokens += raw.stream().mapToLong(e -> estimateTokens(e.snippet())).sum();

            List<EvidenceEvaluation> evaluated = evaluator.evaluate(raw);
            List<EvidenceEvaluation> accepted = evaluator.acceptedOnly(evaluated);
            evidenceAccepted += accepted.size();

            ReportSection section = synthesizer.synthesize(step, accepted);
            sections.add(section);

            if (section.insufficientEvidence()) {
                caveats.add("No sufficiently strong evidence was found for: \"" + step.subQuestion() + "\"");
                sectionConfidences.add(0.0);
            } else {
                double avgScore = accepted.stream().mapToDouble(EvidenceEvaluation::overallScore).average().orElse(0);
                sectionConfidences.add(avgScore);
                // Only evidence that actually survived citation validation goes into
                // sourcesUsed, so every id in section.citedEvidenceIds() is guaranteed
                // to resolve to exactly one Evidence object here (see the test suite).
                java.util.Set<String> citedIds = java.util.Set.copyOf(section.citedEvidenceIds());
                accepted.stream()
                        .filter(e -> citedIds.contains(e.evidence().id()))
                        .forEach(e -> sourcesUsed.add(e.evidence()));
            }
        }

        List<Evidence> dedupedSources = dedupById(sourcesUsed);
        double confidence = round2(sectionConfidences.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        long durationMs = System.currentTimeMillis() - startedAt;

        CostSummary costSummary = new CostSummary(
                toolCalls, cacheHits, cacheMisses, evidenceRetrieved, evidenceAccepted, estimatedTokens, durationMs);

        return new ResearchReport(
                request.question(), plan.extractedTopic(), Instant.now(),
                sections, dedupedSources, confidence, caveats, costSummary,
                synthesizer.isUsingLiveLlm());
    }

    private long estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        int words = text.trim().split("\\s+").length;
        return Math.round(words * 1.3);
    }

    private List<Evidence> dedupById(List<Evidence> evidence) {
        LinkedHashMap<String, Evidence> byId = new LinkedHashMap<>();
        for (Evidence e : evidence) {
            byId.putIfAbsent(e.id(), e);
        }
        return List.copyOf(byId.values());
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
