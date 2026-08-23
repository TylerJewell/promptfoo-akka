package io.akka.promptfoo.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.http.HttpResponses;
import io.akka.promptfoo.application.ScoringEngine;
import io.akka.promptfoo.domain.Assertion;
import io.akka.promptfoo.domain.AssertionSet;
import io.akka.promptfoo.domain.GradingResult;
import io.akka.promptfoo.domain.Subject;

/**
 * The assertion engine's own surface — SPEC-001 §5 end-to-end row. Accepts a subject and an
 * assertion set and returns the same GradingResult shape the source's evaluator produces.
 */
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
@HttpEndpoint("")
public class GradeEndpoint {

  public record GradeRequest(Subject subject, AssertionSet assertions) {}

  @Get("/")
  public HttpResponse index() {
    return HttpResponses.staticResource("index.html");
  }

  @Post("/grade")
  public HttpResponse grade(GradeRequest request) {
    for (Assertion a : request.assertions().assertions()) {
      if (a.type() == null) {
        throw new IllegalArgumentException("assertion type is required");
      }
    }
    GradingResult result = ScoringEngine.grade(request.assertions(), request.subject());
    return HttpResponses.ok(result);
  }
}
