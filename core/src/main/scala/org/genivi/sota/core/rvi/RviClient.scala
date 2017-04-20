/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.rvi

import io.circe._
import org.genivi.sota.core.resolver.ConnectivityClient
import java.time.Instant
import io.circe.shapes._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random

/**
 * Concrete implementation for sending a message to RVI in JSON-RPC format.
 */
class JsonRpcRviClient(transport: Json => Future[Json], ec: ExecutionContext) extends ConnectivityClient {

  import shapeless._
  import shapeless.syntax.singleton._
  import io.circe.generic.auto._
  import org.genivi.sota.core.jsonrpc.client
  import shapeless.record._

  /**
   * Send a JSON-RPC formatted message to RVI.
   *
   * @param service the path to the destination endpoint
   * @param message the message of generic type
   * @param expirationDate the expiration in absolute time
   * @return a future of the transaction ID
   */
  override def sendMessage[A](service: String, message: A, expirationDate: Instant)
    (implicit encoder: Encoder[A]) : Future[Int] = {
    implicit val exec = ec
    client.message.request(
        ('service_name ->> service) ::
        ('timeout ->> expirationDate.toEpochMilli / 1000) ::
        ('parameters ->> Seq(message)) :: HNil,
        Random.nextInt() )
      .run[Record.`'transaction_id -> Int`.T](transport)
      .map( _.get('transaction_id))
  }
}
