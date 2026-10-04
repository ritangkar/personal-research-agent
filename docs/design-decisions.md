# Design Decisions

---

**Decision:** Use a rule-based (regex) planner instead of an LLM call to
break the question into sub-questions.

**Why:** The four-sub-question structure (state / benefits / risks /
trajectory) is the same shape for almost any research question. Reaching
that structure doesn't need language understanding — it needs a topic
phrase stripped out of a sentence, which a handful of regex patterns do
deterministically, for free, and testably.

**Trade-off:** The topic extraction is imperfect for oddly-phrased
questions (see `docs/../README.md` Limitations) and the four sub-questions
are fixed in shape, not tailored to the specific question.

**Future:** An LLM-backed planner is a natural upgrade — but it should
*replace* this class behind the same method signature, not be bolted onto
it, so the rest of the pipeline (which only depends on `List<PlanStep>`)
doesn't change.

---

**Decision:** Validate every citation in `ReportSynthesizer`, independent of
which `LlmClient` produced it — rather than trusting the demo client (which
is safe by construction) and only worrying about validation once a real LLM
is added.

**Why:** Writing the safety net before it's strictly needed means it's
already tested and already proven not to reject the demo client's valid
citations, by the time a live LLM client's output needs checking. It also
means the guarantee doesn't rely on remembering to add validation later.

**Trade-off:** A small amount of work that has no visible effect in demo
mode (the demo client never produces an invalid citation to catch).

**Future:** None needed — this is the intended shape.

---

**Decision:** Bundle one focused demo corpus (12 documents, one topic)
rather than a large multi-topic corpus.

**Why:** The project's point is the orchestration architecture, not
retrieval breadth. A small, hand-composed corpus lets every finding in the
README be verified by reading twelve short documents, and it deliberately
includes one low-credibility source (`D12`) specifically to demonstrate the
evaluator's credibility weighting affecting ranking.

**Trade-off:** Questions outside the bundled topic correctly come back as
low-confidence — which is a feature, not a bug, but is worth stating
plainly so it isn't mistaken for a broken demo.

**Future:** A second, unrelated corpus/topic would demonstrate the planner
and evaluator generalizing, without changing any pipeline code.
