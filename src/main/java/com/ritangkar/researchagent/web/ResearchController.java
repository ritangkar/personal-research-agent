package com.ritangkar.researchagent.web;

import com.ritangkar.researchagent.model.ResearchReport;
import com.ritangkar.researchagent.model.ResearchRequest;
import com.ritangkar.researchagent.service.ResearchAgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private static final String SAMPLE_QUESTION = "What are the effects of a four-day work week?";

    private final ResearchAgentService researchAgentService;

    public ResearchController(ResearchAgentService researchAgentService) {
        this.researchAgentService = researchAgentService;
    }

    /**
     * POST /api/research — runs the full pipeline for any question.
     * Retrieval quality depends on the bundled demo corpus (see sample-data);
     * off-topic questions will correctly come back as low-confidence with
     * "insufficient evidence" sections rather than invented answers.
     */
    @PostMapping
    public ResearchReport research(@Valid @RequestBody ResearchRequest request) {
        return researchAgentService.run(request);
    }

    /**
     * GET /api/research/sample — runs the same live pipeline against a fixed
     * demo question the bundled corpus covers well, so the portfolio and any
     * first-time caller can see a fully-populated report with no setup.
     */
    @GetMapping("/sample")
    public ResearchReport sample() {
        return researchAgentService.run(new ResearchRequest(SAMPLE_QUESTION));
    }
}
