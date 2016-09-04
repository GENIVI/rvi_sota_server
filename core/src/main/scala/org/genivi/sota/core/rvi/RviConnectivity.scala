/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.rvi


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.Uri
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.core.resolver.Connectivity


class RviConnectivity(rviEndpoint: Uri)(implicit system: ActorSystem,
                      mat: ActorMaterializer,
                      ec: ExecutionContext) extends Connectivity {

  override implicit val transport: Json => Future[Json] = HttpTransport(rviEndpoint).requestTransport
  override implicit val client = new JsonRpcRviClient(transport, system.dispatcher)
}
