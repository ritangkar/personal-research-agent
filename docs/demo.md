# Portfolio Demo

**Demo level:** 3 — architecture walkthrough of a real, recorded pipeline run.

Unlike the other Batch 1 projects, this demo does not re-implement the
service in client-side JavaScript — doing so would misrepresent what the
project demonstrates (tool orchestration, not a UI). Instead,
`lab/demos/personal-research-agent.js` walks through the actual JSON
response of `GET /api/research/sample`, captured from a real run of this
service, stage by stage:

1. The plan (4 sub-questions)
2. One tool call and its evidence, before and after evaluation (showing the
   accept/reject line and why)
3. One synthesized section with its citations
4. The final report's confidence, caveats and cost summary

The demo is explicitly labeled "recorded output from a real run of this
service" rather than presented as a live call, since the portfolio has no
backend to call. Running `mvn spring-boot:run` and hitting
`GET /api/research/sample` or `POST /api/research` locally shows the live,
un-recorded version — including trying a question outside the bundled
corpus to see the "insufficient evidence" behavior first-hand.
