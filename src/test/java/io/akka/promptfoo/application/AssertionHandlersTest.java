package io.akka.promptfoo.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.akka.promptfoo.domain.Assertion;
import io.akka.promptfoo.domain.AssertionType;
import io.akka.promptfoo.domain.GradingResult;
import io.akka.promptfoo.domain.Subject;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ports the individual assertion handlers — SPEC-001 §3 R1, R2, R7, R8, R9. */
class AssertionHandlersTest {

  private static Assertion of(AssertionType type, String value, boolean inverse) {
    return new Assertion(type, value, null, null, null, null, null, null, null, inverse);
  }

  @Test
  void containsPassesAndScoresOne() {
    GradingResult r =
        AssertionHandlers.handle(of(AssertionType.CONTAINS, "world", false), new Subject("hello world", null, null));
    assertTrue(r.pass());
    assertEquals(1.0, r.score()); // R1
  }

  @Test
  void containsFailsAndScoresZero() {
    GradingResult r =
        AssertionHandlers.handle(of(AssertionType.CONTAINS, "xyz", false), new Subject("hello world", null, null));
    assertFalse(r.pass());
    assertEquals(0.0, r.score()); // R1
  }

  @Test
  void inverseNegatesBasePass() {
    // R2: not-contains passes when the substring is absent.
    GradingResult r =
        AssertionHandlers.handle(of(AssertionType.CONTAINS, "xyz", true), new Subject("hello world", null, null));
    assertTrue(r.pass());
  }

  @Test
  void icontainsIsCaseInsensitive() {
    GradingResult r =
        AssertionHandlers.handle(of(AssertionType.ICONTAINS, "WORLD", false), new Subject("hello world", null, null));
    assertTrue(r.pass());
  }

  @Test
  void containsAnyMatchesOneOfSeveral() {
    Assertion a = new Assertion(AssertionType.CONTAINS_ANY, null, List.of("foo", "world"), null, null, null, null, null, null, false);
    GradingResult r = AssertionHandlers.handle(a, new Subject("hello world", null, null));
    assertTrue(r.pass());
  }

  @Test
  void containsAllRequiresEveryValue() {
    Assertion a = new Assertion(AssertionType.CONTAINS_ALL, null, List.of("hello", "missing"), null, null, null, null, null, null, false);
    GradingResult r = AssertionHandlers.handle(a, new Subject("hello world", null, null));
    assertFalse(r.pass());
  }

  @Test
  void regexMatches() {
    GradingResult r =
        AssertionHandlers.handle(of(AssertionType.REGEX, "^hello", false), new Subject("hello world", null, null));
    assertTrue(r.pass());
  }

  @Test
  void invalidRegexFailsRatherThanThrows() {
    GradingResult r =
        AssertionHandlers.handle(of(AssertionType.REGEX, "(unterminated", false), new Subject("x", null, null));
    assertFalse(r.pass());
  }

  @Test
  void startsWith() {
    GradingResult r =
        AssertionHandlers.handle(of(AssertionType.STARTS_WITH, "hello", false), new Subject("hello world", null, null));
    assertTrue(r.pass());
  }

  @Test
  void equalsExactMatch() {
    GradingResult r =
        AssertionHandlers.handle(of(AssertionType.EQUALS, "exact", false), new Subject("exact", null, null));
    assertTrue(r.pass());
  }

  // ---- R9: levenshtein, verified against fastest-levenshtein via pfprobe/probe.js -------

  @Test
  void levenshteinDistanceMatchesSourceLibrary() {
    // question-log row 5: distance("kitten","sitting") == 3, confirmed by running the
    // source's own fastest-levenshtein package.
    assertEquals(3, AssertionHandlers.distance("kitten", "sitting"));
  }

  @Test
  void levenshteinPassesWithinDefaultThreshold() {
    Assertion a = new Assertion(AssertionType.LEVENSHTEIN, "kitten", null, null, null, null, null, null, null, false);
    GradingResult r = AssertionHandlers.handle(a, new Subject("sitting", null, null)); // distance 3 <= default 5, > 1
    assertTrue(r.pass());
  }

  @Test
  void levenshteinFailsPastExplicitThreshold() {
    Assertion a = new Assertion(AssertionType.LEVENSHTEIN, "kitten", null, null, null, null, 2.0, null, null, false);
    GradingResult r = AssertionHandlers.handle(a, new Subject("sitting", null, null)); // distance 3 > 2
    assertFalse(r.pass());
  }

  // ---- word-count -------------------------------------------------------------------

  @Test
  void wordCountExact() {
    Assertion a = new Assertion(AssertionType.WORD_COUNT, null, null, 2, null, null, null, null, null, false);
    GradingResult r = AssertionHandlers.handle(a, new Subject("hello world", null, null));
    assertTrue(r.pass());
  }

  @Test
  void wordCountRangeMinMax() {
    Assertion a = new Assertion(AssertionType.WORD_COUNT, null, null, null, 1, 3, null, null, null, false);
    GradingResult r = AssertionHandlers.handle(a, new Subject("one two", null, null));
    assertTrue(r.pass());
  }

  @Test
  void wordCountRangeFailsBelowMinimum() {
    Assertion a = new Assertion(AssertionType.WORD_COUNT, null, null, null, 5, 10, null, null, null, false);
    GradingResult r = AssertionHandlers.handle(a, new Subject("one two", null, null)); // 2 words, below min 5
    assertFalse(r.pass());
  }

  @Test
  void wordCountInverseNegatesRange() {
    Assertion a = new Assertion(AssertionType.WORD_COUNT, null, null, null, 1, 3, null, null, null, true);
    GradingResult r = AssertionHandlers.handle(a, new Subject("one two", null, null));
    assertFalse(r.pass()); // in range, so inverse fails
  }

  // ---- R7: cost / latency require threshold and a measured value ------------------------

  @Test
  void costWithoutThresholdThrows() {
    Assertion a = new Assertion(AssertionType.COST, null, null, null, null, null, null, null, null, false);
    assertThrows(IllegalArgumentException.class, () -> AssertionHandlers.handle(a, new Subject("x", 0.01, null)));
  }

  @Test
  void costWithoutMeasuredValueThrows() {
    Assertion a = new Assertion(AssertionType.COST, null, null, null, null, null, 0.05, null, null, false);
    assertThrows(IllegalArgumentException.class, () -> AssertionHandlers.handle(a, new Subject("x", null, null)));
  }

  @Test
  void costUnderThresholdPasses() {
    Assertion a = new Assertion(AssertionType.COST, null, null, null, null, null, 0.05, null, null, false);
    GradingResult r = AssertionHandlers.handle(a, new Subject("x", 0.02, null));
    assertTrue(r.pass());
  }

  @Test
  void latencyWithoutThresholdThrows() {
    Assertion a = new Assertion(AssertionType.LATENCY, null, null, null, null, null, null, null, null, false);
    assertThrows(IllegalArgumentException.class, () -> AssertionHandlers.handle(a, new Subject("x", null, 100L)));
  }

  @Test
  void latencyWithoutMeasuredValueThrows() {
    Assertion a = new Assertion(AssertionType.LATENCY, null, null, null, null, null, 500.0, null, null, false);
    assertThrows(IllegalArgumentException.class, () -> AssertionHandlers.handle(a, new Subject("x", null, null)));
  }

  @Test
  void latencyUnderThresholdPasses() {
    Assertion a = new Assertion(AssertionType.LATENCY, null, null, null, null, null, 500.0, null, null, false);
    GradingResult r = AssertionHandlers.handle(a, new Subject("x", null, 200L));
    assertTrue(r.pass());
  }
}
