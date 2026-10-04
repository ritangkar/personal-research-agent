package com.ritangkar.researchagent.tool;

import com.ritangkar.researchagent.model.Evidence;
import com.ritangkar.researchagent.model.SourceDocument;
import com.ritangkar.researchagent.repository.CorpusRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Scores the bundled corpus against a query using simple keyword overlap —
 * no embeddings, no network call, fully deterministic. This is intentionally
 * simple: the point of this project is the orchestration around the tool
 * call (planning, retry, caching, evaluation, grounded synthesis), not
 * retrieval sophistication. A real deployment would swap this for a proper
 * search/embedding provider behind the same {@link SearchTool} interface.
 */
@Component
public class MockSearchTool implements SearchTool {

    private static final int MAX_RESULTS = 4;
    private static final double MIN_MATCH_SCORE = 0.12;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-zA-Z0-9]+");
    private static final Set<String> STOPWORDS = Set.of(
            "what", "how", "why", "when", "where", "who", "is", "are", "was", "were",
            "does", "do", "did", "the", "a", "an", "of", "for", "on", "in", "to", "and",
            "or", "about", "with", "this", "that", "it", "its", "as", "at", "be", "has",
            "have", "had", "will", "would", "can", "could", "should"
    );

    private final CorpusRepository corpusRepository;

    public MockSearchTool(CorpusRepository corpusRepository) {
        this.corpusRepository = corpusRepository;
    }

    @Override
    public String name() {
        return "mock-corpus-search";
    }

    @Override
    public List<Evidence> search(String query, String subQuestionId) {
        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) return List.of();

        List<ScoredDoc> scored = new ArrayList<>();
        for (SourceDocument doc : corpusRepository.findAll()) {
            Set<String> docTokens = tokenize(doc.title() + " " + doc.body());
            double score = overlapScore(queryTokens, docTokens);
            if (score >= MIN_MATCH_SCORE) {
                scored.add(new ScoredDoc(doc, score));
            }
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<Evidence> results = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < Math.min(MAX_RESULTS, scored.size()); i++) {
            ScoredDoc sd = scored.get(i);
            results.add(new Evidence(
                    subQuestionId + "-E" + (i + 1),
                    subQuestionId,
                    sd.doc.id(),
                    sd.doc.title(),
                    sd.doc.source(),
                    sd.doc.sourceTier(),
                    sd.doc.publishedYear(),
                    sd.doc.url(),
                    sd.doc.body(),
                    round2(sd.score),
                    now
            ));
        }
        return results;
    }

    private double overlapScore(Set<String> queryTokens, Set<String> docTokens) {
        long overlap = queryTokens.stream().filter(docTokens::contains).count();
        return overlap / (double) queryTokens.size();
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        var matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= 3 && !STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private record ScoredDoc(SourceDocument doc, double score) {
    }
}
