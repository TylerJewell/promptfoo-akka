# Acknowledgements

This project is a port of **[promptfoo/promptfoo](https://github.com/promptfoo/promptfoo)**.

- **Licence.** `promptfoo/promptfoo` is MIT-licensed, copyright Promptfoo 2025 (its
  `LICENSE` file, first line). This port is MIT-licensed as well.

- **Was anything copied verbatim?** Yes — the human-readable reason/error strings
  produced by the deterministic assertion handlers and the scoring engine, listed below.
  Every one is a message shown to a person reading a test result, not application logic;
  reproducing them exactly is what makes `bench/compare.js`'s pass/fail/score comparison
  against the source meaningful, and a caller migrating from promptfoo sees the same
  wording. Found by `python toolkit/copied_strings.py promptfoo --source promptfoo-src`
  and checked one by one against `promptfoo-src/src/assertions/*.ts`:

  - `"Assertion passed"`, `"All assertions passed"` — the generic pass reasons every
    handler and the scoring engine return.
  - `"Expected output \"…\" to … equal \"…\""`, `"Expected output to … contain \"…\""`,
    `"Expected output to … contain one of \"…\""`, `"Expected output to … contain all of
    […]. Missing: […]"`, `"Expected output to … start with \"…\""`, `"Expected output to
    … match regex \"…\""` — the per-type failure reasons in `equals.ts`, `contains.ts`,
    `startsWith.ts`, `regex.ts`.
  - `"Levenshtein distance … is … threshold …"`, `"Word count … …"` — from
    `levenshtein.ts`, `wordCount.ts`.
  - `"Cost assertion must have a threshold"`, `"Cost assertion does not support providers
    that do not return cost"`, `"Latency assertion must have a threshold in
    milliseconds"`, `"Latency assertion does not support cached results. Rerun the eval
    with --no-cache"` — the configuration-error messages `cost.ts`/`latency.ts` throw
    (SPEC-001 R7).
  - `"Aggregate score … threshold"` — the aggregate reason `assertionsResult.ts` builds
    when a numeric threshold decides the set's pass/fail (R5).
  - `"less than or equal to"` / `"greater than"` — the comparator words shared by
    `cost.ts`, `latency.ts`, `levenshtein.ts`'s inverse-aware reasons.

  `'hello world'` also appears in both trees; that one is not a copy, it is the same
  stock example string independently chosen for this port's own tests (`AssertionHandlersTest.java`,
  `ScoringEngineTest.java`, `TimingMain.java`) — `copied_strings.py` correctly flags the
  coincidence, and it is recorded here rather than left unexplained.

- **Is behaviour derived even where no text was copied?** Yes, throughout — the whole
  point of this port. `ScoringEngine` and `AssertionHandlers` reproduce the decision
  procedure in `src/assertions/assertionsResult.ts` and the eight handler files named in
  SPEC-001, rule for rule (see `specs/SPEC-001-promptfoo.md` §3 and
  `docs/question-log.md`).

## Also used

- Akka
