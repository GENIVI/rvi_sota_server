/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry


import io.circe.Json
import org.genivi.sota.data.Namespace
import org.genivi.sota.device_registry.common.{Errors, SlickJsonHelper}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.SlickExtensions._

import scala.concurrent.ExecutionContext

object GroupInfo extends SlickJsonHelper {

  type GroupInfoType = Json
  case class GroupInfo(groupName: String, namespace: Namespace, groupInfo: GroupInfoType)

  // scalastyle:off
  class GroupInfoTable(tag: Tag) extends Table[GroupInfo] (tag, "DeviceGroup") {
    def groupName = column[String]("group_name", O.PrimaryKey)
    def namespace = column[Namespace]("namespace", O.PrimaryKey)
    def groupInfo = column[Json]("group_info")

    def * = (groupName, namespace, groupInfo).shaped <>
      ((GroupInfo.apply _).tupled, GroupInfo.unapply)
  }
  // scalastyle:on

  val groupInfos = TableQuery[GroupInfoTable]

  protected def getGroup(groupName: String, namespace: Namespace) =
    groupInfos.filter(r => r.groupName === groupName && r.namespace === namespace)

  def findByName(groupName: String, namespace: Namespace)(implicit ec: ExecutionContext)
      : DBIO[GroupInfoType] = {
    import org.genivi.sota.db.Operators._
    getGroup(groupName, namespace)
      .result
      .headOption
      .failIfNone(Errors.MissingGroupInfo)
      .map(_.groupInfo)
  }

  def create(groupName: String, namespace: Namespace, data: GroupInfoType)(implicit ec: ExecutionContext)
      : DBIO[Int] =
    (groupInfos += GroupInfo(groupName, namespace, data))
      .handleIntegrityErrors(Errors.ConflictingGroupInfo)

  def update(groupName: String, namespace: Namespace, data: GroupInfoType)(implicit ec: ExecutionContext)
      : DBIO[Int] =
    groupInfos.insertOrUpdate(GroupInfo(groupName, namespace, data))

  def delete(groupName: String, namespace: Namespace)(implicit ec: ExecutionContext): DBIO[Int] =
    getGroup(groupName, namespace).delete
}
