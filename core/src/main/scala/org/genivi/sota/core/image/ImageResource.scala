/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.image

import akka.http.scaladsl.server.{Directive1, Directives, PathMatcher1}
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.core.db.image.DataType.{Commit, Image, ImageId, RefName}
import org.genivi.sota.core.db.image.ImageRepositorySupport
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

object ImageResource {
  case class ImageRequest(commit: Commit, ref: RefName, description: String)
}

class ImageUpdateResource(namespace: Directive1[Namespace])
                         (implicit db: Database, ec: ExecutionContext) extends Directives {

  val ImageIdMatcher: PathMatcher1[ImageId] = JavaUUID.map(u => ImageId(u))

  val DeviceIdMatcher: PathMatcher1[Uuid] = JavaUUID.map(Uuid.fromJava)

  val userRoutes =
    path("image_update" / ImageIdMatcher / DeviceIdMatcher) { (image, device) =>
      complete("")
    }

  val deviceRoutes =
    path("image_update" / DeviceIdMatcher) { device =>
      complete("")
  }

  val route = userRoutes ~ deviceRoutes
}

class ImageResource(namespace: Directive1[Namespace])(implicit db: Database, ec: ExecutionContext)
  extends Directives with ImageRepositorySupport {

  import ImageResource._

  val route = namespace { ns =>
    path("image") {
      post {
        entity(as[ImageRequest]) { img =>
          val f = imageRepository.persist(Image(ns, ImageId.generate(), img.commit, img.ref, img.description))
          complete(f)
        }
      } ~
      get {
        val f = imageRepository.findAll(ns)
        complete(f)
      }
    }
  }
}
