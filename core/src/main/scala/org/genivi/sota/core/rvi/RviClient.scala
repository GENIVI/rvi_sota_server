/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import io.circe.Encoder
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random
import com.github.nscala_time.time.Imports.DateTime

trait RviClient {
  def sendMessage[A](service: String, message: A, expirationDate: DateTime)
                    (implicit encoder: Encoder[A] ) : Future[Int]
}

import io.circe._

class JsonRpcRviClient(transport: Json => Future[Json], ec: ExecutionContext) extends RviClient {

  import shapeless._
  import shapeless.syntax.singleton._
  import io.circe.generic.auto._
  import org.genivi.sota.core.jsonrpc.client
  import shapeless.record._

  override def sendMessage[A](service: String, message: A, expirationDate: DateTime)
    (implicit encoder: Encoder[A]) : Future[Int] = {
    implicit val exec = ec
    client.message.request( ('service_name ->> service) :: ('timeout ->> expirationDate.getMillis() / 1000) :: ('parameters ->> Seq(message)) :: HNil, Random.nextInt() )
      .run[Record.`'transaction_id -> Int`.T](transport)
      .map( _.get('transaction_id))
  }

}