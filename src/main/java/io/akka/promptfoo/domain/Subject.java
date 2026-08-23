package io.akka.promptfoo.domain;

/** The non-deterministic subject being graded — SPEC-001 §2. */
public record Subject(String outputString, Double cost, Long latencyMs) {}
