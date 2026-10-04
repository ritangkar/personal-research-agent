package com.ritangkar.researchagent.planning;

import com.ritangkar.researchagent.model.ResearchPlan;
import com.ritangkar.researchagent.model.ResearchRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchPlannerTest {

    private final ResearchPlanner planner = new ResearchPlanner();

    @Test
    void producesFourStepsCoveringStateBenefitsRisksAndTrajectory() {
        ResearchPlan plan = planner.plan(new ResearchRequest("What are the effects of a four-day work week?"));

        assertThat(plan.steps()).hasSize(4);
        assertThat(plan.steps().get(0).subQuestion()).containsIgnoringCase("current state");
        assertThat(plan.steps().get(1).subQuestion()).containsIgnoringCase("benefits");
        assertThat(plan.steps().get(2).subQuestion()).containsIgnoringCase("risks");
        assertThat(plan.steps().get(3).subQuestion()).containsIgnoringCase("trajectory");
    }

    @Test
    void stripsLeadingQuestionPhrasingFromTheTopic() {
        ResearchPlan plan = planner.plan(new ResearchRequest("What are the effects of a four-day work week?"));
        assertThat(plan.extractedTopic()).isEqualTo("four-day work week");
    }

    @Test
    void fallsBackToTheFullQuestionIfNothingCanBeStripped() {
        ResearchPlan plan = planner.plan(new ResearchRequest("four-day work week"));
        assertThat(plan.extractedTopic()).isEqualTo("four-day work week");
    }

    @Test
    void everyStepHasAUniqueId() {
        ResearchPlan plan = planner.plan(new ResearchRequest("How does remote work affect productivity?"));
        long distinctIds = plan.steps().stream().map(s -> s.id()).distinct().count();
        assertThat(distinctIds).isEqualTo(plan.steps().size());
    }
}
