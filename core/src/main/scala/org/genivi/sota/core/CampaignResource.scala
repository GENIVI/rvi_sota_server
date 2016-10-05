/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1, Route}
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.util.FastFuture
import io.circe.generic.auto._
import org.genivi.sota.core.data.{Campaign, UpdateRequest}
import org.genivi.sota.core.db.Campaigns
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.{Device, Namespace, PackageId, Uuid}
import org.genivi.sota.http.ErrorHandler
import org.genivi.sota.http.UuidDirectives._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.rest.Validation.refined
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import slick.driver.MySQLDriver.api.Database

class CampaignResource(namespaceExtractor: Directive1[Namespace],
                       deviceRegistry: DeviceRegistry, updateService: UpdateService)
                      (implicit db: Database, system: ActorSystem) {
  import system.dispatcher

  import StatusCodes.{Success => _, _}
  import Campaign._

  def createCampaign(ns: Namespace, name: CreateCampaign): Route = {
    complete(Created -> db.run(Campaigns.create(ns, name.name)))
  }

  def deleteCampaign(id: Campaign.Id): Route = {
    complete(db.run(Campaigns.delete(id)))
  }

  def fetchCampaign(id: Campaign.Id): Route = {
    complete(db.run(Campaigns.fetch(id)))
  }

  def launch(id: Campaign.Id): Route = {
    complete(CampaignLauncher.launch(deviceRegistry, updateService, id))
  }

  def listCampaigns(ns: Namespace): Route = {
    complete(db.run(Campaigns.list(ns)))
  }

  def setAsDraft(id: Campaign.Id): Route = {
    complete(db.run(Campaigns.setAsDraft(id)))
  }

  def setCampaignGroups(id: Campaign.Id, groups: SetCampaignGroups): Route = {
    complete(db.run(Campaigns.setGroups(id, groups.groups)))
  }

  def setCampaignName(id: Campaign.Id, name: CreateCampaign): Route = {
    complete(db.run(Campaigns.setName(id, name.name)))
  }

  def setCampaignPackage(id: Campaign.Id, pkg: PackageId): Route = {
    complete(db.run(Campaigns.setPackage(id, pkg)))
  }

  def campaignAllowed(id: Campaign.Id): Future[Namespace] = {
    db.run(Campaigns.fetchMeta(id).map(_.namespace))
  }

  val extractId: Directive1[Campaign.Id] =
    allowExtractor(namespaceExtractor, extractUuid.map(Campaign.Id(_)), campaignAllowed)

  val route = ErrorHandler.handleErrors {
    extractExecutionContext { implicit ec =>
      pathPrefix("campaigns") {
        (pathEnd & namespaceExtractor) { ns =>
          get {
            listCampaigns(ns)
          } ~
          (post & entity(as[CreateCampaign])) { name =>
            createCampaign(ns, name)
          }
        } ~
        extractId { id =>
          pathEnd {
            get {
              fetchCampaign(id)
            } ~
            delete {
              deleteCampaign(id)
            }
          } ~
          (path("draft") & post) {
            setAsDraft(id)
          } ~
          (path("launch") & post) {
            launch(id)
          } ~
          (path("groups") & put & entity(as[SetCampaignGroups])) { groups =>
            setCampaignGroups(id, groups)
          } ~
          (path("name") & put & entity(as[CreateCampaign])) { campName =>
            setCampaignName(id, campName)
          } ~
          (path("package") & put & entity(as[PackageId])) { pkgId =>
            setCampaignPackage(id, pkgId)
          }
        }
      }
    }
  }

}
