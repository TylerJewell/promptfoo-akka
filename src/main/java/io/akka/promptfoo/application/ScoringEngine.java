package io.akka.promptfoo.application;

import io.akka.promptfoo.domain.Assertion;
import io.akka.promptfoo.domain.AssertionSet;
import io.akka.promptfoo.domain.GradingResult;
import io.akka.promptfoo.domain.Subject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines the leaf results of an assertion set into one verdict — SPEC-001 §3 rules
 * R3-R6, ported from src/assertions/assertionsResult.ts's AssertionsResult class.
 */
public final class ScoringEngine {

  private ScoringEngine() {}

  public static GradingResult grade(AssertionSet set, Subject subject) {
    double totalScore = 0;
    double totalWeight = 0;
    String failedReason = null;
    List<GradingResult> componentResults = new java.util.ArrayList<>();
    Map<String, Double> namedScoreTotals = new LinkedHashMap<>();
    Map<String, Double> namedScoreWeights = new LinkedHashMap<>();

    for (Assertion assertion : set.assertions()) {
      GradingResult result = AssertionHandlers.handle(assertion, subject);
      double weight = assertion.weightOrDefault();
      totalScore += result.score() * weight; // R3
      totalWeight += weight;
      componentResults.add(result);

      if (assertion.metric() != null) {
        namedScoreTotals.merge(assertion.metric(), result.score() * weight, Double::sum);
        namedScoreWeights.merge(assertion.metric(), weight, Double::sum);
      }

      if (!result.pass()) {
        failedReason = result.reason(); // R4: last failure wins, matching the source
      }
    }

    double score = totalWeight > 0 ? totalScore / totalWeight : 0;
    boolean pass = failedReason == null;
    String reason = failedReason == null ? "All assertions passed" : failedReason;

    if (set.threshold() != null && !set.threshold().isNaN()) {
      // R5: a numeric threshold overrides every individual component's pass/fail. Gated on
      // the threshold field actually being set (not on truthiness), so threshold: 0 is honored.
      pass = score >= set.threshold();
      String comparator = pass ? "≥" : "<";
      reason = "Aggregate score " + String.format("%.2f", score) + " " + comparator + " " + set.threshold() + " threshold";
    }

    Map<String, Double> namedScores = new LinkedHashMap<>(); // R6
    for (String metric : namedScoreTotals.keySet()) {
      double w = namedScoreWeights.get(metric);
      namedScores.put(metric, w > 0 ? namedScoreTotals.get(metric) / w : 0);
    }

    return new GradingResult(pass, score, reason, namedScores, componentResults);
  }
}
