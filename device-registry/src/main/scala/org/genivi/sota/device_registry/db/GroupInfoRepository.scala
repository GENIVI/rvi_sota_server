/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.db

import org.genivi.sota.data.Group._
import org.genivi.sota.data.{Group, Namespace, Uuid}
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.device_registry.common.{Errors, SlickJsonHelper}
import org.genivi.sota.refined.SlickRefined._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

object GroupInfoRepository extends SlickJsonHelper {

  // scalastyle:off
  class GroupInfoTable(tag: Tag) extends Table[Group] (tag, "DeviceGroup") {
    def id             = column[Uuid]("id", O.PrimaryKey)
    def groupName      = column[Name]("group_name")
    def namespace      = column[Namespace]("namespace")

    def * = (id, groupName, namespace).shaped <>
      ((Group.apply _).tupled, Group.unapply)
  }
  // scalastyle:on

  val groupInfos = TableQuery[GroupInfoTable]

  def list(namespace: Namespace)(implicit ec: ExecutionContext): DBIO[Seq[Group]] =
    groupInfos
      .filter(g => g.namespace === namespace)
      .result

  protected def findByName(groupName: Name, namespace: Namespace) =
    groupInfos.filter(r => r.groupName === groupName && r.namespace === namespace)

  protected def findById(id: Uuid)(implicit ec: ExecutionContext): DBIO[Group] =
    groupInfos
      .filter(r => r.id === id)
      .result
      .failIfNotSingle(Errors.MissingGroup)

  def getIdFromName(groupName: Name, namespace: Namespace)(implicit ec: ExecutionContext): DBIO[Uuid] =
    findByName(groupName, namespace)
      .map(_.id)
      .result
      .failIfNotSingle(Errors.MissingGroup)

  def create(id: Uuid,
             groupName: Name,
             namespace: Namespace)
            (implicit ec: ExecutionContext): DBIO[Uuid] =
      (groupInfos += Group(id, groupName, namespace))
        .handleIntegrityErrors(Errors.ConflictingGroup)
        .map(_ => id)

  def renameGroup(id: Uuid, newGroupName: Name)(implicit ec: ExecutionContext): DBIO[Unit] =
    groupInfos
      .filter(r => r.id === id)
      .map(_.groupName)
      .update(newGroupName)
      .handleSingleUpdateError(Errors.MissingGroup)

  def groupInfoNamespace(groupId: Uuid)(implicit ec: ExecutionContext): DBIO[Namespace] =
    groupInfos
      .filter(_.id === groupId)
      .map(_.namespace)
      .result
      .failIfNotSingle(Errors.MissingGroup)

  def exists(id: Uuid, namespace: Namespace)(implicit ec: ExecutionContext): DBIO[Group] =
    groupInfos
      .filter(d => d.namespace === namespace && d.id === id)
      .result
      .headOption
      .failIfNone(Errors.MissingGroup)

}
