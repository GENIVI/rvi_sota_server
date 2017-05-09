/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry.db

import org.genivi.sota.data.Uuid
import org.genivi.sota.db.SlickAnyVal._
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.device_registry.common.Errors

import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

object PublicCredentialsRepository {
  case class DevicePublicCredentials(device: Uuid, credentials: Array[Byte])

  class PublicCredentialsTable(tag: Tag) extends Table[DevicePublicCredentials] (tag, "DevicePublicCredentials") {
    def device = column[Uuid]("device_uuid")
    def publicCredentials = column[Array[Byte]]("public_credentials")

    def * = (device, publicCredentials).shaped <>
      ((DevicePublicCredentials.apply _).tupled, DevicePublicCredentials.unapply)

    def pk = primaryKey("device_uuid", device)
  }

  val allPublicCredentials = TableQuery[PublicCredentialsTable]

  def findByUuid(uuid: Uuid)(implicit ec: ExecutionContext): DBIO[Array[Byte]] = {
    allPublicCredentials.filter(_.device === uuid)
      .map(_.publicCredentials)
      .result
      .failIfNotSingle(Errors.MissingDevicePublicCredentials)
  }

  def update(uuid: Uuid, creds: Array[Byte])(implicit ec: ExecutionContext): DBIO[Unit] = {
    (allPublicCredentials.insertOrUpdate(DevicePublicCredentials(uuid, creds)))
      .map(_ => ())
  }

}
