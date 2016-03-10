package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.{HttpRequest, StatusCode}
import org.genivi.sota.resolver.test.Result


case class Semantics(
  request   : HttpRequest,
  statusCode: StatusCode,
  result    : Result
)
