/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry.db

import org.genivi.sota.data.Uuid
import org.genivi.sota.db.SlickExtensions
import SlickExtensions._
import org.genivi.sota.device_registry.common.Errors
import slick.driver.MySQLDriver.api._
import slick.lifted.Tag

import scala.concurrent.ExecutionContext

object GroupMemberRepository extends SlickExtensions {

  import org.genivi.sota.db.SlickAnyVal._

  final case class GroupMember(groupId: Uuid, deviceUuid: Uuid)

  // scalastyle:off
  class GroupMembersTable(tag: Tag) extends Table[GroupMember] (tag, "GroupMembers") {
    def groupId     = column[Uuid]("group_id")
    def deviceUuid  = column[Uuid]("device_uuid")
    def deviceFk    = foreignKey("fk_group_members_uuid", deviceUuid, DeviceRepository.devices)(_.uuid)
    def groupFk     = foreignKey("fk_group_members_group_id", groupId, GroupInfoRepository.groupInfos)(_.id)

    def pk = primaryKey("pk_group_members", (groupId, deviceUuid))

    def * = (groupId, deviceUuid).shaped <>
      ((GroupMember.apply _).tupled, GroupMember.unapply)
  }
  // scalastyle:on

  val groupMembers = TableQuery[GroupMembersTable]

  //this method assumes that groupId and deviceId belong to the same namespace
  def addGroupMember(groupId: Uuid, deviceId: Uuid)(implicit ec: ExecutionContext): DBIO[Int] =
    (groupMembers += GroupMember(groupId, deviceId))
      .handleIntegrityErrors(Errors.MemberAlreadyExists)

  def addOrUpdateGroupMember(groupId: Uuid, deviceId: Uuid)(implicit ec: ExecutionContext): DBIO[Int] =
    groupMembers.insertOrUpdate(GroupMember(groupId, deviceId))

  def removeGroupMember(groupId: Uuid, deviceId: Uuid)(implicit ec: ExecutionContext): DBIO[Unit] =
    groupMembers
      .filter(r => r.groupId === groupId && r.deviceUuid === deviceId)
      .delete
      .handleSingleUpdateError(Errors.MissingGroup)

  def listDevicesInGroup(groupId: Uuid, offset: Option[Long] = None, limit: Option[Long] = None)
                        (implicit ec: ExecutionContext): DBIO[Seq[Uuid]] = {
    (offset, limit) match {
      case (None, None) =>
        groupMembers
          .filter(_.groupId === groupId)
          .map(_.deviceUuid)
          .result
      case _ =>
        groupMembers
          .filter(_.groupId === groupId)
          .defaultPaginate(offset, limit)
          .map(_.deviceUuid)
          .result
    }
  }

  def countDevicesInGroup(groupId: Uuid)(implicit ec: ExecutionContext): DBIO[Int] =
    listDevicesInGroup(groupId).map(_.size)

  def listGroupsForDevice(device: Uuid)(implicit ec: ExecutionContext): DBIO[Seq[Uuid]] =
    DeviceRepository.findByUuid(device).flatMap { _ =>
      groupMembers
        .filter(_.deviceUuid === device)
        .map(_.groupId)
        .result
    }

  def removeDeviceFromAllGroups(deviceUuid: Uuid)(implicit ec: ExecutionContext): DBIO[Int] =
    groupMembers
      .filter(_.deviceUuid === deviceUuid)
      .delete
}
