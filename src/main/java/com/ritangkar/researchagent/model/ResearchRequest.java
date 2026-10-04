package com.ritangkar.researchagent.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResearchRequest(
        @NotBlank @Size(min = 8, max = 300) String question
) {
}
