package com.ritangkar.researchagent.tool;

import com.ritangkar.researchagent.model.Evidence;

import java.util.List;

/**
 * A tool the planner's steps can call to retrieve evidence for one
 * sub-question. {@link MockSearchTool} is the only implementation that
 * ships enabled by default — it requires no network access.
 */
public interface SearchTool {

    List<Evidence> search(String query, String subQuestionId);

    String name();
}
