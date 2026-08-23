package io.akka.promptfoo.application;

import io.akka.promptfoo.domain.Assertion;
import io.akka.promptfoo.domain.AssertionSet;
import io.akka.promptfoo.domain.AssertionType;
import io.akka.promptfoo.domain.Subject;
import java.util.List;

/**
 * In-process timing counterpart to bench/timing.js's timeRef -- the same workload, same
 * shape of loop, run inside the JVM rather than over HTTP, so bench/REPORT.md can report
 * the engine's own cost apart from the wire call. Not a JUnit test: run directly.
 */
public class TimingMain {
  public static void main(String[] args) {
    AssertionSet set = new AssertionSet(
        List.of(
            new Assertion(AssertionType.CONTAINS, "hello", null, null, null, null, null, 3.0, null, false),
            new Assertion(AssertionType.CONTAINS, "missing", null, null, null, null, null, 1.0, null, false)),
        0.7,
        null,
        null);
    Subject subject = new Subject("hello world", null, null);

    int windows = 5;
    int reps = 200_000;
    double[] results = new double[windows];
    for (int w = 0; w < windows; w++) {
      long start = System.nanoTime();
      for (int i = 0; i < reps; i++) {
        ScoringEngine.grade(set, subject);
      }
      long end = System.nanoTime();
      results[w] = (double) (end - start) / reps;
    }
    java.util.Arrays.sort(results);
    double median = results[windows / 2];
    System.out.println("{\"engine_ns_per_op\": " + median + ", \"windows\": " + windows + "}");
  }
}
