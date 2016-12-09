/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.client

import akka.http.javadsl.model.ResponseEntity
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.util.FastFuture
import scala.concurrent.ExecutionContext


final case class NoContent()

object NoContent {

  implicit def noContentUnmarshaller(implicit ec: ExecutionContext): Unmarshaller[ResponseEntity, NoContent]
    = Unmarshaller { implicit ec => response =>
      FastFuture.successful(NoContent())
    }

}
