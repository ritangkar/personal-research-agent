package com.ritangkar.researchagent.cache;

import com.ritangkar.researchagent.model.Evidence;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A minimal in-process cache for tool-call results, keyed by (tool name,
 * query, sub-question id). Avoids repeating an identical search within the
 * configured TTL - useful when the same sub-question is re-planned or a
 * request is retried upstream.
 *
 * This is intentionally simple (a ConcurrentHashMap, not a dedicated cache
 * library) - appropriate for a single-instance demo service with a small,
 * short-lived working set. See docs/design-decisions.md.
 */
public class InMemoryEvidenceCache {

    public record CacheResult(List<Evidence> evidence, boolean cacheHit) {
    }

    private record Entry(List<Evidence> value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public InMemoryEvidenceCache(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public CacheResult getOrCompute(String toolName, String query, String subQuestionId,
                                     Supplier<List<Evidence>> compute) {
        String key = cacheKey(toolName, query, subQuestionId);
        Entry existing = store.get(key);
        if (existing != null && !existing.isExpired()) {
            return new CacheResult(existing.value(), true);
        }
        List<Evidence> computed = compute.get();
        store.put(key, new Entry(computed, Instant.now().plusSeconds(ttlSeconds)));
        return new CacheResult(computed, false);
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }

    private String cacheKey(String toolName, String query, String subQuestionId) {
        return toolName + "::" + subQuestionId + "::" + query.toLowerCase(Locale.ROOT).trim();
    }
}
