/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.image

import io.circe.generic.auto._
import akka.http.scaladsl.server.{Directives, _}
import org.genivi.sota.core.db.image.DataType.{Commit, Image, ImageId, ImageUpdate, ImageUpdateId, PullUri, RefName}
import org.genivi.sota.data.{Namespace, Uuid}
import akka.http.scaladsl.model.StatusCodes._
import org.genivi.sota.core.data.UpdateSpec._
import org.genivi.sota.http.ErrorHandler.handleErrors
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.rest.GenericResponseEncoder

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._

object ImageUpdateResource {

  case class PendingImageUpdate(id: ImageUpdateId,
                                imageId: ImageId,
                                commit: Commit,
                                ref: RefName,
                                description: String,
                                pullUri: PullUri)

  implicit val pendingImageUpdateResponseEncoder =
    GenericResponseEncoder { (imageUpdate: ImageUpdate, image: Image) =>
      PendingImageUpdate(
        imageUpdate.id,
        image.id,
        image.commit,
        image.ref,
        image.description,
        image.pullUri
      )
    }
}

class ImageUpdateResource(namespace: Directive1[Namespace])
                         (implicit db: Database, ec: ExecutionContext) extends Directives {

  import org.genivi.sota.core.image.ImageUpdateResource._
  import org.genivi.sota.rest.ResponseConversions._

  val ImageIdMatcher: PathMatcher1[ImageId] = JavaUUID.map(u => ImageId(u))

  val DeviceIdMatcher: PathMatcher1[Uuid] = JavaUUID.map(Uuid.fromJava)

  val imageUpdateProcess = new ImageUpdateProcess

  val userRoutes = namespace { ns =>
    path("image_update" / ImageIdMatcher / DeviceIdMatcher) { (image, device) =>
      complete(imageUpdateProcess.queue(ns, image, device))
    }
  }

  val deviceRoutes =
    path("image_update" / DeviceIdMatcher) { device =>
      complete(imageUpdateProcess.findPendingFor(device).map(_.toResponse))
  }

  val route = handleErrors { userRoutes ~ deviceRoutes }
}
