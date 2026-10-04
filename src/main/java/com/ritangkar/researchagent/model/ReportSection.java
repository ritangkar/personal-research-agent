package com.ritangkar.researchagent.model;

import java.util.List;

public record ReportSection(
        String subQuestionId,
        String subQuestion,
        String answer,
        List<String> citedEvidenceIds,
        boolean insufficientEvidence
) {
}
