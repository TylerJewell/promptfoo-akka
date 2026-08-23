package io.akka.promptfoo.application;

import io.akka.promptfoo.domain.Assertion;
import io.akka.promptfoo.domain.GradingResult;
import io.akka.promptfoo.domain.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The deterministic leaf assertion handlers — SPEC-001 §3 rules R1, R2, R7, R8, R9. Each
 * handler ports one file under promptfoo/src/assertions/ one-for-one; see question-log.md
 * for what was run against the source and what was only read.
 */
public final class AssertionHandlers {

  private AssertionHandlers() {}

  public static GradingResult handle(Assertion assertion, Subject subject) {
    boolean basePass =
        switch (assertion.type()) {
          case EQUALS -> equals(assertion, subject);
          case CONTAINS -> subject.outputString().contains(requireValue(assertion));
          case ICONTAINS ->
              subject
                  .outputString()
                  .toLowerCase()
                  .contains(requireValue(assertion).toLowerCase());
          case CONTAINS_ANY -> requireValues(assertion).stream().anyMatch(subject.outputString()::contains);
          case ICONTAINS_ANY ->
              requireValues(assertion).stream()
                  .anyMatch(v -> subject.outputString().toLowerCase().contains(v.toLowerCase()));
          case CONTAINS_ALL -> requireValues(assertion).stream().allMatch(subject.outputString()::contains);
          case ICONTAINS_ALL ->
              requireValues(assertion).stream()
                  .allMatch(v -> subject.outputString().toLowerCase().contains(v.toLowerCase()));
          case REGEX -> regex(assertion, subject);
          case STARTS_WITH -> subject.outputString().startsWith(requireValue(assertion));
          case LEVENSHTEIN -> levenshtein(assertion, subject) <= levenshteinThreshold(assertion);
          case WORD_COUNT -> wordCount(assertion, subject);
          case COST -> cost(assertion, subject);
          case LATENCY -> latency(assertion, subject);
        };

    boolean pass = basePass != assertion.inverse(); // R2
    return GradingResult.leaf(pass, reason(assertion, subject, pass));
  }

  // ---- equals (equals.ts lines 5-31; question-log row 4) --------------------------------

  private static boolean equals(Assertion assertion, Subject subject) {
    return java.util.Objects.equals(assertion.value(), subject.outputString());
  }

  // ---- regex (regex.ts) ------------------------------------------------------------------

  private static boolean regex(Assertion assertion, Subject subject) {
    try {
      Pattern pattern = Pattern.compile(requireValue(assertion));
      return pattern.matcher(subject.outputString()).find();
    } catch (PatternSyntaxException e) {
      // A malformed pattern fails the assertion rather than propagating, matching the
      // source's caught-and-returned-false-result behavior in regex.ts.
      return false;
    }
  }

  // ---- levenshtein (levenshtein.ts; question-log row 5) ----------------------------------

  static int levenshtein(Assertion assertion, Subject subject) {
    return distance(subject.outputString(), requireValue(assertion));
  }

  private static double levenshteinThreshold(Assertion assertion) {
    return assertion.threshold() == null ? 5 : assertion.threshold();
  }

