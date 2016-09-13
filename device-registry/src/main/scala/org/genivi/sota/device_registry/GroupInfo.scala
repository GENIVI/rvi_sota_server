/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry


import eu.timepit.refined.api.{Refined, Validate}
import io.circe.Json
import org.genivi.sota.data.Namespace
import org.genivi.sota.device_registry.common.{Errors, SlickJsonHelper}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.SlickExtensions._

import scala.concurrent.ExecutionContext

object GroupInfo extends SlickJsonHelper {

  type GroupInfoType = Json
  case class GroupInfo(groupName: Name, namespace: Namespace, groupInfo: GroupInfoType)

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

  case class ValidName()

  type Name = Refined[String, ValidName]

  implicit val validGroupName: Validate.Plain[String, ValidName] =
    Validate.fromPredicate(
      name => name.length > 1
        && name.length <= 100
        && name.forall(c => c.isLetter || c.isDigit),
      name => s"($name should be between two and a hundred alphanumeric characters long.)",
      ValidName()
    )

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
