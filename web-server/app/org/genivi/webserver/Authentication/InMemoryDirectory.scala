/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.webserver.Authentication

import com.unboundid.ldap.listener.{ InMemoryDirectoryServer, InMemoryDirectoryServerConfig, InMemoryListenerConfig}
import com.unboundid.ldif.LDIFReader
import javax.inject.{ Inject, Singleton }
import play.api.{ Logger, Mode }
import play.api.inject.Binding
import play.api.{ Configuration, Environment }
import play.api.inject.{ ApplicationLifecycle, Module }
import scala.concurrent.Future

class InMemoryDirectoryModule extends Module {

  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    if(environment.mode == Mode.Dev || environment.mode == Mode.Test) {
      val config = for {
        c      <- configuration.getConfig("ldap.inmem")
        baseDN <- c.getString("baseDN")
        port   <- c.getInt("port")
      } yield new InMemoryDirectory.Configuration(baseDN, port, c.getString("ldifResource"))
      config.map(x => Seq(
                   bind[InMemoryDirectory.Configuration].to(x).eagerly(),
                   bind[InMemoryDirectory].toSelf.eagerly())
      ).getOrElse(Seq.empty)
    } else {
      Seq.empty
    }
  }

}

object InMemoryDirectory {

  final case class Configuration(baseDN: String, port: Int, ldif: Option[String])

}

class InMemoryDirectory @Inject() (config: InMemoryDirectory.Configuration, lifecycle: ApplicationLifecycle) {

  val serverConfig: InMemoryDirectoryServerConfig = {
    val cfg = new InMemoryDirectoryServerConfig(config.baseDN)
    cfg.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", config.port))
    cfg
  }

  val server = new InMemoryDirectoryServer(serverConfig)
  config.ldif.foreach { resourceName =>
    val resource = this.getClass.getResourceAsStream(resourceName)
    server.importFromLDIF(true, new LDIFReader(resource))
  }
  server.startListening()
  lifecycle.addStopHook { () => Future.successful(close) }
  Logger.info(s"In-memory directory server started on port ${config.port}")

  def serverPort(): Int = server.getListenPort

  def close(): Unit = server.shutDown(true)
}
