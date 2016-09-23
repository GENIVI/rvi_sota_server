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
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import org.genivi.sota.core.data.Campaign
import org.genivi.sota.core.db.Campaigns
import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.http.ErrorHandler
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.rest.Validation.{refined}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import slick.driver.MySQLDriver.api.Database

class CampaignResource(db: Database, namespaceExtractor: Directive1[Namespace])
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import StatusCodes.{Success => _, _}
  import Campaign._

  def createCampaign(ns: Namespace, name: CreateCampaign)
                    (implicit ec: ExecutionContext): Route = {
    complete(Created -> db.run(Campaigns.create(ns, name.name)))
  }

  def deleteCampaign(id: Campaign.Id)
                    (implicit ec: ExecutionContext): Route = {
    complete(db.run(Campaigns.delete(id)))
  }

  def fetchCampaign(id: Campaign.Id)
                   (implicit ec: ExecutionContext): Route = {
    complete(db.run(Campaigns.fetch(id)))
  }

  def listCampaigns(ns: Namespace): Route = {
    complete(db.run(Campaigns.list(ns)))
  }

  def setAsDraft(id: Campaign.Id)
                (implicit ec: ExecutionContext): Route = {
    complete(db.run(Campaigns.setAsDraft(id)))
  }

  def setAsLaunch(id: Campaign.Id)
                 (implicit ec: ExecutionContext): Route = {
    complete(db.run(Campaigns.setAsLaunch(id)))
  }

  def setCampaignGroups(id: Campaign.Id, groups: CampaignGroups)
                       (implicit ec: ExecutionContext): Route = {
    complete(db.run(Campaigns.setGroups(id, groups.groups)))
  }

  def setCampaignName(id: Campaign.Id, name: CreateCampaign)
                       (implicit ec: ExecutionContext): Route = {
    complete(db.run(Campaigns.setName(id, name.name)))
  }

  def setCampaignPackage(id: Campaign.Id, pkg: PackageId)
                        (implicit ec: ExecutionContext): Route = {
    complete(db.run(Campaigns.setPackage(id, pkg)))
  }

  def extractId(ns: Namespace): Directive1[Campaign.Id] =
    refined[Campaign.ValidId](Slash ~ Segment).map(Campaign.Id).flatMap { id =>
      extractExecutionContext.flatMap { implicit ec =>
        val f = db.run(Campaigns.exists(ns, id))

        onSuccess(f).flatMap { _ => provide(id) }
      }
    }

  val route = ErrorHandler.handleErrors {
    extractExecutionContext { implicit ec =>
      (pathPrefix("campaigns") & namespaceExtractor) { ns =>
        pathEnd {
          get {
            listCampaigns(ns)
          } ~
          (post & entity(as[CreateCampaign])) { name =>
            createCampaign(ns, name)
          }
        } ~
        extractId(ns) { id =>
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
            setAsLaunch(id)
          } ~
          (path("groups") & put & entity(as[CampaignGroups])) { groups =>
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
