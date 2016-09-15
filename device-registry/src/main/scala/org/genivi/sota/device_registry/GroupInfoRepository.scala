/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry


import eu.timepit.refined.api.{Refined, Validate}
import io.circe.Json
import org.genivi.sota.data.{GroupInfo, Namespace}
import org.genivi.sota.device_registry.common.{Errors, SlickJsonHelper}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.refined.SlickRefined._

import scala.concurrent.ExecutionContext

object GroupInfoRepository extends SlickJsonHelper {
  import GroupInfo._

  // scalastyle:off
  class GroupInfoTable(tag: Tag) extends Table[GroupInfo] (tag, "DeviceGroup") {
    def groupName = column[Name]("group_name", O.PrimaryKey)
    def namespace = column[Namespace]("namespace", O.PrimaryKey)
    def groupInfo = column[Json]("group_info")

    def * = (groupName, namespace, groupInfo).shaped <>
      ((GroupInfo.apply _).tupled, GroupInfo.unapply)
  }
  // scalastyle:on

  val groupInfos = TableQuery[GroupInfoTable]


  def list(namespace: Namespace)(implicit ec: ExecutionContext): DBIO[Seq[GroupInfo]] =
    groupInfos.filter(g => g.namespace === namespace).result

  protected def getGroup(groupName: Name, namespace: Namespace) =
    groupInfos.filter(r => r.groupName === groupName && r.namespace === namespace)

  def findByName(groupName: Name, namespace: Namespace)(implicit ec: ExecutionContext)
      : DBIO[GroupInfoType] = {
    import org.genivi.sota.db.Operators._
    getGroup(groupName, namespace)
      .result
      .failIfNotSingle(Errors.MissingGroupInfo)
      .map(_.groupInfo)
  }

  def create(groupName: Name, namespace: Namespace, data: GroupInfoType)(implicit ec: ExecutionContext)
      : DBIO[Int] =
    (groupInfos += GroupInfo(groupName, namespace, data))
      .handleIntegrityErrors(Errors.ConflictingGroupInfo)

  def update(groupName: Name, namespace: Namespace, data: GroupInfoType)(implicit ec: ExecutionContext)
      : DBIO[Int] =
    groupInfos.insertOrUpdate(GroupInfo(groupName, namespace, data))

  def delete(groupName: Name, namespace: Namespace)(implicit ec: ExecutionContext): DBIO[Int] =
    getGroup(groupName, namespace).delete
}
