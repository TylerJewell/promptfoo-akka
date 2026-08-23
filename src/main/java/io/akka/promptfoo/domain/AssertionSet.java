package io.akka.promptfoo.domain;

import java.util.List;

/**
 * A flat or one-level-nested set of assertions to combine into a single verdict —
 * SPEC-001 §2. Nesting a set inside a set is rejected (Open decision 4.2).
 */
public record AssertionSet(List<Assertion> assertions, Double threshold, Double weight, String metric) {

  public double weightOrDefault() {
    return weight == null ? 1.0 : weight;
  }
}
