package com.ritangkar.researchagent.model;

import java.util.List;

public record ResearchPlan(
        String originalQuestion,
        String extractedTopic,
        List<PlanStep> steps
) {
}
