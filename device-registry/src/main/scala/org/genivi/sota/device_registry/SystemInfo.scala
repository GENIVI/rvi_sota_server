/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import io.circe.Json
import io.circe.jawn._
import org.genivi.sota.data.Device.Id
import org.genivi.sota.data.Device
import org.genivi.sota.device_registry.common.{Errors, SlickJsonHelper}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.SlickExtensions._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object SystemInfo extends SlickJsonHelper {

  type SystemInfoType = Json
  case class SystemInfo(id: Device.Id, systemInfo: SystemInfoType)

  // scalastyle:off
  class SystemInfoTable(tag: Tag) extends Table[SystemInfo] (tag, "DeviceSystem") {
    def id = column[Id]("uuid")
    def systemInfo = column[Json]("system_info")

    implicit val jsonColumnType = MappedColumnType.base[Json, String](
      {json => json.noSpaces},
      {str  => parse(str).fold(_ => Json.Null, x => x)}
    )

    def * = (id, systemInfo).shaped <>
      ((SystemInfo.apply _).tupled, SystemInfo.unapply)

    def pk = primaryKey("id", id)
  }
  // scalastyle:on

  val systemInfos = TableQuery[SystemInfoTable]

  def exists(id: Id)
            (implicit ec: ExecutionContext): DBIO[SystemInfo] =
    systemInfos
      .filter(_.id === id)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[SystemInfo]](DBIO.failed(Errors.MissingSystemInfo))(DBIO.successful))

  def findById(id: Id)(implicit ec: ExecutionContext): DBIO[SystemInfoType] = {
    import org.genivi.sota.db.Operators._
    systemInfos
      .filter(_.id === id)
      .result
      .headOption
      .failIfNone(Errors.MissingSystemInfo)
      .map(p => p.systemInfo)
  }

  def create(id: Id, data: SystemInfoType)(implicit ec: ExecutionContext): DBIO[Unit] = for {
    _ <- DeviceRepository.findById(id) // check that the device exists
    _ <- exists(id).asTry.flatMap {
      case Success(_) => DBIO.failed(Errors.ConflictingSystemInfo)
      case Failure(_) => DBIO.successful(())
    }
    _ <- systemInfos += SystemInfo(id,data)
  } yield ()

  def update(id: Id, data: SystemInfoType)(implicit ec: ExecutionContext): DBIO[Unit] = for {
    _ <- DeviceRepository.findById(id) // check that the device exists
    _ <- systemInfos.insertOrUpdate(SystemInfo(id,data))
  } yield ()

  def delete(id: Id)(implicit ec: ExecutionContext): DBIO[Int] =
    systemInfos.filter(_.id === id).delete

}
