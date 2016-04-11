package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.{HttpRequest, StatusCode}

case class Semantics(
  request   : HttpRequest,
  statusCode: StatusCode,
  result    : Result
)
