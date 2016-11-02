/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import io.circe.Json
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.device_registry.common.CreateGroupRequest
import org.genivi.sota.device_registry.db._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import slick.driver.MySQLDriver.api._

object UpdateMemberships {
  import JsonMatcher._
  import SystemInfoRepository._

  private val logger = LoggerFactory.getLogger(this.getClass)

  def forGroup(ns: Namespace,
               groupId: Uuid,
               matching: Json,
               discarded: Json)
              (implicit ec: ExecutionContext, db: Database): Future[Unit] = {
    val dbIO = list(ns).flatMap { allDevices =>
      DBIO.seq(allDevices.collect {
        case info
          if compare(disregard(removeIdNrs(info.systemInfo), discarded), matching)
            .equals((matching, Json.Null)) =>
          GroupMemberRepository.addOrUpdateGroupMember(groupId, info.uuid)
      } :_*)
    }

    db.run(dbIO.transactionally).andThen {
      case Failure(ex) =>
        logger.error(s"Got error whilst updating group id $groupId", ex)
    }

  }

  def forDevice(deviceUuid: Uuid, data: Json)
               (implicit ec: ExecutionContext, db: Database): Future[Unit] = {
    val dbIO = for {
      _          <- GroupMemberRepository.removeDeviceFromAllGroups(deviceUuid)
      ns         <- DeviceRepository.deviceNamespace(deviceUuid)
      groupInfos <- GroupInfoRepository.list(ns)
      actions    =  groupInfos.collect {
        case grp if JsonMatcher.compare(data, grp.groupInfo)._1.equals(grp.groupInfo) =>
          GroupMemberRepository.addGroupMember(grp.id, deviceUuid)
      }
      _          <- DBIO.seq(actions :_*)
    } yield ()

    db.run(dbIO.transactionally).andThen {
      case Failure(e) =>
        logger.error(s"Got error whilst updating group id $deviceUuid: ${e.toString}")
    }
  }


  def createGroupFromDevices(request: CreateGroupRequest, namespace: Namespace)
                            (implicit ec: ExecutionContext, db: Database): Future[Uuid] = {
    val commonJsonDbIo = for {
      info1 <- findByUuid(request.device1).map(removeIdNrs)
      info2 <- findByUuid(request.device2).map(removeIdNrs)
    } yield compare(info1, info2)

    db.run(commonJsonDbIo).flatMap { json =>
      json match {
        case (Json.Null, _) =>
          Future.failed(new Throwable("Devices have no common attributes to form a group"))
        case (matching, discarded) =>
          val groupId = Uuid.generate()
          db.run(GroupInfoRepository.create(groupId, request.groupName, namespace, matching, discarded)).andThen {
            case Success(_) => forGroup(namespace, groupId, matching, discarded)
          }
      }
    }
  }
}