  /** Standard unit-cost Levenshtein distance, matching fastest-levenshtein's distance(). */
  static int distance(String a, String b) {
    int[] prev = new int[b.length() + 1];
    int[] curr = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) {
      prev[j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      curr[0] = i;
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
      }
      int[] tmp = prev;
      prev = curr;
      curr = tmp;
    }
    return prev[b.length()];
  }

  // ---- word-count (wordCount.ts; question-log row 9) -------------------------------------

  private static boolean wordCount(Assertion assertion, Subject subject) {
    int count = countWords(subject.outputString());
    if (assertion.wordCountMin() != null || assertion.wordCountMax() != null) {
      boolean min = assertion.wordCountMin() == null || count >= assertion.wordCountMin();
      boolean max = assertion.wordCountMax() == null || count <= assertion.wordCountMax();
      return min && max;
    }
    if (assertion.wordCountExact() == null) {
      throw new IllegalArgumentException("\"word-count\" assertion must have a value");
    }
    return count == assertion.wordCountExact();
  }

  private static int countWords(String text) {
    String trimmed = text.trim();
    if (trimmed.isEmpty()) {
      return 0;
    }
    return trimmed.split("\\s+").length;
  }

  // ---- cost / latency (cost.ts, latency.ts; question-log row 8: config errors throw) -----

  private static boolean cost(Assertion assertion, Subject subject) {
    if (assertion.threshold() == null) {
      throw new IllegalArgumentException("Cost assertion must have a threshold");
    }
    if (subject.cost() == null) {
      throw new IllegalArgumentException(
          "Cost assertion does not support providers that do not return cost");
    }
    return subject.cost() <= assertion.threshold();
  }

  private static boolean latency(Assertion assertion, Subject subject) {
    if (assertion.threshold() == null) {
      throw new IllegalArgumentException("Latency assertion must have a threshold in milliseconds");
    }
    if (subject.latencyMs() == null) {
      throw new IllegalArgumentException(
          "Latency assertion does not support cached results. Rerun the eval with --no-cache");
    }
    return subject.latencyMs() <= assertion.threshold();
  }

  // ---- shared -------------------------------------------------------------------------

  private static String requireValue(Assertion assertion) {
    if (assertion.value() == null || assertion.value().isEmpty()) {
      throw new IllegalArgumentException(
          "\"" + assertion.type() + "\" assertion type must have a string value");
    }
    return assertion.value();
  }

  private static List<String> requireValues(Assertion assertion) {
    if (assertion.values() == null || assertion.values().isEmpty()) {
      throw new IllegalArgumentException(
          "\"" + assertion.type() + "\" assertion type must have an array value");
    }
    return assertion.values();
  }

  private static String reason(Assertion assertion, Subject subject, boolean pass) {
    if (pass) {
      return "Assertion passed";
    }
    String not = assertion.inverse() ? "not " : "";
    return switch (assertion.type()) {
      case EQUALS -> "Expected output \"" + subject.outputString() + "\" to " + not + "equal \"" + assertion.value() + "\"";
      case CONTAINS, ICONTAINS -> "Expected output to " + not + "contain \"" + assertion.value() + "\"";
      case CONTAINS_ANY, ICONTAINS_ANY -> "Expected output to " + not + "contain one of \"" + String.join(", ", requireValues(assertion)) + "\"";
      case CONTAINS_ALL, ICONTAINS_ALL -> {
        List<String> missing = new ArrayList<>();
        for (String v : requireValues(assertion)) {
          boolean has = assertion.type() == io.akka.promptfoo.domain.AssertionType.CONTAINS_ALL
              ? subject.outputString().contains(v)
              : subject.outputString().toLowerCase().contains(v.toLowerCase());
          if (!has) {
            missing.add(v);
          }
        }
        yield "Expected output to " + not + "contain all of [" + String.join(", ", requireValues(assertion))
            + "]. Missing: [" + String.join(", ", missing) + "]";
      }
      case REGEX -> "Expected output to " + not + "match regex \"" + assertion.value() + "\"";
      case STARTS_WITH -> "Expected output to " + not + "start with \"" + assertion.value() + "\"";
      case LEVENSHTEIN -> "Levenshtein distance " + levenshtein(assertion, subject) + " is "
          + (assertion.inverse() ? "less than or equal to" : "greater than") + " threshold " + levenshteinThreshold(assertion);
      case WORD_COUNT -> "Word count " + countWords(subject.outputString()) + " did not satisfy the configured constraint";
      case COST -> "Cost " + subject.cost() + " is " + (assertion.inverse() ? "less than or equal to" : "greater than") + " threshold " + assertion.threshold();
      case LATENCY -> "Latency " + subject.latencyMs() + "ms is " + (assertion.inverse() ? "less than or equal to" : "greater than") + " threshold " + assertion.threshold() + "ms";
    };
  }
}
