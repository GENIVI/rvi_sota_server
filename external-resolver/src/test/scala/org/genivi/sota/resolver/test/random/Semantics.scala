package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.{HttpRequest, StatusCode}

/**
  * Interpreting a command or query results in a [[Semantics]].
  * For computing stats, the original query is kept alongside with its expected [[Result]].
  */
case class Semantics(
  original  : Option[Query] = None,
  request   : HttpRequest,
  statusCode: StatusCode,
  result    : Result
) {
  if (isQuery && result.isEmptyResponse) { throw new IllegalArgumentException }

  def isQuery: Boolean = original.nonEmpty
}

object Semantics {
  def apply(request: HttpRequest,
            statusCode: StatusCode,
            result: Result): Semantics =
    new Semantics(None, request, statusCode, result)
}
