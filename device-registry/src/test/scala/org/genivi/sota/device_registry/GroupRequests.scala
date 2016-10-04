package org.genivi.sota.device_registry

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.client.RequestBuilding.{Get, Post, Put}
import akka.http.scaladsl.model.Uri.Query
import io.circe.Json
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.data.GroupInfo.Name
import org.genivi.sota.data.Uuid

import scala.concurrent.ExecutionContext

trait GroupRequests {

  val groupsApi = "device_groups"

  def listGroups(): HttpRequest = Get(Resource.uri(groupsApi))

  def fetchGroupInfo(id: Uuid): HttpRequest = Get(Resource.uri(groupsApi, id.underlying.get))

  def createGroupInfo(id: Uuid, groupName: Name, json: Json)
                     (implicit ec: ExecutionContext): HttpRequest =
    Post(Resource.uri(groupsApi, id.underlying.get).withQuery(Query("groupName" -> groupName.get)), json)

  def updateGroupInfo(id: Uuid, groupName: Name, json: Json)
                     (implicit ec: ExecutionContext): HttpRequest =
    Put(Resource.uri(groupsApi, id.underlying.get).withQuery(Query("groupName" -> groupName.get)), json)

  def renameGroup(id: Uuid, newGroupName: Name)(implicit ec: ExecutionContext): HttpRequest = {
    Put(Resource.uri(groupsApi, id.underlying.get, "rename").withQuery(Query("groupName" -> newGroupName.get)))
  }
}
