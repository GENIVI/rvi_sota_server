/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.{Directive0, Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import org.genivi.sota.client.DeviceRegistryClient
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.Namespace
import org.genivi.sota.db.BootMigrations
import org.genivi.sota.http.AuthedNamespaceScope
import org.genivi.sota.http.LogDirectives._
import org.genivi.sota.http.{HealthResource, NamespaceDirectives, TraceId}
import org.genivi.sota.messaging.daemon.MessageBusListenerActor.Subscribe
import org.genivi.sota.resolver.components.ComponentDirectives
import org.genivi.sota.resolver.daemon.PackageCreatedListener
import org.genivi.sota.resolver.devices.DeviceDirectives
import org.genivi.sota.resolver.filters.FilterDirectives
import org.genivi.sota.resolver.packages.{PackageDirectives, PackageFiltersResource}
import org.genivi.sota.resolver.resolve.ResolveDirectives
import org.genivi.sota.rest.SotaRejectionHandler.rejectionHandler
import org.slf4j.LoggerFactory
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext
import scala.util.Try


/**
 * Base API routing class.
  *
  * @see {@linktourl http://advancedtelematic.github.io/rvi_sota_server/dev/api.html}
 */
class Routing(namespaceDirective: Directive1[AuthedNamespaceScope],
              deviceRegistry: DeviceRegistry)
  (implicit db: Database, system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext)
 {
   import Directives._

   val route: Route = pathPrefix("api" / "v1" / "resolver") {
     handleRejections(rejectionHandler) {
       new DeviceDirectives(namespaceDirective, deviceRegistry).route ~
       new PackageDirectives(namespaceDirective, deviceRegistry).route ~
       new FilterDirectives(namespaceDirective).route ~
       new ResolveDirectives(namespaceDirective, deviceRegistry).route ~
       new ComponentDirectives(namespaceDirective).route ~
       new PackageFiltersResource(namespaceDirective, deviceRegistry).routes
     }
   }
}


class Settings(val config: Config) {
  val host = config.getString("server.host")
  val port = config.getInt("server.port")

  val deviceRegistryUri = Uri(config.getString("device_registry.baseUri"))
  val deviceRegistryApi = Uri(config.getString("device_registry.devicesUri"))
  val deviceRegistryGroupApi = Uri(config.getString("device_registry.deviceGroupsUri"))
  val deviceRegistryMyApi = Uri(config.getString("device_registry.mydeviceUri"))
}


object Boot extends App with Directives with BootMigrations {
  import org.genivi.sota.http.VersionDirectives.versionHeaders

  implicit val system = ActorSystem("ota-plus-resolver")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val db = Database.forConfig("database")

  lazy val config = system.settings.config
  lazy val settings = new Settings(config)

  lazy val version = {
    val bi = org.genivi.sota.resolver.BuildInfo
    s"${bi.name}/${bi.version}"
  }

  val namespaceDirective = NamespaceDirectives.fromConfig()

  val deviceRegistryClient = new DeviceRegistryClient(
    settings.deviceRegistryUri, settings.deviceRegistryApi,
    settings.deviceRegistryGroupApi, settings.deviceRegistryMyApi
  )

  val routes: Route =
    (TraceId.withTraceId &
      logResponseMetrics("sota-resolver", TraceId.traceMetrics) &
      versionHeaders(version)) {
      Route.seal {
        new Routing(namespaceDirective, deviceRegistryClient).route ~
        new HealthResource(db, org.genivi.sota.resolver.BuildInfo.toMap).route
      }
    }


  val messageBusListener = system.actorOf(PackageCreatedListener.props(db, system.settings.config))
  messageBusListener ! Subscribe

  val host = config.getString("server.host")
  val port = config.getInt("server.port")
  val bindingFuture = Http().bindAndHandle(routes, host, port)

  log.info(s"sota resolver started at http://$host:$port/")

  sys.addShutdownHook {
    Try(db.close())
    Try(system.terminate())
  }
}
