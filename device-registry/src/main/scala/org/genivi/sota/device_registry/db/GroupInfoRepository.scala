/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.db

import io.circe.Json
import org.genivi.sota.data.GroupInfo._
import org.genivi.sota.data.{GroupInfo, Namespace, Uuid}
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.device_registry.common.{Errors, SlickJsonHelper}
import org.genivi.sota.refined.SlickRefined._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

object GroupInfoRepository extends SlickJsonHelper {

  // scalastyle:off
  class GroupInfoTable(tag: Tag) extends Table[GroupInfo] (tag, "DeviceGroup") {
    def id             = column[Uuid]("id", O.PrimaryKey)
    def groupName      = column[Name]("group_name")
    def namespace      = column[Namespace]("namespace")
    def groupInfo      = column[Json]("group_info")
    def discardedAttrs = column[Json]("discarded_attrs")

    def * = (id, groupName, namespace, groupInfo, discardedAttrs).shaped <>
      ((GroupInfo.apply _).tupled, GroupInfo.unapply)
  }
  // scalastyle:on

  val groupInfos = TableQuery[GroupInfoTable]

  def list(namespace: Namespace)(implicit ec: ExecutionContext): DBIO[Seq[GroupInfo]] =
    groupInfos
      .filter(g => g.namespace === namespace)
      .result

  protected def findByName(groupName: Name, namespace: Namespace) =
    groupInfos.filter(r => r.groupName === groupName && r.namespace === namespace)

  protected def findById(id: Uuid)(implicit ec: ExecutionContext): DBIO[GroupInfo] =
    groupInfos
      .filter(r => r.id === id)
      .result
      .failIfNotSingle(Errors.MissingGroupInfo)

  def getGroupInfoById(groupId: Uuid)(implicit ec: ExecutionContext): DBIO[GroupInfoType] =
    findById(groupId)
      .map(_.groupInfo)

  def getIdFromName(groupName: Name, namespace: Namespace)(implicit ec: ExecutionContext): DBIO[Uuid] =
    findByName(groupName, namespace)
      .map(_.id)
      .result
      .failIfNotSingle(Errors.MissingGroupInfo)

  def create(id: Uuid,
             groupName: Name,
             namespace: Namespace,
             groupInfo: GroupInfoType,
             discardedAttrs: GroupInfoType)
            (implicit ec: ExecutionContext): DBIO[Uuid] =
      (groupInfos += GroupInfo(id, groupName, namespace, groupInfo, discardedAttrs))
        .handleIntegrityErrors(Errors.ConflictingGroupInfo)
        .map(_ => id)

  def updateGroupInfo(id: Uuid, groupInfo: GroupInfoType)
                     (implicit ec: ExecutionContext)
      :DBIO[Unit] =
    groupInfos
      .filter(_.id === id)
      .map(_.groupInfo)
      .update(groupInfo)
      .handleSingleUpdateError(Errors.MissingGroupInfo)

  def renameGroup(id: Uuid, newGroupName: Name)(implicit ec: ExecutionContext): DBIO[Unit] =
    groupInfos
      .filter(r => r.id === id)
      .map(_.groupName)
      .update(newGroupName)
      .handleSingleUpdateError(Errors.MissingGroupInfo)

  def groupInfoNamespace(groupId: Uuid)(implicit ec: ExecutionContext): DBIO[Namespace] =
    groupInfos
      .filter(_.id === groupId)
      .map(_.namespace)
      .result
      .failIfNotSingle(Errors.MissingGroupInfo)

  def exists(id: Uuid, namespace: Namespace)(implicit ec: ExecutionContext): DBIO[GroupInfo] =
    groupInfos
      .filter(d => d.namespace === namespace && d.id === id)
      .result
      .headOption
      .flatMap(_.fold[DBIO[GroupInfo]](DBIO.failed(Errors.MissingGroupInfo))(DBIO.successful))

  def discardedAttrs(groupId: Uuid)(implicit ec: ExecutionContext): DBIO[GroupInfoType] =
    findById(groupId)
      .map(_.discardedAttrs)

}
