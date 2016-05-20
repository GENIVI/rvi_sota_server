/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.resolver

import com.github.nscala_time.time.Imports.DateTime
import io.circe.{Encoder, Json}

import scala.concurrent.Future

trait Connectivity {
  implicit val transport: Json => Future[Json]
  implicit val client: ConnectivityClient
}

object DefaultConnectivity extends Connectivity {
  // TODO: to be implemented
  override implicit val transport = { (_: Json) =>
    Future.successful(Json.Null)
  }
  override implicit val client = DefaultConnectivityClient
}

/**
 * Abstract interface for sending a message to the backing communication layer.
 */
trait ConnectivityClient {
  def sendMessage[A](service: String, message: A, expirationDate: DateTime)
                    (implicit encoder: Encoder[A] ) : Future[Int]
}

object DefaultConnectivityClient extends ConnectivityClient {
  override def sendMessage[A](service: String, message: A, expirationDate: DateTime)
                             (implicit encoder: Encoder[A]): Future[Int] = {
    // TODO: to be implemented
    Future.successful(0)
  }
}
