/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.rvi

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.Uri
import scala.concurrent.ExecutionContext

import org.genivi.sota.core.Connectivity
import org.genivi.sota.core.jsonrpc.HttpTransport


class RviConnectivity(implicit system: ActorSystem,
                      mat: ActorMaterializer,
                      ec: ExecutionContext) extends Connectivity {

  val rviUri = Uri(system.settings.config.getString("rvi.endpoint"))
  override implicit val transport = HttpTransport(rviUri).requestTransport
  override implicit val client = new JsonRpcRviClient(transport, system.dispatcher)
}
