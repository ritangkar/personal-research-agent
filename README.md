# Personal Research Agent

A compact but properly engineered research agent in Java + Spring Boot:
**Planning → Tool Calling → Evidence Collection → Evidence Evaluation →
Synthesis → Structured Output.** The interesting part is the orchestration,
not a chat UI — there isn't one.

Part of the [Commerce Engineering Lab](https://ritangkar.github.io/#commerce-lab) —
17 small, production-minded engineering capabilities by Ritangkar Dey.

---

## 1. Project Overview

Give it a question; it plans four sub-questions, retrieves evidence for
each, scores that evidence for relevance/credibility/recency, synthesizes a
grounded answer per sub-question (citing only evidence that passed
evaluation), and returns one structured report — end to end, with **no API
key required**.

## 2. Problem

A "research agent" demo is easy to fake with a single LLM call and hard
prompt engineering. It's much harder — and much more representative of real
agent engineering — to build the surrounding machinery correctly: does it
retry a failed tool call? Does it avoid repeating an identical search? Does
it know what it doesn't know, instead of confidently making something up?

## 3. Solution

A six-stage pipeline (`ResearchAgentService`) where every stage is a
separate, independently-testable component, and where citation-grounding is
enforced in code — not just requested in a prompt.

## 4. Key Features

- **Planning**: turns a free-text question into 4 structured sub-questions
  (current state, benefits, risks, trajectory)
- **Tool calling**: a `SearchTool` interface with a deterministic
  `MockSearchTool` implementation (keyword-overlap search over a bundled
  12-document synthetic corpus) — no network call, no API key, ever required
- **Retry**: a generic exponential-backoff `RetryExecutor` wraps every tool call
- **Caching**: an in-memory TTL cache avoids repeating an identical search
- **Evidence evaluation**: every result is scored for relevance,
  credibility (by source tier) and recency before it's eligible to be cited
- **Grounded synthesis**: the deterministic `DemoLlmClient` only ever cites
  evidence it was given; `ReportSynthesizer` independently re-validates
  every citation against the accepted-evidence set regardless of which
  synthesis backend produced it
- **Cost/token awareness**: every report includes a `costSummary` (tool
  calls, cache hits, evidence volume, a rough token estimate)
- **Honest gaps**: an off-topic question returns low-confidence
  "insufficient evidence" sections instead of an invented answer

## 5. Architecture

```
ResearchRequest
      │
      ▼
ResearchPlanner  ──►  4x PlanStep (sub-questions)
      │
      ▼  (for each step)
CachedSearchTool call (retry-wrapped, TTL-cached)
      │
      ▼
EvidenceEvaluator  ──►  accept/reject + rank by relevance/credibility/recency
      │
      ▼
ReportSynthesizer (+ LlmClient)  ──►  citation-validated ReportSection
      │
      ▼
ResearchReport  (sections + sourcesUsed + confidence + caveats + costSummary)
```

See [`docs/architecture.md`](docs/architecture.md) for the full diagram.

## 6. Technical Approach

- Every pipeline stage is a small Spring bean with a single responsibility,
  wired together by `ResearchAgentService` — not one large method
- `LlmClient` is an interface; `DemoLlmClient` (default, deterministic) and
  `OpenAiCompatibleLlmClient` (optional, gated, network-calling) both
  implement it, and `ReportSynthesizer` validates whichever one's output
  the same way
- `research.llm.enabled=false` by default — the API key is only ever read
  from the `OPENAI_API_KEY` environment variable, never from a config file

## 7. Design Decisions

See [`docs/design-decisions.md`](docs/design-decisions.md). Highlights:

- Rule-based planner over an LLM call for planning — deterministic,
  testable, zero-cost, and the sub-question structure this problem needs
  doesn't require language understanding beyond stripping a question prefix
- Citation validation happens in `ReportSynthesizer`, independent of which
  `LlmClient` ran — so a future, real LLM integration cannot bypass it

## 8. Sample Input

```
GET /api/research/sample
POST /api/research
{ "question": "What are the effects of a four-day work week?" }
```

## 9. Sample Output

Abridged real output from `GET /api/research/sample` (demo mode):

```json
{
  "question": "What are the effects of a four-day work week?",
  "extractedTopic": "four-day work week",
  "sections": [
    {
      "subQuestionId": "S1",
      "subQuestion": "What is the current state of four-day work week?",
      "answer": "Based on 3 sources: Across a multi-employer pilot cohort, output per employee was reported as broadly stable to slightly improved after moving to a four-day week... (Journal of Workplace Studies (synthetic), 2025) ...",
      "citedEvidenceIds": ["S1-E1", "S1-E2", "S1-E3"],
      "insufficientEvidence": false
    }
  ],
  "confidence": 0.79,
  "caveats": [
    "Demo mode: evidence is drawn only from a small bundled synthetic corpus, not the live web — see sample-data/corpus-documents.json."
  ],
  "costSummary": {
    "toolCallsTotal": 4,
    "cacheHits": 0,
    "cacheMisses": 4,
    "evidenceRetrieved": 16,
    "evidenceAccepted": 12,
    "estimatedTokensProcessed": 520
  },
  "llmSynthesisUsed": false
}
```

Ask something the corpus doesn't cover and every section correctly reports
`"insufficientEvidence": true` with `confidence: 0.0` instead of an invented
answer — see the test suite for this exact scenario.

## 10. How to Run

```bash
git clone <your-repo-url>
cd personal-research-agent
mvn spring-boot:run
```

API available at `http://localhost:8083`. No environment variables or API
keys are required for the default (demo) mode.

## 11. How to Test

```bash
mvn test
```

Covers the planner's topic extraction, the evaluator's scoring/ranking
logic, the retry executor's backoff behavior, and full end-to-end pipeline
runs — including the citation-integrity invariant (every cited id resolves
to a real, accepted source) and the off-topic "insufficient evidence" case.

## 12. API Documentation

| Method | Path              | Description                                                |
|--------|-------------------|--------------------------------------------------------------|
| POST   | `/api/research`   | Body: `{ "question": "..." }` — runs the full pipeline       |
| GET    | `/api/research/sample` | Runs the pipeline against a fixed demo question             |

## 13. Portfolio Demo

**Demo level:** 3 — architecture walkthrough. Rather than re-implementing
tool orchestration in client-side JavaScript (which would misrepresent what
this project demonstrates), the portfolio demo
(`lab/demos/personal-research-agent.js`) shows the real, pre-captured output
of `GET /api/research/sample` stage by stage — plan, tool calls, evidence
evaluation, synthesis, final report — clearly labeled as a recorded run of
this actual service, not a live call. See `docs/demo.md`.

## 14. Limitations

- Retrieval is simple keyword overlap over a 12-document corpus about one
  topic (the four-day work week) — good enough to demonstrate the pipeline,
  not a production retrieval system
- The rule-based planner's topic extraction is a set of regex heuristics,
  not natural-language understanding — documented, imperfect cases exist
- `OpenAiCompatibleLlmClient` is written to a real OpenAI-compatible API
  shape but has not been exercised against a live endpoint in this
  environment (no network access during development) — treat it as a
  documented integration point, not a verified one, until you've run it
  with a real key

## 15. Future Enhancements

- Pluggable real search providers behind the same `SearchTool` interface
- A second bundled corpus/topic to demonstrate multi-topic planning
- Persisting `ResearchReport`s so repeated questions can be compared over time

## 16. License

MIT — see [`LICENSE`](LICENSE).
