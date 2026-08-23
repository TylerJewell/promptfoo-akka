package io.akka.promptfoo.domain;

import java.util.List;
import java.util.Map;

/** The verdict returned by grading one assertion or one assertion set — SPEC-001 §2. */
public record GradingResult(
    boolean pass,
    double score,
    String reason,
    Map<String, Double> namedScores,
    List<GradingResult> componentResults) {

  public static GradingResult leaf(boolean pass, String reason) {
    return new GradingResult(pass, pass ? 1.0 : 0.0, reason, Map.of(), List.of());
  }
}
