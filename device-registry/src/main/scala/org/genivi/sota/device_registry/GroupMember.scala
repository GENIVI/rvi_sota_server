/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import org.genivi.sota.data.{Namespace, Uuid}
import slick.lifted.Tag
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.SlickExtensions._

import scala.concurrent.ExecutionContext

object GroupMember {

  final case class GroupMember(groupId: Uuid, namespace: Namespace, deviceUuid: Uuid)

  // scalastyle:off
  class GroupMembersTable(tag: Tag) extends Table[GroupMember] (tag, "GroupMembers") {
    def groupId     = column[Uuid]("group_id")
    def namespace   = column[Namespace]("namespace")
    def deviceUuid  = column[Uuid]("device_uuid")
    def deviceFk    = foreignKey("fk_group_members_uuid", deviceUuid, DeviceRepository.devices)(_.uuid)
    def groupFk     = foreignKey("fk_group_members_group_id", groupId, GroupInfoRepository.groupInfos)(_.id)

    def pk = primaryKey("pk_group_members", (groupId, deviceUuid))

    def * = (groupId, namespace, deviceUuid).shaped <>
      ((GroupMember.apply _).tupled, GroupMember.unapply)
  }
  // scalastyle:on

  val groupMembers = TableQuery[GroupMembersTable]

  //This method should only be called from methods which use `transactionally`, so it purposely doesn't call this itself
  def createGroup(groupId: Uuid, namespace: Namespace, deviceId: Uuid)(implicit ec: ExecutionContext)
      : DBIO[Int] =
    groupMembers += GroupMember(groupId, namespace, deviceId)

}
