/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry.db

import cats.data.State
import cats.std.list._
import cats.syntax.traverse._
import io.circe.Json
import io.circe.jawn._
import org.genivi.sota.data.{Uuid, Namespace}
import org.genivi.sota.device_registry.common.{Errors, SlickJsonHelper}
import org.genivi.sota.db.SlickExtensions._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object SystemInfoRepository extends SlickJsonHelper {

  type SystemInfoType = Json
  case class SystemInfo(uuid: Uuid, systemInfo: SystemInfoType)

  // scalastyle:off
  class SystemInfoTable(tag: Tag) extends Table[SystemInfo] (tag, "DeviceSystem") {
    def uuid = column[Uuid]("uuid")
    def systemInfo = column[Json]("system_info")

    implicit val jsonColumnType = MappedColumnType.base[Json, String](
      {json => json.noSpaces},
      {str  => parse(str).fold(_ => Json.Null, x => x)}
    )

    def * = (uuid, systemInfo).shaped <>
      ((SystemInfo.apply _).tupled, SystemInfo.unapply)

    def pk = primaryKey("uuid", uuid)
  }
  // scalastyle:on

  val systemInfos = TableQuery[SystemInfoTable]

  def removeIdNrs(json: Json): Json = json.arrayOrObject(
    json,
    x => Json.fromValues(x.map(removeIdNrs)),
    x => Json.fromFields(x.toList.collect{case (i,v) if i != "id-nr" => (i, removeIdNrs(v))})
  )
  private def addUniqueIdsSIM(j: Json): State[Int, Json] = j.arrayOrObject(
    State.pure(j),
    _.traverseU(addUniqueIdsSIM).map(Json.fromValues),
    _.toList.traverseU {
      case (other, value) => addUniqueIdsSIM(value).map(other -> _)
    }.flatMap(xs => State{ nr => (nr+1, Json.fromFields(("id-nr" -> Json.fromString(s"$nr")) :: xs))})
  )

  def addUniqueIdNrs(j: Json): Json = addUniqueIdsSIM(j).run(0).value._2

  def list(ns: Namespace)(implicit ec: ExecutionContext): DBIO[Seq[SystemInfo]] =
    DeviceRepository.devices
      .filter(_.namespace === ns)
      .join(systemInfos)
      .on(_.uuid === _.uuid)
      .map(_._2)
      .result

  def exists(uuid: Uuid)
            (implicit ec: ExecutionContext): DBIO[SystemInfo] =
    systemInfos
      .filter(_.uuid === uuid)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[SystemInfo]](DBIO.failed(Errors.MissingSystemInfo))(DBIO.successful))

  def findByUuid(uuid: Uuid)(implicit ec: ExecutionContext): DBIO[SystemInfoType] = {
    import org.genivi.sota.db.Operators._
    val dbIO = for {
      _ <- DeviceRepository.findByUuid(uuid)
      p <- systemInfos
        .filter(_.uuid === uuid)
        .result
        .failIfNotSingle(Errors.MissingSystemInfo)
    } yield p.systemInfo

    dbIO.transactionally
  }

  def create(uuid: Uuid, data: SystemInfoType)(implicit ec: ExecutionContext): DBIO[Unit] = for {
    _ <- DeviceRepository.findByUuid(uuid) // check that the device exists
    _ <- exists(uuid).asTry.flatMap {
      case Success(_) => DBIO.failed(Errors.ConflictingSystemInfo)
      case Failure(_) => DBIO.successful(())
    }
    newData = addUniqueIdNrs(data)
    _ <- systemInfos += SystemInfo(uuid,newData)
  } yield ()

  def update(uuid: Uuid, data: SystemInfoType)(implicit ec: ExecutionContext): DBIO[Unit] = for {
    _ <- DeviceRepository.findByUuid(uuid) // check that the device exists
    newData = addUniqueIdNrs(data)
    _ <- systemInfos.insertOrUpdate(SystemInfo(uuid, newData))
  } yield ()

  def delete(uuid: Uuid)(implicit ec: ExecutionContext): DBIO[Int] =
    systemInfos.filter(_.uuid === uuid).delete

}
