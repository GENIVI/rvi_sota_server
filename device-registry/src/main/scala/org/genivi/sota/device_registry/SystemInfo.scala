/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import cats.data.State
import cats.std.list._
import cats.syntax.traverse._
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

  private def addUniqueIdsSIM(j: Json): State[Int, Json] = j.arrayOrObject(
    State.pure(j),
    _.traverseU(addUniqueIdsSIM).map(Json.fromValues),
    _.toList.traverseU {
      case ("id", value) => State { (nr: Int) =>
        (nr+1, Seq("id" -> value, "id-nr" -> Json.fromString(s"$nr")))
      }
      case (other, value) => addUniqueIdsSIM(value).map (x => Seq[(String, Json)](other -> x))
    }.map(_.flatten).map(Json.fromFields)
  )

  private def addUniqueIdsSI(j: Json): Json = addUniqueIdsSIM(j).run(0).value._2

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
      .failIfNotSingle(Errors.MissingSystemInfo)
      .map(p => p.systemInfo)
  }

  def create(id: Id, data: SystemInfoType)(implicit ec: ExecutionContext): DBIO[Unit] = for {
    _ <- DeviceRepository.findById(id) // check that the device exists
    _ <- exists(id).asTry.flatMap {
      case Success(_) => DBIO.failed(Errors.ConflictingSystemInfo)
      case Failure(_) => DBIO.successful(())
    }
    newData = addUniqueIdsSI(data)
    _ <- systemInfos += SystemInfo(id,newData)
  } yield ()

  def update(id: Id, data: SystemInfoType)(implicit ec: ExecutionContext): DBIO[Unit] = for {
    _ <- DeviceRepository.findById(id) // check that the device exists
    newData = addUniqueIdsSI(data)
    _ <- systemInfos.insertOrUpdate(SystemInfo(id,newData))
  } yield ()

  def delete(id: Id)(implicit ec: ExecutionContext): DBIO[Int] =
    systemInfos.filter(_.id === id).delete

}
