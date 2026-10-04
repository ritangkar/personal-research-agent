package com.ritangkar.researchagent.planning;

import com.ritangkar.researchagent.model.PlanStep;
import com.ritangkar.researchagent.model.ResearchPlan;
import com.ritangkar.researchagent.model.ResearchRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Breaks a free-text research question into a fixed set of sub-questions
 * covering current state, benefits, risks and trajectory — a deliberately
 * simple, deterministic planner (see docs/design-decisions.md for why this
 * isn't an LLM call).
 */
@Component
public class ResearchPlanner {

    private static final List<Pattern> LEADING_PATTERNS = List.of(
            Pattern.compile("^(what|how|why|when|where|who)\\s+(is|are|was|were|does|do|did|would|should|can|could)\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(the\\s+)?(effects?|impact|benefits?|risks?|pros and cons)\\s+of\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(tell me about|explain|describe|research|investigate)\\s+", Pattern.CASE_INSENSITIVE)
    );
    private static final Pattern LEADING_ARTICLE = Pattern.compile("^(a|an|the)\\s+", Pattern.CASE_INSENSITIVE);

    public ResearchPlan plan(ResearchRequest request) {
        String topic = extractTopic(request.question());

        List<PlanStep> steps = List.of(
                new PlanStep("S1", "What is the current state of " + topic + "?",
                        "Establish baseline facts before evaluating any claims."),
                new PlanStep("S2", "What benefits does the evidence report for " + topic + "?",
                        "Surface the strongest documented upside."),
                new PlanStep("S3", "What risks or challenges does the evidence report for " + topic + "?",
                        "Avoid a one-sided answer by explicitly searching for counter-evidence."),
                new PlanStep("S4", "What does the evidence suggest about the trajectory of " + topic + "?",
                        "Give a forward-looking view grounded in the sources, not speculation.")
        );

        return new ResearchPlan(request.question(), topic, steps);
    }

    private String extractTopic(String question) {
        String working = question.trim();
        if (working.endsWith("?")) {
            working = working.substring(0, working.length() - 1);
        }
        for (Pattern p : LEADING_PATTERNS) {
            working = p.matcher(working).replaceFirst("");
        }
        working = LEADING_ARTICLE.matcher(working.trim()).replaceFirst("");
        working = working.trim();
        return working.isEmpty() ? question.trim() : working;
    }
}
