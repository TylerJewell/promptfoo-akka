package io.akka.promptfoo.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.akka.promptfoo.domain.Assertion;
import io.akka.promptfoo.domain.AssertionSet;
import io.akka.promptfoo.domain.AssertionType;
import io.akka.promptfoo.domain.GradingResult;
import io.akka.promptfoo.domain.Subject;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The weighted aggregate scoring engine — SPEC-001 §3 R3-R6, verified against the source's
 * AssertionsResult via pfprobe/probe.js (question-log rows 1-3, 7).
 */
class ScoringEngineTest {

  private static Assertion contains(String value, Double weight, String metric, boolean inverse) {
    return new Assertion(AssertionType.CONTAINS, value, null, null, null, null, null, weight, metric, inverse);
  }

  @Test
  void allPassWithNoThreshold() {
    AssertionSet set = new AssertionSet(List.of(contains("hello", null, null, false), contains("world", null, null, false)), null, null, null);
    GradingResult r = ScoringEngine.grade(set, new Subject("hello world", null, null));
    assertTrue(r.pass());
    assertEquals(1.0, r.score());
  }

  @Test
  void oneFailureFailsSetWithNoThreshold() {
    // R4, question-log row 2: score 0.75 weighted average still fails without a threshold.
    AssertionSet set = new AssertionSet(
        List.of(contains("hello", 3.0, null, false), contains("missing", 1.0, null, false)), null, null, null);
    GradingResult r = ScoringEngine.grade(set, new Subject("hello world", null, null));
    assertEquals(0.75, r.score(), 1e-9);
    assertFalse(r.pass());
  }

  @Test
  void thresholdOverridesIndividualFailure() {
    // R5, question-log row 3: same inputs, threshold 0.7 -> pass, because the aggregate score
    // decides instead of the individual component.
    AssertionSet set = new AssertionSet(
        List.of(contains("hello", 3.0, null, false), contains("missing", 1.0, null, false)), 0.7, null, null);
    GradingResult r = ScoringEngine.grade(set, new Subject("hello world", null, null));
    assertEquals(0.75, r.score(), 1e-9);
    assertTrue(r.pass());
  }

  @Test
  void zeroThresholdIsHonoredNotTreatedAsAbsent() {
    // R5, question-log row 1: threshold: 0 still force-passes via score >= 0, because the
    // gate is `typeof threshold === 'number'`, not truthiness.
    AssertionSet set = new AssertionSet(List.of(contains("missing", null, null, false)), 0.0, null, null);
    GradingResult r = ScoringEngine.grade(set, new Subject("hello world", null, null));
    assertTrue(r.pass());
    assertEquals(0.0, r.score());
  }

  @Test
  void namedScoresNormalizeByTheirOwnWeight() {
    // R6, question-log row 7: a metric's denominator is its own accumulated weight, not the
    // set's total weight.
    AssertionSet set = new AssertionSet(
        List.of(
            contains("hello", 2.0, "greeting", false),
            contains("world", 1.0, "greeting", false),
            contains("other", 5.0, "unrelated", false)),
        null,
        null,
        null);
    GradingResult r = ScoringEngine.grade(set, new Subject("hello world", null, null));
    assertEquals(1.0, r.namedScores().get("greeting"), 1e-9);
    assertEquals(0.0, r.namedScores().get("unrelated"), 1e-9);
  }

  @Test
  void componentResultsCarryOneEntryPerAssertion() {
    AssertionSet set = new AssertionSet(
        List.of(contains("hello", null, null, false), contains("world", null, null, false)), null, null, null);
    GradingResult r = ScoringEngine.grade(set, new Subject("hello world", null, null));
    assertEquals(2, r.componentResults().size());
  }
}
