/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.image

import akka.http.scaladsl.server.{Directive1, Directives}
import io.circe.generic.auto._
import org.genivi.sota.core.db.image.DataType.{Commit, PullUri, RefName}
import org.genivi.sota.core.db.image.ImageRepositorySupport
import org.genivi.sota.data.Namespace
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

object ImageResource {
  case class ImageRequest(commit: Commit, ref: RefName, description: String, pullUri: PullUri)
}

class ImageResource(namespace: Directive1[Namespace])(implicit db: Database, ec: ExecutionContext)
  extends Directives with ImageRepositorySupport {

  import ImageResource._

  val route = namespace { ns =>
    path("image") {
      post {
        entity(as[ImageRequest]) { img =>
          complete(imageRepository.persist(ns, img.commit, img.ref, img.description, img.pullUri))
        }
      } ~
      get {
        val f = imageRepository.findAll(ns)
        complete(f)
      }
    }
  }
}
