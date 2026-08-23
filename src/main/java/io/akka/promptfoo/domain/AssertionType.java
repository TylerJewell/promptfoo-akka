package io.akka.promptfoo.domain;

/** The deterministic assertion types covered by SPEC-001 §1 — no LLM-graded types. */
public enum AssertionType {
  EQUALS,
  CONTAINS,
  ICONTAINS,
  CONTAINS_ANY,
  ICONTAINS_ANY,
  CONTAINS_ALL,
  ICONTAINS_ALL,
  REGEX,
  STARTS_WITH,
  LEVENSHTEIN,
  WORD_COUNT,
  COST,
  LATENCY
}
