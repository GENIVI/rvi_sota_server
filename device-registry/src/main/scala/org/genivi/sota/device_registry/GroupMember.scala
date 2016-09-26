/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import org.genivi.sota.data.GroupInfo.Name
import org.genivi.sota.data.{Namespace, Uuid}
import slick.lifted.Tag
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.device_registry.common.CreateGroupRequest
import org.genivi.sota.refined.SlickRefined._

import scala.concurrent.ExecutionContext

object GroupMember {

  final case class GroupMember(groupName: Name, namespace: Namespace, deviceUuid: Uuid)

  // scalastyle:off
  class GroupMembersTable(tag: Tag) extends Table[GroupMember] (tag, "GroupMembers") {
    def groupName   = column[Name]("group_name")
    def namespace   = column[Namespace]("namespace")
    def deviceUuid  = column[Uuid]("device_uuid")
    def device      = foreignKey("fk_group_members_uuid", deviceUuid, DeviceRepository.devices)(_.uuid)

    def pk = primaryKey("pk_group_members", (groupName, namespace, deviceUuid))

    def * = (groupName, namespace, deviceUuid).shaped <>
      ((GroupMember.apply _).tupled, GroupMember.unapply)
  }
  // scalastyle:on

  val groupMembers = TableQuery[GroupMembersTable]

  //This method should only be called from methods which use `transactionally`, so it purposely doesn't call this itself
  def createGroup(groupInfo: CreateGroupRequest, namespace: Namespace)(implicit ec: ExecutionContext)
      : DBIO[Option[Int]] =
    groupMembers ++= Seq(GroupMember(groupInfo.groupName, namespace, groupInfo.device1),
                         GroupMember(groupInfo.groupName, namespace, groupInfo.device2))

}
