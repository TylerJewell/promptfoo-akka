package io.akka.promptfoo.domain;

import java.util.List;

/**
 * One deterministic assertion — SPEC-001 §2. `value`/`values` carry the shape each type needs:
 * a single string for equals/contains/icontains/regex/starts-with/levenshtein, a list for the
 * contains-any/all family, and the word-count min/max/exact fields for word-count.
 */
public record Assertion(
    AssertionType type,
    String value,
    List<String> values,
    Integer wordCountExact,
    Integer wordCountMin,
    Integer wordCountMax,
    Double threshold,
    Double weight,
    String metric,
    boolean inverse) {

  public double weightOrDefault() {
    return weight == null ? 1.0 : weight;
  }
}
