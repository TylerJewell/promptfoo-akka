# promptfoo-akka

Scores one piece of text against a set of rules and combines the results into a single
pass, fail, and number between zero and one.

A port of [promptfoo/promptfoo](https://github.com/promptfoo/promptfoo) onto **Akka**.

---

## Where it came from

promptfoo is a tool for testing the output of language models against rules a person
writes — does the answer contain a phrase, match a pattern, stay under a word count, cost
less than a limit. This port takes only that grading step: given one answer and one set of
rules, produce the same pass/fail/score verdict promptfoo's own grading engine would.

The specifications this port was built from are in
[TylerJewell/akka-specify-harness](https://github.com/TylerJewell/akka-specify-harness)
under `promptfoo-port/`.

---

## promptfoo → this port

📉 789 source lines → **323 lines**<br>
📁 9 files → **9 files**<br>
🖥️ 1 process (as part of the full CLI/server) → **1 process**<br>
⚡ same-process call, 164 ns/op → **same-process call, 313 ns/op**<br>
🎯 grading agreement across 14 mixed workloads → **14/14**

Full method and the numbers that did not make this list: [`bench/REPORT.md`](../promptfoo-port/bench/REPORT.md).

---

## What it took to build

⏱️ **0.6 hours** from the first command to the published repository, **0.6** of them active<br>
💬 **305** exchanges with the model<br>
✍️ **134,860** tokens written by the model, **51,710,069** counting everything sent and re-sent<br>
🙋 **0** questions to a human<br>
🧪 **29** tests

```bash
python toolkit/tokens.py --port promptfoo    # turns, tokens, elapsed and active time
```

The record of every question, and where the time went, is in
[`port-log/`](../port-log).

---

## What it does

From the specification:

- **A rule either passes or fails a piece of text; there is no partial credit at that
  level.** Each individual check — does it contain a word, match a pattern, stay under a
  cost — returns a plain yes or no.
- **A group of rules combines by a weighted average, and one failing rule fails the whole
  group unless a passing score is explicitly set.** Without a passing score, every rule in
  the group has to pass. With one set, the average across the group decides instead, even
  if one rule on its own failed.
- **A passing score of zero still counts.** A group can be configured to accept any
  average at all — the setting is still "on," it is just set as low as it goes.
- **Some checks need information the caller must supply, and refuse to run without it.**
  Checking a cost limit or a time limit without ever being told the actual cost or time is
  treated as a mistake in how the check was set up, not as a failed check.

---

## Design decisions

**A weighted average, not a simple pass count.** Some rules matter more than others when
scoring a batch of them together, so each one can be given a weight, and the group's score
is the average weighted by that number rather than a plain fraction passed. This lets one
important rule outweigh several minor ones without needing a separate pass just for it.

**A passing score overrides individual failures rather than sitting beside them.** When a
passing score is set on a group, the group's own pass/fail no longer looks at whether
every rule passed — it looks only at the weighted average against that score. This matches
what a passing score is for: letting "mostly right" count as good enough, instead of
requiring perfection and a separate number.

**A named score is averaged by its own weight, not the group's.** A rule can be tagged
with a name, and several rules can share the same name to build up one combined score
under that name. That combined score divides by the weight of only the rules carrying
that name, not by the weight of everything in the group. This means adding an unrelated,
heavily weighted rule to a group never waters down a named score that has nothing to do
with it.

---

## Running it — the developer path

### Requirements

- Java 21 or newer
- Maven 3.9 or newer
- An Akka download token — run `akka code token` once

### Start the service

```bash
mvn compile
akka local run
```

The service starts on **port 9063**.

### Try it

```bash
curl -X POST http://localhost:9063/grade \
  -H "Content-Type: application/json" \
  -d '{"subject":{"outputString":"hello world"},"assertions":{"assertions":[{"type":"CONTAINS","value":"hello","weight":3,"inverse":false},{"type":"CONTAINS","value":"missing","weight":1,"inverse":false}],"threshold":0.7}}'
```

Or open http://localhost:9063/ for a small page that does the same thing.

---

## Model providers

This port never calls a language model. It grades text that was already produced
somewhere else — the answer to grade, and the rules to grade it against, are both given
directly in the request.

---

## Configuration

| Variable | Default | Notes |
|---|---|---|
| `akka.javasdk.dev-mode.http-port` | `9063` | set in `application.conf`, not an environment variable |

---

## Where it differs from promptfoo

Everything not listed here behaves the same way on purpose, including the parts that
look like mistakes.

- **How a rule is told to check the opposite of its normal check.** promptfoo reads this
  from the rule's own name — a name starting with "not" means check the opposite. This
  port takes it as its own separate yes/no setting on the rule instead, because this port's
  request shape is a plain data record, not a short name meant to be typed by a person
  configuring a test file.
- **How a list of values is given to a "contains one of these" or "contains all of these"
  rule.** promptfoo accepts either a real list or one string with the values separated by
  commas, with a small parser for quoted values containing commas of their own. This port
  accepts only a real list. The comma-separated-string form is a convenience for someone
  typing a test file by hand; this port's request shape is already a structured record, so
  there was no string to split in the first place.
- **How a group of rules is shown on screen.** promptfoo shows the pass/fail and score of
  every rule as part of its own results table, alongside everything else a test run
  produces. This port ships a small page of its own showing just the grading result for
  one request, rather than reusing promptfoo's full results table, because that table is
  part of a much larger application (managing test runs, calling language models, storing
  history) that this port does not include. Comparing the two pages' appearance side by
  side was not done — doing so would mean running that larger application, including a
  live call to a language model, which this port was not given the means to do.

---

## Licence

promptfoo/promptfoo is MIT-licensed, © Promptfoo 2025. This port reimplements the
behaviour described above; several human-readable messages are copied verbatim where
doing so is what makes the two systems' results comparable — see `ACKNOWLEDGEMENTS.md`.
