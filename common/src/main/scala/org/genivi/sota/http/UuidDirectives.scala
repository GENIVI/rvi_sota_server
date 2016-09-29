package org.genivi.sota.http
import akka.http.scaladsl.server.{Directive1, Directives}
import Directives._
import org.genivi.sota.data.Uuid
import org.genivi.sota.rest.Validation._

object UuidDirectives {
  lazy val extractUuid: Directive1[Uuid] = refined[Uuid.Valid](Slash ~ Segment).map(Uuid(_))
}
