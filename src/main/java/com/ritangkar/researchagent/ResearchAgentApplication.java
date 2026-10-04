package com.ritangkar.researchagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Personal Research Agent.
 *
 * Demonstrates the orchestration architecture of a research agent —
 * planning, tool calling, evidence evaluation, retry, caching, cost
 * awareness and citation-grounded synthesis — not a chatbot UI.
 *
 * Runs fully in "demo mode" by default: {@link
 * com.ritangkar.researchagent.tool.MockSearchTool} and {@link
 * com.ritangkar.researchagent.synthesis.llm.DemoLlmClient} require no
 * network access and no API key. See docs/architecture.md.
 */
@SpringBootApplication
public class ResearchAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResearchAgentApplication.class, args);
    }
}
